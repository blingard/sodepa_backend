package com.sodepa.erp.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.WorkflowEntityStateSnapshot;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.workflow.application.WorkflowSnapshotMerger;
import com.sodepa.erp.workflow.application.WorkflowStateService;
import com.sodepa.erp.workflow.infrastructure.entities.*;
import com.sodepa.erp.workflow.infrastructure.repo.WorkflowStateOutboxRepository;
import com.sodepa.erp.workflow.presentation.responses.WorkflowCurrentDataResponse;
import com.sodepa.erp.workflow.presentation.responses.WorkflowEntityStateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowStateServiceTest {

    @Mock
    private ClickHouseManager clickHouseManager;

    @Mock
    private WorkflowStateOutboxRepository outboxRepository;

    private WorkflowSnapshotMerger merger;
    private WorkflowStateService stateService;

    @BeforeEach
    void setUp() {
        merger = new WorkflowSnapshotMerger(new ObjectMapper());
        stateService = new WorkflowStateService(clickHouseManager, outboxRepository, merger);
    }

    private WorkflowInstanceEntity buildInstance() {
        WorkflowDefinitionEntity def = WorkflowDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .code("WF-ACHATS-EB")
                .build();
        WorkflowEtapeDefinitionEntity etape = WorkflowEtapeDefinitionEntity.builder()
                .id(UUID.randomUUID())
                .codeEtape("VISA_CHEF")
                .ordre(1)
                .build();
        return WorkflowInstanceEntity.builder()
                .id(UUID.randomUUID())
                .workflowDefinition(def)
                .entityName("EXPRESSION_BESOIN")
                .entityId(UUID.randomUUID().toString())
                .statut(StatutWorkflowInstance.EN_COURS)
                .etapeCourante(etape)
                .dateInitiation(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Écriture synchrone ClickHouse nominale : zéro écriture Outbox quand ClickHouse répond")
    void shouldWriteDirectlyToClickHouseWithoutOutboxWhenAvailable() throws Exception {
        WorkflowInstanceEntity instance = buildInstance();
        when(outboxRepository.existsByInstanceIdAndSyncedFalse(instance.getId())).thenReturn(false);
        when(clickHouseManager.findLatestWorkflowEntityState(instance.getId())).thenReturn(Optional.empty());

        WorkflowEntityStateSnapshot v1 = stateService.recordInitiation(
                instance,
                "CREATE",
                Map.of("motif", "Achat Serveur", "montant", 2500000),
                "u1",
                "flora.fuben",
                "POSTE_AGENT"
        );

        assertEquals(1, v1.getVersion());
        assertEquals("CLICKHOUSE", v1.getStorageSource());
        verify(clickHouseManager, times(1)).saveWorkflowEntityState(any());
        verify(outboxRepository, never()).save(any());
        assertEquals(1, merger.extractCachedVersion(instance.getDonneesContexte()));
    }

    @Test
    @DisplayName("Failover Outbox + monotonie de version : si ClickHouse tombe à l'étape 2, Outbox sauve v2 sans collision et rejoue au redémarrage")
    void shouldFailoverToOutboxPreservingVersionAndReplayWhenClickHouseRecovers() throws Exception {
        WorkflowInstanceEntity instance = buildInstance();
        UUID instId = instance.getId();

        // v1 réussit dans ClickHouse
        when(outboxRepository.existsByInstanceIdAndSyncedFalse(instId)).thenReturn(false);
        when(clickHouseManager.findLatestWorkflowEntityState(instId)).thenReturn(Optional.empty());
        WorkflowEntityStateSnapshot v1 = stateService.recordInitiation(
                instance, "CREATE", Map.of("motif", "Initial EB"), "u1", "flora", "POSTE_AGENT");
        assertEquals(1, v1.getVersion());
        assertEquals("CLICKHOUSE", v1.getStorageSource());

        // ClickHouse tombe avant v2
        when(clickHouseManager.findLatestWorkflowEntityState(instId)).thenThrow(new SQLException("Connection refused"));
        doThrow(new SQLException("Connection refused")).when(clickHouseManager).saveWorkflowEntityState(any());

        WorkflowTaskEntity task1 = WorkflowTaskEntity.builder()
                .id(UUID.randomUUID())
                .etapeDefinition(instance.getEtapeCourante())
                .build();

        ArgumentCaptor<WorkflowStateOutboxEntity> outboxCaptor = ArgumentCaptor.forClass(WorkflowStateOutboxEntity.class);
        when(outboxRepository.save(outboxCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        WorkflowEntityStateSnapshot v2 = stateService.recordStepDecision(
                instance,
                task1,
                WorkflowStateService.EVENT_STEP_PATCHED,
                Map.of("budgetDisponible", true, "ligneBudgetaireImputation", "605200"),
                "workflows/inst/e1/justif.pdf",
                "u2",
                "checker",
                "POSTE_DAF",
                "Validé sous secours Outbox"
        );

        // Grâce au cache de version sur l'instance, v2 reçoit bien version=2 (et non 1)
        assertEquals(2, v2.getVersion());
        assertEquals("POSTGRES_OUTBOX", v2.getStorageSource());
        WorkflowStateOutboxEntity savedOutbox = outboxCaptor.getValue();
        assertFalse(savedOutbox.isSynced());
        assertEquals(2, savedOutbox.getVersion());
        assertTrue(savedOutbox.getFullSnapshotJson().contains("605200"));

        // Lecture /current-data pendant panne ClickHouse retourne bien v2 depuis l'Outbox
        when(outboxRepository.existsByInstanceIdAndSyncedFalse(instId)).thenReturn(true);
        when(clickHouseManager.isAvailable()).thenReturn(false);
        when(outboxRepository.findTopByInstanceIdAndSyncedFalseOrderByVersionDescCreatedAtDesc(instId))
                .thenReturn(Optional.of(savedOutbox));

        WorkflowCurrentDataResponse duringOutage = stateService.getCurrentData(instance);
        assertEquals(2, duringOutage.getVersion());
        assertEquals("POSTGRES_OUTBOX", duringOutage.getStorageSource());
        assertFalse(duringOutage.isSyncedToClickHouse());
        assertEquals("605200", duringOutage.getData().get("ligneBudgetaireImputation"));
        assertEquals(1, duringOutage.getAttachments().size());

        // Redémarrage ClickHouse -> replayPendingOutbox pousse v2 et marque synced=true
        doNothing().when(clickHouseManager).saveWorkflowEntityState(any());
        when(clickHouseManager.isAvailable()).thenReturn(true);
        when(clickHouseManager.existsWorkflowEntityState(savedOutbox.getId())).thenReturn(false);
        when(outboxRepository.findBySyncedFalseOrderByCreatedAtAscVersionAsc()).thenReturn(List.of(savedOutbox));

        stateService.replayPendingOutbox();

        assertTrue(savedOutbox.isSynced());
        assertNotNull(savedOutbox.getSyncedAt());
        // v1 (succès) + v2 (tentative échouée vers ClickHouse) + v2 (rejeu réussi depuis Outbox) = 3 appels
        verify(clickHouseManager, times(3)).saveWorkflowEntityState(any());

        // Lecture historique combiné
        when(outboxRepository.existsByInstanceIdAndSyncedFalse(instId)).thenReturn(false);
        when(clickHouseManager.findWorkflowEntityStates(instId)).thenReturn(List.of(v1, v2));
        when(outboxRepository.findByInstanceIdAndSyncedFalseOrderByVersionAscCreatedAtAsc(instId))
                .thenReturn(Collections.emptyList());

        List<WorkflowEntityStateResponse> history = stateService.getStateHistory(instance);
        assertEquals(2, history.size());
        assertEquals(1, history.get(0).getVersion());
        assertEquals(2, history.get(1).getVersion());
        assertTrue(history.get(1).isSyncedToClickHouse());
    }
}
