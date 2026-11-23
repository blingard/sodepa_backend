package com.sodepa.erp.audit.application.usecase;

import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.DuckDbAuditManager;
import com.sodepa.erp.configuration.RabbitMqConfig;
import com.sodepa.erp.configuration.RequestTrackingEvent;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service de publication des événements d'audit et de transactions vers les files d'attente RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    private DuckDbAuditManager duckDbAuditManager;

    /**
     * DTO représentant le message d'une transaction comptable répliquée.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionMessage {
        UUID id;
        String compteCode;
        BigDecimal debit;
        BigDecimal credit;
        String libelle;
        LocalDate dateComptable;
        UUID sectionId;
    }

    /**
     * DTO représentant le message d'audit d'une action utilisateur.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityMessage {
        UUID id;
        String userId;
        String username;
        String sessionId;
        String action;
        String method;
        String uri;
        LocalDateTime timestamp;
        String ipAddress;
    }

    /**
     * Publie une transaction financière vers RabbitMQ.
     * 
     * @param msg la transaction comptable à publier
     */
    public void publishTransaction(TransactionMessage msg) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.AUDIT_EXCHANGE, RabbitMqConfig.ROUTING_TRANSACTION, msg);
        } catch (AmqpException e) {
            log.warn("RabbitMQ indisponible — publication transaction différée ignorée pour {} : {}",
                    msg != null ? msg.getId() : null, e.getMessage());
        }
    }

    /**
     * Publie une activité ou trace d'audit utilisateur vers RabbitMQ, avec repli non bloquant vers DuckDB.
     *
     * @param msg l'activité à publier
     */
    public void publishActivity(RequestTrackingEvent msg) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.AUDIT_EXCHANGE, RabbitMqConfig.ROUTING_ACTIVITY, msg);
        } catch (AmqpException e) {
            log.warn("RabbitMQ indisponible — bascule de l'audit HTTP {} {} vers DuckDB : {}",
                    msg != null ? msg.httpMethod() : null, msg != null ? msg.uri() : null, e.getMessage());
            if (duckDbAuditManager != null && msg != null) {
                try {
                    duckDbAuditManager.saveUserActivity(
                            msg.userId(),
                            msg.username(),
                            msg.sessionId(),
                            "HTTP_" + msg.responseStatus(),
                            msg.httpMethod(),
                            msg.uri(),
                            msg.correlationId(),
                            msg.remoteIp()
                    );
                } catch (Exception duckEx) {
                    log.debug("Échec repli DuckDB ignoré : {}", duckEx.getMessage());
                }
            }
        }
    }

    /**
     * Publie un événement Maker-Checker vers RabbitMQ.
     *
     * @param msg l'événement Maker-Checker à publier
     */
    public void publishMakerChecker(MakerCheckerMessageInput msg) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.AUDIT_EXCHANGE, RabbitMqConfig.ROUTING_MAKER_CHECKER, msg);
        } catch (AmqpException e) {
            log.warn("RabbitMQ indisponible — événement Maker-Checker ignoré pour {} ({}) : {}",
                    msg != null ? msg.entityName() : null, msg != null ? msg.entityPk() : null, e.getMessage());
        }
    }
}
