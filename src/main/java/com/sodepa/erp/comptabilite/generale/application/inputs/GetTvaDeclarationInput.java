package com.sodepa.erp.comptabilite.generale.application.inputs;

/**
 * Données d'entrée pour la récupération de la déclaration TVA.
 */
public record GetTvaDeclarationInput(
    int annee,
    int mois
) {}
