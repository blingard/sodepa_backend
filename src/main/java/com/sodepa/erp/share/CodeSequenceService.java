package com.sodepa.erp.share;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Générateur atomique de numéros séquentiels basé sur des séquences PostgreSQL
 * (avec repli mémoire thread-safe pour les tests unitaires hors base relationnelle).
 */
@Slf4j
@Service
public class CodeSequenceService {

    private static final Pattern SAFE_SEQ_NAME = Pattern.compile("^seq_code_[a-z0-9_]{1,48}$");
    private static final ConcurrentHashMap<String, AtomicLong> FALLBACK_COUNTERS = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Object> sequenceLocks = new ConcurrentHashMap<>();
    private final Set<String> synchronizedSequences = ConcurrentHashMap.newKeySet();

    @Autowired(required = false)
    private EntityManager entityManager;

    /**
     * Alloue de façon atomique le prochain numéro séquentiel (jamais inférieur au nombre d'enregistrements déjà présents).
     */
    public long nextSequenceValue(String sequenceName, LongSupplier existingCountSupplier) {
        if (sequenceName == null || !SAFE_SEQ_NAME.matcher(sequenceName).matches()) {
            throw new IllegalArgumentException("Nom de séquence invalide : " + sequenceName);
        }
        long existing = existingCountSupplier != null ? Math.max(0L, existingCountSupplier.getAsLong()) : 0L;
        if (entityManager == null) {
            return allocateMemoryFallback(sequenceName, existing);
        }

        if (!synchronizedSequences.contains(sequenceName)) {
            Object lock = sequenceLocks.computeIfAbsent(sequenceName, k -> new Object());
            synchronized (lock) {
                if (!synchronizedSequences.contains(sequenceName)) {
                    if (existing > 0L) {
                        entityManager.createNativeQuery(
                                        "SELECT setval('public." + sequenceName + "', " +
                                                "GREATEST((SELECT last_value FROM public." + sequenceName + "), ?1), true)")
                                .setParameter(1, existing)
                                .getSingleResult();
                    }
                    synchronizedSequences.add(sequenceName);
                }
            }
        }

        Object raw = entityManager.createNativeQuery("SELECT nextval('public." + sequenceName + "')")
                .getSingleResult();
        return ((Number) raw).longValue();
    }

    /**
     * Helper statique compatible avec les services instanciés sans contexte Spring (tests Mockito).
     */
    public static long nextOrCount(CodeSequenceService service, String sequenceName, LongSupplier existingCountSupplier) {
        if (service != null) {
            return service.nextSequenceValue(sequenceName, existingCountSupplier);
        }
        long existing = existingCountSupplier != null ? Math.max(0L, existingCountSupplier.getAsLong()) : 0L;
        return allocateMemoryFallback(sequenceName, existing);
    }

    private static long allocateMemoryFallback(String sequenceName, long existing) {
        return FALLBACK_COUNTERS
                .computeIfAbsent(sequenceName, k -> new AtomicLong(existing))
                .updateAndGet(prev -> Math.max(prev + 1L, existing + 1L));
    }
}
