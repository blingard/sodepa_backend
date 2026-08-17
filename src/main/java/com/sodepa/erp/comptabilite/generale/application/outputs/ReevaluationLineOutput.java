package com.sodepa.erp.comptabilite.generale.application.outputs;

import lombok.Builder;
import java.math.BigDecimal;

/**
 * Ligne de résultat de la réévaluation.
 */
@Builder
public record ReevaluationLineOutput(
    String compteCode,
    String typeDevise,
    BigDecimal soldeDevise,
    BigDecimal valeurLivreXof,
    BigDecimal coursCloture,
    BigDecimal valeurReevalueeXof,
    BigDecimal ecart,
    String natureEcart
) {}
