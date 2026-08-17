package com.sodepa.erp.audit.infrastructure.listener;

import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.application.usecase.AuditEventPublisher;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.configuration.RabbitMqConfig;
import com.sodepa.erp.configuration.RequestTrackingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consommateur de messages RabbitMQ chargé de l'insertion asynchrone dans ClickHouse (OLAP).
 */
@Component
@RequiredArgsConstructor
public class AuditRabbitListener {

    private final ClickHouseManager clickHouseManager;

    /**
     * Consomme les événements de transactions comptables validées et les enregistre dans ClickHouse.
     * 
     * @param msg le message contenant les détails de la transaction
     */
    @RabbitListener(queues = RabbitMqConfig.TRANSACTIONS_QUEUE)
    public void consumeTransaction(AuditEventPublisher.TransactionMessage msg) {
        clickHouseManager.saveTransaction(
                msg.getId(),
                msg.getCompteCode(),
                msg.getDebit(),
                msg.getCredit(),
                msg.getLibelle(),
                msg.getDateComptable(),
                msg.getSectionId()
        );
    }

    /**
     * Consomme les événements de tracking d'activité utilisateur et les enregistre dans ClickHouse.
     * 
     * @param msg le message contenant les détails de l'activité
     */
    @RabbitListener(queues = RabbitMqConfig.ACTIVITIES_QUEUE)
    public void consumeActivity(RequestTrackingEvent msg) {
        clickHouseManager.saveActivity(msg);
    }

    /**
     * Consomme les événements Maker-Checker et les enregistre dans ClickHouse.
     * 
     * @param msg le message contenant les détails de l'événement Maker-Checker
     */
    @RabbitListener(queues = RabbitMqConfig.MAKER_CHECKER_QUEUE)
    public void consumeMakerChecker(MakerCheckerMessageInput msg) {
        clickHouseManager.saveMakerChecker(msg);
    }
}
