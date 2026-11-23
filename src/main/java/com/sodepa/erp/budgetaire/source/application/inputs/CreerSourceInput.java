package com.sodepa.erp.budgetaire.source.application.inputs;

import com.sodepa.erp.budgetaire.source.infrastructure.entities.TypeSourceFinancement;

/**
 * Demande de création d'une source de financement.
 *
 * @param code     code, mis en majuscules par le serveur
 * @param libelle  libellé (« Ressources propres »)
 * @param type     ressources propres, subvention ou partenaire
 * @param bailleur partenaire ou bailleur ; obligatoire pour un partenaire
 */
public record CreerSourceInput(String code, String libelle, TypeSourceFinancement type, String bailleur) {
}
