package com.sodepa.erp.rh.application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.application.WorkflowStateService;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.presentation.responses.WorkflowCurrentDataResponse;
import com.sodepa.erp.organigramme.infrastructure.entities.DerogationPermissionEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutDerogation;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.DerogationPermissionRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.organigramme.presentation.requests.AccorderDerogationRequest;
import com.sodepa.erp.organigramme.presentation.responses.DerogationPermissionResponse;
import com.sodepa.erp.rh.infrastructure.entities.*;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.rh.infrastructure.repo.GrilleSalarialeRepository;
import com.sodepa.erp.rh.presentation.requests.CreerEmployeRequest;
import com.sodepa.erp.rh.presentation.requests.ModifierEmployeRequest;
import com.sodepa.erp.rh.presentation.responses.EmployeResponse;
import com.sodepa.erp.rh.presentation.responses.GrilleSalarialeResponse;
import com.sodepa.erp.rh.infrastructure.repo.ContratTravailRepository;
import com.sodepa.erp.rh.presentation.requests.ApprobationContratDgRequest;
import com.sodepa.erp.rh.presentation.responses.ContratTravailResponse;
import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.presentation.requests.DemarrerWorkflowRequest;
import com.sodepa.erp.share.CodeSequenceService;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `RhService` au sein de la SODEPA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RhService {

    private final EmployeRepository employeRepository;
    private final GrilleSalarialeRepository grilleSalarialeRepository;
    private final UniteOrganisationnelleRepository uniteRepository;
    private final PosteRepository posteRepository;
    private final UserRepository userRepository;
    private final DerogationPermissionRepository derogationPermissionRepository;
    private final ClickHouseManager clickHouseManager;
    private final UtilsService utilsService;
    private final ObjectMapper objectMapper;
        private final ContratTravailRepository contratTravailRepository;
    private final WorkflowService workflowService;
    private final WorkflowStateService workflowStateService;

    private static final List<String> EMPLOYE_WORKFLOW_ENTITY_NAMES = List.of(
            "EMPLOYE",
            "RH_EMPLOYE",
            EmployeEntity.class.getSimpleName(),
            EmployeEntity.class.getName()
    );

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.sodepa.erp.authentication.application.ports.KeycloakProvisioningPort keycloakProvisioningPort;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CodeSequenceService codeSequenceService;

    /**
     * Résout le compte utilisateur associé à un employé ou en crée un avec un identifiant IAM valide.
     */
    private UtilisateurEntity resolveOrCreateUtilisateur(CreerEmployeRequest req, boolean failIfUserIdMissing) {
        UtilisateurEntity utilisateur = null;
        if (req.getUtilisateurId() != null) {
            utilisateur = failIfUserIdMissing
                    ? userRepository.findById(req.getUtilisateurId())
                            .orElseThrow(() -> new IllegalArgumentException("Compte utilisateur introuvable avec l'ID: " + req.getUtilisateurId()))
                    : userRepository.findById(req.getUtilisateurId()).orElse(null);
        } else if (req.getEmail() != null && !req.getEmail().isBlank()) {
            utilisateur = userRepository.findByEmail(req.getEmail().trim()).orElse(null);
        }

        if (utilisateur != null) {
            return utilisateur;
        }

        String prenomClean = req.getPrenom() != null ? req.getPrenom().trim() : "";
        String nomClean = req.getNom() != null ? req.getNom().trim() : "";
        String baseEmail = (req.getEmail() != null && !req.getEmail().isBlank())
                ? req.getEmail().trim().toLowerCase()
                : (prenomClean.toLowerCase().replaceAll("\\s+", ".") + "."
                        + nomClean.toLowerCase().replaceAll("\\s+", ".") + "@sodepa.cm");
        String username = baseEmail.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
        if (userRepository.findByUsername(username).isPresent()) {
            username = username + "." + CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_employe", employeRepository::count);
        }
        final String finalUsername = username;
        return userRepository.findByEmail(baseEmail).orElseGet(() -> {
            UUID userId = UUID.randomUUID();
            UUID iamId = provisionKeycloakSafe(userId, finalUsername, baseEmail, prenomClean, nomClean);
            UtilisateurEntity u = UtilisateurEntity.builder()
                    .id(userId)
                    .username(finalUsername)
                    .nom(nomClean)
                    .prenom(prenomClean)
                    .email(baseEmail)
                    .iam(iamId)
                    .photoProfile("")
                    .actif(true)
                    .telephones(req.getTelephone() != null && !req.getTelephone().isBlank()
                            ? java.util.Set.of(req.getTelephone().trim())
                            : java.util.Collections.emptySet())
                    .permissions(new java.util.HashSet<>())
                    .build();
            return userRepository.save(u);
        });
    }

    /**
     * Provisionne l'utilisateur dans Keycloak si disponible, avec repli sur un UUID technique local.
     */
    private UUID provisionKeycloakSafe(UUID userId, String username, String email, String prenom, String nom) {
        if (keycloakProvisioningPort == null) {
            return userId;
        }
        try {
            UUID iamId = keycloakProvisioningPort.createKeycloakUser(userId, username, email, prenom, nom, true);
            return iamId != null ? iamId : userId;
        } catch (Exception e) {
            log.warn("Provisionnement Keycloak indisponible pour {} ({}), utilisation d'un IAM technique local", username, e.getMessage());
            return userId;
        }
    }

    public boolean canValidate(Permissions permission) {
        try {
            utilsService.hasPermission(permission);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<GrilleSalarialeResponse> getGrilleSalariale() {
        return grilleSalarialeRepository.findByOrderByCategorieAscEchelonAsc().stream()
                .map(this::mapToGrilleResponse)
                .collect(Collectors.toList());
    }

    private boolean hasRhReadPrivileges() {
        if (utilsService == null || org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
            return true;
        }
        return utilsService.hasAnyRoleOrPermission(
                java.util.List.of("ADMIN", "DRH", "DAF", "DG"),
                Permissions.LIST_EMPLOYE,
                Permissions.VIEW_EMPLOYE,
                Permissions.CREATE_EMPLOYE,
                Permissions.UPDATE_EMPLOYE,
                Permissions.VALIDATE_EMPLOYE,
                Permissions.DELETE_EMPLOYE
        );
    }

    private void assertCanReadEmploye(EmployeEntity employe) {
        if (hasRhReadPrivileges()) {
            return;
        }
        UUID currentUserId = utilsService.getCurrentUserIdOrNull();
        UUID ownerUserId = (employe != null && employe.getUtilisateur() != null) ? employe.getUtilisateur().getId() : null;
        if (currentUserId != null && currentUserId.equals(ownerUserId)) {
            return;
        }
        throw new AccesRefuseException("Accès refusé aux données RH confidentielles (RIB/CNPS/salaire) d'un autre collaborateur.");
    }

    @Transactional
    public List<EmployeResponse> listerEmployes(UUID uniteId, StatutActivite statut, String recherche) {
        boolean privileged = hasRhReadPrivileges();
        UUID currentUserId = utilsService != null ? utilsService.getCurrentUserIdOrNull() : null;

        Map<UUID, WorkflowInstanceEntity> activeById = workflowStateService.findActiveInstancesByEntityUuid(EMPLOYE_WORKFLOW_ENTITY_NAMES);
        java.util.Set<UUID> materializedIds = new java.util.HashSet<>();
        List<EmployeResponse> responses = new java.util.ArrayList<>();

        List<EmployeEntity> sourceList;
        if (!privileged) {
            sourceList = currentUserId != null
                    ? employeRepository.findByUtilisateurId(currentUserId).map(List::of).orElseGet(List::of)
                    : List.of();
        } else if (uniteId != null) {
            sourceList = employeRepository.findByUniteOrganisationnelleId(uniteId);
        } else if (statut != null) {
            sourceList = employeRepository.findByStatutActivite(statut);
        } else {
            sourceList = employeRepository.findAll();
        }

        for (EmployeEntity e : sourceList) {
            materializedIds.add(e.getId());
            boolean matchUnite = uniteId == null || (e.getUniteOrganisationnelle() != null && e.getUniteOrganisationnelle().getId().equals(uniteId));
            boolean matchStatut = statut == null || e.getStatutActivite() == statut;
            boolean matchSearch = matchesEmployeSearch(e.getNom(), e.getPrenom(), e.getMatricule(), recherche);
            if (!matchUnite || !matchStatut || !matchSearch) {
                continue;
            }
            EmployeResponse res = mapToEmployeResponse(e);
            WorkflowInstanceEntity activeWf = activeById.get(e.getId());
            if (activeWf != null) {
                enrichirEmployeAvecWorkflow(res, activeWf);
            }
            responses.add(res);
        }

        // Employés en cours de recrutement (ClickHouse / Outbox, pas encore matérialisés dans rh_employes)
        for (Map.Entry<UUID, WorkflowInstanceEntity> entry : activeById.entrySet()) {
            if (materializedIds.contains(entry.getKey())) {
                continue;
            }
            WorkflowInstanceEntity wf = entry.getValue();
            if (!privileged && (currentUserId == null || wf.getInitiateur() == null || !currentUserId.equals(wf.getInitiateur().getId()))) {
                continue;
            }
            WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
            Map<String, Object> data = current.getData() != null ? current.getData() : new java.util.LinkedHashMap<>();
            EmployeResponse draft = buildDraftEmployeFromWorkflow(entry.getKey(), wf, data, current);
            boolean matchUnite = uniteId == null || uniteId.equals(draft.getUniteId());
            boolean matchStatut = statut == null || statut == draft.getStatutActivite();
            boolean matchSearch = matchesEmployeSearch(draft.getNom(), draft.getPrenom(), draft.getMatricule(), recherche);
            if (matchUnite && matchStatut && matchSearch) {
                responses.add(draft);
            }
        }

        return responses;
    }

    @Transactional
    public EmployeResponse getEmployeById(UUID id) {
        java.util.Optional<EmployeEntity> opt = employeRepository.findById(id);
        java.util.Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService.findActiveInstanceForEntity(EMPLOYE_WORKFLOW_ENTITY_NAMES, id);
        if (opt.isPresent()) {
            assertCanReadEmploye(opt.get());
            EmployeResponse res = mapToEmployeResponse(opt.get());
            activeWfOpt.ifPresent(wf -> enrichirEmployeAvecWorkflow(res, wf));
            return res;
        }
        if (activeWfOpt.isPresent()) {
            WorkflowInstanceEntity wf = activeWfOpt.get();
            if (!hasRhReadPrivileges()) {
                UUID currentUserId = utilsService.getCurrentUserIdOrNull();
                if (currentUserId == null || wf.getInitiateur() == null || !currentUserId.equals(wf.getInitiateur().getId())) {
                    throw new AccesRefuseException("Accès refusé aux données RH d'un recrutement en cours.");
                }
            }
            WorkflowCurrentDataResponse current = workflowStateService.getCurrentData(wf);
            Map<String, Object> data = current.getData() != null ? current.getData() : new java.util.LinkedHashMap<>();
            return buildDraftEmployeFromWorkflow(id, wf, data, current);
        }
        throw new IllegalArgumentException("Employé introuvable avec l'ID: " + id);
    }

    @Transactional
    public EmployeResponse getEmployeByMatricule(String matricule) {
        java.util.Optional<EmployeEntity> opt = employeRepository.findByMatricule(matricule);
        if (opt.isPresent()) {
            EmployeEntity e = opt.get();
            assertCanReadEmploye(e);
            EmployeResponse res = mapToEmployeResponse(e);
            workflowStateService.findActiveInstanceForEntity(EMPLOYE_WORKFLOW_ENTITY_NAMES, e.getId())
                    .ifPresent(wf -> enrichirEmployeAvecWorkflow(res, wf));
            return res;
        }
        if (matricule != null && !matricule.isBlank()) {
            boolean privileged = hasRhReadPrivileges();
            UUID currentUserId = utilsService != null ? utilsService.getCurrentUserIdOrNull() : null;
            Map<UUID, WorkflowInstanceEntity> activeById = workflowStateService.findActiveInstancesByEntityUuid(EMPLOYE_WORKFLOW_ENTITY_NAMES);
            for (Map.Entry<UUID, WorkflowInstanceEntity> entry : activeById.entrySet()) {
                WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(entry.getValue());
                Map<String, Object> data = current.getData() != null ? current.getData() : new java.util.LinkedHashMap<>();
                if (matricule.equalsIgnoreCase(asString(data.get("matricule")))) {
                    WorkflowInstanceEntity wf = entry.getValue();
                    if (!privileged && (currentUserId == null || wf.getInitiateur() == null || !currentUserId.equals(wf.getInitiateur().getId()))) {
                        throw new AccesRefuseException("Accès refusé aux données RH confidentielles.");
                    }
                    return buildDraftEmployeFromWorkflow(entry.getKey(), entry.getValue(), data, current);
                }
            }
        }
        throw new IllegalArgumentException("Employé introuvable avec le matricule: " + matricule);
    }

    @Transactional
    public EmployeResponse creerEmploye(CreerEmployeRequest req) {
        utilsService.hasPermission(Permissions.CREATE_EMPLOYE);

        // =========================================================================
        // ÉTAPE 0 : Contrôle d'éligibilité du Maker et existence du Workflow
        // =========================================================================
        if (utilsService.getCurrentUserData() != null && utilsService.getCurrentUserData().userId() != null) {
            try {
                UUID.fromString(utilsService.getCurrentUserData().userId());
                workflowService.verifierEligibiliteMaker("WF-RH-RECRUTEMENT");
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                log.warn("Contrôle Maker workflow pour le recrutement : {}", e.getMessage());
                throw e;
            }
        }

        // =========================================================================
        // ÉTAPE 1 : Pré-validations strictes sans aucune écriture prématurée PostgreSQL / Keycloak
        // =========================================================================
        UtilisateurEntity existingUser = null;
        if (req.getUtilisateurId() != null) {
            existingUser = userRepository.findById(req.getUtilisateurId())
                    .orElseThrow(() -> new IllegalArgumentException("Compte utilisateur introuvable avec l'ID: " + req.getUtilisateurId()));
        } else if (req.getEmail() != null && !req.getEmail().isBlank()) {
            existingUser = userRepository.findByEmail(req.getEmail().trim()).orElse(null);
        }

        if (existingUser != null) {
            if ("admin_maker".equalsIgnoreCase(existingUser.getUsername())
                    || "admin_checker".equalsIgnoreCase(existingUser.getUsername())) {
                throw new IllegalArgumentException("Les comptes racines d'initialisation système ('admin_maker', 'admin_checker') ne peuvent pas être des employés SODEPA.");
            }
            if (employeRepository.existsByUtilisateurId(existingUser.getId())) {
                throw new IllegalArgumentException("L'utilisateur " + existingUser.getUsername() + " possède déjà un dossier administratif employé actif.");
            }
        }

        // Résolution de la grille indiciaire, unité et poste en lecture seule
        GrilleSalarialeEntity grille = grilleSalarialeRepository
                .findByCategorieAndEchelon(req.getCategorie(), req.getEchelon().toUpperCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Échelon " + req.getEchelon() + " invalide pour la catégorie " + req.getCategorie()));

        UniteOrganisationnelleEntity unite = null;
        if (req.getUniteOrganisationnelleId() != null) {
            unite = uniteRepository.findById(req.getUniteOrganisationnelleId())
                    .orElseThrow(() -> new IllegalArgumentException("Unité organisationnelle introuvable"));
        }

        PosteEntity poste = null;
        if (req.getPosteId() != null) {
            poste = posteRepository.findById(req.getPosteId())
                    .orElseThrow(() -> new IllegalArgumentException("Poste introuvable"));
        }

        // =========================================================================
        // ÉTAPE 2 : Préparation des identifiants et état initial ClickHouse (zéro INSERT métier Postgres)
        // =========================================================================
        long activeCount = workflowStateService.findActiveInstancesByEntityUuid(EMPLOYE_WORKFLOW_ENTITY_NAMES).size();
        long count = employeRepository.count() + activeCount + 1;
        String matricule = String.format("SDP-%d-%04d", LocalDate.now().getYear(), count);
        while (employeRepository.findByMatricule(matricule).isPresent()) {
            count++;
            matricule = String.format("SDP-%d-%04d", LocalDate.now().getYear(), count);
        }
        UUID employeId = UUID.randomUUID();

        long contratCount = contratTravailRepository.count() + activeCount + 1;
        String numeroContrat = String.format("CT-%d-%04d", LocalDate.now().getYear(), contratCount);
        while (contratTravailRepository.findByNumeroContrat(numeroContrat).isPresent()) {
            contratCount++;
            numeroContrat = String.format("CT-%d-%04d", LocalDate.now().getYear(), contratCount);
        }
        UUID contratId = UUID.randomUUID();
        TypeContrat typeContrat = req.getTypeContrat() != null ? req.getTypeContrat() : TypeContrat.CDI;

        Map<String, Object> payload = new java.util.LinkedHashMap<>(objectMapper.convertValue(req, Map.class));
        payload.put("id", employeId.toString());
        payload.put("matricule", matricule);
        payload.put("contratId", contratId.toString());
        payload.put("numeroContrat", numeroContrat);
        payload.put("typeContrat", typeContrat.name());
        payload.put("statutActivite", StatutActivite.EN_FONCTION.name());
        payload.put("statutContrat", StatutContrat.EN_ATTENTE_VALIDATION.name());
        payload.put("grilleSalarialeId", grille.getId().toString());
        payload.put("categorie", grille.getCategorie());
        payload.put("echelon", grille.getEchelon());
        payload.put("groupe", grille.getGroupe());
        payload.put("salaireBase", grille.getSalaireBase());
        payload.put("tauxHoraire", grille.getTauxHoraire());
        if (existingUser != null) {
            payload.put("utilisateurId", existingUser.getId().toString());
            payload.put("username", existingUser.getUsername());
        }
        if (unite != null) {
            payload.put("uniteCode", unite.getCode());
            payload.put("uniteIntitule", unite.getIntitule());
        }
        if (poste != null) {
            payload.put("posteCode", poste.getCodePoste());
            payload.put("posteIntitule", poste.getIntitule());
        }

        // =========================================================================
        // ÉTAPE 3 : Soumission unifiée vers le Moteur Workflow (instance unique WF-RH-RECRUTEMENT)
        // =========================================================================
        UUID ticketUuid = workflowService.submitChange(
                MakerCheckerEntityName.EMPLOYE,
                employeId.toString(),
                payload,
                MakerCheckerOperationType.CREATE,
                "WF-RH-RECRUTEMENT"
        );

        WorkflowInstanceEntity activeWf = workflowStateService
                .findActiveInstanceForEntity(EMPLOYE_WORKFLOW_ENTITY_NAMES, employeId)
                .orElse(null);
        UUID wfInstanceId = activeWf != null ? activeWf.getId() : null;

        trackClickHouse(MakerCheckerEntityName.EMPLOYE.name(), employeId.toString(), Map.of(
                "action", "SOUMISSION_CREATION_EMPLOYE_WORKFLOW",
                "matricule", matricule,
                "nom", req.getNom(),
                "prenom", req.getPrenom(),
                "numeroContrat", numeroContrat,
                "ticketMakerChecker", ticketUuid.toString(),
                "workflowInstanceId", wfInstanceId != null ? wfInstanceId.toString() : "N/A"
        ));

        EmployeResponse draftResp = buildDraftEmployeFromWorkflow(employeId, activeWf, payload, null);
        draftResp.setMakerCheckerTicketId(ticketUuid);
        log.info("Dossier recrutement {} ({}) initié dans ClickHouse sans écriture anticipée PostgreSQL (Instance: {})",
                matricule, employeId, wfInstanceId);
        return draftResp;
    }

    @Transactional
    public EmployeResponse modifierEmploye(UUID id, ModifierEmployeRequest req) {
        utilsService.hasPermission(Permissions.UPDATE_EMPLOYE);
        EmployeEntity entity = employeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable avec l'ID: " + id));

        if (workflowStateService.findActiveInstanceForEntity(EMPLOYE_WORKFLOW_ENTITY_NAMES, id).isPresent()) {
            throw new ConflitMetierException("Une demande de modification est déjà en cours de validation pour cet employé.");
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>(objectMapper.convertValue(req, Map.class));
        payload.put("id", id.toString());

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.EMPLOYE,
                id.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Modification employé soumise au workflow (ClickHouse State Store): {} (Ticket: {})", entity.getMatricule(), uuid);

        trackClickHouse(MakerCheckerEntityName.EMPLOYE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_MODIFIER_EMPLOYE",
                "matricule", entity.getMatricule(),
                "ticket", uuid.toString()
        ));

        EmployeResponse response = mapToEmployeResponse(entity);
        workflowStateService.findActiveInstanceForEntity(EMPLOYE_WORKFLOW_ENTITY_NAMES, id)
                .ifPresent(wf -> enrichirEmployeAvecWorkflow(response, wf));
        return response;
    }

    @Transactional
    public void supprimerEmploye(UUID id) {
        utilsService.hasPermission(Permissions.DELETE_EMPLOYE);
        EmployeEntity entity = employeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable avec l'ID: " + id));

        if (workflowStateService.findActiveInstanceForEntity(EMPLOYE_WORKFLOW_ENTITY_NAMES, id).isPresent()) {
            throw new ConflitMetierException("Un circuit de validation est déjà en cours pour cet employé.");
        }

        Map<String, Object> payload = Map.of(
                "id", id.toString(),
                "matricule", entity.getMatricule()
        );

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.EMPLOYE,
                id.toString(),
                payload,
                MakerCheckerOperationType.DELETE
        );
        log.info("Suppression employé soumise au workflow (zéro suppression anticipée Postgres): {} (Ticket: {})", entity.getMatricule(), uuid);

        trackClickHouse(MakerCheckerEntityName.EMPLOYE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_SUPPRIMER_EMPLOYE",
                "matricule", entity.getMatricule(),
                "ticket", uuid.toString()
        ));
    }

    @Transactional
    public void validateOrRejectEmploye(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_EMPLOYE);
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(utilsService.getCurrentUserData().userId());
        if (checkerOutput.maker() != null && checkerOutput.maker().id() != null && checkerOutput.maker().id().equals(checkerId)) {
            throw new RuntimeException("A checker cannot vote on their own request.");
        }

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }

        workflowService.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            workflowService.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
            log.info("Opération Employé rejetée par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID employeId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                if (!employeRepository.existsById(employeId)) {
                    creationEmploye(checkerOutput.payload(), employeId);
                }
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                updateEmploye(checkerOutput.payload(), employeId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.DELETE) {
                deleteEmploye(employeId);
            }
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Opération Employé approuvée par le Checker {}. Ticket: {}, Employé PK: {}", checkerId, checkerOutput.id(), employeId);
        }
    }

    private <T> T convertPayloadSafe(Map<String, Object> payload, Class<T> targetClass) {
        return objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .convertValue(payload, targetClass);
    }

    private EmployeEntity creationEmploye(Map<String, Object> payload, UUID employeId) {
        CreerEmployeRequest req = convertPayloadSafe(payload, CreerEmployeRequest.class);
        String matricule = asString(payload.get("matricule"));
        if (matricule == null || matricule.isBlank() || employeRepository.findByMatricule(matricule).isPresent()) {
            long seq = CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_employe", employeRepository::count);
            matricule = String.format("SDP-%d-%04d", LocalDate.now().getYear(), seq);
            while (employeRepository.findByMatricule(matricule).isPresent()) {
                seq++;
                matricule = String.format("SDP-%d-%04d", LocalDate.now().getYear(), seq);
            }
        }

        UtilisateurEntity utilisateur = resolveOrCreateUtilisateur(req, false);

        GrilleSalarialeEntity grille = null;
        UUID grilleId = parseUuidSafe(asString(payload.get("grilleSalarialeId")));
        if (grilleId != null) {
            grille = grilleSalarialeRepository.findById(grilleId).orElse(null);
        }
        if (grille == null && req.getCategorie() != null && req.getEchelon() != null) {
            grille = grilleSalarialeRepository
                    .findByCategorieAndEchelon(req.getCategorie(), req.getEchelon().toUpperCase().trim())
                    .orElse(null);
        }

        UniteOrganisationnelleEntity unite = null;
        if (req.getUniteOrganisationnelleId() != null) {
            unite = uniteRepository.findById(req.getUniteOrganisationnelleId()).orElse(null);
        }

        PosteEntity poste = null;
        if (req.getPosteId() != null) {
            poste = posteRepository.findById(req.getPosteId()).orElse(null);
        }

        EmployeEntity entity = EmployeEntity.builder()
                .id(employeId)
                .matricule(matricule)
                .nom(req.getNom() != null ? req.getNom().trim() : "")
                .prenom(req.getPrenom() != null ? req.getPrenom().trim() : "")
                .sexe(req.getSexe())
                .dateNaissance(req.getDateNaissance())
                .lieuNaissance(req.getLieuNaissance())
                .situationMatrimoniale(req.getSituationMatrimoniale() != null ? req.getSituationMatrimoniale() : SituationMatrimoniale.CELIBATAIRE)
                .nombreEnfantsACharge(req.getNombreEnfantsACharge() != null ? req.getNombreEnfantsACharge() : 0)
                .email(req.getEmail() != null && !req.getEmail().isBlank() ? req.getEmail().trim() : (utilisateur != null ? utilisateur.getEmail() : null))
                .telephone(req.getTelephone())
                .adresse(req.getAdresse())
                .numeroCnps(req.getNumeroCnps())
                .compteBancaireRib(req.getCompteBancaireRib())
                .banqueNom(req.getBanqueNom())
                .typeContrat(req.getTypeContrat() != null ? req.getTypeContrat() : TypeContrat.CDI)
                .dateEmbauche(req.getDateEmbauche() != null ? req.getDateEmbauche() : LocalDate.now())
                .dateFinEssai(req.getDateFinEssai())
                .dateFinContrat(req.getDateFinContrat())
                .statutActivite(StatutActivite.EN_FONCTION)
                .uniteOrganisationnelle(unite)
                .poste(poste)
                .grilleSalariale(grille)
                .utilisateur(utilisateur)
                .build();

        EmployeEntity saved = employeRepository.save(entity);

        if (contratTravailRepository.findFirstByEmployeIdOrderByDateCreationDesc(employeId).isEmpty()) {
            UUID contratId = parseUuidSafe(asString(payload.get("contratId")));
            if (contratId == null) {
                contratId = UUID.randomUUID();
            }
            String numeroContrat = asString(payload.get("numeroContrat"));
            if (numeroContrat == null || numeroContrat.isBlank() || contratTravailRepository.findByNumeroContrat(numeroContrat).isPresent()) {
                long cSeq = CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_contrat_travail", contratTravailRepository::count);
                numeroContrat = String.format("CT-%d-%04d", LocalDate.now().getYear(), cSeq);
                while (contratTravailRepository.findByNumeroContrat(numeroContrat).isPresent()) {
                    cSeq++;
                    numeroContrat = String.format("CT-%d-%04d", LocalDate.now().getYear(), cSeq);
                }
            }
            UUID wfId = workflowStateService.findActiveInstanceForEntity(EMPLOYE_WORKFLOW_ENTITY_NAMES, employeId)
                    .map(WorkflowInstanceEntity::getId)
                    .orElse(parseUuidSafe(asString(payload.get("workflowInstanceId"))));
            UUID ticketId = parseUuidSafe(asString(payload.get("makerCheckerRequestId")));

            ContratTravailEntity contrat = ContratTravailEntity.builder()
                    .id(contratId)
                    .numeroContrat(numeroContrat)
                    .employe(saved)
                    .typeContrat(saved.getTypeContrat())
                    .statut(StatutContrat.APPROUVE_DG)
                    .dateDebut(saved.getDateEmbauche())
                    .dateFinEssai(saved.getDateFinEssai())
                    .dateFinContrat(saved.getDateFinContrat())
                    .categorie(grille != null ? grille.getCategorie() : req.getCategorie())
                    .echelon(grille != null ? grille.getEchelon() : req.getEchelon())
                    .salaireBase(grille != null ? grille.getSalaireBase() : null)
                    .uniteOrganisationnelle(unite)
                    .poste(poste)
                    .signeParDg(true)
                    .dateSignatureDg(LocalDate.now())
                    .referenceNoteServiceDg(asString(payload.get("referenceNoteServiceDg")))
                    .workflowInstanceId(wfId)
                    .makerCheckerTicketId(ticketId)
                    .build();
            contratTravailRepository.save(contrat);
        }

        log.info("Employé et contrat matérialisés en PostgreSQL à l'approbation finale: {} ({})", saved.getId(), saved.getMatricule());
        return saved;
    }

    private EmployeEntity updateEmploye(Map<String, Object> payload, UUID employeId) {
        ModifierEmployeRequest req = convertPayloadSafe(payload, ModifierEmployeRequest.class);
        EmployeEntity entity = employeRepository.findById(employeId)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable avec l'ID: " + employeId));

        if (req.getNom() != null && !req.getNom().isBlank()) entity.setNom(req.getNom().trim());
        if (req.getPrenom() != null && !req.getPrenom().isBlank()) entity.setPrenom(req.getPrenom().trim());
        if (req.getSituationMatrimoniale() != null) entity.setSituationMatrimoniale(req.getSituationMatrimoniale());
        if (req.getNombreEnfantsACharge() != null) entity.setNombreEnfantsACharge(req.getNombreEnfantsACharge());
        if (req.getEmail() != null) entity.setEmail(req.getEmail());
        if (req.getTelephone() != null) entity.setTelephone(req.getTelephone());
        if (req.getAdresse() != null) entity.setAdresse(req.getAdresse());
        if (req.getNumeroCnps() != null) entity.setNumeroCnps(req.getNumeroCnps());
        if (req.getCompteBancaireRib() != null) entity.setCompteBancaireRib(req.getCompteBancaireRib());
        if (req.getBanqueNom() != null) entity.setBanqueNom(req.getBanqueNom());
        if (req.getTypeContrat() != null) entity.setTypeContrat(req.getTypeContrat());
        if (req.getStatutActivite() != null) entity.setStatutActivite(req.getStatutActivite());
        if (req.getDateFinEssai() != null) entity.setDateFinEssai(req.getDateFinEssai());
        if (req.getDateFinContrat() != null) entity.setDateFinContrat(req.getDateFinContrat());

        if (req.getUniteOrganisationnelleId() != null) {
            UniteOrganisationnelleEntity u = uniteRepository.findById(req.getUniteOrganisationnelleId())
                    .orElseThrow(() -> new IllegalArgumentException("Unité organisationnelle introuvable"));
            entity.setUniteOrganisationnelle(u);
        }

        if (req.getPosteId() != null) {
            PosteEntity p = posteRepository.findById(req.getPosteId())
                    .orElseThrow(() -> new IllegalArgumentException("Poste introuvable"));
            entity.setPoste(p);
        }

        if (req.getCategorie() != null && req.getEchelon() != null) {
            GrilleSalarialeEntity g = grilleSalarialeRepository
                    .findByCategorieAndEchelon(req.getCategorie(), req.getEchelon().toUpperCase().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Échelon invalide"));
            entity.setGrilleSalariale(g);
        }

        return employeRepository.save(entity);
    }

    private void deleteEmploye(UUID employeId) {
        employeRepository.findById(employeId).ifPresent(employeRepository::delete);
    }

    // ==========================================
    // PERMISSIONS DÉROGÉES (DG / DIRECTION)
    // ==========================================

    @Transactional
    public DerogationPermissionResponse accorderDerogation(AccorderDerogationRequest req) {
        utilsService.hasPermission(Permissions.CREATE_DEROGATION);

        if (req.getDateFin().isBefore(req.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }

        UtilisateurEntity beneficiaire = userRepository.findById(req.getUtilisateurId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur bénéficiaire introuvable: " + req.getUtilisateurId()));

        if ("admin_maker".equalsIgnoreCase(beneficiaire.getUsername()) || "admin_checker".equalsIgnoreCase(beneficiaire.getUsername())) {
            throw new IllegalArgumentException("Les comptes racines d'initialisation système détiennent déjà tous les privilèges sans dérogation.");
        }

        String accordeParId = null;
        try {
            accordeParId = utilsService.getCurrentUserData().userId();
        } catch (Exception ignored) {
        }

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new java.util.HashMap<>(objectMapper.convertValue(req, Map.class));
        payload.put("id", id.toString());
        payload.put("accordeParId", accordeParId);

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.DEROGATION_PERMISSION,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Octroi de dérogation soumis au Maker-Checker pour {} (Ticket: {})", beneficiaire.getUsername(), uuid);

        trackClickHouse(MakerCheckerEntityName.DEROGATION_PERMISSION.name(), id.toString(), Map.of(
                "action", "SOUMISSION_ACCORDER_DEROGATION",
                "beneficiaire", beneficiaire.getUsername(),
                "permission", req.getPermission().name(),
                "ticket", uuid.toString()
        ));

        return derogationPermissionRepository.findById(id)
                .map(this::mapToDerogationResponse)
                .orElseGet(() -> DerogationPermissionResponse.builder()
                        .id(id)
                        .utilisateurId(beneficiaire.getId())
                        .username(beneficiaire.getUsername())
                        .nomComplet(beneficiaire.getNom() + " " + beneficiaire.getPrenom())
                        .permission(req.getPermission())
                        .dateDebut(req.getDateDebut())
                        .dateFin(req.getDateFin())
                        .statut(StatutDerogation.ACTIVE)
                        .motif(req.getMotif())
                        .referenceDecision(req.getReferenceDecision())
                        .accordeParNom("Direction Générale")
                        .dateCreation(Instant.now())
                        .build());
    }

    @Transactional
    public DerogationPermissionResponse revoquerDerogation(UUID derogationId) {
        utilsService.hasPermission(Permissions.UPDATE_DEROGATION);
        DerogationPermissionEntity der = derogationPermissionRepository.findById(derogationId)
                .orElseThrow(() -> new IllegalArgumentException("Dérogation introuvable avec l'ID: " + derogationId));

        Map<String, Object> payload = Map.of(
                "id", derogationId.toString(),
                "action", "REVOQUER"
        );

        UUID uuid = workflowService.submitChange(
                MakerCheckerEntityName.DEROGATION_PERMISSION,
                derogationId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Révocation dérogation soumise au Maker-Checker pour {} (Ticket: {})", der.getUtilisateur().getUsername(), uuid);

        trackClickHouse(MakerCheckerEntityName.DEROGATION_PERMISSION.name(), derogationId.toString(), Map.of(
                "action", "SOUMISSION_REVOQUER_DEROGATION",
                "beneficiaire", der.getUtilisateur().getUsername(),
                "ticket", uuid.toString()
        ));

        return mapToDerogationResponse(derogationPermissionRepository.findById(derogationId).orElse(der));
    }

    @Transactional
    public void validateOrRejectDerogation(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_DEROGATION);
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(utilsService.getCurrentUserData().userId());
        if (checkerOutput.maker() != null && checkerOutput.maker().id() != null && checkerOutput.maker().id().equals(checkerId)) {
            throw new RuntimeException("A checker cannot vote on their own request.");
        }

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }

        workflowService.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            workflowService.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
            log.info("Opération Dérogation rejetée par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID derogationId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationDerogation(checkerOutput.payload(), derogationId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                updateDerogation(checkerOutput.payload(), derogationId);
            }
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Opération Dérogation approuvée par le Checker {}. Ticket: {}, Dérogation PK: {}", checkerId, checkerOutput.id(), derogationId);
        }
    }

    private DerogationPermissionEntity creationDerogation(Map<String, Object> payload, UUID derogationId) {
        AccorderDerogationRequest req = objectMapper.convertValue(payload, AccorderDerogationRequest.class);
        UtilisateurEntity beneficiaire = userRepository.findById(req.getUtilisateurId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur bénéficiaire introuvable: " + req.getUtilisateurId()));

        String accordeParId = (String) payload.get("accordeParId");
        UtilisateurEntity accordeur = null;
        if (accordeParId != null) {
            accordeur = userRepository.findById(UUID.fromString(accordeParId)).orElse(null);
        }

        DerogationPermissionEntity derogation = DerogationPermissionEntity.builder()
                .id(derogationId)
                .utilisateur(beneficiaire)
                .permission(req.getPermission())
                .dateDebut(req.getDateDebut())
                .dateFin(req.getDateFin())
                .statut(StatutDerogation.ACTIVE)
                .motif(req.getMotif())
                .referenceDecision(req.getReferenceDecision())
                .accordePar(accordeur)
                .dateCreation(Instant.now())
                .build();

        return derogationPermissionRepository.save(derogation);
    }

    private DerogationPermissionEntity updateDerogation(Map<String, Object> payload, UUID derogationId) {
        DerogationPermissionEntity der = derogationPermissionRepository.findById(derogationId)
                .orElseThrow(() -> new IllegalArgumentException("Dérogation introuvable avec l'ID: " + derogationId));
        der.setStatut(StatutDerogation.REVOQUEE);
        return derogationPermissionRepository.save(der);
    }

    @Transactional(readOnly = true)
    public List<DerogationPermissionResponse> listerDerogations(UUID utilisateurId) {
        List<DerogationPermissionEntity> list = (utilisateurId != null)
                ? derogationPermissionRepository.findByUtilisateurId(utilisateurId)
                : derogationPermissionRepository.findAll();
        return list.stream().map(this::mapToDerogationResponse).collect(Collectors.toList());
    }

    private void trackClickHouse(String entityName, String entityPk, Object payloadObj) {
        try {
            String actorId = "SYSTEM";
            try {
                actorId = utilsService.getCurrentUserData().userId();
            } catch (Exception ignored) {
            }

            String payloadStr = objectMapper.writeValueAsString(payloadObj);
            clickHouseManager.saveMakerChecker(
                    MakerCheckerMessageInput.builder()
                            .id(UUID.randomUUID())
                            .entityName(entityName)
                            .entityPk(entityPk)
                            .payload(payloadStr)
                            .timestamp(LocalDateTime.now())
                            .maker_id(actorId)
                            .checker_id(actorId)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Journalisation ClickHouse non bloquante ignorée pour {} ({}): {}", entityName, entityPk, e.getMessage());
        }
    }

    private EmployeResponse mapToEmployeResponse(EmployeEntity e) {
        EmployeResponse.EmployeResponseBuilder builder = EmployeResponse.builder()
                .id(e.getId())
                .matricule(e.getMatricule())
                .nom(e.getNom())
                .prenom(e.getPrenom())
                .nomComplet(e.getPrenom() + " " + e.getNom())
                .sexe(e.getSexe())
                .dateNaissance(e.getDateNaissance())
                .lieuNaissance(e.getLieuNaissance())
                .situationMatrimoniale(e.getSituationMatrimoniale())
                .nombreEnfantsACharge(e.getNombreEnfantsACharge())
                .email(e.getEmail())
                .telephone(e.getTelephone())
                .adresse(e.getAdresse())
                .numeroCnps(e.getNumeroCnps())
                .compteBancaireRib(e.getCompteBancaireRib())
                .banqueNom(e.getBanqueNom())
                .typeContrat(e.getTypeContrat())
                .dateEmbauche(e.getDateEmbauche())
                .dateFinEssai(e.getDateFinEssai())
                .dateFinContrat(e.getDateFinContrat())
                .statutActivite(e.getStatutActivite())
                .utilisateurId(e.getUtilisateur() != null ? e.getUtilisateur().getId() : null)
                .username(e.getUtilisateur() != null ? e.getUtilisateur().getUsername() : null)
                .userId(e.getUtilisateur() != null ? e.getUtilisateur().getId().toString() : null)
                .dateCreation(e.getDateCreation());

        if (e.getUniteOrganisationnelle() != null) {
            builder.uniteId(e.getUniteOrganisationnelle().getId())
                   .uniteCode(e.getUniteOrganisationnelle().getCode())
                   .uniteIntitule(e.getUniteOrganisationnelle().getIntitule());
        }

        if (e.getPoste() != null) {
            builder.posteId(e.getPoste().getId())
                   .posteCode(e.getPoste().getCodePoste())
                   .posteIntitule(e.getPoste().getIntitule());
        }

        if (e.getGrilleSalariale() != null) {
            builder.grilleId(e.getGrilleSalariale().getId())
                   .categorie(e.getGrilleSalariale().getCategorie())
                   .echelon(e.getGrilleSalariale().getEchelon())
                   .groupe(e.getGrilleSalariale().getGroupe())
                   .salaireBase(e.getGrilleSalariale().getSalaireBase())
                   .tauxHoraire(e.getGrilleSalariale().getTauxHoraire());
        }

        // Contrat de travail le plus récent
        contratTravailRepository.findFirstByEmployeIdOrderByDateCreationDesc(e.getId()).ifPresent(c -> {
            builder.contratId(c.getId())
                   .numeroContrat(c.getNumeroContrat())
                   .statutContrat(c.getStatut())
                   .referenceNoteServiceDg(c.getReferenceNoteServiceDg())
                   .dateSignatureDg(c.getDateSignatureDg())
                   .signeParDg(c.getSigneParDg())
                   .workflowInstanceId(c.getWorkflowInstanceId())
                   .makerCheckerTicketId(c.getMakerCheckerTicketId());
        });

        return builder.build();
    }

    private DerogationPermissionResponse mapToDerogationResponse(DerogationPermissionEntity d) {
        return DerogationPermissionResponse.builder()
                .id(d.getId())
                .utilisateurId(d.getUtilisateur().getId())
                .username(d.getUtilisateur().getUsername())
                .nomComplet(d.getUtilisateur().getNom() + " " + d.getUtilisateur().getPrenom())
                .permission(d.getPermission())
                .dateDebut(d.getDateDebut())
                .dateFin(d.getDateFin())
                .statut(d.getStatut())
                .motif(d.getMotif())
                .referenceDecision(d.getReferenceDecision())
                .accordeParNom(d.getAccordePar() != null ? d.getAccordePar().getNom() + " " + d.getAccordePar().getPrenom() : "Direction Générale")
                .dateCreation(d.getDateCreation())
                .build();
    }

    private GrilleSalarialeResponse mapToGrilleResponse(GrilleSalarialeEntity g) {
        return GrilleSalarialeResponse.builder()
                .id(g.getId())
                .categorie(g.getCategorie())
                .echelon(g.getEchelon())
                .groupe(g.getGroupe())
                .salaireBase(g.getSalaireBase())
                .tauxHoraire(g.getTauxHoraire())
                .actif(g.getActif())
                .build();
    }

    // =========================================================================
    // GESTION DES CONTRATS ET APPROBATION FINALE DU DIRECTEUR GÉNÉRAL
    // =========================================================================

    @Transactional
    public void finaliserApprobationRecrutementDg(UUID employeId, String commentaire, UUID dgUserId) {
        finaliserApprobationRecrutementDg(employeId, commentaire, dgUserId, null);
    }

    @Transactional
    public void finaliserApprobationRecrutementDg(UUID employeId, String commentaire, UUID dgUserId, Map<String, Object> finalData) {
        if (utilsService != null && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            boolean allowed = utilsService.hasAnyRoleOrPermission(
                    java.util.List.of("ADMIN", "DG", "DRH"),
                    Permissions.VALIDATE_EMPLOYE,
                    Permissions.WORKFLOW_DECISION
            );
            if (!allowed) {
                throw new AccesRefuseException("Seule l'autorité DG/DRH ou un décideur workflow habilité peut finaliser ce recrutement.");
            }
        }
        log.info("Finalisation de l'approbation DG du recrutement pour l'employé: {}", employeId);

        EmployeEntity employe = employeRepository.findById(employeId).orElse(null);
        if (employe == null && finalData != null && !finalData.isEmpty()) {
            employe = creationEmploye(finalData, employeId);
        }
        if (employe == null) {
            log.warn("Employé introuvable pour la finalisation DG: {}", employeId);
            return;
        }

        ContratTravailEntity contrat = contratTravailRepository.findFirstByEmployeIdOrderByDateCreationDesc(employeId).orElse(null);
        if (contrat == null) {
            log.warn("Aucun contrat de travail trouvé pour l'employé {}", employe.getMatricule());
            return;
        }

        long seqNote = CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_contrat_travail", contratTravailRepository::count);
        String refNoteService = (commentaire != null && !commentaire.isBlank())
                ? commentaire.trim()
                : String.format("NS-%d/%03d/DG/SODEPA", LocalDate.now().getYear(), seqNote);

        contrat.setStatut(StatutContrat.APPROUVE_DG);
        contrat.setSigneParDg(true);
        contrat.setDateSignatureDg(LocalDate.now());
        if (contrat.getReferenceNoteServiceDg() == null || contrat.getReferenceNoteServiceDg().isBlank() || (commentaire != null && !commentaire.isBlank())) {
            contrat.setReferenceNoteServiceDg(refNoteService);
        }
        contratTravailRepository.save(contrat);

        employe.setStatutActivite(StatutActivite.EN_FONCTION);
        employeRepository.save(employe);

        if (contrat.getMakerCheckerTicketId() != null) {
            try {
                MakerCheckerOutput checkerOutput = workflowService.findById(contrat.getMakerCheckerTicketId());
                if (checkerOutput.status() == MakerCheckerStatus.PENDING) {
                    workflowService.update(
                            contrat.getMakerCheckerTicketId(),
                            MakerCheckerStatus.ACCEPTED,
                            "Approbation définitive par le Directeur Général (Note de Service: " + refNoteService + ")",
                            dgUserId != null ? dgUserId.toString() : "DIRECTEUR_GENERAL"
                    );
                }
            } catch (Exception e) {
                log.warn("Erreur mise à jour ticket Maker-Checker lors de l'approbation DG: {}", e.getMessage());
            }
        }

        trackClickHouse(MakerCheckerEntityName.EMPLOYE.name(), employeId.toString(), Map.of(
                "action", "APPROBATION_FINALE_DG_RECRUTEMENT",
                "matricule", employe.getMatricule(),
                "numeroContrat", contrat.getNumeroContrat(),
                "referenceNoteServiceDg", refNoteService,
                "approbateurDg", dgUserId != null ? dgUserId.toString() : "DIRECTEUR_GENERAL"
        ));

        log.info("Recrutement employé {} validé par le DG avec la note de service {}", employe.getMatricule(), refNoteService);
    }

    private void enrichirEmployeAvecWorkflow(EmployeResponse res, WorkflowInstanceEntity wf) {
        if (res == null || wf == null) {
            return;
        }
        res.setEnCoursDeModification(true);
        res.setWorkflowEnCours(true);
        res.setWorkflowInstanceId(wf.getId());
        res.setWorkflowStatut(wf.getStatut() != null ? wf.getStatut().name() : null);
        WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
        res.setWorkflowOperationType(current.getOperationType() != null ? current.getOperationType() : "UPDATE");
        if (current.getAttachments() != null && !current.getAttachments().isEmpty()) {
            res.setPiecesJointesWorkflow(current.getAttachments());
        }
    }

    private EmployeResponse buildDraftEmployeFromWorkflow(
            UUID employeId,
            WorkflowInstanceEntity wf,
            Map<String, Object> data,
            WorkflowCurrentDataResponse current
    ) {
        CreerEmployeRequest req = convertPayloadSafe(data, CreerEmployeRequest.class);
        String nom = req.getNom() != null ? req.getNom().trim() : asString(data.get("nom"));
        String prenom = req.getPrenom() != null ? req.getPrenom().trim() : asString(data.get("prenom"));
        UUID uniteId = req.getUniteOrganisationnelleId() != null ? req.getUniteOrganisationnelleId() : parseUuidSafe(asString(data.get("uniteOrganisationnelleId")));
        UUID posteId = req.getPosteId() != null ? req.getPosteId() : parseUuidSafe(asString(data.get("posteId")));
        UUID ticketId = parseUuidSafe(asString(data.get("makerCheckerRequestId")));

        return EmployeResponse.builder()
                .id(employeId)
                .matricule(asString(data.get("matricule")))
                .nom(nom)
                .prenom(prenom)
                .nomComplet(((prenom != null ? prenom : "") + " " + (nom != null ? nom : "")).trim())
                .sexe(req.getSexe())
                .dateNaissance(req.getDateNaissance())
                .lieuNaissance(req.getLieuNaissance())
                .situationMatrimoniale(req.getSituationMatrimoniale())
                .nombreEnfantsACharge(req.getNombreEnfantsACharge())
                .email(req.getEmail())
                .telephone(req.getTelephone())
                .adresse(req.getAdresse())
                .numeroCnps(req.getNumeroCnps())
                .compteBancaireRib(req.getCompteBancaireRib())
                .banqueNom(req.getBanqueNom())
                .typeContrat(req.getTypeContrat() != null ? req.getTypeContrat() : TypeContrat.CDI)
                .dateEmbauche(req.getDateEmbauche())
                .dateFinEssai(req.getDateFinEssai())
                .dateFinContrat(req.getDateFinContrat())
                .statutActivite(StatutActivite.EN_FONCTION)
                .contratId(parseUuidSafe(asString(data.get("contratId"))))
                .numeroContrat(asString(data.get("numeroContrat")))
                .statutContrat(StatutContrat.EN_ATTENTE_VALIDATION)
                .signeParDg(false)
                .workflowInstanceId(wf != null ? wf.getId() : null)
                .makerCheckerTicketId(ticketId)
                .uniteId(uniteId)
                .uniteCode(asString(data.get("uniteCode")))
                .uniteIntitule(asString(data.get("uniteIntitule")))
                .posteId(posteId)
                .posteCode(asString(data.get("posteCode")))
                .posteIntitule(asString(data.get("posteIntitule")))
                .grilleId(parseUuidSafe(asString(data.get("grilleSalarialeId"))))
                .categorie(req.getCategorie())
                .echelon(req.getEchelon())
                .salaireBase(asBigDecimal(data.get("salaireBase")))
                .tauxHoraire(asBigDecimal(data.get("tauxHoraire")))
                .utilisateurId(req.getUtilisateurId())
                .username(asString(data.get("username")))
                .dateCreation(wf != null && wf.getDateInitiation() != null
                        ? LocalDateTime.ofInstant(wf.getDateInitiation(), java.time.ZoneId.systemDefault())
                        : LocalDateTime.now())
                .enCoursDeModification(true)
                .workflowEnCours(wf != null)
                .workflowStatut(wf != null && wf.getStatut() != null ? wf.getStatut().name() : "EN_COURS")
                .workflowOperationType(current != null && current.getOperationType() != null ? current.getOperationType() : "CREATE")
                .piecesJointesWorkflow(current != null ? current.getAttachments() : new java.util.ArrayList<>())
                .build();
    }

    private boolean matchesEmployeSearch(String nom, String prenom, String matricule, String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return true;
        }
        String q = recherche.toLowerCase().trim();
        return (nom != null && nom.toLowerCase().contains(q))
                || (prenom != null && prenom.toLowerCase().contains(q))
                || (matricule != null && matricule.toLowerCase().contains(q));
    }

    private String asString(Object val) {
        return val != null ? val.toString() : null;
    }

    private UUID parseUuidSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private java.math.BigDecimal asBigDecimal(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof java.math.BigDecimal bd) {
            return bd;
        }
        try {
            return new java.math.BigDecimal(val.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional
    public ContratTravailResponse approuverContratDg(UUID contratId, ApprobationContratDgRequest req) {
        utilsService.hasPermission(Permissions.VALIDATE_EMPLOYE);

        ContratTravailEntity contrat = contratTravailRepository.findById(contratId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable avec l'ID: " + contratId));

        contrat.setStatut(StatutContrat.APPROUVE_DG);
        contrat.setSigneParDg(true);
        contrat.setDateSignatureDg(req.getDateSignature() != null ? req.getDateSignature() : LocalDate.now());
        contrat.setReferenceNoteServiceDg(req.getReferenceNoteServiceDg());
        contrat = contratTravailRepository.save(contrat);

        EmployeEntity employe = contrat.getEmploye();
        if (employe != null) {
            employe.setStatutActivite(StatutActivite.EN_FONCTION);
            employeRepository.save(employe);
        }

        if (contrat.getMakerCheckerTicketId() != null) {
            try {
                workflowService.update(
                        contrat.getMakerCheckerTicketId(),
                        MakerCheckerStatus.ACCEPTED,
                        "Approbation formelle DG : " + req.getReferenceNoteServiceDg(),
                        utilsService.getCurrentUserData().userId()
                );
            } catch (Exception e) {
                log.warn("Erreur Maker-Checker lors de l'approbation contrat {}: {}", contratId, e.getMessage());
            }
        }

        trackClickHouse("CONTRAT_TRAVAIL", contratId.toString(), Map.of(
                "action", "APPROBATION_MANUELLE_DG_CONTRAT",
                "numeroContrat", contrat.getNumeroContrat(),
                "referenceNoteServiceDg", req.getReferenceNoteServiceDg()
        ));

        return mapToContratResponse(contrat);
    }

    @Transactional(readOnly = true)
    public List<ContratTravailResponse> listerContrats(UUID employeId, StatutContrat statut) {
        boolean privileged = hasRhReadPrivileges();
        UUID currentUserId = utilsService != null ? utilsService.getCurrentUserIdOrNull() : null;
        List<ContratTravailEntity> baseList;
        if (!privileged) {
            EmployeEntity self = currentUserId != null ? employeRepository.findByUtilisateurId(currentUserId).orElse(null) : null;
            if (self == null) {
                return List.of();
            }
            if (employeId != null && !employeId.equals(self.getId())) {
                throw new AccesRefuseException("Accès refusé aux contrats de travail d'un autre collaborateur.");
            }
            baseList = contratTravailRepository.findByEmployeId(self.getId());
        } else if (employeId != null) {
            baseList = contratTravailRepository.findByEmployeId(employeId);
        } else if (statut != null) {
            baseList = contratTravailRepository.findByStatut(statut);
        } else {
            baseList = contratTravailRepository.findAll();
        }
        return baseList.stream()
                .filter(c -> statut == null || c.getStatut() == statut)
                .map(this::mapToContratResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContratTravailResponse getContratById(UUID id) {
        ContratTravailEntity contrat = contratTravailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable avec l'ID: " + id));
        assertCanReadEmploye(contrat.getEmploye());
        return mapToContratResponse(contrat);
    }

    public ContratTravailResponse mapToContratResponse(ContratTravailEntity c) {
        return ContratTravailResponse.builder()
                .id(c.getId())
                .numeroContrat(c.getNumeroContrat())
                .employeId(c.getEmploye() != null ? c.getEmploye().getId() : null)
                .employeMatricule(c.getEmploye() != null ? c.getEmploye().getMatricule() : null)
                .employeNomComplet(c.getEmploye() != null ? c.getEmploye().getPrenom() + " " + c.getEmploye().getNom() : null)
                .typeContrat(c.getTypeContrat())
                .statut(c.getStatut())
                .dateDebut(c.getDateDebut())
                .dateFinEssai(c.getDateFinEssai())
                .dateFinContrat(c.getDateFinContrat())
                .categorie(c.getCategorie())
                .echelon(c.getEchelon())
                .salaireBase(c.getSalaireBase())
                .referenceNoteServiceDg(c.getReferenceNoteServiceDg())
                .dateSignatureDg(c.getDateSignatureDg())
                .signeParDg(c.getSigneParDg())
                .workflowInstanceId(c.getWorkflowInstanceId())
                .makerCheckerTicketId(c.getMakerCheckerTicketId())
                .uniteId(c.getUniteOrganisationnelle() != null ? c.getUniteOrganisationnelle().getId() : null)
                .uniteIntitule(c.getUniteOrganisationnelle() != null ? c.getUniteOrganisationnelle().getIntitule() : null)
                .posteId(c.getPoste() != null ? c.getPoste().getId() : null)
                .posteIntitule(c.getPoste() != null ? c.getPoste().getIntitule() : null)
                .dateCreation(c.getDateCreation())
                .build();
    }
}
