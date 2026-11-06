package com.sodepa.erp.workflow.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.workflow.infrastructure.entities.*;
import com.sodepa.erp.workflow.infrastructure.repo.*;
import com.sodepa.erp.audit.application.inputs.WorkflowEventMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.workflow.presentation.requests.*;
import com.sodepa.erp.workflow.presentation.responses.WorkflowDefinitionResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowInstanceResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowMetierResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowTacheResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service applicatif principal du Moteur de Workflow Universel SODEPA.
 * <p>
 * Ce service orchestre l'ensemble du cycle de vie des circuits d'approbation transverses :
 * <ul>
 *   <li>Référentiel dynamique des opérations métiers ({@link WorkflowMetierEntity}) évitant tout code en dur</li>
 *   <li>Centrage sur l'employé ({@link EmployeEntity}) et son unité organisationnelle</li>
 *   <li>Obligation stricte de paramétrage de workflow par unité opérationnelle</li>
 *   <li>Bypass de sécurité pour la gouvernance et les superviseurs (DG, DAF, PCA, Administrateurs)</li>
 *   <li>Contrôle d'éligibilité du Maker en amont (rôles et postes autorisés)</li>
 *   <li>Génération en parallèle des tâches pour les acteurs multiples d'une étape</li>
 *   <li>Évaluation dynamique de complétion d'étape (validateurs obligatoires 'REQUIRED' vs optionnels)</li>
 *   <li>Protection étanche contre la modification croisée et respect du principe 'Need-to-Know'</li>
 *   <li>Enregistrement systématique dans la piste d'audit infalsifiable</li>
 *   <li>Publication d'événements Spring découplés lors de la clôture complète d'un circuit</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    /** Référentiel des opérations / métiers du workflow. */
    private final WorkflowMetierRepository workflowMetierRepository;

    /** Référentiel des définitions et modèles de circuits de validation. */
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    /** Référentiel des étapes configurées pour chaque circuit. */
    private final WorkflowEtapeDefinitionRepository workflowEtapeRepository;

    /** Référentiel des acteurs configurés sur les étapes. */
    private final WorkflowEtapeActeurDefinitionRepository workflowEtapeActeurRepository;

    /** Référentiel des instances concrètes de circuits en cours ou terminés. */
    private final WorkflowInstanceRepository workflowInstanceRepository;

    /** Référentiel des tâches individuelles attribuées aux valideurs. */
    private final WorkflowTacheRepository workflowTacheRepository;

    /** Référentiel de la piste d'audit et d'immuabilité des actions. */
    private final WorkflowHistoriqueAuditRepository workflowHistoriqueAuditRepository;

    /** Référentiel des utilisateurs et comptes techniques du système. */
    private final UserRepository userRepository;

    /** Référentiel des employés SODEPA (clé métier centrale). */
    private final EmployeRepository employeRepository;

    /** Moteur Jackson pour la sérialisation des métadonnées de contexte en JSON. */
    private final ObjectMapper objectMapper;

    /** Émetteur d'événements applicatifs Spring pour le découplage événementiel. */
    private final ApplicationEventPublisher eventPublisher;

    /** Référentiel des unités organisationnelles SODEPA. */
    private final UniteOrganisationnelleRepository uniteRepository;

    /** Gestionnaire d'audit analytique ClickHouse (OLAP). */
    private final ClickHouseManager clickHouseManager;

    private void auditClickHouseAsync(WorkflowEventMessageInput input) {
        if (clickHouseManager == null) return;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                clickHouseManager.saveWorkflowEvent(input);
            } catch (Exception e) {
                log.warn("Impossible d'enregistrer l'événement de workflow dans ClickHouse: {}", e.getMessage());
            }
        });
    }

    /**
     * Détermine si un employé ou utilisateur bénéficie du statut de Superviseur ou Administrateur
     * lui conférant une vue panoramique globale sans restriction territoriale ou cellulaire
     * (Directeur Général, DAF, PCA, Membres du CA, Contrôle de gestion, Audit interne, Administrateurs).
     *
     * @param employe L'employé concerné (peut être null).
     * @param user    Le compte utilisateur technique (peut être null).
     * @return {@code true} si exempté des restrictions cellulaires, {@code false} sinon.
     */
    public boolean isSuperviseurOuAdmin(EmployeEntity employe, UtilisateurEntity user) {
        // 1. Contrôle sur le compte utilisateur technique (Rôles ADMIN / SUPER_ADMIN)
        if (user != null) {
            String uname = user.getUsername().toLowerCase();
            if (uname.contains("admin") || uname.contains("dg") || uname.contains("daf")) {
                return true;
            }
            if (user.getPermissions() != null && user.getPermissions().stream()
                    .anyMatch(p -> p.name().contains("ADMIN") || p.name().contains("SUPERVISE"))) {
                return true;
            }
        }

        // 2. Contrôle sur la fiche de poste et l'organigramme de l'employé
        if (employe != null && employe.getPoste() != null) {
            String codePoste = (employe.getPoste().getCodePoste() != null ? employe.getPoste().getCodePoste() : "").toUpperCase();
            String intitulePoste = (employe.getPoste().getIntitule() != null ? employe.getPoste().getIntitule() : "").toUpperCase();
            String signaturePoste = codePoste + " " + intitulePoste;

            if (signaturePoste.contains("DIRECTEUR_GENERAL") || signaturePoste.contains("DIRECTEUR GENERAL") || signaturePoste.contains("DG")
                    || signaturePoste.contains("DIRECTEUR_DAF") || signaturePoste.contains("DAF") || signaturePoste.contains("FINANCIER")
                    || signaturePoste.contains("PCA") || signaturePoste.contains("CONSEIL")
                    || signaturePoste.contains("AUDIT_INTERNE") || signaturePoste.contains("AUDITEUR")
                    || signaturePoste.contains("CONTROLEUR_GESTION") || signaturePoste.contains("CONTROLE DE GESTION")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Démarre une instance de workflow à partir du code de l'OPÉRATION MÉTIER (ex: "RECRUITMENT", "SERVICE_MEMO")
     * et de l'EMPLOYÉ initiateur, sans aucun code de workflow écrit en dur.
     *
     * @param request Requête contenant le code métier, l'employé initiateur et l'entité cible.
     * @return DTO représentant l'instance nouvellement instanciée.
     * @throws IllegalArgumentException Si l'opération métier ou l'employé est introuvable.
     * @throws SecurityException        Si l'opération est réservée à une autre unité ou si l'employé n'est pas éligible Maker.
     * @throws IllegalStateException    Si l'unité de l'employé ne possède aucun circuit configuré pour ce métier (Obligation stricte).
     */
    @Transactional
    public WorkflowInstanceResponse demarrerWorkflowMetier(DemarrerWorkflowMetierRequest request) {
        // 1. Résolution de l'employé initiateur
        EmployeEntity employe = employeRepository.findById(request.getEmployeId())
                .orElseThrow(() -> new IllegalArgumentException("Employé initiateur introuvable avec l'ID: " + request.getEmployeId()));

        UniteOrganisationnelleEntity unite = employe.getUniteOrganisationnelle();
        if (unite == null) {
            throw new IllegalStateException("L'employé " + employe.getMatricule() + " n'est rattaché à aucune unité organisationnelle");
        }

        // 2. Résolution de l'opération métier
        WorkflowMetierEntity metier = workflowMetierRepository.findByCode(request.getCodeMetier().trim())
                .orElseThrow(() -> new IllegalArgumentException("Opération métier introuvable avec le code: " + request.getCodeMetier()));

        if (!metier.isActive()) {
            throw new IllegalStateException("L'opération métier [" + metier.getTitle() + "] est actuellement désactivée");
        }

        // 3. Vérification d'exclusivité métier (si non transversal)
        if (!metier.isTransversal() && metier.getAuthorizedUnit() != null) {
            if (!unite.getId().equals(metier.getAuthorizedUnit().getId())) {
                throw new SecurityException("L'opération métier [" + metier.getTitle()
                        + "] est strictement réservée à l'unité [" + metier.getAuthorizedUnit().getIntitule()
                        + "]. Votre unité actuelle est [" + unite.getIntitule() + "].");
            }
        }

        // 4. Résolution du workflow paramétré pour cette unité spécifique (Obligation stricte - Choix A2)
        WorkflowDefinitionEntity def = workflowDefinitionRepository
                .findByMetierIdAndUniteOrganisationnelleIdAndActifTrue(metier.getId(), unite.getId())
                .or(() -> workflowDefinitionRepository.findByMetierCodeAndUniteId(metier.getCode(), unite.getId()))
                .orElseThrow(() -> new IllegalStateException("Aucun circuit de validation n'est configuré pour l'opération ["
                        + metier.getTitle() + "] dans l'unité [" + unite.getIntitule()
                        + "]. Veuillez paramétrer le workflow de votre unité avant de continuer."));

        // 5. Contrôle d'éligibilité du Maker
        UtilisateurEntity userInitiateur = employe.getUtilisateur() != null ? employe.getUtilisateur() : userRepository.findAll().stream().findFirst().orElse(null);
        verifierEligibiliteMaker(def.getCode(), userInitiateur != null ? userInitiateur.getId() : null);

        // 6. Extraction de la première étape
        WorkflowEtapeDefinitionEntity premiereEtape = workflowEtapeRepository
                .findFirstByWorkflowDefinitionIdOrderByOrdreAsc(def.getId())
                .orElseThrow(() -> new IllegalStateException("Le circuit de validation [" + def.getCode() + "] ne possède aucune étape configurée"));

        // 7. Sérialisation du contexte en JSON
        String contexteJson = null;
        if (request.getContexte() != null) {
            try {
                contexteJson = objectMapper.writeValueAsString(request.getContexte());
            } catch (JsonProcessingException e) {
                log.warn("Impossible de sérialiser le contexte de workflow", e);
            }
        }

        // 8. Création de l'instance liée à l'employé et à son unité
        WorkflowInstanceEntity instance = WorkflowInstanceEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .entityName(request.getEntityName())
                .entityId(request.getEntityId())
                .initiateurEmploye(employe)
                .initiateur(userInitiateur)
                .uniteOrganisationnelle(unite)
                .statut(StatutWorkflowInstance.EN_COURS)
                .etapeCourante(premiereEtape)
                .donneesContexte(contexteJson)
                .dateInitiation(Instant.now())
                .build();

        instance = workflowInstanceRepository.save(instance);

        // 9. Génération en parallèle des tâches pour les acteurs de la première étape
        creerTachesPourEtape(instance, premiereEtape);

        // 10. Audit immuable
        UUID acteurAuditId = userInitiateur != null ? userInitiateur.getId() : employe.getId();
        enregistrerAudit(instance, premiereEtape.getCodeEtape(), acteurAuditId, ActionWorkflowAudit.INITIATION, null,
                "Initiation de l'opération [" + metier.getTitle() + "] par l'employé " + employe.getMatricule() + " (" + unite.getIntitule() + ")");

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .instanceId(instance.getId())
                .metierCode(metier.getCode())
                .definitionId(def.getId())
                .etapeId(premiereEtape.getId())
                .employeId(employe.getId())
                .matricule(employe.getMatricule())
                .userId(userInitiateur != null ? userInitiateur.getId().toString() : null)
                .username(userInitiateur != null ? userInitiateur.getUsername() : null)
                .uniteId(unite.getId())
                .actionType("DEMARRAGE_WORKFLOW")
                .decision("INITIE")
                .commentaire("Initiation par " + employe.getMatricule() + " (" + unite.getIntitule() + ")")
                .payload(contexteJson)
                .timestamp(java.time.LocalDateTime.now())
                .build());

        log.info("Workflow opération [{}] démarré pour l'employé {} ({}) - Instance: {}",
                metier.getCode(), employe.getMatricule(), unite.getIntitule(), instance.getId());

        return mapToInstanceResponse(instance);
    }

    /**
     * Vérifie si un circuit actif existe et si l'utilisateur est habilité à l'initier en tant que Maker.
     */
    @Transactional(readOnly = true)
    public void verifierEligibiliteMaker(String workflowCode, UUID utilisateurId) {
        if (workflowCode == null || workflowCode.isBlank()) {
            throw new IllegalArgumentException("Le code de workflow est obligatoire pour la vérification du Maker");
        }

        WorkflowDefinitionEntity def = workflowDefinitionRepository.findByCode(workflowCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Aucun circuit de validation configuré pour le code : " + workflowCode));

        if (!def.isActif()) {
            throw new IllegalStateException("Le circuit de validation [" + workflowCode + "] est actuellement désactivé");
        }

        if (utilisateurId == null) {
            return;
        }

        UtilisateurEntity utilisateur = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + utilisateurId));

        EmployeEntity employe = employeRepository.findByUtilisateurId(utilisateurId).orElse(null);

        // Les administrateurs et superviseurs sont exemptés
        if (isSuperviseurOuAdmin(employe, utilisateur)) {
            return;
        }

        if (!utilisateur.isActif()) {
            throw new SecurityException("Le compte utilisateur " + utilisateur.getUsername() + " est inactif");
        }

        boolean hasRolesRestriction = def.getRolesInitiateurs() != null && !def.getRolesInitiateurs().isBlank();
        boolean hasPostesRestriction = def.getPostesInitiateurs() != null && !def.getPostesInitiateurs().isBlank();

        if (!hasRolesRestriction && !hasPostesRestriction) {
            return;
        }

        boolean roleAutorise = false;
        if (hasRolesRestriction) {
            List<String> rolesAttendus = Arrays.stream(def.getRolesInitiateurs().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            roleAutorise = rolesAttendus.stream().anyMatch(role ->
                    utilisateur.getUsername().toLowerCase().contains(role)
                            || utilisateur.getPermissions().stream().anyMatch(p -> p.name().equalsIgnoreCase(role))
            );
        }

        boolean posteAutorise = false;
        if (hasPostesRestriction) {
            List<String> postesAttendus = Arrays.stream(def.getPostesInitiateurs().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            String monPoste = (employe != null && employe.getPoste() != null)
                    ? (employe.getPoste().getCodePoste() + " " + employe.getPoste().getIntitule()).toLowerCase()
                    : "";

            posteAutorise = postesAttendus.stream().anyMatch(poste ->
                    monPoste.contains(poste) || utilisateur.getUsername().toLowerCase().contains(poste)
            );
        }

        if (!roleAutorise && !posteAutorise) {
            throw new SecurityException("L'employé / utilisateur " + utilisateur.getUsername()
                    + " n'est pas habilité à initier le circuit [" + workflowCode + "]. Rôles/postes requis : "
                    + (hasRolesRestriction ? def.getRolesInitiateurs() : "") + " "
                    + (hasPostesRestriction ? def.getPostesInitiateurs() : ""));
        }
    }

    /**
     * Teste si un utilisateur peut initier un workflow sans lever d'exception.
     */
    @Transactional(readOnly = true)
    public boolean peutInitierWorkflow(String workflowCode, UUID utilisateurId) {
        try {
            verifierEligibiliteMaker(workflowCode, utilisateurId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Récupère la liste de toutes les opérations métiers configurées dans le système.
     */
    @Transactional(readOnly = true)
    public List<WorkflowMetierResponse> listerMetiers() {
        return workflowMetierRepository.findAll().stream()
                .map(this::mapToMetierResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère la liste des définitions de circuits avec filtrage de sécurité par unité.
     * Les superviseurs/administrateurs voient tout ; les agents voient les circuits de leur unité et les transverses.
     *
     * @param connectedUserId Identifiant de l'utilisateur connecté (optionnel).
     * @return Liste des définitions autorisées.
     */
    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> listerDefinitions(UUID connectedUserId) {
        List<WorkflowDefinitionEntity> all = workflowDefinitionRepository.findAll();

        if (connectedUserId == null) {
            return all.stream().map(this::mapToDefinitionResponse).collect(Collectors.toList());
        }

        UtilisateurEntity user = userRepository.findById(connectedUserId).orElse(null);
        EmployeEntity emp = employeRepository.findByUtilisateurId(connectedUserId).orElse(null);

        // Si administrateur ou superviseur : vue intégrale
        if (isSuperviseurOuAdmin(emp, user)) {
            return all.stream().map(this::mapToDefinitionResponse).collect(Collectors.toList());
        }

        // Sinon : filtrage strict sur l'unité organisationnelle de l'employé
        UUID monUniteId = (emp != null && emp.getUniteOrganisationnelle() != null)
                ? emp.getUniteOrganisationnelle().getId() : null;

        return all.stream()
                .filter(d -> d.getUniteOrganisationnelle() == null || (monUniteId != null && d.getUniteOrganisationnelle().getId().equals(monUniteId)))
                .map(this::mapToDefinitionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> listerDefinitions() {
        return listerDefinitions(null);
    }

    /**
     * Démarre une instance de workflow via son code ou ID de définition (rétrocompatibilité).
     */
    @Transactional
    public WorkflowInstanceResponse demarrerWorkflow(DemarrerWorkflowRequest request) {
        WorkflowDefinitionEntity def;
        if (request.getWorkflowDefinitionId() != null) {
            def = workflowDefinitionRepository.findById(request.getWorkflowDefinitionId())
                    .orElseThrow(() -> new IllegalArgumentException("Définition introuvable avec l'ID: " + request.getWorkflowDefinitionId()));
        } else if (request.getWorkflowDefinitionCode() != null) {
            def = workflowDefinitionRepository.findByCode(request.getWorkflowDefinitionCode())
                    .orElseThrow(() -> new IllegalArgumentException("Définition introuvable avec le code: " + request.getWorkflowDefinitionCode()));
        } else {
            throw new IllegalArgumentException("L'identifiant ou le code du circuit est obligatoire");
        }

        verifierEligibiliteMaker(def.getCode(), request.getInitiateurId());

        UtilisateurEntity initiateur = userRepository.findById(request.getInitiateurId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable: " + request.getInitiateurId()));

        EmployeEntity employeInitiateur = employeRepository.findByUtilisateurId(request.getInitiateurId()).orElse(null);

        WorkflowEtapeDefinitionEntity premiereEtape = workflowEtapeRepository
                .findFirstByWorkflowDefinitionIdOrderByOrdreAsc(def.getId())
                .orElseThrow(() -> new IllegalStateException("Le circuit ne possède aucune étape"));

        String contexteJson = null;
        if (request.getContexte() != null) {
            try {
                contexteJson = objectMapper.writeValueAsString(request.getContexte());
            } catch (JsonProcessingException e) {
                log.warn("Erreur sérialisation contexte", e);
            }
        }

        WorkflowInstanceEntity instance = WorkflowInstanceEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .entityName(request.getEntityName())
                .entityId(request.getEntityId())
                .initiateur(initiateur)
                .initiateurEmploye(employeInitiateur)
                .uniteOrganisationnelle(employeInitiateur != null ? employeInitiateur.getUniteOrganisationnelle() : def.getUniteOrganisationnelle())
                .statut(StatutWorkflowInstance.EN_COURS)
                .etapeCourante(premiereEtape)
                .donneesContexte(contexteJson)
                .dateInitiation(Instant.now())
                .build();

        instance = workflowInstanceRepository.save(instance);
        creerTachesPourEtape(instance, premiereEtape);
        enregistrerAudit(instance, premiereEtape.getCodeEtape(), initiateur.getId(), ActionWorkflowAudit.INITIATION, null, "Initiation du circuit");

        return mapToInstanceResponse(instance);
    }

    /**
     * Traite une tâche de validation avec vérification d'assignation étanche (Verrou 1 anti-usurpation).
     */
    @Transactional
    public WorkflowTacheResponse traiterTache(UUID tacheId, TraiterTacheWorkflowRequest request) {
        WorkflowTacheEntity tache = workflowTacheRepository.findById(tacheId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche introuvable: " + tacheId));

        if (tache.getStatut() != StatutWorkflowTache.EN_ATTENTE) {
            throw new IllegalStateException("Cette tâche a déjà été traitée (Statut: " + tache.getStatut() + ")");
        }

        UUID acteurId = request.getActeurId() != null ? request.getActeurId() : tache.getAssigneA().getId();
        UtilisateurEntity userActeur = userRepository.findById(acteurId).orElse(null);
        EmployeEntity empActeur = employeRepository.findByUtilisateurId(acteurId).orElse(null);

        // Verrou 1 de sécurité : Seul l'assigné réel (ou un Superviseur/Admin) peut poser une décision
        boolean isAssigneUser = tache.getAssigneA() != null && tache.getAssigneA().getId().equals(acteurId);
        boolean isAssigneEmp = tache.getAssigneAEmploye() != null && empActeur != null && tache.getAssigneAEmploye().getId().equals(empActeur.getId());
        boolean isAdmin = isSuperviseurOuAdmin(empActeur, userActeur);

        if (!isAssigneUser && !isAssigneEmp && !isAdmin) {
            throw new SecurityException("Action interdite : vous n'êtes pas le validateur assigné à cette tâche.");
        }

        tache.setDecision(request.getDecision());
        tache.setCommentaire(request.getCommentaire());
        tache.setDateDecision(Instant.now());
        tache.setStatut(request.getDecision() == DecisionWorkflow.REJETE ? StatutWorkflowTache.REJETEE : StatutWorkflowTache.APPROUVEE);
        workflowTacheRepository.save(tache);

        WorkflowInstanceEntity instance = tache.getWorkflowInstance();
        WorkflowEtapeDefinitionEntity etapeCourante = tache.getEtapeDefinition();

        ActionWorkflowAudit actionAudit = switch (request.getDecision()) {
            case APPROUVE -> ActionWorkflowAudit.APPROBATION;
            case REJETE -> ActionWorkflowAudit.REJET;
            case DEMANDE_REVISION -> ActionWorkflowAudit.DEMANDE_REVISION;
        };

        enregistrerAudit(instance, etapeCourante.getCodeEtape(), acteurId, actionAudit, request.getDecision(), request.getCommentaire());

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .instanceId(instance.getId())
                .metierCode(instance.getWorkflowDefinition() != null && instance.getWorkflowDefinition().getMetier() != null ? instance.getWorkflowDefinition().getMetier().getCode() : null)
                .definitionId(instance.getWorkflowDefinition() != null ? instance.getWorkflowDefinition().getId() : null)
                .etapeId(etapeCourante != null ? etapeCourante.getId() : null)
                .tacheId(tache.getId())
                .acteurId(tache.getEtapeActeur() != null ? tache.getEtapeActeur().getId() : null)
                .employeId(empActeur != null ? empActeur.getId() : (tache.getAssigneAEmploye() != null ? tache.getAssigneAEmploye().getId() : null))
                .matricule(empActeur != null ? empActeur.getMatricule() : (tache.getAssigneAEmploye() != null ? tache.getAssigneAEmploye().getMatricule() : null))
                .userId(userActeur != null ? userActeur.getId().toString() : acteurId.toString())
                .username(userActeur != null ? userActeur.getUsername() : null)
                .uniteId(instance.getUniteOrganisationnelle() != null ? instance.getUniteOrganisationnelle().getId() : null)
                .actionType("DECISION_TACHE")
                .decision(request.getDecision().name())
                .commentaire(request.getCommentaire())
                .timestamp(java.time.LocalDateTime.now())
                .build());

        if (request.getDecision() == DecisionWorkflow.REJETE) {
            if (tache.isObligatoire()) {
                instance.setStatut(StatutWorkflowInstance.REJETE);
                instance.setDateCloture(Instant.now());
                workflowInstanceRepository.save(instance);
                log.info("Workflow instance {} REJETÉE (décision d'un validateur obligatoire à l'étape {})",
                        instance.getId(), etapeCourante.getCodeEtape());
            } else {
                evaluerProgressionEtape(instance, etapeCourante, acteurId, request.getCommentaire());
            }
        } else if (request.getDecision() == DecisionWorkflow.DEMANDE_REVISION) {
            instance.setStatut(StatutWorkflowInstance.SUSPENDU);
            workflowInstanceRepository.save(instance);
            log.info("Workflow instance {} : Demande de révision formulée à l'étape {}", instance.getId(), etapeCourante.getCodeEtape());
        } else if (request.getDecision() == DecisionWorkflow.APPROUVE) {
            evaluerProgressionEtape(instance, etapeCourante, acteurId, request.getCommentaire());
        }

        return mapToTacheResponse(tache);
    }

    private void evaluerProgressionEtape(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etapeCourante, UUID dernierActeurId, String commentaire) {
        List<WorkflowTacheEntity> tachesEtape = workflowTacheRepository
                .findByWorkflowInstanceIdAndEtapeDefinitionId(instance.getId(), etapeCourante.getId());

        if (tachesEtape.isEmpty()) {
            return;
        }

        List<WorkflowTacheEntity> tachesObligatoires = tachesEtape.stream()
                .filter(WorkflowTacheEntity::isObligatoire)
                .collect(Collectors.toList());

        boolean toutesObligatoiresValidees = tachesObligatoires.stream()
                .allMatch(t -> t.getStatut() == StatutWorkflowTache.APPROUVEE);

        if (!toutesObligatoiresValidees) {
            log.info("Étape {} : En attente de validation des acteurs obligatoires restants", etapeCourante.getCodeEtape());
            return;
        }

        List<WorkflowTacheEntity> tachesOptionnelles = tachesEtape.stream()
                .filter(t -> !t.isObligatoire())
                .collect(Collectors.toList());

        boolean optionnelsSatisfaits = true;
        if (!tachesOptionnelles.isEmpty()) {
            long approuveesOpt = tachesOptionnelles.stream().filter(t -> t.getStatut() == StatutWorkflowTache.APPROUVEE).count();
            PolitiqueValidation pol = etapeCourante.getPolitiqueValidation() != null ? etapeCourante.getPolitiqueValidation() : PolitiqueValidation.UN_SEUL_SUFFIT;

            optionnelsSatisfaits = switch (pol) {
                case UN_SEUL_SUFFIT -> approuveesOpt >= 1;
                case TOUS_DOIVENT_VALIDER -> approuveesOpt == tachesOptionnelles.size();
                case MAJORITE -> approuveesOpt > (tachesOptionnelles.size() / 2);
            };
        }

        if (toutesObligatoiresValidees && optionnelsSatisfaits) {
            passerAEtapeSuivante(instance, etapeCourante, dernierActeurId, commentaire);
        }
    }

    private void passerAEtapeSuivante(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etapeCourante, UUID acteurId, String commentaire) {
        List<WorkflowEtapeDefinitionEntity> etapes = workflowEtapeRepository
                .findByWorkflowDefinitionIdOrderByOrdreAsc(instance.getWorkflowDefinition().getId());

        Optional<WorkflowEtapeDefinitionEntity> suivanteOpt = etapes.stream()
                .filter(e -> e.getOrdre() > etapeCourante.getOrdre())
                .findFirst();

        if (suivanteOpt.isPresent()) {
            WorkflowEtapeDefinitionEntity suivante = suivanteOpt.get();
            instance.setEtapeCourante(suivante);
            workflowInstanceRepository.save(instance);

            creerTachesPourEtape(instance, suivante);
            log.info("Workflow instance {} avance à l'étape: {}", instance.getId(), suivante.getCodeEtape());
        } else {
            instance.setStatut(StatutWorkflowInstance.APPROUVE);
            instance.setDateCloture(Instant.now());
            workflowInstanceRepository.save(instance);

            enregistrerAudit(instance, etapeCourante.getCodeEtape(), acteurId, ActionWorkflowAudit.CLOTURE, DecisionWorkflow.APPROUVE, "Circuit approuvé dans sa totalité : " + (commentaire != null ? commentaire : ""));
            log.info("Workflow instance {} TOTALEMENT APPROUVÉE !", instance.getId());

            eventPublisher.publishEvent(new WorkflowInstanceCompletedEvent(
                    instance.getId(),
                    instance.getWorkflowDefinition() != null ? instance.getWorkflowDefinition().getCode() : null,
                    instance.getEntityName(),
                    instance.getEntityId(),
                    etapeCourante.getCodeEtape(),
                    acteurId,
                    commentaire
            ));
        }
    }

    /**
     * Génère en parallèle les tâches pour les acteurs d'une étape en résolvant l'employé et l'utilisateur.
     */
    private void creerTachesPourEtape(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etape) {
        List<WorkflowEtapeActeurDefinitionEntity> acteurs = workflowEtapeActeurRepository.findByEtapeDefinitionId(etape.getId());
        if (acteurs.isEmpty() && etape.getActeurs() != null && !etape.getActeurs().isEmpty()) {
            acteurs = etape.getActeurs();
        }

        if (!acteurs.isEmpty()) {
            boolean isUniqueActeur = acteurs.size() == 1;

            for (WorkflowEtapeActeurDefinitionEntity act : acteurs) {
                EmployeEntity assigneEmploye = act.getEmploye();
                UtilisateurEntity assigneUser = act.getUtilisateur();

                // Résolution dynamique si non fixé
                if (assigneEmploye == null && assigneUser == null) {
                    assigneEmploye = resoudreEmployeAssigne(instance, act.getValeurAssignation(), act.getUniteOrganisationnelle());
                    if (assigneEmploye != null) {
                        assigneUser = assigneEmploye.getUtilisateur();
                    } else {
                        assigneUser = resoudreUtilisateurAssigne(instance, act.getValeurAssignation());
                    }
                } else if (assigneEmploye != null && assigneUser == null) {
                    assigneUser = assigneEmploye.getUtilisateur();
                }

                if (assigneUser == null) {
                    assigneUser = instance.getInitiateur();
                }

                Integer slaHeures = act.getDelaiSlaHeures() != null ? act.getDelaiSlaHeures() : etape.getDelaiSlaHeures();
                Instant echeance = (slaHeures != null && slaHeures > 0) ? Instant.now().plus(slaHeures, ChronoUnit.HOURS) : null;
                boolean obligatoire = isUniqueActeur || act.isObligatoire();

                WorkflowTacheEntity tache = WorkflowTacheEntity.builder()
                        .id(UUID.randomUUID())
                        .workflowInstance(instance)
                        .etapeDefinition(etape)
                        .etapeActeur(act)
                        .intituleAction(act.getIntituleAction())
                        .obligatoire(obligatoire)
                        .assigneAEmploye(assigneEmploye)
                        .assigneA(assigneUser)
                        .statut(StatutWorkflowTache.EN_ATTENTE)
                        .dateCreation(Instant.now())
                        .dateEcheance(echeance)
                        .build();

                workflowTacheRepository.save(tache);
            }
        } else {
            // Fallback classique
            EmployeEntity assigneEmploye = resoudreEmployeAssigne(instance, etape.getValeurAssignation(), null);
            UtilisateurEntity assigneUser = assigneEmploye != null ? assigneEmploye.getUtilisateur() : resoudreUtilisateurAssigne(instance, etape.getValeurAssignation());
            if (assigneUser == null) {
                assigneUser = instance.getInitiateur();
            }

            Instant echeance = (etape.getDelaiSlaHeures() != null && etape.getDelaiSlaHeures() > 0)
                    ? Instant.now().plus(etape.getDelaiSlaHeures(), ChronoUnit.HOURS) : null;

            WorkflowTacheEntity tache = WorkflowTacheEntity.builder()
                    .id(UUID.randomUUID())
                    .workflowInstance(instance)
                    .etapeDefinition(etape)
                    .intituleAction(etape.getIntitule())
                    .obligatoire(true)
                    .assigneAEmploye(assigneEmploye)
                    .assigneA(assigneUser)
                    .statut(StatutWorkflowTache.EN_ATTENTE)
                    .dateCreation(Instant.now())
                    .dateEcheance(echeance)
                    .build();

            workflowTacheRepository.save(tache);
        }
    }

    private EmployeEntity resoudreEmployeAssigne(WorkflowInstanceEntity instance, String valeurAssignation, UniteOrganisationnelleEntity restrictionUnite) {
        if (valeurAssignation == null || valeurAssignation.isBlank()) {
            return instance.getInitiateurEmploye();
        }

        String search = valeurAssignation.toLowerCase().trim();
        List<EmployeEntity> employes = employeRepository.findAll();

        return employes.stream()
                .filter(e -> restrictionUnite == null || (e.getUniteOrganisationnelle() != null && e.getUniteOrganisationnelle().getId().equals(restrictionUnite.getId())))
                .filter(e -> {
                    String poste = (e.getPoste() != null) ? (e.getPoste().getCodePoste() + " " + e.getPoste().getIntitule()).toLowerCase() : "";
                    String nom = (e.getPrenom() + " " + e.getNom()).toLowerCase();
                    return poste.contains(search) || nom.contains(search);
                })
                .findFirst()
                .orElse(null);
    }

    private UtilisateurEntity resoudreUtilisateurAssigne(WorkflowInstanceEntity instance, String valeurAssignation) {
        if (valeurAssignation != null && !valeurAssignation.isBlank()) {
            String search = valeurAssignation.toLowerCase().trim();
            Optional<UtilisateurEntity> matched = userRepository.findAll().stream()
                    .filter(UtilisateurEntity::isActif)
                    .filter(u -> u.getUsername().toLowerCase().contains(search)
                            || (u.getNom() != null && u.getNom().toLowerCase().contains(search)))
                    .findFirst();
            if (matched.isPresent()) {
                return matched.get();
            }
        }
        return instance.getInitiateur();
    }

    private void enregistrerAudit(WorkflowInstanceEntity instance, String etapeCode, UUID acteurId, ActionWorkflowAudit action, DecisionWorkflow decision, String commentaire) {
        WorkflowHistoriqueAuditEntity audit = WorkflowHistoriqueAuditEntity.builder()
                .id(UUID.randomUUID())
                .workflowInstance(instance)
                .etapeCode(etapeCode)
                .acteurId(acteurId)
                .action(action)
                .decision(decision)
                .commentaire(commentaire)
                .horodatage(Instant.now())
                .build();

        workflowHistoriqueAuditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowTacheResponse> getMesTaches(UUID utilisateurId, StatutWorkflowTache statut, Pageable pageable) {
        StatutWorkflowTache st = statut != null ? statut : StatutWorkflowTache.EN_ATTENTE;
        return workflowTacheRepository.findByAssigneAIdAndStatut(utilisateurId, st, pageable)
                .map(this::mapToTacheResponse);
    }

    /**
     * Consultation d'un dossier avec application stricte du principe Need-to-Know et bypass Superviseurs.
     */
    @Transactional(readOnly = true)
    public WorkflowInstanceResponse getInstanceById(UUID instanceId, UUID connectedUserId) {
        WorkflowInstanceEntity entity = workflowInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance de workflow introuvable: " + instanceId));

        if (connectedUserId == null) {
            return mapToInstanceResponse(entity);
        }

        UtilisateurEntity user = userRepository.findById(connectedUserId).orElse(null);
        EmployeEntity emp = employeRepository.findByUtilisateurId(connectedUserId).orElse(null);

        // 1. Superviseur ou Administrateur : accès garanti
        if (isSuperviseurOuAdmin(emp, user)) {
            return mapToInstanceResponse(entity);
        }

        // 2. L'agent est l'initiateur du dossier
        if (entity.getInitiateur() != null && entity.getInitiateur().getId().equals(connectedUserId)) {
            return mapToInstanceResponse(entity);
        }
        if (emp != null && entity.getInitiateurEmploye() != null && entity.getInitiateurEmploye().getId().equals(emp.getId())) {
            return mapToInstanceResponse(entity);
        }

        // 3. L'instance appartient à la cellule de l'agent
        if (emp != null && emp.getUniteOrganisationnelle() != null && entity.getUniteOrganisationnelle() != null) {
            if (emp.getUniteOrganisationnelle().getId().equals(entity.getUniteOrganisationnelle().getId())) {
                return mapToInstanceResponse(entity);
            }
        }

        // 4. L'agent possède une tâche sur cette instance (Need-to-Know)
        boolean hasTask = entity.getTaches().stream().anyMatch(t ->
                (t.getAssigneA() != null && t.getAssigneA().getId().equals(connectedUserId))
                        || (emp != null && t.getAssigneAEmploye() != null && t.getAssigneAEmploye().getId().equals(emp.getId())));

        if (hasTask) {
            return mapToInstanceResponse(entity);
        }

        throw new SecurityException("Accès refusé au dossier : vous ne disposez pas des habilitations requises pour consulter cette instance.");
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse getInstanceById(UUID instanceId) {
        return getInstanceById(instanceId, null);
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowInstanceResponse> getInstanceParEntite(String entityName, String entityId) {
        return workflowInstanceRepository.findByEntityNameAndEntityId(entityName, entityId)
                .map(this::mapToInstanceResponse);
    }

    private WorkflowInstanceResponse mapToInstanceResponse(WorkflowInstanceEntity e) {
        List<WorkflowTacheResponse> taches = workflowTacheRepository.findByWorkflowInstanceId(e.getId()).stream()
                .map(this::mapToTacheResponse)
                .collect(Collectors.toList());

        return WorkflowInstanceResponse.builder()
                .id(e.getId())
                .definitionId(e.getWorkflowDefinition() != null ? e.getWorkflowDefinition().getId() : null)
                .definitionCode(e.getWorkflowDefinition() != null ? e.getWorkflowDefinition().getCode() : null)
                .definitionIntitule(e.getWorkflowDefinition() != null ? e.getWorkflowDefinition().getIntitule() : null)
                .uniteOrganisationnelleId(e.getUniteOrganisationnelle() != null ? e.getUniteOrganisationnelle().getId() : null)
                .uniteOrganisationnelleNom(e.getUniteOrganisationnelle() != null ? e.getUniteOrganisationnelle().getIntitule() : null)
                .entityName(e.getEntityName())
                .entityId(e.getEntityId())
                .initiateurEmployeId(e.getInitiateurEmploye() != null ? e.getInitiateurEmploye().getId() : null)
                .initiateurEmployeMatricule(e.getInitiateurEmploye() != null ? e.getInitiateurEmploye().getMatricule() : null)
                .initiateurEmployeNom(e.getInitiateurEmploye() != null ? e.getInitiateurEmploye().getPrenom() + " " + e.getInitiateurEmploye().getNom() : null)
                .initiateurId(e.getInitiateur() != null ? e.getInitiateur().getId() : null)
                .initiateurNom(e.getInitiateur() != null ? e.getInitiateur().getPrenom() + " " + e.getInitiateur().getNom() : null)
                .statut(e.getStatut())
                .etapeCouranteCode(e.getEtapeCourante() != null ? e.getEtapeCourante().getCodeEtape() : null)
                .etapeCouranteIntitule(e.getEtapeCourante() != null ? e.getEtapeCourante().getIntitule() : null)
                .dateInitiation(e.getDateInitiation())
                .dateCloture(e.getDateCloture())
                .taches(taches)
                .build();
    }

    private WorkflowTacheResponse mapToTacheResponse(WorkflowTacheEntity t) {
        return WorkflowTacheResponse.builder()
                .id(t.getId())
                .instanceId(t.getWorkflowInstance().getId())
                .acteurDefinitionId(t.getEtapeActeur() != null ? t.getEtapeActeur().getId() : null)
                .intituleAction(t.getIntituleAction() != null ? t.getIntituleAction() : (t.getEtapeDefinition() != null ? t.getEtapeDefinition().getIntitule() : null))
                .obligatoire(t.isObligatoire())
                .entityName(t.getWorkflowInstance().getEntityName())
                .entityId(t.getWorkflowInstance().getEntityId())
                .etapeCode(t.getEtapeDefinition() != null ? t.getEtapeDefinition().getCodeEtape() : null)
                .etapeIntitule(t.getEtapeDefinition() != null ? t.getEtapeDefinition().getIntitule() : null)
                .assigneAEmployeId(t.getAssigneAEmploye() != null ? t.getAssigneAEmploye().getId() : null)
                .assigneAEmployeMatricule(t.getAssigneAEmploye() != null ? t.getAssigneAEmploye().getMatricule() : null)
                .assigneAEmployeNom(t.getAssigneAEmploye() != null ? t.getAssigneAEmploye().getPrenom() + " " + t.getAssigneAEmploye().getNom() : null)
                .assigneAEmployePoste((t.getAssigneAEmploye() != null && t.getAssigneAEmploye().getPoste() != null) ? t.getAssigneAEmploye().getPoste().getIntitule() : null)
                .assigneAId(t.getAssigneA() != null ? t.getAssigneA().getId() : null)
                .assigneANom(t.getAssigneA() != null ? t.getAssigneA().getPrenom() + " " + t.getAssigneA().getNom() : null)
                .statut(t.getStatut())
                .decision(t.getDecision())
                .commentaire(t.getCommentaire())
                .dateCreation(t.getDateCreation())
                .dateEcheance(t.getDateEcheance())
                .dateDecision(t.getDateDecision())
                .build();
    }


    // ═══════════════════════════════════════════════════════════════════════
    // GESTION ET CYCLE DE VIE DES DÉFINITIONS, ÉTAPES ET VALIDATEURS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Crée une nouvelle définition de workflow avec ses étapes et ses validateurs multi-rôles.
     */
    @Transactional
    public WorkflowDefinitionResponse creerDefinition(CreerWorkflowDefinitionRequest request) {
        if (workflowDefinitionRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Un circuit avec le code [" + request.getCode() + "] existe déjà.");
        }

        WorkflowMetierEntity metier = null;
        if (request.getMetierId() != null) {
            metier = workflowMetierRepository.findById(request.getMetierId()).orElse(null);
        } else if (request.getMetierCode() != null) {
            metier = workflowMetierRepository.findByCode(request.getMetierCode()).orElse(null);
        }

        UniteOrganisationnelleEntity unite = null;
        if (request.getUniteOrganisationnelleId() != null) {
            unite = uniteRepository.findById(request.getUniteOrganisationnelleId())
                    .orElseThrow(() -> new IllegalArgumentException("Unité organisationnelle introuvable avec l'ID: " + request.getUniteOrganisationnelleId()));
        }

        WorkflowDefinitionEntity def = WorkflowDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .code(request.getCode())
                .intitule(request.getIntitule())
                .description(request.getDescription())
                .domaine(request.getDomaine())
                .metier(metier)
                .uniteOrganisationnelle(unite)
                .rolesInitiateurs(request.getRolesInitiateurs())
                .postesInitiateurs(request.getPostesInitiateurs())
                .version(1)
                .actif(request.getActif() != null ? request.getActif() : true)
                .dateCreation(Instant.now())
                .build();

        if (request.getEtapes() != null) {
            for (CreerWorkflowDefinitionRequest.CreerEtapeDefinitionRequest etapeReq : request.getEtapes()) {
                WorkflowEtapeDefinitionEntity etape = WorkflowEtapeDefinitionEntity.builder()
                        .id(UUID.randomUUID())
                        .workflowDefinition(def)
                        .ordre(etapeReq.getOrdre())
                        .codeEtape(etapeReq.getCodeEtape())
                        .intitule(etapeReq.getIntitule())
                        .typeEtape(etapeReq.getTypeEtape())
                        .politiqueValidation(etapeReq.getPolitiqueValidation() != null ? etapeReq.getPolitiqueValidation() : PolitiqueValidation.UN_SEUL_SUFFIT)
                        .strategieAssignation(etapeReq.getStrategieAssignation())
                        .valeurAssignation(etapeReq.getValeurAssignation())
                        .delaiSlaHeures(etapeReq.getDelaiSlaHeures())
                        .actionDepassementSla(etapeReq.getActionDepassementSla() != null ? etapeReq.getActionDepassementSla() : ActionDepassementSla.RELANCE_EMAIL)
                        .actif(etapeReq.getActif() != null ? etapeReq.getActif() : true)
                        .build();

                if (etapeReq.getActeurs() != null) {
                    for (CreerWorkflowDefinitionRequest.CreerActeurDefinitionRequest actReq : etapeReq.getActeurs()) {
                        EmployeEntity emp = actReq.getEmployeId() != null ? employeRepository.findById(actReq.getEmployeId()).orElse(null) : null;
                        UtilisateurEntity usr = actReq.getUtilisateurId() != null ? userRepository.findById(actReq.getUtilisateurId()).orElse(null) : null;
                        UniteOrganisationnelleEntity actUnite = actReq.getUniteOrganisationnelleId() != null ? uniteRepository.findById(actReq.getUniteOrganisationnelleId()).orElse(null) : null;

                        WorkflowEtapeActeurDefinitionEntity acteur = WorkflowEtapeActeurDefinitionEntity.builder()
                                .id(UUID.randomUUID())
                                .etapeDefinition(etape)
                                .intituleAction(actReq.getIntituleAction())
                                .employe(emp)
                                .utilisateur(usr)
                                .uniteOrganisationnelle(actUnite)
                                .strategieAssignation(actReq.getStrategieAssignation())
                                .valeurAssignation(actReq.getValeurAssignation())
                                .obligatoire(actReq.getObligatoire() != null ? actReq.getObligatoire() : true)
                                .delaiSlaHeures(actReq.getDelaiSlaHeures())
                                .actionDepassementSla(actReq.getActionDepassementSla() != null ? actReq.getActionDepassementSla() : ActionDepassementSla.RELANCE_EMAIL)
                                .actif(actReq.getActif() != null ? actReq.getActif() : true)
                                .build();
                        etape.addActeur(acteur);
                    }
                }
                def.addEtape(etape);
            }
        }

        def = workflowDefinitionRepository.save(def);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .definitionId(def.getId())
                .metierCode(metier != null ? metier.getCode() : null)
                .uniteId(unite != null ? unite.getId() : null)
                .actionType("CREATION_DEFINITION")
                .commentaire("Création du circuit de workflow: " + def.getCode())
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToDefinitionResponse(def);
    }

    /**
     * Modifie un circuit de workflow existant (intitulé, étapes, validateurs, versioning).
     */
    @Transactional
    public WorkflowDefinitionResponse modifierDefinition(UUID id, ModifierWorkflowDefinitionRequest request) {
        WorkflowDefinitionEntity def = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Définition introuvable avec l'ID: " + id));

        def.setIntitule(request.getIntitule());
        def.setDescription(request.getDescription());
        def.setDomaine(request.getDomaine());
        def.setRolesInitiateurs(request.getRolesInitiateurs());
        def.setPostesInitiateurs(request.getPostesInitiateurs());
        if (request.getActif() != null) {
            def.setActif(request.getActif());
        }
        def.setVersion(def.getVersion() + 1);

        if (request.getMetierId() != null) {
            def.setMetier(workflowMetierRepository.findById(request.getMetierId()).orElse(null));
        }
        if (request.getUniteOrganisationnelleId() != null) {
            def.setUniteOrganisationnelle(uniteRepository.findById(request.getUniteOrganisationnelleId()).orElse(null));
        }

        if (request.getEtapes() != null) {
            def.getEtapes().clear();
            for (CreerWorkflowDefinitionRequest.CreerEtapeDefinitionRequest etapeReq : request.getEtapes()) {
                WorkflowEtapeDefinitionEntity etape = WorkflowEtapeDefinitionEntity.builder()
                        .id(UUID.randomUUID())
                        .workflowDefinition(def)
                        .ordre(etapeReq.getOrdre())
                        .codeEtape(etapeReq.getCodeEtape())
                        .intitule(etapeReq.getIntitule())
                        .typeEtape(etapeReq.getTypeEtape())
                        .politiqueValidation(etapeReq.getPolitiqueValidation() != null ? etapeReq.getPolitiqueValidation() : PolitiqueValidation.UN_SEUL_SUFFIT)
                        .strategieAssignation(etapeReq.getStrategieAssignation())
                        .valeurAssignation(etapeReq.getValeurAssignation())
                        .delaiSlaHeures(etapeReq.getDelaiSlaHeures())
                        .actionDepassementSla(etapeReq.getActionDepassementSla() != null ? etapeReq.getActionDepassementSla() : ActionDepassementSla.RELANCE_EMAIL)
                        .actif(etapeReq.getActif() != null ? etapeReq.getActif() : true)
                        .build();

                if (etapeReq.getActeurs() != null) {
                    for (CreerWorkflowDefinitionRequest.CreerActeurDefinitionRequest actReq : etapeReq.getActeurs()) {
                        EmployeEntity emp = actReq.getEmployeId() != null ? employeRepository.findById(actReq.getEmployeId()).orElse(null) : null;
                        UtilisateurEntity usr = actReq.getUtilisateurId() != null ? userRepository.findById(actReq.getUtilisateurId()).orElse(null) : null;
                        UniteOrganisationnelleEntity actUnite = actReq.getUniteOrganisationnelleId() != null ? uniteRepository.findById(actReq.getUniteOrganisationnelleId()).orElse(null) : null;

                        WorkflowEtapeActeurDefinitionEntity acteur = WorkflowEtapeActeurDefinitionEntity.builder()
                                .id(UUID.randomUUID())
                                .etapeDefinition(etape)
                                .intituleAction(actReq.getIntituleAction())
                                .employe(emp)
                                .utilisateur(usr)
                                .uniteOrganisationnelle(actUnite)
                                .strategieAssignation(actReq.getStrategieAssignation())
                                .valeurAssignation(actReq.getValeurAssignation())
                                .obligatoire(actReq.getObligatoire() != null ? actReq.getObligatoire() : true)
                                .delaiSlaHeures(actReq.getDelaiSlaHeures())
                                .actionDepassementSla(actReq.getActionDepassementSla() != null ? actReq.getActionDepassementSla() : ActionDepassementSla.RELANCE_EMAIL)
                                .actif(actReq.getActif() != null ? actReq.getActif() : true)
                                .build();
                        etape.addActeur(acteur);
                    }
                }
                def.addEtape(etape);
            }
        }

        def = workflowDefinitionRepository.save(def);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .definitionId(def.getId())
                .actionType("MODIFICATION_DEFINITION")
                .commentaire("Mise à jour du circuit de workflow: " + def.getCode() + " (v" + def.getVersion() + ")")
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToDefinitionResponse(def);
    }

    /**
     * Active ou désactive une étape spécifique d'un circuit de validation.
     */
    @Transactional
    public WorkflowDefinitionResponse changerStatutEtape(UUID definitionId, UUID etapeId, boolean actif) {
        WorkflowEtapeDefinitionEntity etape = workflowEtapeRepository.findById(etapeId)
                .orElseThrow(() -> new IllegalArgumentException("Étape introuvable avec l'ID: " + etapeId));
        etape.setActif(actif);
        workflowEtapeRepository.save(etape);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .definitionId(definitionId)
                .etapeId(etapeId)
                .actionType("STATUT_ETAPE")
                .commentaire("Statut de l'étape " + etape.getCodeEtape() + " changé à: " + (actif ? "ACTIF" : "INACTIF"))
                .timestamp(java.time.LocalDateTime.now())
                .build());

        WorkflowDefinitionEntity def = workflowDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Définition introuvable: " + definitionId));
        return mapToDefinitionResponse(def);
    }

    /**
     * Active ou désactive un validateur spécifique d'une étape de workflow.
     */
    @Transactional
    public WorkflowDefinitionResponse changerStatutActeur(UUID definitionId, UUID etapeId, UUID acteurId, boolean actif) {
        WorkflowEtapeActeurDefinitionEntity acteur = workflowEtapeActeurRepository.findById(acteurId)
                .orElseThrow(() -> new IllegalArgumentException("Validateur introuvable avec l'ID: " + acteurId));
        acteur.setActif(actif);
        workflowEtapeActeurRepository.save(acteur);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .definitionId(definitionId)
                .etapeId(etapeId)
                .acteurId(acteurId)
                .actionType("STATUT_ACTEUR")
                .commentaire("Statut du validateur " + acteur.getIntituleAction() + " changé à: " + (actif ? "ACTIF" : "INACTIF"))
                .timestamp(java.time.LocalDateTime.now())
                .build());

        WorkflowDefinitionEntity def = workflowDefinitionRepository.findById(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Définition introuvable: " + definitionId));
        return mapToDefinitionResponse(def);
    }

    /**
     * Crée une opération métier dans le registre transversal.
     * Si l'utilisateur n'est pas exempté (Admin, DG, DAF), requiert un workflow de validation.
     */
    @Transactional
    public WorkflowMetierResponse creerMetier(CreerWorkflowMetierRequest request, UUID connectedUserId) {
        if (workflowMetierRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Un métier avec le code [" + request.getCode() + "] existe déjà.");
        }

        UtilisateurEntity user = connectedUserId != null ? userRepository.findById(connectedUserId).orElse(null) : null;
        EmployeEntity emp = connectedUserId != null ? employeRepository.findByUtilisateurId(connectedUserId).orElse(null) : null;

        boolean exempt = isSuperviseurOuAdmin(emp, user);

        UniteOrganisationnelleEntity authUnit = null;
        if (request.getAuthorizedUnitId() != null) {
            authUnit = uniteRepository.findById(request.getAuthorizedUnitId()).orElse(null);
        }

        WorkflowMetierEntity metier = WorkflowMetierEntity.builder()
                .id(UUID.randomUUID())
                .code(request.getCode().trim().toUpperCase())
                .title(request.getTitle())
                .description(request.getDescription())
                .domain(request.getDomain())
                .isTransversal(request.getIsTransversal() != null ? request.getIsTransversal() : false)
                .authorizedUnit(authUnit)
                .isActive(exempt ? (request.getIsActive() != null ? request.getIsActive() : true) : false)
                .createdAt(Instant.now())
                .build();

        metier = workflowMetierRepository.save(metier);

        // Si l'utilisateur n'est pas exempté (Admin/DG/DAF), un workflow de création de métier est requis
        if (!exempt && connectedUserId != null) {
            try {
                Map<String, Object> payload = Map.of(
                        "metierId", metier.getId().toString(),
                        "metierCode", metier.getCode(),
                        "title", metier.getTitle()
                );
                demarrerWorkflowMetier(DemarrerWorkflowMetierRequest.builder()
                        .codeMetier("CREATION_METIER")
                        .entityName("WORKFLOW_METIER")
                        .entityId(metier.getId().toString())
                        .employeId(emp != null ? emp.getId() : null)
                        .contexte(payload)
                        .build());
            } catch (Exception e) {
                log.warn("Aucun circuit 'CREATION_METIER' configuré ou erreur de démarrage: {}. Métier enregistré en attente d'activation.", e.getMessage());
            }
        }

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .metierCode(metier.getCode())
                .userId(connectedUserId != null ? connectedUserId.toString() : null)
                .actionType("CREATION_METIER")
                .commentaire("Création du processus métier: " + metier.getCode() + " (Exempté: " + exempt + ")")
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToMetierResponse(metier);
    }

    /**
     * Modifie une opération métier existante.
     */
    @Transactional
    public WorkflowMetierResponse modifierMetier(UUID id, ModifierWorkflowMetierRequest request) {
        WorkflowMetierEntity metier = workflowMetierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Métier introuvable avec l'ID: " + id));

        metier.setTitle(request.getTitle());
        metier.setDescription(request.getDescription());
        metier.setDomain(request.getDomain());
        if (request.getIsTransversal() != null) {
            metier.setIsTransversal(request.getIsTransversal());
        }
        if (request.getIsActive() != null) {
            metier.setIsActive(request.getIsActive());
        }
        if (request.getAuthorizedUnitId() != null) {
            metier.setAuthorizedUnit(uniteRepository.findById(request.getAuthorizedUnitId()).orElse(null));
        }

        metier = workflowMetierRepository.save(metier);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .metierCode(metier.getCode())
                .actionType("MODIFICATION_METIER")
                .commentaire("Mise à jour du processus métier: " + metier.getCode())
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToMetierResponse(metier);
    }

    /**
     * Active ou désactive une opération métier.
     */
    @Transactional
    public WorkflowMetierResponse changerStatutMetier(UUID id, boolean isActive) {
        WorkflowMetierEntity metier = workflowMetierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Métier introuvable avec l'ID: " + id));
        metier.setIsActive(isActive);
        metier = workflowMetierRepository.save(metier);

        auditClickHouseAsync(WorkflowEventMessageInput.builder()
                .id(UUID.randomUUID())
                .metierCode(metier.getCode())
                .actionType("STATUT_METIER")
                .commentaire("Statut du métier " + metier.getCode() + " changé à: " + (isActive ? "ACTIF" : "INACTIF"))
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return mapToMetierResponse(metier);
    }

    private WorkflowDefinitionResponse mapToDefinitionResponse(WorkflowDefinitionEntity def) {
        List<WorkflowDefinitionResponse.WorkflowEtapeResponse> etapes = def.getEtapes().stream()
                .map(et -> {
                    List<WorkflowDefinitionResponse.WorkflowEtapeActeurResponse> acteursDto = et.getActeurs() != null
                            ? et.getActeurs().stream()
                            .map(act -> WorkflowDefinitionResponse.WorkflowEtapeActeurResponse.builder()
                                    .id(act.getId())
                                    .intituleAction(act.getIntituleAction())
                                    .employeId(act.getEmploye() != null ? act.getEmploye().getId() : null)
                                    .employeMatricule(act.getEmploye() != null ? act.getEmploye().getMatricule() : null)
                                    .employeNom(act.getEmploye() != null ? act.getEmploye().getPrenom() + " " + act.getEmploye().getNom() : null)
                                    .utilisateurId(act.getUtilisateur() != null ? act.getUtilisateur().getId() : null)
                                    .utilisateurNom(act.getUtilisateur() != null ? act.getUtilisateur().getPrenom() + " " + act.getUtilisateur().getNom() : null)
                                    .uniteOrganisationnelleId(act.getUniteOrganisationnelle() != null ? act.getUniteOrganisationnelle().getId() : null)
                                    .uniteOrganisationnelleNom(act.getUniteOrganisationnelle() != null ? act.getUniteOrganisationnelle().getIntitule() : null)
                                    .strategieAssignation(act.getStrategieAssignation() != null ? act.getStrategieAssignation().name() : null)
                                    .valeurAssignation(act.getValeurAssignation())
                                    .obligatoire(act.isObligatoire())
                                    .delaiSlaHeures(act.getDelaiSlaHeures())
                                    .actif(act.getActif() != null ? act.getActif() : true)
                                    .build())
                            .collect(Collectors.toList())
                            : Collections.emptyList();

                    return WorkflowDefinitionResponse.WorkflowEtapeResponse.builder()
                            .id(et.getId())
                            .ordre(et.getOrdre())
                            .codeEtape(et.getCodeEtape())
                            .intitule(et.getIntitule())
                            .typeEtape(et.getTypeEtape() != null ? et.getTypeEtape().name() : null)
                            .politiqueValidation(et.getPolitiqueValidation() != null ? et.getPolitiqueValidation().name() : null)
                            .strategieAssignation(et.getStrategieAssignation() != null ? et.getStrategieAssignation().name() : null)
                            .valeurAssignation(et.getValeurAssignation())
                            .delaiSlaHeures(et.getDelaiSlaHeures())
                            .actif(et.getActif() != null ? et.getActif() : true)
                            .acteurs(acteursDto)
                            .build();
                })
                .collect(Collectors.toList());

        return WorkflowDefinitionResponse.builder()
                .id(def.getId())
                .metierId(def.getMetier() != null ? def.getMetier().getId() : null)
                .metierCode(def.getMetier() != null ? def.getMetier().getCode() : null)
                .metierTitle(def.getMetier() != null ? def.getMetier().getTitle() : null)
                .uniteOrganisationnelleId(def.getUniteOrganisationnelle() != null ? def.getUniteOrganisationnelle().getId() : null)
                .uniteOrganisationnelleNom(def.getUniteOrganisationnelle() != null ? def.getUniteOrganisationnelle().getIntitule() : null)
                .code(def.getCode())
                .intitule(def.getIntitule())
                .description(def.getDescription())
                .domaine(def.getDomaine())
                .version(def.getVersion())
                .actif(def.isActif())
                .rolesInitiateurs(def.getRolesInitiateurs())
                .postesInitiateurs(def.getPostesInitiateurs())
                .etapes(etapes)
                .build();
    }

    private WorkflowMetierResponse mapToMetierResponse(WorkflowMetierEntity m) {
        return WorkflowMetierResponse.builder()
                .id(m.getId())
                .code(m.getCode())
                .title(m.getTitle())
                .description(m.getDescription())
                .domain(m.getDomain())
                .isTransversal(m.isTransversal())
                .authorizedUnitId(m.getAuthorizedUnit() != null ? m.getAuthorizedUnit().getId() : null)
                .authorizedUnitName(m.getAuthorizedUnit() != null ? m.getAuthorizedUnit().getIntitule() : null)
                .isActive(m.isActive())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
