package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.ValiderEtapeInput;
import com.sodepa.erp.budget.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation permettant la validation d'une étape de workflow budgétaire
 * (ex: engagement, liquidation ou mandatement).
 * <p>
 * Délègue l'exécution de la validation et la mise à jour des statuts au {@link RapprochementAdapter}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ValiderEtapeWorkflowUseCase implements UseCase<ValiderEtapeInput, Void> {

    /** Adaptateur de persistance et de rapprochement budgétaire. */
    private final RapprochementAdapter adapter;

    /**
     * Exécute la validation d'étape pour l'engagement budgétaire cible.
     *
     * @param input Données d'entrée contenant l'identifiant de l'engagement, l'étape et la décision.
     * @return {@code null} (opération sans valeur de retour).
     */
    @Override
    public Void execute(ValiderEtapeInput input) {
        adapter.validerEtapeWorkflow(input);
        return null;
    }
}
