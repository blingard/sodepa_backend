package com.sodepa.erp.budgetaire.commun;

/**
 * Ce qu'une demande en attente changerait, vu par le référentiel qu'elle vise.
 *
 * <p>
 * {@code proposition} et {@code actuel} sont des fiches du même type : celle
 * que la demande produirait une fois appliquée, et celle en vigueur. L'écran
 * du valideur les compare champ par champ ; une création n'a pas de fiche en
 * vigueur.
 * </p>
 *
 * @param operation   opération demandée : le type maker-checker ne la dit pas,
 *                    une modification et une réactivation y étant toutes deux
 *                    des {@code UPDATE}
 * @param motif       motif saisi par l'auteur (désactivation, réactivation)
 * @param proposition fiche telle qu'elle sera après application
 * @param actuel      fiche en vigueur, {@code null} pour une création
 * @param <T>         type de fiche du référentiel
 */
public record ProjectionDemande<T>(OperationReferentiel operation, String motif, T proposition, T actuel) {

    /** Projection d'une création : rien n'est encore en vigueur. */
    public static <T> ProjectionDemande<T> creation(T proposition) {
        return new ProjectionDemande<>(OperationReferentiel.CREER, null, proposition, null);
    }
}
