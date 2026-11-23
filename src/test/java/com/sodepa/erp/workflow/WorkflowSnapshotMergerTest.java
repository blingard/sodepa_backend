package com.sodepa.erp.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.workflow.application.WorkflowSnapshotMerger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowSnapshotMergerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WorkflowSnapshotMerger merger = new WorkflowSnapshotMerger(objectMapper);

    @Test
    @DisplayName("Fusion incrémentale : deep-merge sur maps, remplacement sur scalaires/listes et accumulation des pièces MinIO")
    @SuppressWarnings("unchecked")
    void shouldMergeStepPatchesAndAccumulateAttachmentsCleanly() {
        Map<String, Object> v1 = new LinkedHashMap<>();
        v1.put("motif", "Achat initial");
        v1.put("budgetDisponible", false);
        v1.put("meta", Map.of("source", "PORTAIL", "urgent", false));
        v1.put("lignes", List.of(Map.of("designation", "Item A", "quantite", 2)));

        UUID task1 = UUID.randomUUID();
        Map<String, Object> patchStep1 = new LinkedHashMap<>();
        patchStep1.put("budgetDisponible", true);
        patchStep1.put("ligneBudgetaireImputation", "605100-SODEPA");
        patchStep1.put("meta", Map.of("urgent", true, "verificateur", "DAF"));
        patchStep1.put(WorkflowSnapshotMerger.META_VERSION_KEY, 999);

        WorkflowSnapshotMerger.MergeResult r1 = merger.merge(
                v1,
                Collections.emptyList(),
                patchStep1,
                "workflows/inst/etapes/E1/doc1.pdf",
                "VISA_DAF",
                1,
                task1,
                "daf.user",
                "POSTE_DAF"
        );

        assertEquals("Achat initial", r1.domainSnapshot().get("motif"));
        assertEquals(true, r1.domainSnapshot().get("budgetDisponible"));
        assertEquals("605100-SODEPA", r1.domainSnapshot().get("ligneBudgetaireImputation"));
        assertFalse(r1.domainSnapshot().containsKey(WorkflowSnapshotMerger.META_VERSION_KEY));

        Map<String, Object> mergedMeta = (Map<String, Object>) r1.domainSnapshot().get("meta");
        assertEquals("PORTAIL", mergedMeta.get("source"));
        assertEquals(true, mergedMeta.get("urgent"));
        assertEquals("DAF", mergedMeta.get("verificateur"));
        assertEquals(1, r1.attachments().size());
        assertEquals("workflows/inst/etapes/E1/doc1.pdf", r1.attachments().get(0).get("url"));

        UUID task2 = UUID.randomUUID();
        Map<String, Object> patchStep2 = Map.of(
                "lignes", List.of(
                        Map.of("designation", "Item A modifié", "quantite", 4),
                        Map.of("designation", "Item B", "quantite", 1)
                )
        );
        WorkflowSnapshotMerger.MergeResult r2 = merger.merge(
                r1.domainSnapshot(),
                r1.attachments(),
                patchStep2,
                "workflows/inst/etapes/E2/visa_dg.pdf",
                "VISA_DG",
                2,
                task2,
                "dg.user",
                "POSTE_DG"
        );

        List<Map<String, Object>> lignes = (List<Map<String, Object>>) r2.domainSnapshot().get("lignes");
        assertEquals(2, lignes.size());
        assertEquals("Item A modifié", lignes.get(0).get("designation"));
        assertEquals(2, r2.attachments().size());
        assertEquals("VISA_DG", r2.attachments().get(1).get("etapeCode"));
    }

    @Test
    @DisplayName("Cache instance : extraction fiable version, operationType et pièces jointes hors ClickHouse")
    void shouldPersistAndExtractInternalCacheMetadataCleanly() {
        Map<String, Object> domain = Map.of("motif", "Test cache", "lignes", List.of(Map.of("quantite", 3)));
        List<Map<String, Object>> attachments = List.of(Map.of("url", "minio://doc.pdf", "etapeCode", "E1"));

        Map<String, Object> cache = merger.buildInstanceCachePayload(domain, attachments, 3, "CREATE");
        String json = merger.toJson(cache);

        assertEquals(3, merger.extractCachedVersion(json));
        assertEquals("CREATE", merger.extractCachedOperationType(json));
        assertEquals(1, merger.extractCachedAttachments(json).size());

        Map<String, Object> stripped = merger.stripInternalMetadata(merger.parseMap(json));
        assertEquals("Test cache", stripped.get("motif"));
        assertFalse(stripped.containsKey(WorkflowSnapshotMerger.META_VERSION_KEY));
        assertFalse(stripped.containsKey(WorkflowSnapshotMerger.META_ATTACHMENTS_KEY));
        assertFalse(stripped.containsKey(WorkflowSnapshotMerger.META_OPERATION_TYPE_KEY));
    }
}
