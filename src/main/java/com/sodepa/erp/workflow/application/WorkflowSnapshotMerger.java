package com.sodepa.erp.workflow.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Composant pur de fusion incrémentale d'instantanés d'états de workflow.
 * <p>
 * À chaque validation ou révision d'étape, fusionne les champs modifiés ({@code champsModifiesJson})
 * sur le dernier état connu, accumule les pièces jointes MinIO par étape et sépare les métadonnées
 * internes de secours du payload métier destiné à PostgreSQL et aux APIs de consultation.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowSnapshotMerger {

    public static final String META_ATTACHMENTS_KEY = "_workflowAttachments";
    public static final String META_VERSION_KEY = "_workflowVersion";
    public static final String META_OPERATION_TYPE_KEY = "_workflowOperationType";

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public record MergeResult(
            Map<String, Object> domainSnapshot,
            Map<String, Object> patchApplied,
            List<Map<String, Object>> attachments
    ) {}

    public MergeResult merge(
            Map<String, Object> baseDomainSnapshot,
            List<Map<String, Object>> baseAttachments,
            Map<String, Object> rawPatch,
            String uploadedFileUrl,
            String etapeCode,
            Integer etapeOrdre,
            UUID taskId,
            String actorUsername,
            String actorPoste
    ) {
        Map<String, Object> merged = stripInternalMetadata(baseDomainSnapshot);
        Map<String, Object> cleanPatch = stripInternalMetadata(rawPatch);

        if (!cleanPatch.isEmpty()) {
            deepMergeInto(merged, cleanPatch);
        }

        List<Map<String, Object>> attachments = copyAttachments(baseAttachments);
        if (uploadedFileUrl != null && !uploadedFileUrl.isBlank()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("url", uploadedFileUrl);
            entry.put("etapeCode", etapeCode);
            entry.put("etapeOrdre", etapeOrdre);
            entry.put("taskId", taskId != null ? taskId.toString() : null);
            entry.put("uploadedBy", actorUsername);
            entry.put("poste", actorPoste);
            entry.put("uploadedAt", Instant.now().toString());
            attachments.add(entry);
        }

        return new MergeResult(merged, cleanPatch, attachments);
    }

    public Map<String, Object> combineInitialPayload(Map<String, Object> contexte, Map<String, Object> initialData) {
        Map<String, Object> combined = new LinkedHashMap<>();
        if (contexte != null && !contexte.isEmpty()) {
            deepMergeInto(combined, stripInternalMetadata(contexte));
        }
        if (initialData != null && !initialData.isEmpty()) {
            deepMergeInto(combined, stripInternalMetadata(initialData));
        }
        return combined;
    }

    public Map<String, Object> stripInternalMetadata(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (META_ATTACHMENTS_KEY.equals(key)
                    || META_VERSION_KEY.equals(key)
                    || META_OPERATION_TYPE_KEY.equals(key)) {
                continue;
            }
            copy.put(key, deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    public Map<String, Object> buildInstanceCachePayload(
            Map<String, Object> domainSnapshot,
            List<Map<String, Object>> attachments,
            int version,
            String operationType
    ) {
        Map<String, Object> cache = stripInternalMetadata(domainSnapshot);
        cache.put(META_VERSION_KEY, version);
        if (operationType != null && !operationType.isBlank()) {
            cache.put(META_OPERATION_TYPE_KEY, operationType);
        }
        if (attachments != null && !attachments.isEmpty()) {
            cache.put(META_ATTACHMENTS_KEY, copyAttachments(attachments));
        }
        return cache;
    }

    public int extractCachedVersion(String json) {
        Map<String, Object> map = parseMap(json);
        Object val = map.get(META_VERSION_KEY);
        if (val instanceof Number num) {
            return num.intValue();
        }
        if (val != null) {
            try {
                return Integer.parseInt(val.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public String extractCachedOperationType(String json) {
        Map<String, Object> map = parseMap(json);
        Object val = map.get(META_OPERATION_TYPE_KEY);
        return val != null ? val.toString() : null;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> extractCachedAttachments(String json) {
        Map<String, Object> map = parseMap(json);
        Object val = map.get(META_ATTACHMENTS_KEY);
        if (val instanceof List<?> rawList) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> copied = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        if (e.getKey() != null) {
                            copied.put(e.getKey().toString(), e.getValue());
                        }
                    }
                    result.add(copied);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    public Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, MAP_TYPE);
            return parsed != null ? parsed : new LinkedHashMap<>();
        } catch (Exception e) {
            log.debug("Impossible de parser JSON en Map: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    public List<Map<String, Object>> parseListOfMaps(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> parsed = objectMapper.readValue(json, LIST_MAP_TYPE);
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            log.debug("Impossible de parser JSON en List<Map>: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de sérialisation JSON de l'état workflow: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void deepMergeInto(Map<String, Object> target, Map<String, Object> patch) {
        for (Map.Entry<String, Object> entry : patch.entrySet()) {
            String key = entry.getKey();
            Object newVal = entry.getValue();
            Object existingVal = target.get(key);
            if (existingVal instanceof Map<?, ?> existingMap && newVal instanceof Map<?, ?> newMap) {
                Map<String, Object> nestedTarget = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : existingMap.entrySet()) {
                    if (e.getKey() != null) {
                        nestedTarget.put(e.getKey().toString(), deepCopyValue(e.getValue()));
                    }
                }
                Map<String, Object> nestedPatch = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : newMap.entrySet()) {
                    if (e.getKey() != null) {
                        nestedPatch.put(e.getKey().toString(), e.getValue());
                    }
                }
                deepMergeInto(nestedTarget, nestedPatch);
                target.put(key, nestedTarget);
            } else {
                target.put(key, deepCopyValue(newVal));
            }
        }
    }

    private List<Map<String, Object>> copyAttachments(List<Map<String, Object>> source) {
        List<Map<String, Object>> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }
        for (Map<String, Object> item : source) {
            if (item != null) {
                copy.add(new LinkedHashMap<>(item));
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> m) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null) {
                    copy.put(e.getKey().toString(), deepCopyValue(e.getValue()));
                }
            }
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object item : list) {
                copy.add(deepCopyValue(item));
            }
            return copy;
        }
        return value;
    }
}
