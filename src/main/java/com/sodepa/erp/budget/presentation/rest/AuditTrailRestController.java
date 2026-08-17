package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.outputs.AuditTrailOutput;
import com.sodepa.erp.budget.application.usecase.ConsulterAuditTrailBudgetUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la consultation des traces d'audit (audit trail).
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditTrailRestController {

    private final ConsulterAuditTrailBudgetUseCase consulterAuditTrailBudgetUseCase;

    /**
     * Recherche les logs d'audit pour une entité spécifique.
     * 
     * @param entiteNom le nom simple de la classe de l'entité (ex: 'BudgetPlanEntity')
     * @param entiteId l'identifiant de l'enregistrement de l'entité
     * @return la liste des traces d'audit correspondantes
     */
    @GetMapping("/logs")
    public List<AuditTrailOutput> getLogs(
            @RequestParam String entiteNom,
            @RequestParam UUID entiteId) {
        return consulterAuditTrailBudgetUseCase.execute(new ConsulterAuditTrailBudgetUseCase.Input(entiteNom, entiteId));
    }
}
