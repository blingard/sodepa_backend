package com.sodepa.erp.comptabilite.generale.application.inputs;

import com.sodepa.erp.utils.StatutImmobilisation;
import org.springframework.data.domain.Pageable;

/**
 * Critères de recherche du registre des immobilisations.
 *
 * @param pageable pagination et tri
 * @param recherche fragment cherché dans le code ou la désignation ; {@code null} pour tout
 * @param statut restriction sur l'état du bien ; {@code null} pour tous
 */
public record RechercheImmoInput(
    Pageable pageable,
    String recherche,
    StatutImmobilisation statut
) {
}
