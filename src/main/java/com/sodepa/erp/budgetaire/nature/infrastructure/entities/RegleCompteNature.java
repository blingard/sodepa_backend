package com.sodepa.erp.budgetaire.nature.infrastructure.entities;

import java.util.Set;

/**
 * Cohérence entre la section, le sens et le compte SYSCOHADA d'une nature.
 *
 * <table>
 *   <caption>Classes de compte admises</caption>
 *   <tr><th>Section</th><th>Sens</th><th>Comptes</th></tr>
 *   <tr><td>Fonctionnement</td><td>Dépense</td><td>classe 6 ; 81, 83, 85 (charges HAO)</td></tr>
 *   <tr><td>Produits</td><td>Recette</td><td>classe 7 ; 82, 84, 86, 88 (produits HAO)</td></tr>
 *   <tr><td>Investissement</td><td>Dépense</td><td>classe 2 (immobilisations)</td></tr>
 *   <tr><td>Investissement</td><td>Recette</td><td>classe 1 (subventions d'investissement, emprunts)</td></tr>
 * </table>
 *
 * <p>
 * Toute autre combinaison est une erreur de saisie : une vente imputée en
 * charge fausserait à la fois le budget et la comptabilité. La règle est
 * écrite ici plutôt que paramétrée ; elle deviendra un paramètre (US-BUD-112)
 * si la SODEPA y a des exceptions.
 * </p>
 */
public final class RegleCompteNature {

    private static final Set<String> CHARGES_HAO = Set.of("81", "83", "85");
    private static final Set<String> PRODUITS_HAO = Set.of("82", "84", "86", "88");

    private RegleCompteNature() {
    }

    /** Vrai si le compte convient à une nature de cette section et de ce sens. */
    public static boolean admis(SectionBudgetaire section, SensBudgetaire sens, String codeCompte) {
        if (codeCompte == null || codeCompte.isEmpty()) {
            return false;
        }
        char classe = codeCompte.charAt(0);
        String sousClasse = codeCompte.length() >= 2 ? codeCompte.substring(0, 2) : "";
        return switch (section) {
            case FONCTIONNEMENT -> sens == SensBudgetaire.DEPENSE && (classe == '6' || CHARGES_HAO.contains(sousClasse));
            case PRODUITS -> sens == SensBudgetaire.RECETTE && (classe == '7' || PRODUITS_HAO.contains(sousClasse));
            case INVESTISSEMENT -> sens == SensBudgetaire.DEPENSE ? classe == '2' : classe == '1';
        };
    }

    /** Explication rendue à l'utilisateur quand le compte ne convient pas. */
    public static String attendu(SectionBudgetaire section, SensBudgetaire sens) {
        return switch (section) {
            case FONCTIONNEMENT -> sens == SensBudgetaire.DEPENSE
                    ? "une nature de fonctionnement s'impute sur un compte de charges (classe 6, ou 81, 83, 85)"
                    : "une nature de fonctionnement est une dépense : les recettes relèvent de la section des produits";
            case PRODUITS -> sens == SensBudgetaire.RECETTE
                    ? "une nature de produits s'impute sur un compte de produits (classe 7, ou 82, 84, 86, 88)"
                    : "une nature de produits est une recette";
            case INVESTISSEMENT -> sens == SensBudgetaire.DEPENSE
                    ? "une dépense d'investissement s'impute sur un compte d'immobilisation (classe 2)"
                    : "une recette d'investissement s'impute sur un compte de ressources durables (classe 1)";
        };
    }
}
