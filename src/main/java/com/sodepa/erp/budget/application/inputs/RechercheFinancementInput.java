package com.sodepa.erp.budget.application.inputs;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Critères de recherche des financements.
 *
 * @param pageable pagination et tri
 * @param banqueId prêteur ; {@code null} pour tous
 * @param type nature du financement (PRET, LEASING…) ; {@code null} pour tous
 */
public record RechercheFinancementInput(
    Pageable pageable,
    UUID banqueId,
    String type
) {
}
