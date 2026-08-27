package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Financement vu depuis la liste.
 *
 * <p>
 * Sans son échéancier, à la différence de {@link FinancementOutput} : un
 * emprunt sur vingt ans porte deux cent quarante échéances, qui n'ont rien à
 * faire dans une liste. Le capital restant dû résume la position ; le détail
 * se demande par {@code GET /financement/{id}}.
 * </p>
 *
 * @param capitalRestantDu solde de la plus ancienne échéance non soldée, ou
 *        zéro si l'emprunt est intégralement remboursé
 * @param echeancesRestantes nombre d'échéances encore à payer
 */
public record FinancementSmartOutput(
    UUID id,
    UUID banqueId,
    String intitule,
    String type,
    BigDecimal capitalEmprunte,
    BigDecimal tauxNominal,
    LocalDate dateEffet,
    Integer dureeMois,
    String periodicite,
    String statut,
    BigDecimal capitalRestantDu,
    int echeancesRestantes
) {}
