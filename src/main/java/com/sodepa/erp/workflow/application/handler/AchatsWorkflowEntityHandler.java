package com.sodepa.erp.workflow.application.handler;

import com.sodepa.erp.achats.infrastructure.entities.ExpressionBesoinEntity;
import com.sodepa.erp.achats.infrastructure.entities.StatutExpressionBesoin;
import com.sodepa.erp.achats.infrastructure.repo.ExpressionBesoinRepository;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowTaskEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Handler spécialisé de gestion du cycle de vie du workflow pour le module Achats (ex: Expression de Besoin).
 * <p>
 * Permet l'interaction multi-modules : lorsque le workflow universel avance, ce handler met à jour
 * l'état fonctionnel de l'expression de besoin (BROUILLON -> VALIDEE_HIERARCHIE -> VISEE_BUDGET -> APPROUVEE_DG).
 * </p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class AchatsWorkflowEntityHandler implements WorkflowEntityHandler {

    private final ExpressionBesoinRepository expressionBesoinRepository;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private com.sodepa.erp.achats.application.AchatsService achatsService;

    private static final String ENTITY_EXPRESSION_BESOIN = "EXPRESSION_BESOIN";
    private static final String ENTITY_EB_SHORT = "EB";
    private static final String ENTITY_CLASS = ExpressionBesoinEntity.class.getName();
    private static final String ENTITY_SIMPLE = ExpressionBesoinEntity.class.getSimpleName();

    private static final String ENTITY_FOURNISSEUR = "FOURNISSEUR";
    private static final String ENTITY_FRN_SHORT = "FRN";
    private static final String ENTITY_FRN_CLASS = com.sodepa.erp.achats.infrastructure.entities.FournisseurEntity.class.getName();
    private static final String ENTITY_FRN_SIMPLE = com.sodepa.erp.achats.infrastructure.entities.FournisseurEntity.class.getSimpleName();

    private boolean isFournisseurEntity(String entityName) {
        if (entityName == null) {
            return false;
        }
        String clean = entityName.trim();
        return clean.equalsIgnoreCase(ENTITY_FOURNISSEUR)
                || clean.equalsIgnoreCase(ENTITY_FRN_SHORT)
                || clean.equalsIgnoreCase(ENTITY_FRN_SIMPLE)
                || clean.equals(ENTITY_FRN_CLASS);
    }

    private boolean isExpressionBesoinEntity(String entityName) {
        if (entityName == null) {
            return false;
        }
        String clean = entityName.trim();
        return clean.equalsIgnoreCase(ENTITY_EXPRESSION_BESOIN)
                || clean.equalsIgnoreCase(ENTITY_EB_SHORT)
                || clean.equalsIgnoreCase(ENTITY_SIMPLE)
                || clean.equals(ENTITY_CLASS);
    }

    @Override
    public boolean supports(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            return false;
        }
        return isExpressionBesoinEntity(entityName) || isFournisseurEntity(entityName);
    }

    @Override
    @Transactional
    public void onStepApproved(WorkflowTaskEntity task, Map<String, Object> modifiedFields) {
        if (task == null || task.getWorkflowInstance() == null) {
            return;
        }
        // Aucune écriture ni mutation en table métier PostgreSQL pendant les étapes intermédiaires :
        // l'état consolidé et les pièces jointes sont historisés étape par étape dans ClickHouse / Outbox.
        WorkflowInstanceEntity instance = task.getWorkflowInstance();
        String codeEtape = task.getEtapeDefinition() != null ? task.getEtapeDefinition().getCodeEtape() : "N/A";
        log.debug("[ACHATS-WORKFLOW] Étape {} validée sur instance {} ({}) — état conservé dans ClickHouse avant clôture.",
                codeEtape, instance.getId(), instance.getEntityId());
    }

    @Override
    @Transactional
    public void onWorkflowCompleted(WorkflowInstanceEntity instance) {
        onWorkflowCompleted(instance, null);
    }

    @Override
    @Transactional
    public void onWorkflowCompleted(WorkflowInstanceEntity instance, Map<String, Object> finalData) {
        if (instance == null) {
            return;
        }
        UUID entityId = parseUuid(instance.getEntityId());
        if (entityId == null) {
            return;
        }

        if (isFournisseurEntity(instance.getEntityName())) {
            if (finalData != null && !finalData.isEmpty()) {
                achatsService.finaliserFournisseurDepuisWorkflow(entityId, finalData);
            }
            return;
        }

        if (expressionBesoinRepository.findById(entityId).isPresent()) {
            expressionBesoinRepository.findById(entityId).ifPresent(eb -> {
                if (eb.getStatut() != StatutExpressionBesoin.APPROUVEE_DG) {
                    eb.setStatut(StatutExpressionBesoin.APPROUVEE_DG);
                    expressionBesoinRepository.save(eb);
                }
                log.info("[ACHATS-WORKFLOW] Expression de besoin {} validée et totalement APPROUVEE_DG", eb.getNumero());
            });
            return;
        }

        // Cas d'un workflow démarré directement sans ticket Maker-Checker : matérialisation finale à partir de finalData
        if (finalData != null && !finalData.isEmpty()) {
            achatsService.finaliserExpressionBesoinDepuisWorkflow(entityId, finalData);
        }
    }

    @Override
    @Transactional
    public void onWorkflowRejected(WorkflowInstanceEntity instance, String motif) {
        if (instance == null) {
            return;
        }
        // Zéro création et zéro écrasement destructif dans achat_expressions_besoin sur rejet workflow
        log.warn("[ACHATS-WORKFLOW] Instance {} ({}) REJETEE sans matérialisation PostgreSQL. Motif: {}",
                instance.getId(), instance.getEntityId(), motif);
    }

    private UUID parseUuid(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(idStr.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Identifiant entité non UUID pour ExpressionBesoin: {}", idStr);
            return null;
        }
    }
}
