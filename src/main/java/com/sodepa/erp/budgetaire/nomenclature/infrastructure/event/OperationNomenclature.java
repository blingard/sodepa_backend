package com.sodepa.erp.budgetaire.nomenclature.infrastructure.event;

import com.sodepa.erp.utils.MakerCheckerOperationType;

/**
 * Opération demandée sur un élément de la nomenclature.
 *
 * <p>
 * Le maker-checker ne connaît que quatre types d'opération ; la réactivation
 * n'en a pas. L'opération exacte voyage donc dans la charge utile, et le type
 * maker-checker n'en est que le reflet, pour les écrans et l'audit.
 * </p>
 */
public enum OperationNomenclature {

    CREER(MakerCheckerOperationType.CREATE),
    MODIFIER(MakerCheckerOperationType.UPDATE),
    DESACTIVER(MakerCheckerOperationType.DELETE),
    REACTIVER(MakerCheckerOperationType.UPDATE);

    private final MakerCheckerOperationType typeMakerChecker;

    OperationNomenclature(MakerCheckerOperationType typeMakerChecker) {
        this.typeMakerChecker = typeMakerChecker;
    }

    public MakerCheckerOperationType typeMakerChecker() {
        return typeMakerChecker;
    }
}
