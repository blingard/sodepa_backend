package com.sodepa.erp.workflow.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Registre Spring injectant dynamiquement tous les {@link WorkflowEntityHandler} disponibles
 * dans le conteneur applicatif SODEPA.
 * <p>
 * Prend en charge l'orchestration multi-modules : lorsqu'un workflow impacte plusieurs modules
 * (ex: Achats, Budget, Comptabilité, RH), tous les handlers abonnés sont exécutés dans l'ordre.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEntityHandlerRegistry {

    private final List<WorkflowEntityHandler> handlers;

    /**
     * Recherche la liste ordonnée de tous les handlers prenant en charge l'entité métier cible.
     * Permet le traitement transverse multi-modules (ex: Achats + Budget + Comptabilité).
     *
     * @param entityName Nom technique de l'entité cible.
     * @return Liste de tous les handlers spécialisés supportant l'entité, ou le fallback par défaut.
     */
    public List<WorkflowEntityHandler> getHandlers(String entityName) {
        if (entityName == null || handlers == null || handlers.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Filtrer les handlers métiers spécifiques supportant l'entité
        List<WorkflowEntityHandler> specificHandlers = handlers.stream()
                .filter(h -> !(h instanceof DefaultWorkflowEntityHandler))
                .filter(h -> {
                    try {
                        return h.supports(entityName);
                    } catch (Exception e) {
                        log.warn("Erreur lors de l'évaluation de supports() sur {}: {}", h.getClass().getSimpleName(), e.getMessage());
                        return false;
                    }
                })
                .toList();

        if (!specificHandlers.isEmpty()) {
            return specificHandlers;
        }

        // 2. Si aucun handler spécifique n'est trouvé, renvoyer le handler par défaut
        return handlers.stream()
                .filter(h -> h instanceof DefaultWorkflowEntityHandler)
                .toList();
    }

    /**
     * Recherche le premier handler approprié pour un type d'entité métier donné (rétrocompatibilité).
     *
     * @param entityName Nom technique de l'entité cible.
     * @return Le premier handler correspondant s'il existe, sinon {@link Optional#empty()}.
     */
    public Optional<WorkflowEntityHandler> getHandler(String entityName) {
        List<WorkflowEntityHandler> list = getHandlers(entityName);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }
}
