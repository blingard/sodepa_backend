package com.sodepa.erp.comptabilite.generale.application.inputs;

import lombok.Builder;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entrée pour une ligne d'écriture comptable.
 */
@Builder
public record LigneInput(
        String compteCode,
        UUID tiersId,
        BigDecimal debit,
        BigDecimal credit,
        String libelleLigne
) {}
