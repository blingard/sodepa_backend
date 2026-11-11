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

    private static final String ENTITY_EXPRESSION_BESOIN = "EXPRESSION_BESOIN";
    private static final String ENTITY_EB_SHORT = "EB";
    private static final String ENTITY_CLASS = ExpressionBesoinEntity.class.getName();
    private static final String ENTITY_SIMPLE = ExpressionBesoinEntity.class.getSimpleName();

    @Override
    public boolean supports(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            return false;
        }
        String clean = entityName.trim();
        return clean.equalsIgnoreCase(ENTITY_EXPRESSION_BESOIN)
                || clean.equalsIgnoreCase(ENTITY_EB_SHORT)
                || clean.equalsIgnoreCase(ENTITY_SIMPLE)
                || clean.equals(ENTITY_CLASS);
    }

    @Override
    @Transactional
    public void onStepApproved(WorkflowTaskEntity task, Map<String, Object> modifiedFields) {
        if (task == null || task.getWorkflowInstance() == null) {
            return;
        }
        WorkflowInstanceEntity instance = task.getWorkflowInstance();
        UUID ebId = parseUuid(instance.getEntityId());
        if (ebId == null) {
            return;
        }

        expressionBesoinRepository.findById(ebId).ifPresent(eb -> {
            String codeEtape = task.getEtapeDefinition() != null ? task.getEtapeDefinition().getCodeEtape().toUpperCase() : "";
            String intituleEtape = task.getEtapeDefinition() != null ? task.getEtapeDefinition().getIntitule().toUpperCase() : "";

            if (codeEtape.contains("BUDGET") || intituleEtape.contains("BUDGET") || intituleEtape.contains("FINANCE")) {
                eb.setStatut(StatutExpressionBesoin.VISEE_BUDGET);
                log.info("[ACHATS-WORKFLOW] Expression de besoin {} marquée VISEE_BUDGET à l'étape {}", eb.getNumero(), codeEtape);
            } else if (codeEtape.contains("HIERARCHIE") || intituleEtape.contains("HIERARCH") || intituleEtape.contains("CHEF")) {
                eb.setStatut(StatutExpressionBesoin.VALIDEE_HIERARCHIE);
                log.info("[ACHATS-WORKFLOW] Expression de besoin {} marquée VALIDEE_HIERARCHIE à l'étape {}", eb.getNumero(), codeEtape);
            }
            expressionBesoinRepository.save(eb);
        });
    }

    @Override
    @Transactional
    public void onWorkflowCompleted(WorkflowInstanceEntity instance) {
        if (instance == null) {
            return;
        }
        UUID ebId = parseUuid(instance.getEntityId());
        if (ebId == null) {
            return;
        }

        expressionBesoinRepository.findById(ebId).ifPresent(eb -> {
            eb.setStatut(StatutExpressionBesoin.APPROUVEE_DG);
            expressionBesoinRepository.save(eb);
            log.info("[ACHATS-WORKFLOW] Expression de besoin {} validée et totalement APPROUVEE_DG", eb.getNumero());
        });
    }

    @Override
    @Transactional
    public void onWorkflowRejected(WorkflowInstanceEntity instance, String motif) {
        if (instance == null) {
            return;
        }
        UUID ebId = parseUuid(instance.getEntityId());
        if (ebId == null) {
            return;
        }

        expressionBesoinRepository.findById(ebId).ifPresent(eb -> {
            eb.setStatut(StatutExpressionBesoin.REJETEE);
            expressionBesoinRepository.save(eb);
            log.warn("[ACHATS-WORKFLOW] Expression de besoin {} REJETEE suite à décision workflow. Motif: {}", eb.getNumero(), motif);
        });
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
