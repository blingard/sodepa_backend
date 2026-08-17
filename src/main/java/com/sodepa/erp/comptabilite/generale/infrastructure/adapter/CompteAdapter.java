package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.comptabilite.generale.application.inputs.CreateCompteInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateCompteInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.CompteOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.CompteSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.CompteEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.event.CompteEventInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service métier gérant le référentiel des comptes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompteAdapter {
    private final MakerCheckerEnginePort makerCheckerEngine;
    private final CompteRepository compteRepository;
    private final ObjectMapper objectMapper;
    private final UtilsService utilsService;

    private final static int MAX_PAGE_SIZE = 100;

    /**
     * Initie la création d'un compte comptable.
     *
     * @param request les données du compte
     */
    @Transactional
    public void initCreateCompte(CreateCompteInput request) {
        utilsService.hasPermission(Permissions.INIT_CREATE_COMPTE);
        if (compteRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalArgumentException("Le code compte " + request.code() + " est déjà utilisé.");
        }
        UUID entityPk = UUID.randomUUID();
        Map<String, Object> payload = toPayload(request, entityPk, Boolean.TRUE.equals(request.isAuxiliaire()));
        makerCheckerEngine.submitChange(
                MakerCheckerEntityName.COMPTE,
                entityPk.toString(),
                payload, MakerCheckerOperationType.CREATE
        );
    }

    /**
     * Récupère un compte par son identifiant.
     *
     * @param id identifiant
     * @return compte
     */
    @Transactional(readOnly = true)
    public CompteOutput getCompteById(UUID id) {
        utilsService.hasPermission(Permissions.GET_FULL_COMPTE_INFO);
        CompteEntity entity = compteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable avec l'ID: " + id));

        return CompteOutput.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .intitule(entity.getIntitule())
                .parentCode(entity.getParentCode())
                .niveau(entity.getNiveau())
                .typeAnalytique(entity.getTypeAnalytique())
                .nature(entity.getNature())
                .isAuxiliaire(entity.getIsAuxiliaire())
                .build();
    }

    /**
     * Récupère un compte actif par son identifiant.
     *
     * @param id identifiant
     * @return compte
     */
    @Transactional(readOnly = true)
    public CompteSmartOutput getActiveCompteById(UUID id) {
        CompteEntity entity = compteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable avec l'ID: " + id));

        return map(entity);
    }

    /**
     * Initie la mise à jour d'un compte.
     *
     * @param request données
     */
    @Transactional
    public void updateCompte(UpdateCompteInput request) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_COMPTE);
        UUID id = request.id();
        CompteEntity entity = compteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable avec l'ID: " + id));

        if (!entity.getCode().equals(request.code())) {
            throw new IllegalArgumentException("Erreur: vous ne pouvez pas modifier le code d'un compte.");
        }

        Map<String, Object> payload = toPayload(request, id);
        makerCheckerEngine.submitChange(
                MakerCheckerEntityName.COMPTE,
                id.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
    }

    /**
     * Liste tous les comptes.
     *
     * @return liste des comptes
     */
    @Transactional(readOnly = true)
    public List<CompteSmartOutput> listAllComptes() {
        return compteRepository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    /**
     * Récupère la liste des comptes par page.
     *
     * @param pageable pagination
     * @return page de comptes
     */
    @Transactional(readOnly = true)
    public PageRecord<CompteSmartOutput> getComptesByPage(Pageable pageable) {
        utilsService.hasPermission(Permissions.GET_FULL_COMPTE_INFO);
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        try {
            Page<CompteEntity> page = compteRepository.findAll(pageable);
            boolean paged = page.getPageable().isPaged();
            return new PageRecord<>(
                    page.getContent().stream().map(this::map).toList(),
                    page.isEmpty(),
                    page.isFirst(),
                    page.isLast(),
                    page.getNumber(),
                    page.getNumberOfElements(),
                    PageableRecord.builder()
                            .offset(paged ? page.getPageable().getOffset() : 0L)
                            .pageNumber(paged ? page.getPageable().getPageNumber() : 0L)
                            .pageSize(paged ? page.getPageable().getPageSize() : 0L)
                            .paged(paged)
                            .sort(page.getPageable().getSort())
                            .unpaged(!paged)
                            .build(),
                    page.getSize(),
                    page.getSort(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );
        }catch (Exception ex){
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

    }

    /**
     * Supprime un compte.
     *
     * @param id identifiant
     */
    @Transactional
    public void deleteCompte(UUID id) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_COMPTE);
        CompteEntity entity = compteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable avec l'ID: " + id));
        compteRepository.delete(entity);
    }

    /**
     * Valider ou Rejeter une demande.
     *
     * @param input données
     */
    @Transactional
    public void validateOrReject(ValidateOrRejectSubmissionInput input) {
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format("Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_COMPTE);
        UUID checkerId = UUID.fromString(utilsService.getCurrentUser().getUserData().get().userId());

        if (checkerOutput.maker().id().equals(checkerId)) {
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

        validation(checkerOutput);

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            makerCheckerEngine.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID compteId = UUID.fromString(checkerOutput.entityPk());
            CompteEventInput eventInput = objectMapper.convertValue(checkerOutput.payload(), CompteEventInput.class);
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creation(eventInput, compteId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                update(eventInput);
            }
            makerCheckerEngine.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
        }
    }

    private void validation(MakerCheckerOutput request) {
        makerCheckerEngine.validate(request.id(), request.createdAt(), request.expiredAt());
        Optional<CompteEntity> entityOpt = compteRepository.findById(UUID.fromString(request.entityPk()));
        if (request.checkerOperationType() == MakerCheckerOperationType.CREATE && entityOpt.isPresent()) {
            throw new RuntimeException("You cannot create a Compte that already exists");
        } else if (request.checkerOperationType() == MakerCheckerOperationType.UPDATE && entityOpt.isEmpty()) {
            throw new RuntimeException("You cannot update a Compte that does not exist");
        }
    }

    private void creation(CompteEventInput input, UUID id) {
        CompteEntity entity = CompteEntity.builder()
                .id(id)
                .code(input.code())
                .intitule(input.intitule())
                .parentCode(input.parentCode())
                .niveau(input.niveau())
                .typeAnalytique(input.typeAnalytique())
                .nature(input.nature())
                .isAuxiliaire(input.isAuxiliaire())
                .build();
        compteRepository.save(entity);
        log.info("Processing approved Compte creation. PK: {}", id);
    }

    private void update(CompteEventInput input) {
        CompteEntity entity = compteRepository.findById(input.id())
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable avec l'ID: " + input.id()));
        entity.setIntitule(input.intitule());
        entity.setParentCode(input.parentCode());
        entity.setNiveau(input.niveau());
        entity.setTypeAnalytique(input.typeAnalytique());
        entity.setNature(input.nature());
        entity.setIsAuxiliaire(input.isAuxiliaire());
        compteRepository.save(entity);
        log.info("Processing approved Compte update. PK: {}", input.id());
    }

    private CompteSmartOutput map(CompteEntity entity) {
        return CompteSmartOutput.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .intitule(entity.getIntitule())
                .build();
    }

    private Map<String, Object> toPayload(CreateCompteInput input, UUID entityPk, boolean isAuxiliaire) {
        CompteEventInput event = new CompteEventInput(
                entityPk,
                input.code(),
                input.intitule(),
                input.parentCode(),
                input.niveau(),
                input.typeAnalytique(),
                input.nature(),
                isAuxiliaire,
                utilsService.getCurrentUser().getUserData().get().userId()
        );
        return objectMapper.convertValue(event, new TypeReference<>() {});
    }

    private Map<String, Object> toPayload(UpdateCompteInput input, UUID entityPk) {
        CompteEventInput event = new CompteEventInput(
                entityPk,
                input.code(),
                input.intitule(),
                input.parentCode(),
                input.niveau(),
                input.typeAnalytique(),
                input.nature(),
                Boolean.TRUE.equals(input.isAuxiliaire()),
                utilsService.getCurrentUser().getUserData().get().userId()
        );
        return objectMapper.convertValue(event, new TypeReference<>() {});
    }
}
