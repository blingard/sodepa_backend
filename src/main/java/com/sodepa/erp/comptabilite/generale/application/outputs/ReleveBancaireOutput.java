package com.sodepa.erp.comptabilite.generale.application.outputs;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Résultat d'un relevé bancaire.
 */
@Builder
public record ReleveBancaireOutput(
    UUID id,
    BanqueInfo banque,
    LocalDate dateReleve,
    BigDecimal soldeInitial,
    BigDecimal soldeFinal,
    boolean valide,
    List<LigneReleveBancaireOutput> lignes
) {
    @Builder
    public record BanqueInfo(UUID id, String nom) {}
}