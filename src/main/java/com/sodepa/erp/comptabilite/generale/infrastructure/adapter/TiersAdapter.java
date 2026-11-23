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
import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.application.WorkflowStateService;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.presentation.responses.WorkflowCurrentDataResponse;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.SubmissionOutput;
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
    private static final List<String> TIERS_WORKFLOW_ENTITY_NAMES = List.of("TIERS", "TiersEntity");

    private final WorkflowService workflowService;
    private final WorkflowStateService workflowStateService;
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
    public SubmissionOutput initCreateTiers(CreateTiersInput request) {
        utilsService.hasPermission(Permissions.INIT_CREATE_TIERS);
        if (tiersRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalArgumentException("Le code tiers " + request.code() + " est déjà utilisé.");
        }
        if (request.compteCollectifCode() != null && !compteRepository.existsByCode(request.compteCollectifCode())) {
            throw new IllegalArgumentException("Le compte " + request.compteCollectifCode() + " n'existe pas dans le plan comptable.");
        }
        UUID entityPk = UUID.randomUUID();
        Map<String, Object> payload = toPayload(request, entityPk, true);
        UUID requestId = workflowService.submitChange(
                MakerCheckerEntityName.TIERS,
                entityPk.toString(),
                payload, MakerCheckerOperationType.CREATE
        );
        return new SubmissionOutput(requestId, entityPk);
    }

    /**
     * Récupère un tiers par son identifiant unique.
     *
     * @param id l'identifiant du tiers.
     * @return le tiers correspondant.
     */
    @Transactional
    public TiersOutput getTiersById(UUID id) {
        utilsService.hasPermission(Permissions.GET_FULL_TIERS_INFO);
        Optional<TiersEntity> opt = tiersRepository.findById(id);
        Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService.findActiveInstanceForEntity(TIERS_WORKFLOW_ENTITY_NAMES, id);

        if (opt.isPresent()) {
            TiersEntity entity = opt.get();
            if (activeWfOpt.isPresent()) {
                WorkflowInstanceEntity wf = activeWfOpt.get();
                WorkflowCurrentDataResponse current = workflowStateService.getCurrentData(wf);
                Map<String, Object> data = current.getData() != null ? current.getData() : Map.of();
                List<String> attachments = current.getAttachments() == null ? List.of()
                        : current.getAttachments().stream().map(m -> m.get("url") != null ? m.get("url").toString() : m.toString()).toList();
                TypeTiers tt = entity.getTypeTiers();
                if (data.get("typeTiers") != null) {
                    try { tt = TypeTiers.valueOf(data.get("typeTiers").toString()); } catch (Exception ignored) {}
                }
                return new TiersOutput(
                        entity.getId(),
                        data.get("code") != null ? data.get("code").toString() : entity.getCode(),
                        data.get("raisonSociale") != null ? data.get("raisonSociale").toString() : entity.getRaisonSociale(),
                        data.get("adresse") != null ? data.get("adresse").toString() : entity.getAdresse(),
                        data.get("telephone") != null ? data.get("telephone").toString() : entity.getTelephone(),
                        data.get("email") != null ? data.get("email").toString() : entity.getEmail(),
                        tt,
                        data.get("actif") != null ? Boolean.valueOf(data.get("actif").toString()) : entity.getActif(),
                        data.get("compteCollectifCode") != null ? data.get("compteCollectifCode").toString() : entity.getCompteCollectifCode(),
                        true,
                        true,
                        wf.getId(),
                        wf.getStatut() != null ? wf.getStatut().name() : null,
                        current.getOperationType() != null ? current.getOperationType() : "UPDATE",
                        attachments
                );
            }
            return new TiersOutput(entity.getId(), entity.getCode(), entity.getRaisonSociale(),
                    entity.getAdresse(), entity.getTelephone(), entity.getEmail(),
                    entity.getTypeTiers(), entity.getActif(), entity.getCompteCollectifCode());
        }

        if (activeWfOpt.isPresent()) {
            WorkflowInstanceEntity wf = activeWfOpt.get();
            WorkflowCurrentDataResponse current = workflowStateService.getCurrentData(wf);
            Map<String, Object> data = current.getData() != null ? current.getData() : Map.of();
            List<String> attachments = current.getAttachments() == null ? List.of()
                    : current.getAttachments().stream().map(m -> m.get("url") != null ? m.get("url").toString() : m.toString()).toList();
            TypeTiers tt = TypeTiers.FOURNISSEUR;
            if (data.get("typeTiers") != null) {
                try { tt = TypeTiers.valueOf(data.get("typeTiers").toString()); } catch (Exception ignored) {}
            }
            return new TiersOutput(
                    id,
                    data.get("code") != null ? data.get("code").toString() : "",
                    data.get("raisonSociale") != null ? data.get("raisonSociale").toString() : "",
                    data.get("adresse") != null ? data.get("adresse").toString() : "",
                    data.get("telephone") != null ? data.get("telephone").toString() : "",
                    data.get("email") != null ? data.get("email").toString() : "",
                    tt,
                    data.get("actif") == null || Boolean.parseBoolean(data.get("actif").toString()),
                    data.get("compteCollectifCode") != null ? data.get("compteCollectifCode").toString() : "401",
                    true,
                    true,
                    wf.getId(),
                    wf.getStatut() != null ? wf.getStatut().name() : null,
                    current.getOperationType() != null ? current.getOperationType() : "CREATE",
                    attachments
            );
        }

        throw new IllegalArgumentException("Tiers introuvable avec l'ID: " + id);
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
        workflowService.submitChange(MakerCheckerEntityName.TIERS, id.toString(), payload, MakerCheckerOperationType.UPDATE);
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
    @Transactional
    public PageRecord<TiersSmartOutput> getTiersByPage(Pageable pageable) {
        utilsService.hasPermission(Permissions.GET_FULL_TIERS_INFO);
        if(pageable.getPageSize() > MAX_PAGE_SIZE)
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    MAX_PAGE_SIZE,
                    pageable.getSort()
            );
        Page<TiersEntity> entityPage = tiersRepository.findAll(pageable);
        Map<UUID, WorkflowInstanceEntity> activeById = workflowStateService.findActiveInstancesByEntityUuid(TIERS_WORKFLOW_ENTITY_NAMES);
        Set<UUID> materializedIds = new HashSet<>();
        List<TiersSmartOutput> items = new ArrayList<>();

        for (TiersEntity entity : entityPage.getContent()) {
            materializedIds.add(entity.getId());
            WorkflowInstanceEntity wf = activeById.get(entity.getId());
            if (wf != null) {
                WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
                Map<String, Object> data = current.getData() != null ? current.getData() : Map.of();
                List<String> attachments = current.getAttachments() == null ? List.of()
                        : current.getAttachments().stream().map(m -> m.get("url") != null ? m.get("url").toString() : m.toString()).toList();
                TypeTiers tt = entity.getTypeTiers();
                if (data.get("typeTiers") != null) {
                    try { tt = TypeTiers.valueOf(data.get("typeTiers").toString()); } catch (Exception ignored) {}
                }
                items.add(new TiersSmartOutput(
                        entity.getId(),
                        data.get("code") != null ? data.get("code").toString() : entity.getCode(),
                        data.get("raisonSociale") != null ? data.get("raisonSociale").toString() : entity.getRaisonSociale(),
                        tt,
                        true,
                        true,
                        wf.getId(),
                        wf.getStatut() != null ? wf.getStatut().name() : null,
                        current.getOperationType() != null ? current.getOperationType() : "UPDATE",
                        attachments
                ));
            } else {
                items.add(this.map(entity));
            }
        }

        if (pageable.getPageNumber() == 0) {
            for (Map.Entry<UUID, WorkflowInstanceEntity> entry : activeById.entrySet()) {
                if (materializedIds.contains(entry.getKey()) || tiersRepository.existsById(entry.getKey())) {
                    continue;
                }
                WorkflowInstanceEntity wf = entry.getValue();
                WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
                Map<String, Object> data = current.getData() != null ? current.getData() : Map.of();
                List<String> attachments = current.getAttachments() == null ? List.of()
                        : current.getAttachments().stream().map(m -> m.get("url") != null ? m.get("url").toString() : m.toString()).toList();
                TypeTiers tt = TypeTiers.FOURNISSEUR;
                if (data.get("typeTiers") != null) {
                    try { tt = TypeTiers.valueOf(data.get("typeTiers").toString()); } catch (Exception ignored) {}
                }
                items.add(new TiersSmartOutput(
                        entry.getKey(),
                        data.get("code") != null ? data.get("code").toString() : "",
                        data.get("raisonSociale") != null ? data.get("raisonSociale").toString() : "",
                        tt,
                        true,
                        true,
                        wf.getId(),
                        wf.getStatut() != null ? wf.getStatut().name() : null,
                        current.getOperationType() != null ? current.getOperationType() : "CREATE",
                        attachments
                ));
            }
        }

        boolean paged = entityPage.getPageable().isPaged();
        return new PageRecord<>(
                items,
                items.isEmpty(),
                entityPage.isFirst(),
                entityPage.isLast(),
                entityPage.getNumber(),
                items.size(),
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
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
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
            workflowService.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID entityId = UUID.fromString(checkerOutput.entityPk());
            TiersEventInput eventInput = objectMapper.convertValue(checkerOutput.payload(), TiersEventInput.class);
            if(checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creation(eventInput, entityId);
            }else if(checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                update(eventInput);
            }
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
        }
    }

    private void validation(MakerCheckerOutput request) {
        workflowService.validate(request.id(), request.createdAt(), request.expiredAt());
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
