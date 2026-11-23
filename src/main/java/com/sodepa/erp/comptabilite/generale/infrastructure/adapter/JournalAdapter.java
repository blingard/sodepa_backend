package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.comptabilite.generale.application.inputs.CreateJournalInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateJournalInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.JournalOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.JournalSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.JournalEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.event.JournalEventInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.JournalRepository;
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
 * Service métier gérant le référentiel des journaux comptables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalAdapter {
    private static final List<String> JOURNAL_WORKFLOW_ENTITY_NAMES = List.of("JOURNAL", "JournalEntity");

    private final WorkflowService workflowService;
    private final WorkflowStateService workflowStateService;
    private final JournalRepository journalRepository;
    private final ObjectMapper objectMapper;
    private final UtilsService utilsService;

    private final static int MAX_PAGE_SIZE = 100;

    /**
     * Initie la création d'un journal comptable.
     *
     * @param request les données du journal
     */
    @Transactional
    public SubmissionOutput initCreateJournal(CreateJournalInput request) {
        utilsService.hasPermission(Permissions.INIT_CREATE_JOURNAL);
        if (journalRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalArgumentException("Le code journal " + request.code() + " est déjà utilisé.");
        }
        UUID entityPk = UUID.randomUUID();
        Map<String, Object> payload = toPayload(request, entityPk, true);
        UUID requestId = workflowService.submitChange(
                MakerCheckerEntityName.JOURNAL,
                entityPk.toString(),
                payload, MakerCheckerOperationType.CREATE
        );
        return new SubmissionOutput(requestId, entityPk);
    }

    /**
     * Récupère un journal par son identifiant.
     *
     * @param id identifiant
     * @return journal
     */
    @Transactional
    public JournalOutput getJournalById(UUID id) {
        utilsService.hasPermission(Permissions.GET_FULL_JOURNAL_INFO);
        Optional<JournalEntity> opt = journalRepository.findById(id);
        Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService.findActiveInstanceForEntity(JOURNAL_WORKFLOW_ENTITY_NAMES, id);

        if (opt.isPresent()) {
            JournalEntity entity = opt.get();
            if (activeWfOpt.isPresent()) {
                return buildJournalOutputFromWorkflow(id, entity, activeWfOpt.get());
            }
            return JournalOutput.builder()
                    .id(entity.getId())
                    .code(entity.getCode())
                    .intitule(entity.getIntitule())
                    .typeJournal(entity.getTypeJournal())
                    .actif(entity.getActif())
                    .build();
        }

        if (activeWfOpt.isPresent()) {
            return buildJournalOutputFromWorkflow(id, null, activeWfOpt.get());
        }

        throw new IllegalArgumentException("Journal introuvable avec l'ID: " + id);
    }

    /**
     * Récupère un journal actif par son identifiant.
     *
     * @param id identifiant
     * @return journal
     */
    @Transactional(readOnly = true)
    public JournalSmartOutput getActiveJournalById(UUID id) {
        JournalEntity entity = journalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable avec l'ID: " + id));

        return map(entity);
    }

    /**
     * Initie la mise à jour d'un journal.
     *
     * @param request données
     */
    @Transactional
    public void updateJournal(UpdateJournalInput request) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_JOURNAL);
        UUID id = request.id();
        JournalEntity entity = journalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable avec l'ID: " + id));

        if (!entity.getCode().equals(request.code())) {
            throw new IllegalArgumentException("Erreur: vous ne pouvez pas modifier le code d'un journal.");
        }

        Map<String, Object> payload = toPayload(request, id);
        workflowService.submitChange(
                MakerCheckerEntityName.JOURNAL,
                id.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
    }

    /**
     * Alterne le statut actif/inactif d'un journal.
     *
     * @param id identifiant
     */
    @Transactional
    public void toggleActive(UUID id) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_JOURNAL);
        JournalEntity entity = journalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable avec l'ID: " + id));

        UpdateJournalInput updateInput = UpdateJournalInput.builder()
                .id(id)
                .code(entity.getCode())
                .intitule(entity.getIntitule())
                .typeJournal(entity.getTypeJournal())
                .actif(!entity.getActif())
                .build();

        Map<String, Object> payload = toPayload(updateInput, id);
        workflowService.submitChange(
                MakerCheckerEntityName.JOURNAL,
                id.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
    }

    /**
     * Liste tous les journaux.
     *
     * @return liste des journaux
     */
    @Transactional(readOnly = true)
    public List<JournalSmartOutput> listAllJournaux() {
        return journalRepository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    /**
     * Récupère la liste des journaux par page.
     *
     * @param pageable pagination
     * @return page de journaux
     */
    @Transactional
    public PageRecord<JournalSmartOutput> getJournauxByPage(Pageable pageable) {
        utilsService.hasPermission(Permissions.GET_FULL_JOURNAL_INFO);
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        Page<JournalEntity> page = journalRepository.findAll(pageable);
        Map<UUID, WorkflowInstanceEntity> activeById = workflowStateService.findActiveInstancesByEntityUuid(JOURNAL_WORKFLOW_ENTITY_NAMES);
        Set<UUID> materializedIds = new HashSet<>();
        List<JournalSmartOutput> items = new ArrayList<>();

        for (JournalEntity entity : page.getContent()) {
            materializedIds.add(entity.getId());
            WorkflowInstanceEntity wf = activeById.get(entity.getId());
            if (wf != null) {
                items.add(buildJournalSmartFromWorkflow(entity.getId(), entity, wf));
            } else {
                items.add(this.map(entity));
            }
        }

        if (pageable.getPageNumber() == 0) {
            for (Map.Entry<UUID, WorkflowInstanceEntity> entry : activeById.entrySet()) {
                if (materializedIds.contains(entry.getKey()) || journalRepository.existsById(entry.getKey())) {
                    continue;
                }
                items.add(buildJournalSmartFromWorkflow(entry.getKey(), null, entry.getValue()));
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
    }

    private JournalOutput buildJournalOutputFromWorkflow(UUID id, JournalEntity base, WorkflowInstanceEntity wf) {
        WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
        Map<String, Object> data = current.getData() != null ? current.getData() : Map.of();
        List<String> attachments = current.getAttachments() == null ? List.of()
                : current.getAttachments().stream().map(m -> m.get("url") != null ? m.get("url").toString() : m.toString()).toList();
        CodeJournal cj = base != null ? base.getCode() : CodeJournal.OD;
        if (data.get("code") != null) {
            try { cj = CodeJournal.valueOf(data.get("code").toString()); } catch (Exception ignored) {}
        }
        return JournalOutput.builder()
                .id(id)
                .code(cj)
                .intitule(data.get("intitule") != null ? data.get("intitule").toString() : (base != null ? base.getIntitule() : ""))
                .typeJournal(data.get("typeJournal") != null ? data.get("typeJournal").toString() : (base != null ? base.getTypeJournal() : "DIVERS"))
                .actif(data.get("actif") != null ? Boolean.valueOf(data.get("actif").toString()) : (base == null || Boolean.TRUE.equals(base.getActif())))
                .enCoursDeModification(true)
                .workflowEnCours(true)
                .workflowInstanceId(wf.getId())
                .workflowStatut(wf.getStatut() != null ? wf.getStatut().name() : null)
                .workflowOperationType(current.getOperationType() != null ? current.getOperationType() : (base != null ? "UPDATE" : "CREATE"))
                .piecesJointesWorkflow(attachments)
                .build();
    }

    private JournalSmartOutput buildJournalSmartFromWorkflow(UUID id, JournalEntity base, WorkflowInstanceEntity wf) {
        JournalOutput full = buildJournalOutputFromWorkflow(id, base, wf);
        return JournalSmartOutput.builder()
                .id(full.id())
                .code(full.code())
                .intitule(full.intitule())
                .enCoursDeModification(full.enCoursDeModification())
                .workflowEnCours(full.workflowEnCours())
                .workflowInstanceId(full.workflowInstanceId())
                .workflowStatut(full.workflowStatut())
                .workflowOperationType(full.workflowOperationType())
                .piecesJointesWorkflow(full.piecesJointesWorkflow())
                .build();
    }

    /**
     * Valider ou Rejeter une demande.
     *
     * @param input données
     */
    @Transactional
    public void validateOrReject(ValidateOrRejectSubmissionInput input) {
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format("Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_JOURNAL);
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
            workflowService.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID journalId = UUID.fromString(checkerOutput.entityPk());
            JournalEventInput eventInput = objectMapper.convertValue(checkerOutput.payload(), JournalEventInput.class);
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creation(eventInput, journalId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                update(eventInput);
            }
            workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
        }
    }

    private void validation(MakerCheckerOutput request) {
        workflowService.validate(request.id(), request.createdAt(), request.expiredAt());
        Optional<JournalEntity> entityOpt = journalRepository.findById(UUID.fromString(request.entityPk()));
        if (request.checkerOperationType() == MakerCheckerOperationType.CREATE && entityOpt.isPresent()) {
            throw new RuntimeException("You cannot create a Journal that already exists");
        } else if (request.checkerOperationType() == MakerCheckerOperationType.UPDATE && entityOpt.isEmpty()) {
            throw new RuntimeException("You cannot update a Journal that does not exist");
        }
    }

    private void creation(JournalEventInput input, UUID id) {
        JournalEntity entity = JournalEntity.builder()
                .id(id)
                .code(input.code())
                .intitule(input.intitule())
                .typeJournal(input.typeJournal())
                .actif(input.actif() != null ? input.actif() : true)
                .build();
        journalRepository.save(entity);
        log.info("Processing approved Journal creation. PK: {}", id);
    }

    private void update(JournalEventInput input) {
        JournalEntity entity = journalRepository.findById(input.id())
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable avec l'ID: " + input.id()));
        entity.setIntitule(input.intitule());
        entity.setTypeJournal(input.typeJournal());
        if (input.actif() != null) {
            entity.setActif(input.actif());
        }
        journalRepository.save(entity);
        log.info("Processing approved Journal update. PK: {}", input.id());
    }

    private JournalSmartOutput map(JournalEntity entity) {
        return JournalSmartOutput.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .intitule(entity.getIntitule())
                .build();
    }

    private Map<String, Object> toPayload(CreateJournalInput input, UUID entityPk, boolean isActif) {
        JournalEventInput event = new JournalEventInput(
                entityPk,
                input.code(),
                input.intitule(),
                input.typeJournal(),
                isActif,
                utilsService.getCurrentUser().getUserData().get().userId()
        );
        return objectMapper.convertValue(event, new TypeReference<>() {});
    }

    private Map<String, Object> toPayload(UpdateJournalInput input, UUID entityPk) {
        JournalEventInput event = new JournalEventInput(
                entityPk,
                input.code(),
                input.intitule(),
                input.typeJournal(),
                input.actif(),
                utilsService.getCurrentUser().getUserData().get().userId()
        );
        return objectMapper.convertValue(event, new TypeReference<>() {});
    }
}
