package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Énumération des politiques de consensus appliquées lorsqu'une étape de workflow implique
 * plusieurs collaborateurs ou un collège de signataires (ex: commission des marchés, collège DAF + DG).
 */
public enum PolitiqueValidation {

    /**
     * Dès que l'un des membres du groupe ou rôle désigné valide la tâche, l'étape est considérée
     * comme franchie avec succès (principe du premier répondeur habilité).
     */
    ONLY_ONE_CAN_VALIDATE,

    /**
     * Chaque personne assignée doit impérativement apposer son approbation pour que l'étape soit validée
     * (principe de consensus unanime, ex: double signature bancaire pour décaissement trésorerie).
     */
    EVERY_BODY_CAN_VALIDATE,

    /**
     * La validation est acquise dès lors que plus de 50% des approbateurs convoqués ont émis un avis favorable
     * (ex: délibérations de commission d'évaluation technique).
     */
    MUST_HAVE_MAJORITY,
}
