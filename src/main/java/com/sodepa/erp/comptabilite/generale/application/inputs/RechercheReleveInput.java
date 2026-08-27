package com.sodepa.erp.comptabilite.generale.application.inputs;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Critères de recherche des relevés bancaires.
 *
 * @param pageable pagination et tri
 * @param banqueId banque émettrice ; {@code null} pour toutes
 * @param valide restreint aux relevés rapprochés ou non ; {@code null} pour tous
 */
public record RechercheReleveInput(
    Pageable pageable,
    UUID banqueId,
    Boolean valide
) {
}
