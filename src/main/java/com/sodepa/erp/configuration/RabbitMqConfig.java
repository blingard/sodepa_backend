package com.sodepa.erp.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration AMQP pour RabbitMQ déclarant les échangeurs, les files d'attente et la sérialisation JSON.
 */
@Configuration
public class RabbitMqConfig {

    public static final String AUDIT_EXCHANGE = "sodepa.audit.exchange";
    public static final String TRANSACTIONS_QUEUE = "sodepa.audit.transactions.queue";
    public static final String ACTIVITIES_QUEUE = "sodepa.audit.activities.queue";

    public static final String MAKER_CHECKER_QUEUE = "sodepa.audit.makerchecker.queue";

    public static final String ROUTING_TRANSACTION = "audit.transaction";
    public static final String ROUTING_ACTIVITY = "audit.activity";
    public static final String ROUTING_MAKER_CHECKER = "audit.makerchecker";

    /**
     * Déclare l'échangeur principal de type Topic pour router les messages d'audit.
     * 
     * @return le TopicExchange
     */
    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(AUDIT_EXCHANGE);
    }

    /**
     * Déclare la file d'attente pour la réplication analytique des transactions comptables.
     * 
     * @return la Queue
     */
    @Bean
    public Queue transactionsQueue() {
        return QueueBuilder.durable(TRANSACTIONS_QUEUE).build();
    }

    /**
     * Déclare la file d'attente pour le tracking des actions utilisateurs.
     * 
     * @return la Queue
     */
    @Bean
    public Queue activitiesQueue() {
        return QueueBuilder.durable(ACTIVITIES_QUEUE).build();
    }

    /**
     * Lie la file des transactions à l'échangeur avec la clé de routage spécifique.
     */
    @Bean
    public Binding bindingTransactions(Queue transactionsQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(transactionsQueue).to(auditExchange).with(ROUTING_TRANSACTION);
    }

    /**
     * Lie la file d'activité utilisateur à l'échangeur avec la clé de routage spécifique.
     */
    @Bean
    public Binding bindingActivities(Queue activitiesQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(activitiesQueue).to(auditExchange).with(ROUTING_ACTIVITY);
    }

    /**
     * Déclare la file d'attente pour l'audit des événements Maker-Checker.
     *
     * @return la Queue
     */
    @Bean
    public Queue makerCheckerQueue() {
        return QueueBuilder.durable(MAKER_CHECKER_QUEUE).build();
    }

    /**
     * Lie la file Maker-Checker à l'échangeur avec la clé de routage spécifique.
     */
    @Bean
    public Binding bindingMakerChecker(Queue makerCheckerQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(makerCheckerQueue).to(auditExchange).with(ROUTING_MAKER_CHECKER);
    }

    /**
     * Configure la sérialisation des messages en JSON pour la transmission AMQP.
     * 
     * @return le MessageConverter configuré avec Jackson
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
