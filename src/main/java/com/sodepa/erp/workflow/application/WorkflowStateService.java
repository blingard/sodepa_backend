package com.sodepa.erp.workflow.application;

import com.sodepa.erp.audit.application.inputs.WorkflowEntityStateSnapshot;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowInstance;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowEtapeDefinitionEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowStateOutboxEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowTaskEntity;
import com.sodepa.erp.workflow.infrastructure.repo.WorkflowInstanceRepository;
import com.sodepa.erp.workflow.infrastructure.repo.WorkflowStateOutboxRepository;
import com.sodepa.erp.workflow.presentation.responses.WorkflowCurrentDataResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowEntityStateResponse;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service de gestion des instantanés versionnés d'état des instances de workflow.
 * <p>
 * Garantit l'écriture synchrone dans ClickHouse ({@code clickhouse_workflow_entity_states})
 * doublée d'une table Outbox de secours dans le périmètre moteur PostgreSQL
 * ({@code workflows_states_outbox}) rejouée automatiquement lorsque ClickHouse redevient joignable.
 * </p>
 */
@Slf4j
@Service
public class WorkflowStateService {

    public static final String EVENT_INITIATED = "INITIATED";
    public static final String EVENT_STEP_PATCHED = "STEP_PATCHED";
    public static final String EVENT_REVISION_PATCHED = "REVISION_PATCHED";
    public static final String EVENT_COMPLETED = "COMPLETED";
    public static final String EVENT_REJECTED = "REJECTED";

    private static final String SOURCE_CLICKHOUSE = "CLICKHOUSE";
    private static final String SOURCE_OUTBOX = "POSTGRES_OUTBOX";
    private static final String SOURCE_CONTEXT_FALLBACK = "INSTANCE_CONTEXT_FALLBACK";
    private static final List<StatutWorkflowInstance> ACTIVE_STATUTS = List.of(
            StatutWorkflowInstance.EN_COURS,
            StatutWorkflowInstance.SUSPENDU
    );

    private final ClickHouseManager clickHouseManager;
    private final WorkflowStateOutboxRepository outboxRepository;
    private final WorkflowSnapshotMerger snapshotMerger;
    private final WorkflowInstanceRepository workflowInstanceRepository;

    @Autowired
    public WorkflowStateService(
            ClickHouseManager clickHouseManager,
            WorkflowStateOutboxRepository outboxRepository,
            WorkflowSnapshotMerger snapshotMerger,
            WorkflowInstanceRepository workflowInstanceRepository
    ) {
        this.clickHouseManager = clickHouseManager;
        this.outboxRepository = outboxRepository;
        this.snapshotMerger = snapshotMerger;
        this.workflowInstanceRepository = workflowInstanceRepository;
    }

    public WorkflowStateService(
            ClickHouseManager clickHouseManager,
            WorkflowStateOutboxRepository outboxRepository,
            WorkflowSnapshotMerger snapshotMerger
    ) {
        this(clickHouseManager, outboxRepository, snapshotMerger, null);
    }

    /**
     * Enregistre l'état initial (v1) dans ClickHouse / Outbox lors du démarrage d'une instance.
     */
    public WorkflowEntityStateSnapshot recordInitiation(
            WorkflowInstanceEntity instance,
            String operationType,
            Map<String, Object> initialData,
            String actorUserId,
            String actorUsername,
            String actorPoste
    ) {
        WorkflowEtapeDefinitionEntity premiereEtape = instance.getEtapeCourante();
        return recordStateTransition(
                instance,
                EVENT_INITIATED,
                operationType != null && !operationType.isBlank() ? operationType : "WORKFLOW",
                premiereEtape != null ? premiereEtape.getOrdre() : 1,
                premiereEtape != null ? premiereEtape.getCodeEtape() : null,
                null,
                actorUserId,
                actorUsername,
                actorPoste,
                initialData,
                null,
                "Initiation du circuit de validation"
        );
    }

    /**
     * Enregistre une nouvelle version d'état suite à la validation d'une étape ou à une révision soumise.
     */
    public WorkflowEntityStateSnapshot recordStepDecision(
            WorkflowInstanceEntity instance,
            WorkflowTaskEntity task,
            String eventType,
            Map<String, Object> patchFields,
            String uploadedFileUrl,
            String actorUserId,
            String actorUsername,
            String actorPoste,
            String commentaire
    ) {
        WorkflowEtapeDefinitionEntity etape = task != null && task.getEtapeDefinition() != null
                ? task.getEtapeDefinition()
                : instance.getEtapeCourante();

        return recordStateTransition(
                instance,
                eventType != null ? eventType : EVENT_STEP_PATCHED,
                null,
                etape != null ? etape.getOrdre() : null,
                etape != null ? etape.getCodeEtape() : null,
                task != null ? task.getId() : null,
                actorUserId,
                actorUsername,
                actorPoste,
                patchFields,
                uploadedFileUrl,
                commentaire
        );
    }

    /**
     * Enregistre un événement terminal (COMPLETED / REJECTED) conservant le dernier état consolidé.
     */
    public WorkflowEntityStateSnapshot recordTerminalEvent(
            WorkflowInstanceEntity instance,
            String eventType,
            Integer etapeOrdre,
            String etapeCode,
            String actorUserId,
            String actorUsername,
            String actorPoste,
            String commentaire
    ) {
        return recordStateTransition(
                instance,
                eventType,
                null,
                etapeOrdre,
                etapeCode,
                null,
                actorUserId,
                actorUsername,
                actorPoste,
                null,
                null,
                commentaire
        );
    }

    /**
     * Restitue la projection courante consolidée d'une instance (données fusionnées + pièces jointes + source).
     */
    public WorkflowCurrentDataResponse getCurrentData(WorkflowInstanceEntity instance) {
        flushOutboxForInstance(instance.getId());

        Optional<WorkflowEntityStateSnapshot> latestOpt = resolveLatestSnapshot(instance);
        Map<String, Object> domainData;
        List<Map<String, Object>> attachments;
        int version;
        String eventType;
        String operationType;
        String storageSource;
        Instant updatedAt;

        if (latestOpt.isPresent()) {
            WorkflowEntityStateSnapshot latest = latestOpt.get();
            domainData = snapshotMerger.stripInternalMetadata(
                    snapshotMerger.parseMap(latest.getFullSnapshotJson()));
            attachments = snapshotMerger.parseListOfMaps(latest.getAttachmentsJson());
            if (attachments.isEmpty()) {
                attachments = snapshotMerger.extractCachedAttachments(instance.getDonneesContexte());
            }
            version = latest.getVersion();
            eventType = latest.getEventType();
            operationType = latest.getOperationType();
            storageSource = latest.getStorageSource();
            updatedAt = latest.getCreatedAt();
        } else {
            domainData = snapshotMerger.stripInternalMetadata(
                    snapshotMerger.parseMap(instance.getDonneesContexte()));
            attachments = snapshotMerger.extractCachedAttachments(instance.getDonneesContexte());
            version = snapshotMerger.extractCachedVersion(instance.getDonneesContexte());
            eventType = null;
            operationType = snapshotMerger.extractCachedOperationType(instance.getDonneesContexte());
            storageSource = SOURCE_CONTEXT_FALLBACK;
            updatedAt = instance.getDateCloture() != null ? instance.getDateCloture() : instance.getDateInitiation();
        }

        boolean pendingOutbox = outboxRepository.existsByInstanceIdAndSyncedFalse(instance.getId());
        return WorkflowCurrentDataResponse.builder()
                .instanceId(instance.getId())
                .entityName(instance.getEntityName())
                .entityId(instance.getEntityId())
                .operationType(operationType != null ? operationType : "WORKFLOW")
                .statutWorkflow(instance.getStatut())
                .etapeCouranteCode(instance.getEtapeCourante() != null ? instance.getEtapeCourante().getCodeEtape() : null)
                .version(version)
                .lastEventType(eventType)
                .storageSource(storageSource)
                .syncedToClickHouse(!pendingOutbox && SOURCE_CLICKHOUSE.equals(storageSource))
                .materializedInPostgres(instance.getStatut() == StatutWorkflowInstance.APPROUVE)
                .lastUpdatedAt(updatedAt)
                .data(domainData)
                .attachments(attachments)
                .build();
    }

    /**
     * Restitue l'historique chronologique complet des états (ClickHouse + éventuelle queue Outbox non encore synchronisée).
     */
    public List<WorkflowEntityStateResponse> getStateHistory(WorkflowInstanceEntity instance) {
        flushOutboxForInstance(instance.getId());

        Map<Integer, WorkflowEntityStateSnapshot> byVersion = new TreeMap<>();
        if (clickHouseManager != null) {
            try {
                List<WorkflowEntityStateSnapshot> chStates = clickHouseManager.findWorkflowEntityStates(instance.getId());
                for (WorkflowEntityStateSnapshot s : chStates) {
                    s.setStorageSource(SOURCE_CLICKHOUSE);
                    byVersion.put(s.getVersion(), s);
                }
            } catch (Exception e) {
                log.warn("Lecture ClickHouse indisponible pour historique instance {}: {}", instance.getId(), e.getMessage());
            }
        }

        List<WorkflowStateOutboxEntity> outboxRows = outboxRepository
                .findByInstanceIdAndSyncedFalseOrderByVersionAscCreatedAtAsc(instance.getId());
        for (WorkflowStateOutboxEntity row : outboxRows) {
            WorkflowEntityStateSnapshot s = toSnapshot(row);
            s.setStorageSource(SOURCE_OUTBOX);
            byVersion.put(row.getVersion(), s);
        }

        return byVersion.values().stream()
                .map(s -> WorkflowEntityStateResponse.builder()
                        .id(s.getId())
                        .instanceId(s.getInstanceId())
                        .version(s.getVersion())
                        .eventType(s.getEventType())
                        .entityName(s.getEntityName())
                        .entityPk(s.getEntityPk())
                        .operationType(s.getOperationType())
                        .definitionCode(s.getDefinitionCode())
                        .etapeOrdre(s.getEtapeOrdre())
                        .etapeCode(s.getEtapeCode())
                        .taskId(s.getTaskId())
                        .acteurUserId(s.getActeurUserId())
                        .acteurUsername(s.getActeurUsername())
                        .acteurPoste(s.getActeurPoste())
                        .patch(snapshotMerger.parseMap(s.getPatchJson()))
                        .snapshot(snapshotMerger.stripInternalMetadata(
                                snapshotMerger.parseMap(s.getFullSnapshotJson())))
                        .attachments(snapshotMerger.parseListOfMaps(s.getAttachmentsJson()))
                        .commentaire(s.getCommentaire())
                        .storageSource(s.getStorageSource())
                        .syncedToClickHouse(SOURCE_CLICKHOUSE.equals(s.getStorageSource()))
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Projection rapide O(1) depuis le cache moteur PostgreSQL ({@code instance.getDonneesContexte()})
     * pour les listings paginés, sans déclencher de requête ClickHouse par ligne.
     */
    public WorkflowCurrentDataResponse getCachedCurrentData(WorkflowInstanceEntity instance) {
        if (instance == null) {
            return null;
        }
        String rawCache = instance.getDonneesContexte();
        if (rawCache == null || rawCache.isBlank()) {
            return getCurrentData(instance);
        }
        Map<String, Object> domainData = snapshotMerger.stripInternalMetadata(snapshotMerger.parseMap(rawCache));
        if (domainData == null || domainData.isEmpty()) {
            return getCurrentData(instance);
        }
        List<Map<String, Object>> attachments = snapshotMerger.extractCachedAttachments(rawCache);
        int version = snapshotMerger.extractCachedVersion(rawCache);
        String operationType = snapshotMerger.extractCachedOperationType(rawCache);
        Instant updatedAt = instance.getDateCloture() != null ? instance.getDateCloture() : instance.getDateInitiation();
        boolean pendingOutbox = outboxRepository.existsByInstanceIdAndSyncedFalse(instance.getId());
        return WorkflowCurrentDataResponse.builder()
                .instanceId(instance.getId())
                .entityName(instance.getEntityName())
                .entityId(instance.getEntityId())
                .operationType(operationType != null ? operationType : "WORKFLOW")
                .statutWorkflow(instance.getStatut())
                .etapeCouranteCode(instance.getEtapeCourante() != null ? instance.getEtapeCourante().getCodeEtape() : null)
                .version(version)
                .storageSource(pendingOutbox ? SOURCE_OUTBOX : SOURCE_CONTEXT_FALLBACK)
                .syncedToClickHouse(!pendingOutbox)
                .materializedInPostgres(instance.getStatut() == StatutWorkflowInstance.APPROUVE)
                .lastUpdatedAt(updatedAt)
                .data(domainData)
                .attachments(attachments)
                .build();
    }

    /**
     * Retourne uniquement la Map métier fusionnée la plus récente d'une instance
     * (utilisée lors de la projection liste ou de la matérialisation finale PostgreSQL).
     */
    public Map<String, Object> getLatestMergedDataMap(WorkflowInstanceEntity instance) {
        if (instance == null) {
            return new LinkedHashMap<>();
        }
        WorkflowCurrentDataResponse current = getCurrentData(instance);
        return current.getData() != null ? new LinkedHashMap<>(current.getData()) : new LinkedHashMap<>();
    }

    /**
     * Retourne en O(1) le résumé métier consolidé d'une instance à partir du cache moteur
     * synchronisé à chaque transition ({@code instance.getDonneesContexte()}), avec repli sur ClickHouse
     * uniquement si le cache est vide. Évite d'ouvrir N connexions JDBC ClickHouse lors du listing paginé
     * des boîtes de réception (DISP-01).
     */
    public Map<String, Object> getCachedOrLiveDomainData(WorkflowInstanceEntity instance) {
        if (instance == null) {
            return new LinkedHashMap<>();
        }
        String rawCache = instance.getDonneesContexte();
        if (rawCache != null && !rawCache.isBlank()) {
            Map<String, Object> parsed = snapshotMerger.stripInternalMetadata(snapshotMerger.parseMap(rawCache));
            if (parsed != null && !parsed.isEmpty()) {
                return parsed;
            }
        }
        return getLatestMergedDataMap(instance);
    }

    /**
     * Recherche toutes les instances actives (EN_COURS / SUSPENDU) pour un ensemble de noms d'entités,
     * indexées par UUID d'entité (en conservant la plus récente en cas de doublon historique).
     */
    public Map<UUID, WorkflowInstanceEntity> findActiveInstancesByEntityUuid(Collection<String> entityNames) {
        Map<UUID, WorkflowInstanceEntity> map = new LinkedHashMap<>();
        if (workflowInstanceRepository == null || entityNames == null || entityNames.isEmpty()) {
            return map;
        }
        for (WorkflowInstanceEntity wf : workflowInstanceRepository.findByEntityNameInAndStatutIn(entityNames, ACTIVE_STATUTS)) {
            UUID entityUuid = parseUuidSafe(wf.getEntityId());
            if (entityUuid == null) {
                continue;
            }
            WorkflowInstanceEntity existing = map.get(entityUuid);
            if (existing == null || (wf.getDateInitiation() != null && existing.getDateInitiation() != null
                    && wf.getDateInitiation().isAfter(existing.getDateInitiation()))) {
                map.put(entityUuid, wf);
            }
        }
        return map;
    }

    /**
     * Recherche l'instance active la plus récente (EN_COURS / SUSPENDU) pour une entité cible précise.
     */
    public Optional<WorkflowInstanceEntity> findActiveInstanceForEntity(Collection<String> entityNames, UUID entityId) {
        if (workflowInstanceRepository == null || entityNames == null || entityNames.isEmpty() || entityId == null) {
            return Optional.empty();
        }
        return workflowInstanceRepository.findFirstByEntityNameInAndEntityIdAndStatutInOrderByDateInitiationDesc(
                entityNames, entityId.toString(), ACTIVE_STATUTS);
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

    private static final int MAX_OUTBOX_RETRIES = 10;
    private final java.util.concurrent.atomic.AtomicLong replayTickCounter =
            new java.util.concurrent.atomic.AtomicLong(0L);

    /**
     * Rejoue automatiquement toutes les entrées Outbox en attente dès que ClickHouse redevient disponible,
     * en isolant les messages toxiques (poison pill) dépassant {@link #MAX_OUTBOX_RETRIES} tentatives.
     */
    @Scheduled(fixedDelayString = "${workflow.clickhouse.outbox-replay-ms:15000}")
    @SchedulerLock(name = "workflowOutboxReplay", lockAtMostFor = "PT2M", lockAtLeastFor = "PT5S")
    public void replayPendingOutbox() {
        long tick = replayTickCounter.incrementAndGet();
        if (tick <= 3) {
            log.info("[WORKFLOW-OUTBOX] Tick #{} actif (ShedLock workflowOutboxReplay).", tick);
        }
        List<WorkflowStateOutboxEntity> pending = outboxRepository.findBySyncedFalseOrderByCreatedAtAscVersionAsc();
        if (pending.isEmpty()) {
            return;
        }
        if (clickHouseManager == null || !(clickHouseManager.isAvailable() || clickHouseManager.probeAvailability())) {
            log.debug("[WORKFLOW-OUTBOX] {} état(s) en attente mais ClickHouse indisponible.", pending.size());
            return;
        }
        int replayed = 0;
        for (WorkflowStateOutboxEntity row : pending) {
            if (row.getRetryCount() >= MAX_OUTBOX_RETRIES) {
                log.warn("[WORKFLOW-OUTBOX] Ligne poison-pill ignorée après {} tentatives (id={}, instance={}).",
                        row.getRetryCount(), row.getId(), row.getInstanceId());
                continue;
            }
            if (!trySyncOutboxRow(row)) {
                if (clickHouseManager.isCircuitOpen() || !clickHouseManager.isAvailable()) {
                    break;
                }
                continue;
            }
            replayed++;
        }
        if (replayed > 0) {
            log.info("[WORKFLOW-OUTBOX] {} état(s) rejoué(s) avec succès vers ClickHouse.", replayed);
        }
    }

    /**
     * Tente de synchroniser immédiatement les éventuels états en attente d'une instance précise.
     */
    public boolean flushOutboxForInstance(UUID instanceId) {
        if (instanceId == null || !outboxRepository.existsByInstanceIdAndSyncedFalse(instanceId)) {
            return true;
        }
        if (clickHouseManager == null || !(clickHouseManager.isAvailable() || clickHouseManager.probeAvailability())) {
            return false;
        }
        List<WorkflowStateOutboxEntity> pending = outboxRepository
                .findByInstanceIdAndSyncedFalseOrderByVersionAscCreatedAtAsc(instanceId);
        for (WorkflowStateOutboxEntity row : pending) {
            if (!trySyncOutboxRow(row)) {
                return false;
            }
        }
        return true;
    }

    private WorkflowEntityStateSnapshot recordStateTransition(
            WorkflowInstanceEntity instance,
            String eventType,
            String explicitOperationType,
            Integer etapeOrdre,
            String etapeCode,
            UUID taskId,
            String actorUserId,
            String actorUsername,
            String actorPoste,
            Map<String, Object> patchFields,
            String uploadedFileUrl,
            String commentaire
    ) {
        flushOutboxForInstance(instance.getId());

        Optional<WorkflowEntityStateSnapshot> latestOpt = resolveLatestSnapshot(instance);

        int cachedVersion = snapshotMerger.extractCachedVersion(instance.getDonneesContexte());
        int latestVersion = latestOpt.map(WorkflowEntityStateSnapshot::getVersion).orElse(0);
        int nextVersion = Math.max(latestVersion, cachedVersion) + 1;

        Map<String, Object> baseDomain = latestOpt
                .map(s -> snapshotMerger.parseMap(s.getFullSnapshotJson()))
                .orElseGet(() -> snapshotMerger.parseMap(instance.getDonneesContexte()));

        List<Map<String, Object>> baseAttachments = latestOpt
                .map(s -> snapshotMerger.parseListOfMaps(s.getAttachmentsJson()))
                .filter(list -> !list.isEmpty())
                .orElseGet(() -> snapshotMerger.extractCachedAttachments(instance.getDonneesContexte()));

        String resolvedOperationType = explicitOperationType;
        if (resolvedOperationType == null || resolvedOperationType.isBlank()) {
            resolvedOperationType = latestOpt.map(WorkflowEntityStateSnapshot::getOperationType)
                    .orElseGet(() -> {
                        String cachedOp = snapshotMerger.extractCachedOperationType(instance.getDonneesContexte());
                        return cachedOp != null ? cachedOp : "WORKFLOW";
                    });
        }

        WorkflowSnapshotMerger.MergeResult merged = snapshotMerger.merge(
                baseDomain,
                baseAttachments,
                patchFields,
                uploadedFileUrl,
                etapeCode,
                etapeOrdre,
                taskId,
                actorUsername,
                actorPoste
        );

        String fullSnapshotJson = snapshotMerger.toJson(merged.domainSnapshot());
        String patchJson = snapshotMerger.toJson(merged.patchApplied());
        String attachmentsJson = snapshotMerger.toJson(merged.attachments());

        // Cache moteur dans Postgres (table workflows_instances uniquement, jamais table métier)
        Map<String, Object> cachePayload = snapshotMerger.buildInstanceCachePayload(
                merged.domainSnapshot(), merged.attachments(), nextVersion, resolvedOperationType);
        instance.setDonneesContexte(snapshotMerger.toJson(cachePayload));

        String defCode = instance.getWorkflowDefinition() != null ? instance.getWorkflowDefinition().getCode() : null;
        WorkflowEntityStateSnapshot snapshot = WorkflowEntityStateSnapshot.builder()
                .id(UUID.randomUUID())
                .instanceId(instance.getId())
                .version(nextVersion)
                .eventType(eventType)
                .entityName(instance.getEntityName())
                .entityPk(instance.getEntityId())
                .operationType(resolvedOperationType)
                .definitionCode(defCode)
                .etapeOrdre(etapeOrdre)
                .etapeCode(etapeCode)
                .taskId(taskId)
                .acteurUserId(actorUserId)
                .acteurUsername(actorUsername)
                .acteurPoste(actorPoste)
                .patchJson(patchJson)
                .fullSnapshotJson(fullSnapshotJson != null ? fullSnapshotJson : "{}")
                .attachmentsJson(attachmentsJson)
                .commentaire(commentaire)
                .createdAt(Instant.now())
                .build();

        persistSyncOrOutbox(snapshot);
        return snapshot;
    }

    private Optional<WorkflowEntityStateSnapshot> resolveLatestSnapshot(WorkflowInstanceEntity instance) {
        WorkflowEntityStateSnapshot chLatest = null;
        boolean hasUnsyncedOutbox = outboxRepository.existsByInstanceIdAndSyncedFalse(instance.getId());

        if (clickHouseManager != null) {
            try {
                chLatest = clickHouseManager.findLatestWorkflowEntityState(instance.getId()).orElse(null);
                if (chLatest != null) {
                    chLatest.setStorageSource(SOURCE_CLICKHOUSE);
                }
            } catch (Exception e) {
                log.debug("ClickHouse inaccessible lors de la lecture d'état pour instance {}: {}", instance.getId(), e.getMessage());
            }
        }

        WorkflowEntityStateSnapshot outboxLatest = hasUnsyncedOutbox
                ? outboxRepository.findTopByInstanceIdAndSyncedFalseOrderByVersionDescCreatedAtDesc(instance.getId())
                        .map(row -> {
                            WorkflowEntityStateSnapshot s = toSnapshot(row);
                            s.setStorageSource(SOURCE_OUTBOX);
                            return s;
                        })
                        .orElse(null)
                : null;

        if (chLatest == null && outboxLatest == null) {
            return Optional.empty();
        }
        if (outboxLatest != null && (chLatest == null || outboxLatest.getVersion() >= chLatest.getVersion())) {
            return Optional.of(outboxLatest);
        }
        return Optional.of(chLatest);
    }

    private void persistSyncOrOutbox(WorkflowEntityStateSnapshot snapshot) {
        if (outboxRepository.existsByInstanceIdAndSyncedFalse(snapshot.getInstanceId())) {
            saveToOutbox(snapshot, "File Outbox en attente pour préserver l'ordre séquentiel des versions");
            snapshot.setStorageSource(SOURCE_OUTBOX);
            return;
        }
        if (clickHouseManager == null) {
            saveToOutbox(snapshot, "ClickHouseManager absent");
            snapshot.setStorageSource(SOURCE_OUTBOX);
            return;
        }
        try {
            clickHouseManager.saveWorkflowEntityState(snapshot);
            snapshot.setStorageSource(SOURCE_CLICKHOUSE);
        } catch (Exception e) {
            log.warn("[WORKFLOW-STATE] ClickHouse indisponible (instance={}, v={}), repli Outbox PostgreSQL : {}",
                    snapshot.getInstanceId(), snapshot.getVersion(), e.getMessage());
            saveToOutbox(snapshot, e.getMessage());
            snapshot.setStorageSource(SOURCE_OUTBOX);
        }
    }

    private void saveToOutbox(WorkflowEntityStateSnapshot snapshot, String error) {
        String truncatedError = error != null && error.length() > 1500 ? error.substring(0, 1500) : error;
        WorkflowStateOutboxEntity row = WorkflowStateOutboxEntity.builder()
                .id(snapshot.getId())
                .instanceId(snapshot.getInstanceId())
                .version(snapshot.getVersion())
                .eventType(snapshot.getEventType())
                .entityName(snapshot.getEntityName())
                .entityPk(snapshot.getEntityPk())
                .operationType(snapshot.getOperationType())
                .definitionCode(snapshot.getDefinitionCode())
                .etapeOrdre(snapshot.getEtapeOrdre())
                .etapeCode(snapshot.getEtapeCode())
                .taskId(snapshot.getTaskId())
                .acteurUserId(snapshot.getActeurUserId())
                .acteurUsername(snapshot.getActeurUsername())
                .acteurPoste(snapshot.getActeurPoste())
                .patchJson(snapshot.getPatchJson())
                .fullSnapshotJson(snapshot.getFullSnapshotJson())
                .attachmentsJson(snapshot.getAttachmentsJson())
                .commentaire(snapshot.getCommentaire())
                .synced(false)
                .retryCount(0)
                .lastError(truncatedError)
                .createdAt(snapshot.getCreatedAt() != null ? snapshot.getCreatedAt() : Instant.now())
                .build();
        outboxRepository.save(row);
    }

    private boolean trySyncOutboxRow(WorkflowStateOutboxEntity row) {
        try {
            if (!clickHouseManager.existsWorkflowEntityState(row.getId())) {
                clickHouseManager.saveWorkflowEntityState(toSnapshot(row));
            }
            row.setSynced(true);
            row.setSyncedAt(Instant.now());
            row.setLastError(null);
            outboxRepository.save(row);
            return true;
        } catch (Exception e) {
            row.setRetryCount(row.getRetryCount() + 1);
            String msg = e.getMessage();
            row.setLastError(msg != null && msg.length() > 1500 ? msg.substring(0, 1500) : msg);
            outboxRepository.save(row);
            log.warn("[WORKFLOW-OUTBOX] Échec rejeu ligne {} (instance={}, v={}) : {}",
                    row.getId(), row.getInstanceId(), row.getVersion(), msg);
            return false;
        }
    }

    private WorkflowEntityStateSnapshot toSnapshot(WorkflowStateOutboxEntity row) {
        return WorkflowEntityStateSnapshot.builder()
                .id(row.getId())
                .instanceId(row.getInstanceId())
                .version(row.getVersion())
                .eventType(row.getEventType())
                .entityName(row.getEntityName())
                .entityPk(row.getEntityPk())
                .operationType(row.getOperationType())
                .definitionCode(row.getDefinitionCode())
                .etapeOrdre(row.getEtapeOrdre())
                .etapeCode(row.getEtapeCode())
                .taskId(row.getTaskId())
                .acteurUserId(row.getActeurUserId())
                .acteurUsername(row.getActeurUsername())
                .acteurPoste(row.getActeurPoste())
                .patchJson(row.getPatchJson())
                .fullSnapshotJson(row.getFullSnapshotJson())
                .attachmentsJson(row.getAttachmentsJson())
                .commentaire(row.getCommentaire())
                .createdAt(row.getCreatedAt())
                .build();
    }
}
