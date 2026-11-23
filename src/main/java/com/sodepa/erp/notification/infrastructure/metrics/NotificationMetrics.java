package com.sodepa.erp.notification.infrastructure.metrics;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.OutboxStatus;
import com.sodepa.erp.notification.infrastructure.repo.NotificationOutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Instrumentation Micrometer / Prometheus du pipeline multi-canal des notifications ERP SODEPA.
 */
@Component
public class NotificationMetrics {

    private static final List<OutboxStatus> PENDING_STATUSES = List.of(
            OutboxStatus.PENDING, OutboxStatus.PUBLISHED, OutboxStatus.FAILED
    );

    private final MeterRegistry meterRegistry;
    private final NotificationOutboxRepository outboxRepository;

    @Autowired
    public NotificationMetrics(
            @Autowired(required = false) MeterRegistry meterRegistry,
            @Autowired(required = false) NotificationOutboxRepository outboxRepository) {
        this.meterRegistry = meterRegistry;
        this.outboxRepository = outboxRepository;
    }

    @PostConstruct
    void registerGauges() {
        if (meterRegistry != null && outboxRepository != null) {
            Gauge.builder("sodepa.notifications.outbox.pending", this, NotificationMetrics::countPendingOutbox)
                    .description("Nombre de notifications en attente ou en échec temporaire dans l'Outbox")
                    .register(meterRegistry);
        }
    }

    private double countPendingOutbox() {
        try {
            return outboxRepository != null ? outboxRepository.countByStatusIn(PENDING_STATUSES) : 0d;
        } catch (Exception e) {
            return 0d;
        }
    }

    public void recordSent(CanalNotification canal, String provider, long elapsedNanos) {
        if (meterRegistry == null) {
            return;
        }
        String canalTag = safeTag(canal != null ? canal.name() : "UNKNOWN");
        String providerTag = safeTag(provider);
        Counter.builder("sodepa.notifications.sent.total")
                .tag("canal", canalTag)
                .tag("provider", providerTag)
                .register(meterRegistry)
                .increment();
        Timer.builder("sodepa.notifications.dispatch.duration")
                .tag("canal", canalTag)
                .tag("provider", providerTag)
                .tag("outcome", "SENT")
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(0L, elapsedNanos)));
    }

    public void recordFailed(CanalNotification canal, String provider, long elapsedNanos) {
        if (meterRegistry == null) {
            return;
        }
        String canalTag = safeTag(canal != null ? canal.name() : "UNKNOWN");
        String providerTag = safeTag(provider);
        Counter.builder("sodepa.notifications.failed.total")
                .tag("canal", canalTag)
                .tag("provider", providerTag)
                .register(meterRegistry)
                .increment();
        Timer.builder("sodepa.notifications.dispatch.duration")
                .tag("canal", canalTag)
                .tag("provider", providerTag)
                .tag("outcome", "FAILED")
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(0L, elapsedNanos)));
    }

    public void recordSkipped(CanalNotification canal, String reason) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("sodepa.notifications.skipped.total")
                .tag("canal", safeTag(canal != null ? canal.name() : "UNKNOWN"))
                .tag("reason", safeTag(reason))
                .register(meterRegistry)
                .increment();
    }

    public void recordFallback(CanalNotification fromCanal, CanalNotification toCanal) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("sodepa.notifications.fallback.total")
                .tag("from", safeTag(fromCanal != null ? fromCanal.name() : "UNKNOWN"))
                .tag("to", safeTag(toCanal != null ? toCanal.name() : "UNKNOWN"))
                .register(meterRegistry)
                .increment();
    }

    private String safeTag(String raw) {
        return (raw == null || raw.isBlank()) ? "none" : raw.trim();
    }
}
