package com.sodepa.erp.user.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.user.UserAdapterInterface;
import com.sodepa.erp.share.FileStorageService;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
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
import org.springframework.web.multipart.MultipartFile;
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

    private final UserRepository userRepository;
    private final MakerCheckerEnginePort makerCheckerEngine;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final UtilsService utilsService;
    private final KeycloakProvisioningPort keycloakProvisioningPort;

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

    @Transactional(readOnly = true)
    public UserOutput getUserOutputById(UUID id) {
        return userRepository.findById(id).map(this::toOutput)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID: " + id));
    }

    @Transactional(readOnly = true)
    public PageRecord<UserOutput> getPage(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        Page<UtilisateurEntity> page = userRepository.findAll(pageable);
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
    public void initCreateUser(CreateUserInput input) {
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
        makerCheckerEngine.submitChange(
                MakerCheckerEntityName.USER,
                newUserId.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
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
        makerCheckerEngine.submitChange(
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
        makerCheckerEngine.submitChange(
                MakerCheckerEntityName.USER,
                user.getId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
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
        makerCheckerEngine.submitChange(
                MakerCheckerEntityName.USER,
                user.getId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
    }

    @Transactional
    public void validateOrReject(ValidateOrRejectSubmissionInput input) {
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new IllegalArgumentException(String.format(
                    "Transition invalide: impossible de passer de %s à %s.", currentStatus, input.decision()));
        }

        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_USER);

        UUID checkerId = UUID.fromString(utilsService.getCurrentUser().getUserData().get().userId());

        if (checkerOutput.maker().id().equals(checkerId)) {
            throw new IllegalArgumentException("Le validateur doit être différent de l'initiateur.");
        }

        validation(checkerOutput);

        if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            makerCheckerEngine.update(checkerOutput.id(), MakerCheckerStatus.REJECTED, input.notes(), checkerId.toString());
        } else if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            UUID entityId = UUID.fromString(checkerOutput.entityPk());
            UserEventInput event = objectMapper.convertValue(checkerOutput.payload(), UserEventInput.class);
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creation(event, entityId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                update(event);
            }
            makerCheckerEngine.update(checkerOutput.id(), MakerCheckerStatus.ACCEPTED, input.notes(), checkerId.toString());
        }
    }

    private void validation(MakerCheckerOutput request) {
        makerCheckerEngine.validate(request.id(), request.createdAt(), request.expiredAt());
        UtilisateurEntity entity = userRepository.findById(UUID.fromString(request.entityPk())).orElse(null);
        if (request.checkerOperationType() == MakerCheckerOperationType.CREATE && Objects.nonNull(entity)) {
            throw new IllegalArgumentException("L'utilisateur existe déjà.");
        } else if (request.checkerOperationType() == MakerCheckerOperationType.UPDATE && Objects.isNull(entity)) {
            throw new IllegalArgumentException("L'utilisateur n'existe pas.");
        }
    }

    private void creation(UserEventInput payload, UUID id) {
        UtilisateurEntity entity = UtilisateurEntity.builder()
                .id(id)
                .username(payload.username())
                .nom(payload.nom())
                .prenom(payload.prenom())
                .email(payload.email())
                .photoProfile(payload.photoProfile())
                .telephones(payload.telephones())
                .permissions(payload.permissions())
                .actif(payload.actif())
                .build();
        userRepository.save(entity);
        log.info("Création approuvée de l'utilisateur. PK: {}", id);
        keycloakProvisioningPort.createKeycloakUser(
                id, 
                payload.username(), 
                payload.email(), 
                payload.prenom(), 
                payload.nom(), 
                payload.actif()
        );
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
                entity.getPhotoProfile(),
                entity.isActif(),
                entity.getTelephones(),
                entity.getPermissions()
        );
    }
}
