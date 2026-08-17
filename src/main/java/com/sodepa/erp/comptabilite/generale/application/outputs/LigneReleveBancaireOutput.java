package com.sodepa.erp.comptabilite.generale.application.outputs;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Résultat d'une ligne de relevé bancaire.
 */
@Builder
public record LigneReleveBancaireOutput(
    UUID id,
    LocalDate dateTransaction,
    String libelle,
    BigDecimal montant,
    boolean rapproche
) {}