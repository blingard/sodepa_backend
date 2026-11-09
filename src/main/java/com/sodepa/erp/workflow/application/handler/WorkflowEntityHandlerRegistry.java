package com.sodepa.erp.workflow.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Registre Spring injectant dynamiquement tous les {@link WorkflowEntityHandler} disponibles
 * dans le conteneur applicatif SODEPA.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEntityHandlerRegistry {

    private final List<WorkflowEntityHandler> handlers;

    /**
     * Recherche le handler approprié pour un type d'entité métier donné.
     *
     * @param entityName Nom technique de l'entité cible.
     * @return Le handler correspondant s'il existe, sinon {@link Optional#empty()}.
     */
    public Optional<WorkflowEntityHandler> getHandler(String entityName) {
        if (entityName == null || handlers == null) {
            return Optional.empty();
        }
        return handlers.stream()
                .filter(h -> h.supports(entityName))
                .findFirst();
    }
}
