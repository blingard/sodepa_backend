package com.sodepa.erp.budgetaire.commun;

import com.sodepa.erp.utils.MakerCheckerOperationType;

/**
 * Opération demandée sur un référentiel budgétaire (nomenclature, structures…).
 *
 * <p>
 * Le maker-checker ne connaît que quatre types d'opération ; la réactivation
 * n'en a pas. L'opération exacte voyage donc dans la charge utile, et le type
 * maker-checker n'en est que le reflet, pour les écrans et l'audit.
 * </p>
 */
public enum OperationReferentiel {

    CREER(MakerCheckerOperationType.CREATE),
    MODIFIER(MakerCheckerOperationType.UPDATE),
    DESACTIVER(MakerCheckerOperationType.DELETE),
    REACTIVER(MakerCheckerOperationType.UPDATE);

    private final MakerCheckerOperationType typeMakerChecker;

    OperationReferentiel(MakerCheckerOperationType typeMakerChecker) {
        this.typeMakerChecker = typeMakerChecker;
    }

    public MakerCheckerOperationType typeMakerChecker() {
        return typeMakerChecker;
    }
}
