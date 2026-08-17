package com.sodepa.erp.audit.application.usecase;

import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.configuration.RabbitMqConfig;
import com.sodepa.erp.configuration.RequestTrackingEvent;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service de publication des événements d'audit et de transactions vers les files d'attente RabbitMQ.
 */
@Service
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final RabbitTemplate rabbitTemplate;

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
        rabbitTemplate.convertAndSend(RabbitMqConfig.AUDIT_EXCHANGE, RabbitMqConfig.ROUTING_TRANSACTION, msg);
    }

    /**
     * Publie une activité ou trace d'audit utilisateur vers RabbitMQ.
     * 
     * @param msg l'activité à publier
     */
    public void publishActivity(RequestTrackingEvent msg) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.AUDIT_EXCHANGE, RabbitMqConfig.ROUTING_ACTIVITY, msg);
    }

    /**
     * Publie un événement Maker-Checker vers RabbitMQ.
     * 
     * @param msg l'événement Maker-Checker à publier
     */
    public void publishMakerChecker(MakerCheckerMessageInput msg) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.AUDIT_EXCHANGE, RabbitMqConfig.ROUTING_MAKER_CHECKER, msg);
    }
}
