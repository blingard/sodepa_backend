package com.sodepa.erp.user.infrastructure.adapter;
import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.application.WorkflowStateService;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.presentation.responses.WorkflowCurrentDataResponse;

import com.sodepa.erp.user.application.BootstrapInitializationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.budget.infrastructure.repo.AuditTrailRepository;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.user.UserAdapterInterface;
import com.sodepa.erp.share.*;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.user.application.inputs.*;
import com.sodepa.erp.user.application.outputs.UserOutput;
import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.event.UserEventInput;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sodepa.erp.authentication.application.ports.KeycloakProvisioningPort;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptateur pour la gestion des utilisateurs, implémentant l'interface commune de récupération d'utilisateur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdapter implements UserAdapterInterface {

    private static final java.util.List<String> USER_WORKFLOW_ENTITY_NAMES = java.util.List.of("USER", "UtilisateurEntity");

    private final UserRepository userRepository;
    private final WorkflowService workflowService;
    private final WorkflowStateService workflowStateService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final UtilsService utilsService;
    private final KeycloakProvisioningPort keycloakProvisioningPort;
    private final AuditTrailRepository auditTrailRepository;
    private final BootstrapInitializationService bootstrapInitializationService;
    private final UserManagementEnginePort userManagementEnginePort;

    private static final int MAX_PAGE_SIZE = 100;
    private static final String PROFILE_PICTURES_PATH = "/user/profile_pictures";

    @Override
    @Transactional(readOnly = true)
    public UserRecordSmartOutput getUserById(UUID id) {
        if (id == null) return null;
        return userRepository.findById(id).map(user -> UserRecordSmartOutput.builder()
                .id(user.getId())
                .name(user.getNom() + " " + user.getPrenom())
                .image(user.getPhotoProfile())
                .status(user.isActif())
                .build()).orElse(null);
    }

    @Transactional
    public UserOutput getUserOutputById(UUID id) {
        java.util.Optional<UtilisateurEntity> opt = userRepository.findById(id);
        java.util.Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService.findActiveInstanceForEntity(USER_WORKFLOW_ENTITY_NAMES, id);

        if (opt.isPresent()) {
            UtilisateurEntity entity = opt.get();
            if (activeWfOpt.isPresent()) {
                return buildUserOutputFromWorkflow(id, entity, activeWfOpt.get());
            }
            return toOutput(entity);
        }

        if (activeWfOpt.isPresent()) {
            return buildUserOutputFromWorkflow(id, null, activeWfOpt.get());
        }

        throw new IllegalArgumentException("Utilisateur introuvable avec l'ID: " + id);
    }

    @Transactional(readOnly = true)
    public UserOutput getUserOutPutProfile() {
        UserData userData = utilsService.getCurrentUserData();
        return userRepository.findById(UUID.fromString(userData.userId())).map(this::toOutput)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
    }

    @Transactional
    public PageRecord<UserOutput> getPage(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        Page<UtilisateurEntity> page = userRepository.findAll(pageable);
        Map<UUID, WorkflowInstanceEntity> activeById = workflowStateService.findActiveInstancesByEntityUuid(USER_WORKFLOW_ENTITY_NAMES);
        java.util.Set<UUID> materializedIds = new java.util.HashSet<>();
        java.util.List<UserOutput> items = new java.util.ArrayList<>();

        for (UtilisateurEntity entity : page.getContent()) {
            materializedIds.add(entity.getId());
            WorkflowInstanceEntity wf = activeById.get(entity.getId());
            if (wf != null) {
                items.add(buildUserOutputFromWorkflow(entity.getId(), entity, wf));
            } else {
                items.add(this.toOutput(entity));
            }
        }

        if (pageable.getPageNumber() == 0) {
            for (Map.Entry<UUID, WorkflowInstanceEntity> entry : activeById.entrySet()) {
                if (materializedIds.contains(entry.getKey()) || userRepository.existsById(entry.getKey())) {
                    continue;
                }
                items.add(buildUserOutputFromWorkflow(entry.getKey(), null, entry.getValue()));
            }
        }

        boolean paged = page.getPageable().isPaged();
        return new PageRecord<>(
                items,
                items.isEmpty(),
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                items.size(),
                PageableRecord.builder()
                        .offset(paged ? page.getPageable().getOffset() : 0L)
                        .pageNumber(paged ? (long) page.getPageable().getPageNumber() : 0L)
                        .pageSize(paged ? (long) page.getPageable().getPageSize() : 0L)
                        .paged(paged)
                        .sort(page.getPageable().getSort())
                        .unpaged(!paged)
                        .build(),
                page.getSize(),
                page.getSort(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private UserOutput buildUserOutputFromWorkflow(UUID id, UtilisateurEntity base, WorkflowInstanceEntity wf) {
        WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
        Map<String, Object> data = current.getData() != null ? current.getData() : Map.of();
        java.util.List<String> attachments = current.getAttachments() == null ? java.util.List.of()
                : current.getAttachments().stream().map(m -> m.get("url") != null ? m.get("url").toString() : m.toString()).toList();
        UserEventInput evt = null;
        try {
            evt = objectMapper.convertValue(data, UserEventInput.class);
        } catch (Exception ignored) {}

        return new UserOutput(
                id,
                evt != null && evt.username() != null ? evt.username() : (base != null ? base.getUsername() : ""),
                evt != null && evt.nom() != null ? evt.nom() : (base != null ? base.getNom() : ""),
                evt != null && evt.prenom() != null ? evt.prenom() : (base != null ? base.getPrenom() : ""),
                evt != null && evt.email() != null ? evt.email() : (base != null ? base.getEmail() : ""),
                evt != null && evt.photoProfile() != null ? evt.photoProfile() : (base != null ? base.getPhotoProfile() : ""),
                evt != null ? evt.actif() : (base == null || base.isActif()),
                evt != null && evt.telephones() != null ? evt.telephones() : (base != null ? base.getTelephones() : java.util.Set.of()),
                evt != null && evt.permissions() != null ? evt.permissions() : (base != null ? base.getPermissions() : java.util.Set.of()),
                true,
                true,
                wf.getId(),
                wf.getStatut() != null ? wf.getStatut().name() : null,
                current.getOperationType() != null ? current.getOperationType() : (base != null ? "UPDATE" : "CREATE"),
                attachments
        );
    }

    @Transactional(readOnly = true)
    public PageRecord<UserOutput> search(SearchUsersInput input) {
        Pageable pageable = input.pageable();
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        Page<UtilisateurEntity> page = userRepository.searchUsers(
                input.nom(), input.prenom(), input.email(), input.telephone(), pageable
        );
        boolean paged = page.getPageable().isPaged();
        return new PageRecord<>(
                page.getContent().stream().map(this::toOutput).collect(Collectors.toList()),
                page.isEmpty(),
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                page.getNumberOfElements(),
                PageableRecord.builder()
                        .offset(paged ? page.getPageable().getOffset() : 0L)
                        .pageNumber(paged ? (long) page.getPageable().getPageNumber() : 0L)
                        .pageSize(paged ? (long) page.getPageable().getPageSize() : 0L)
                        .paged(paged)
                        .sort(page.getPageable().getSort())
                        .unpaged(!paged)
                        .build(),
                page.getSize(),
                page.getSort(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional
    public SubmissionOutput initCreateUser(CreateUserInput input) {
        utilsService.hasPermission(Permissions.INIT_CREATE_USER);

        if (userRepository.findByUsername(input.username()).isPresent()) {
            throw new IllegalArgumentException("Le nom d'utilisateur " + input.username() + " existe déjà");
        }
        if (userRepository.findByEmail(input.email()).isPresent()) {
            throw new IllegalArgumentException("L'e-mail " + input.email() + " est déjà utilisé");
        }

        String photoUrl = null;
        if (input.photoProfile() != null && !input.photoProfile().isEmpty()) {
            photoUrl = fileStorageService.storeFileImage(input.photoProfile(), PROFILE_PICTURES_PATH);
        }

        UUID newUserId = UUID.randomUUID();
        String currentUserId = utilsService.getCurrentUser().getUserData().get().userId();

        UserEventInput event = new UserEventInput(
                newUserId,
                input.username(),
                input.nom(),
                input.prenom(),
                input.email(),
                photoUrl,
                input.telephones(),
                input.permissions(),
                true,
                currentUserId
        );

        Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
        UUID requestId = workflowService.submitChange(
                MakerCheckerEntityName.USER,
                newUserId.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );

        // Les deux identifiants dont le client a besoin ensuite : `requestId`
        // pour appeler `validate_or_reject`, `newUserId` pour retrouver le
        // compte une fois la demande acceptée.
        return new SubmissionOutput(requestId, newUserId);
    }

    /**
     * Demandes de création ou de modification d'utilisateur en attente d'un
     * checker.
     *
     * <p>
     * Réservé à qui peut trancher : lister ce qu'on n'a pas le droit de valider
     * exposerait des données de comptes sans contrepartie.
     * </p>
     */
    @Transactional(readOnly = true)
    public PageRecord<MakerCheckerSmartOutput> findPendingRequests(Pageable pageable) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_USER);

        // Ses propres soumissions sont écartées : le maker-checker interdit de
        // valider ce qu'on a soumis soi-même, et `validateOrReject` le refuse
        // par un 400. Les afficher ici revenait à proposer une action qui ne
        // pouvait qu'échouer.
        String moi = utilsService.getCurrentUser().getUserData().get().userId();
        return workflowService.findAllToValideByOther(
                pageable, MakerCheckerEntityName.USER, MakerCheckerStatus.PENDING, moi);
    }

    @Transactional
    public void initUpdateUser(UpdateUserInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_USER);

        UtilisateurEntity user = userRepository.findById(input.id())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID: " + input.id()));

        String currentUserId = utilsService.getCurrentUser().getUserData().get().userId();

        UserEventInput event = new UserEventInput(
                user.getId(),
                user.getUsername(),
                input.nom(),
                input.prenom(),
                input.email(),
                user.getPhotoProfile(),
                input.telephones(),
                user.getPermissions(),
                input.actif(),
                currentUserId
        );

        Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
        workflowService.submitChange(
                MakerCheckerEntityName.USER,
                user.getId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
    }

    @Transactional
    public void initChangePhoto(ChangePhotoInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_USER);

        UtilisateurEntity user = userRepository.findById(input.id())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID: " + input.id()));

        String photoUrl = null;
        if (input.photoProfile() != null && !input.photoProfile().isEmpty()) {
            photoUrl = fileStorageService.storeFileImage(input.photoProfile(), PROFILE_PICTURES_PATH);
        }

        String currentUserId = utilsService.getCurrentUser().getUserData().get().userId();

        UserEventInput event = new UserEventInput(
                user.getId(),
                user.getUsername(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                photoUrl,
                user.getTelephones(),
                user.getPermissions(),
                user.isActif(),
                currentUserId
        );

        Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
        workflowService.submitChange(
                MakerCheckerEntityName.USER,
                user.getId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
    }

    @Transactional
    public void changePhoto(ChangePhotoInput input) {
        UserData userData = utilsService.getCurrentUserData();

        UtilisateurEntity user = userRepository.findById(UUID.fromString(userData.userId()))
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID: " + userData.userId()));

        String photoUrl = null;
        if (input.photoProfile() != null && !input.photoProfile().isEmpty()) {
            photoUrl = fileStorageService.storeFileImage(input.photoProfile(), PROFILE_PICTURES_PATH);
        }

        String currentUserId = userData.userId();

        UserEventInput event = new UserEventInput(
                user.getId(),
                user.getUsername(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                photoUrl,
                user.getTelephones(),
                user.getPermissions(),
                user.isActif(),
                currentUserId
        );

        Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
        UUID requestId = workflowService.submitChange(
                MakerCheckerEntityName.USER,
                user.getId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE_IMAGE
        );

        // Mise à jour de l'utilisateur
        user.setPhotoProfile(photoUrl);
        userRepository.save(user);

        // Validation automatique par le système
        workflowService.update(requestId, MakerCheckerStatus.ACCEPTED, "Validation automatique par le système", "SYSTEM");

        log.info("Photo de profil mise à jour pour l'utilisateur {} ({})", user.getUsername(), user.getId());
    }

    @Transactional
    public void initUpdatePermissions(UpdatePermissionsInput input) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_USER);

        UtilisateurEntity user = userRepository.findById(input.id())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID: " + input.id()));

        String currentUserId = utilsService.getCurrentUser().getUserData().get().userId();

        UserEventInput event = new UserEventInput(
                user.getId(),
                user.getUsername(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getPhotoProfile(),
                user.getTelephones(),
                input.permissions(),
                user.isActif(),
                currentUserId
        );

        Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
        workflowService.submitChange(
                MakerCheckerEntityName.USER,
                user.getId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
    }

    @Transactional
    public void validateOrReject(ValidateOrRejectSubmissionInput input) {
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new IllegalArgumentException(String.format(
                    "Transition invalide: impossible de passer de %s à %s.", currentStatus, input.decision()));
        }

        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_USER);

        UUID checkerId = UUID.fromString(utilsService.getCurrentUser().getUserData().get().userId());

        if (checkerOutput.maker().id().equals(checkerId)) {
            // 409 : la demande est valide, c'est l'état du système qui s'y
            // oppose. Un 400 laissait croire à un corps mal formé, que le
            // client aurait tenté de corriger sans jamais y parvenir.
            throw new ConflitMetierException(
                    "Vous ne pouvez pas valider une demande que vous avez soumise.");
        }

        validation(checkerOutput);

        if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            workflowService.update(checkerOutput.id(), MakerCheckerStatus.REJECTED, input.notes(), checkerId.toString());
        } else if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            UUID entityId = UUID.fromString(checkerOutput.entityPk());
            UserEventInput event = objectMapper.convertValue(checkerOutput.payload(), UserEventInput.class);
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creation(event, entityId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                update(event);
            }
            workflowService.update(checkerOutput.id(), MakerCheckerStatus.ACCEPTED, input.notes(), checkerId.toString());
        }
    }

    private void validation(MakerCheckerOutput request) {
        workflowService.validate(request.id(), request.createdAt(), request.expiredAt());
        UtilisateurEntity entity = userRepository.findById(UUID.fromString(request.entityPk())).orElse(null);
        if (request.checkerOperationType() == MakerCheckerOperationType.CREATE && Objects.nonNull(entity)) {
            throw new IllegalArgumentException("L'utilisateur existe déjà.");
        } else if (request.checkerOperationType() == MakerCheckerOperationType.UPDATE && Objects.isNull(entity)) {
            throw new IllegalArgumentException("L'utilisateur n'existe pas.");
        }
    }

    private void creation(UserEventInput payload, UUID id) {
        // Le compte Keycloak est créé AVANT l'enregistrement en base : c'est lui
        // qui fournit l'identifiant `iam`, colonne NOT NULL de `utilisateurs`.
        // L'ordre inverse faisait échouer toute validation de création sur une
        // violation de contrainte. Même séquence que `UserSeeder`.
        UUID iam = keycloakProvisioningPort.createKeycloakUser(
                id,
                payload.username(),
                payload.email(),
                payload.prenom(),
                payload.nom(),
                payload.actif()
        );

        UtilisateurEntity entity = UtilisateurEntity.builder()
                .id(id)
                .iam(iam)
                .username(payload.username())
                .nom(payload.nom())
                .prenom(payload.prenom())
                .email(payload.email())
                .photoProfile(payload.photoProfile())
                .telephones(payload.telephones())
                .permissions(payload.permissions())
                .actif(payload.actif())
                .build();
        UtilisateurEntity saved = userRepository.save(entity);
        log.info("Création approuvée de l'utilisateur. PK: {}", id);
        if (!"admin_maker".equalsIgnoreCase(payload.username()) && !"admin_checker".equalsIgnoreCase(payload.username())) {
            bootstrapInitializationService.ensureBootstrapStructuresAndDeactivateRoots(saved);
        }
    }

    private void update(UserEventInput payload) {
        UtilisateurEntity entity = userRepository.findById(payload.id())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID: " + payload.id()));

        entity.setNom(payload.nom());
        entity.setPrenom(payload.prenom());
        entity.setEmail(payload.email());
        if (payload.photoProfile() != null) {
            entity.setPhotoProfile(payload.photoProfile());
        }
        entity.setTelephones(payload.telephones());
        entity.setPermissions(payload.permissions());
        entity.setActif(payload.actif());

        userRepository.save(entity);
        log.info("Mise à jour approuvée de l'utilisateur. PK: {}", payload.id());
    }

    private UserOutput toOutput(UtilisateurEntity entity) {
        return new UserOutput(
                entity.getId(),
                entity.getUsername(),
                entity.getNom(),
                entity.getPrenom(),
                entity.getEmail(),
                fileStorageService.getPresignedUrlFromFullUrl(entity.getPhotoProfile(), 1),
                entity.isActif(),
                entity.getTelephones(),
                userManagementEnginePort.resolveEffectivePermissions(entity.getId())
        );
    }
}
