package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.outputs.AuditTrailOutput;
import com.sodepa.erp.budget.application.usecase.ConsulterAuditTrailBudgetUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Service de consultation de la Piste d'Audit Budgétaire (Module 02 - BUD / Audit).
 * <p>
 * Permet aux auditeurs internes (DCAI), contrôleurs de gestion et commissaires aux comptes
 * de retracer l'historique complet et inaltérable des événements survenus sur tout objet financier :
 * <ul>
 *   <li>Modifications et arbitrages apportés aux budgets annuels.</li>
 *   <li>Virements et réallocations d'enveloppes de crédits (avec motifs et décisions).</li>
 *   <li>Engagements de dépenses, liquidations comptables et annulations.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Piste d'Audit Budgétaire", description = "Historique inaltérable et traçabilité des opérations budgétaires")
public class AuditTrailRestController {

    private final ConsulterAuditTrailBudgetUseCase consulterAuditTrailBudgetUseCase;

    /**
     * Recherche et restitue l'historique chronologique des actions effectuées sur un enregistrement financier.
     * <p>
     * Chaque entrée d'audit précise l'action réalisée (CREATION, APPROBATION, REALLOCATION, ANNULATION),
     * l'horodatage exact, l'identité de l'agent opérateur et les détails des modifications.
     * 
     * @param entiteNom type de l'objet audité (ex: 'BudgetPlanEntity', 'BudgetItemEntity', 'BudgetEngagementEntity')
     * @param entiteId identifiant unique de l'enregistrement concerné
     * @return la chronologie complète des événements d'audit correspondants
     */
    @GetMapping("/logs")
    @Operation(summary = "Consulter l'historique d'audit d'une entité budgétaire")
    public List<AuditTrailOutput> getLogs(
            @RequestParam String entiteNom,
            @RequestParam UUID entiteId) {
        return consulterAuditTrailBudgetUseCase.execute(new ConsulterAuditTrailBudgetUseCase.Input(entiteNom, entiteId));
    }
}
