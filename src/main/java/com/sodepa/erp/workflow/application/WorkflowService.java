package com.sodepa.erp.workflow.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.workflow.infrastructure.entities.*;
import com.sodepa.erp.workflow.infrastructure.repo.*;
import com.sodepa.erp.workflow.presentation.requests.DemarrerWorkflowRequest;
import com.sodepa.erp.workflow.presentation.requests.TraiterTacheWorkflowRequest;
import com.sodepa.erp.workflow.presentation.responses.WorkflowDefinitionResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowInstanceResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowTacheResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowEtapeDefinitionRepository workflowEtapeRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowTacheRepository workflowTacheRepository;
    private final WorkflowHistoriqueAuditRepository workflowHistoriqueAuditRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> listerDefinitions() {
        return workflowDefinitionRepository.findAll().stream()
                .map(this::mapToDefinitionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkflowInstanceResponse demarrerWorkflow(DemarrerWorkflowRequest request) {
        WorkflowDefinitionEntity def = null;
        if (request.getWorkflowDefinitionId() != null) {
            def = workflowDefinitionRepository.findById(request.getWorkflowDefinitionId())
                    .orElseThrow(() -> new IllegalArgumentException("Définition de workflow introuvable avec l'ID: " + request.getWorkflowDefinitionId()));
        } else if (request.getWorkflowDefinitionCode() != null) {
            def = workflowDefinitionRepository.findByCode(request.getWorkflowDefinitionCode())
                    .orElseThrow(() -> new IllegalArgumentException("Définition de workflow introuvable avec le code: " + request.getWorkflowDefinitionCode()));
        } else {
            throw new IllegalArgumentException("L'identifiant ou le code du circuit de validation est obligatoire");
        }

        UtilisateurEntity initiateur = userRepository.findById(request.getInitiateurId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur initiateur introuvable: " + request.getInitiateurId()));

        // Trouver la première étape ordonnée par ordre ascendant
        WorkflowEtapeDefinitionEntity premiereEtape = workflowEtapeRepository
                .findFirstByWorkflowDefinitionIdOrderByOrdreAsc(def.getId())
                .orElseThrow(() -> new IllegalStateException("Le circuit de validation ne possède aucune étape configurée"));

        String contexteJson = null;
        if (request.getContexte() != null) {
            try {
                contexteJson = objectMapper.writeValueAsString(request.getContexte());
            } catch (JsonProcessingException e) {
                log.warn("Impossible de sérialiser le contexte de workflow", e);
            }
        }

        WorkflowInstanceEntity instance = WorkflowInstanceEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .entityName(request.getEntityName())
                .entityId(request.getEntityId())
                .initiateur(initiateur)
                .statut(StatutWorkflowInstance.EN_COURS)
                .etapeCourante(premiereEtape)
                .donneesContexte(contexteJson)
                .dateInitiation(Instant.now())
                .build();

        instance = workflowInstanceRepository.save(instance);

        // Assigner la première tâche
        creerTachePourEtape(instance, premiereEtape);

        // Journaliser dans l'audit infalsifiable
        enregistrerAudit(instance, premiereEtape.getCodeEtape(), initiateur.getId(), ActionWorkflowAudit.INITIATION, null, "Initiation du circuit de validation");

        log.info("Circuit de validation démarré avec succès. Instance: {}, Entité: {}/{}", instance.getId(), instance.getEntityName(), instance.getEntityId());
        return mapToInstanceResponse(instance);
    }

    @Transactional
    public WorkflowTacheResponse traiterTache(UUID tacheId, TraiterTacheWorkflowRequest request) {
        WorkflowTacheEntity tache = workflowTacheRepository.findById(tacheId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche de workflow introuvable: " + tacheId));

        if (tache.getStatut() != StatutWorkflowTache.EN_ATTENTE) {
            throw new IllegalStateException("Cette tâche a déjà été traitée (Statut: " + tache.getStatut() + ")");
        }

        UUID acteurId = request.getActeurId() != null ? request.getActeurId() : tache.getAssigneA().getId();

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

        if (request.getDecision() == DecisionWorkflow.REJETE) {
            instance.setStatut(StatutWorkflowInstance.REJETE);
            instance.setDateCloture(Instant.now());
            workflowInstanceRepository.save(instance);
            log.info("Workflow instance {} REJETÉE à l'étape {}", instance.getId(), etapeCourante.getCodeEtape());
        } else if (request.getDecision() == DecisionWorkflow.DEMANDE_REVISION) {
            instance.setStatut(StatutWorkflowInstance.SUSPENDU);
            workflowInstanceRepository.save(instance);
            log.info("Workflow instance {} : Demande de révision à l'étape {}", instance.getId(), etapeCourante.getCodeEtape());
        } else if (request.getDecision() == DecisionWorkflow.APPROUVE) {
            // Passer à l'étape suivante
            passerAEtapeSuivante(instance, etapeCourante);
        }

        return mapToTacheResponse(tache);
    }

    private void passerAEtapeSuivante(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etapeCourante) {
        List<WorkflowEtapeDefinitionEntity> etapes = workflowEtapeRepository
                .findByWorkflowDefinitionIdOrderByOrdreAsc(instance.getWorkflowDefinition().getId());

        Optional<WorkflowEtapeDefinitionEntity> suivanteOpt = etapes.stream()
                .filter(e -> e.getOrdre() > etapeCourante.getOrdre())
                .findFirst();

        if (suivanteOpt.isPresent()) {
            WorkflowEtapeDefinitionEntity suivante = suivanteOpt.get();
            instance.setEtapeCourante(suivante);
            workflowInstanceRepository.save(instance);

            creerTachePourEtape(instance, suivante);
            log.info("Workflow instance {} avance à l'étape: {}", instance.getId(), suivante.getCodeEtape());
        } else {
            // Toutes les étapes sont franchies avec succès
            instance.setStatut(StatutWorkflowInstance.APPROUVE);
            instance.setDateCloture(Instant.now());
            workflowInstanceRepository.save(instance);

            enregistrerAudit(instance, etapeCourante.getCodeEtape(), instance.getInitiateur().getId(), ActionWorkflowAudit.CLOTURE, DecisionWorkflow.APPROUVE, "Circuit approuvé dans sa totalité");
            log.info("Workflow instance {} TOTALEMENT APPROUVÉE !", instance.getId());
        }
    }

    private void creerTachePourEtape(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etape) {
        UtilisateurEntity assigne = resoudreUtilisateurAssigne(instance, etape);

        Instant echeance = null;
        if (etape.getDelaiSlaHeures() != null && etape.getDelaiSlaHeures() > 0) {
            echeance = Instant.now().plus(etape.getDelaiSlaHeures(), ChronoUnit.HOURS);
        }

        WorkflowTacheEntity tache = WorkflowTacheEntity.builder()
                .id(UUID.randomUUID())
                .workflowInstance(instance)
                .etapeDefinition(etape)
                .assigneA(assigne)
                .statut(StatutWorkflowTache.EN_ATTENTE)
                .dateCreation(Instant.now())
                .dateEcheance(echeance)
                .build();

        workflowTacheRepository.save(tache);
    }

    private UtilisateurEntity resoudreUtilisateurAssigne(WorkflowInstanceEntity instance, WorkflowEtapeDefinitionEntity etape) {
        // Fallback par défaut: initiateur ou premier utilisateur actif
        return userRepository.findAll().stream()
                .filter(UtilisateurEntity::isActif)
                .findFirst()
                .orElse(instance.getInitiateur());
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

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse getInstanceById(UUID instanceId) {
        WorkflowInstanceEntity entity = workflowInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance de workflow introuvable: " + instanceId));
        return mapToInstanceResponse(entity);
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
                .entityName(e.getEntityName())
                .entityId(e.getEntityId())
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
                .entityName(t.getWorkflowInstance().getEntityName())
                .entityId(t.getWorkflowInstance().getEntityId())
                .etapeCode(t.getEtapeDefinition() != null ? t.getEtapeDefinition().getCodeEtape() : null)
                .etapeIntitule(t.getEtapeDefinition() != null ? t.getEtapeDefinition().getIntitule() : null)
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

    private WorkflowDefinitionResponse mapToDefinitionResponse(WorkflowDefinitionEntity def) {
        List<WorkflowDefinitionResponse.WorkflowEtapeResponse> etapes = def.getEtapes().stream()
                .map(et -> WorkflowDefinitionResponse.WorkflowEtapeResponse.builder()
                        .id(et.getId())
                        .ordre(et.getOrdre())
                        .codeEtape(et.getCodeEtape())
                        .intitule(et.getIntitule())
                        .typeEtape(et.getTypeEtape() != null ? et.getTypeEtape().name() : null)
                        .politiqueValidation(et.getPolitiqueValidation() != null ? et.getPolitiqueValidation().name() : null)
                        .strategieAssignation(et.getStrategieAssignation() != null ? et.getStrategieAssignation().name() : null)
                        .valeurAssignation(et.getValeurAssignation())
                        .delaiSlaHeures(et.getDelaiSlaHeures())
                        .build())
                .collect(Collectors.toList());

        return WorkflowDefinitionResponse.builder()
                .id(def.getId())
                .code(def.getCode())
                .intitule(def.getIntitule())
                .description(def.getDescription())
                .domaine(def.getDomaine())
                .version(def.getVersion())
                .actif(def.isActif())
                .etapes(etapes)
                .build();
    }
}
