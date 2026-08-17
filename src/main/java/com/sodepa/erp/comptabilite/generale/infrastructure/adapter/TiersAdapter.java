package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.comptabilite.generale.application.inputs.CreateTiersInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateTiersInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.TiersOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.TiersSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.event.TiersEventInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.TiersRepository;
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
 * Service métier gérant le référentiel des tiers (clients, fournisseurs, etc.).
 *
 * <p><b>Rôle dans le système :</b></p>
 * Permet de définir les tiers de l'entreprise et de les gérer (création, modification, validation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TiersAdapter {
    private final MakerCheckerEnginePort makerCheckerEngine;
    private final TiersRepository tiersRepository;
    private final CompteRepository compteRepository;
    private final ObjectMapper objectMapper;
    private final UtilsService utilsService;

    private final static int MAX_PAGE_SIZE = 100;

    /**
     * Enregistre un nouveau tiers dans le système.
     *
     * @param request les données de création du tiers.
     * @throws IllegalArgumentException si le code existe déjà ou si le compte n'est pas présent dans le plan.
     */
    @Transactional
    public void initCreateTiers(CreateTiersInput request) {
        utilsService.hasPermission(Permissions.INIT_CREATE_TIERS);
        if (tiersRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalArgumentException("Le code tiers " + request.code() + " est déjà utilisé.");
        }
        if (request.compteCollectifCode() != null && !compteRepository.existsByCode(request.compteCollectifCode())) {
            throw new IllegalArgumentException("Le compte " + request.compteCollectifCode() + " n'existe pas dans le plan comptable.");
        }
        UUID entityPk = UUID.randomUUID();
        Map<String, Object> payload = toPayload(request, entityPk, true);
        makerCheckerEngine.submitChange(
                MakerCheckerEntityName.TIERS,
                entityPk.toString(),
                payload, MakerCheckerOperationType.CREATE
        );
    }

    /**
     * Récupère un tiers par son identifiant unique.
     *
     * @param id l'identifiant du tiers.
     * @return le tiers correspondant.
     */
    @Transactional(readOnly = true)
    public TiersOutput getTiersById(UUID id) {
        utilsService.hasPermission(Permissions.GET_FULL_TIERS_INFO);
        TiersEntity entity = tiersRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tiers introuvable avec l'ID: " + id));

        return new TiersOutput(entity.getId(), entity.getCode(), entity.getRaisonSociale(),
                entity.getAdresse(), entity.getTelephone(), entity.getEmail(),
                entity.getTypeTiers(), entity.getActif(), entity.getCompteCollectifCode());
    }

    /**
     * Récupère un tiers actif par son identifiant unique.
     *
     * @param id l'identifiant du tiers.
     * @return le tiers correspondant.
     */
    @Transactional(readOnly = true)
    public TiersSmartOutput getActiveTiersById(UUID id) {
        TiersEntity entity = tiersRepository.findByIdAndActifIsTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Tiers introuvable avec l'ID: " + id));

        return new TiersSmartOutput(entity.getId(), entity.getCode(), entity.getRaisonSociale(), entity.getTypeTiers());
    }

    private TiersEntity getTiers(UUID id){
        TiersEntity tiers = getNullableTiers(id);
        if(Objects.isNull(tiers))
            throw new IllegalArgumentException("Tiers introuvable avec l'ID: " + id);
        return tiers;
    }

    private TiersEntity getNullableTiers(UUID id){
        return tiersRepository.findById(id).orElse(null);
    }

    /**
     * Met à jour les informations d'un tiers existant.
     * @param request les nouvelles données du tiers.
     */
    @Transactional
    public void updateTiers(UpdateTiersInput request) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_TIERS);
        UUID id = request.id();
        TiersEntity tiers = getTiers(id);

        // Validation de l'unicité du code s'il a changé
        if (!tiers.getCode().equals(request.code())) {
            throw new IllegalArgumentException("Erreur: vous ne pouvez pas modifier le code d'un tiers.");
        }

        if (request.compteCollectifCode() != null && !compteRepository.existsByCode(request.compteCollectifCode())) {
            throw new IllegalArgumentException("Le compte " + request.compteCollectifCode() + " n'existe pas dans le plan comptable.");
        }

        Map<String, Object> payload = toPayload(request, id);
        makerCheckerEngine.submitChange(MakerCheckerEntityName.TIERS, id.toString(), payload, MakerCheckerOperationType.UPDATE);
    }

    /**
     * Récupère la liste de tous les tiers enregistrés et actifs.
     *
     * @return la liste des tiers.
     */
    @Transactional(readOnly = true)
    public Set<TiersSmartOutput> listAllActiveTiers() {
        return tiersRepository.findAllByActifIsTrueOrderByRaisonSocialeAsc().stream().map(this::map).collect(Collectors.toSet());
    }

    /**
     * Récupère la liste de tous les tiers enregistrés par page.
     *
     * @return la liste des tiers.
     */
    @Transactional(readOnly = true)
    public PageRecord<TiersSmartOutput> getTiersByPage(Pageable pageable) {
        utilsService.hasPermission(Permissions.GET_FULL_TIERS_INFO);
        if(pageable.getPageSize() > MAX_PAGE_SIZE)
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    MAX_PAGE_SIZE,
                    pageable.getSort()
            );
        Page<TiersEntity> entityPage = tiersRepository.findAll(pageable);

        boolean paged = entityPage.getPageable().isPaged();
        return new PageRecord<>(
                entityPage.getContent().stream().map(this::map).toList(),
                entityPage.isEmpty(),
                entityPage.isFirst(),
                entityPage.isLast(),
                entityPage.getNumber(),
                entityPage.getNumberOfElements(),
                PageableRecord.builder()
                        .offset(paged ? entityPage.getPageable().getOffset() : 0L)
                        .pageNumber(paged ? entityPage.getPageable().getPageNumber() : 0L)
                        .pageSize(paged ? entityPage.getPageable().getPageSize() : 0L)
                        .paged(paged)
                        .sort(entityPage.getPageable().getSort())
                        .unpaged(!paged)
                        .build(),
                entityPage.getSize(),
                entityPage.getSort(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    /**
     * Valider, Rejeter ou Expirer une demande pour un tiers.
     */
    @Transactional
    public void validateOrReject(ValidateOrRejectSubmissionInput input) {
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING)
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));

        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_TIERS);

        UUID checkerId = UUID.fromString(utilsService.getCurrentUser().getUserData().get().userId());

        if (checkerOutput.maker().id().equals(checkerId))
            throw new RuntimeException("A checker cannot vote on their own request.");

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }
        validation(checkerOutput);
        if(MakerCheckerStatus.REJECTED.equals(newStatus)){
            makerCheckerEngine.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID entityId = UUID.fromString(checkerOutput.entityPk());
            TiersEventInput eventInput = objectMapper.convertValue(checkerOutput.payload(), TiersEventInput.class);
            if(checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creation(eventInput, entityId);
            }else if(checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                update(eventInput);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
        }
    }

    private void validation(MakerCheckerOutput request) {
        makerCheckerEngine.validate(request.id(), request.createdAt(), request.expiredAt());
        TiersEntity entity = getNullableTiers(UUID.fromString(request.entityPk()));
        if ((request.checkerOperationType() == MakerCheckerOperationType.CREATE) && (Objects.nonNull(entity))) {
            throw new RuntimeException("You cannot create a Tiers who already exist");
        } else if ((request.checkerOperationType() == MakerCheckerOperationType.UPDATE) && (Objects.isNull(entity))) {
            throw new RuntimeException("You cannot update a tiers who not exist");
        }
    }

    private void update(TiersEventInput input) {
        TiersEntity entity = getTiers(input.id());
        entity.setRaisonSociale(input.raisonSociale());
        entity.setAdresse(input.adresse());
        entity.setTelephone(input.telephone());
        entity.setEmail(input.email());
        entity.setTypeTiers(input.typeTiers());
        entity.setActif(input.actif());
        entity.setCompteCollectifCode(input.compteCollectifCode());
        tiersRepository.save(entity);
        log.info("Processing approved Tiers update. PK: {}", input.id());
    }

    private void creation(TiersEventInput input, UUID entityId) {
        TiersEntity entity = TiersEntity.builder()
                .id(entityId)
                .code(input.code())
                .raisonSociale(input.raisonSociale())
                .adresse(input.adresse())
                .telephone(input.telephone())
                .email(input.email())
                .typeTiers(input.typeTiers())
                .actif(input.actif())
                .compteCollectifCode(input.compteCollectifCode())
                .build();
        tiersRepository.save(entity);
        log.info("Processing approved Tiers creation. PK: {}", entityId);
    }

    private TiersSmartOutput map(TiersEntity entity){
        return new TiersSmartOutput(entity.getId(), entity.getCode(), entity.getRaisonSociale(), entity.getTypeTiers());
    }

    private Map<String, Object> toPayload(CreateTiersInput input, UUID entityPk, boolean actif) {
        TiersEventInput event = new TiersEventInput(
                entityPk, input.code(), input.raisonSociale(), input.adresse(), input.telephone(), input.email(),
                input.typeTiers(), actif, input.compteCollectifCode(), utilsService.getCurrentUser().getUserData().get().userId());
        return objectMapper.convertValue(event, new TypeReference<>() {});
    }

    private Map<String, Object> toPayload(UpdateTiersInput input, UUID entityPk) {
        TiersEventInput event = new TiersEventInput(
                entityPk, input.code(), input.raisonSociale(), input.adresse(), input.telephone(), input.email(),
                input.typeTiers(), input.actif(), input.compteCollectifCode(), utilsService.getCurrentUser().getUserData().get().userId());
        return objectMapper.convertValue(event, new TypeReference<>() {});
    }
}
