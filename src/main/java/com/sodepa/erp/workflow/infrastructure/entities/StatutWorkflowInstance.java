package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Énumération représentant le statut global du cycle de vie d'une instance de workflow.
 * <p>
 * Une instance correspond à l'exécution d'un circuit de validation donné pour un document
 * ou objet métier précis (ex: Bon d'Engagement n°2026-0042, Demande de Congé n°105).
 * </p>
 */
public enum StatutWorkflowInstance {

    /**
     * Le circuit est actif : des tâches sont en cours de traitement ou en attente d'approbation.
     */
    EN_COURS,

    /**
     * Le circuit s'est achevé avec succès : toutes les étapes obligatoires ont reçu une décision favorable.
     * Le document métier lié peut être exécuté (comptabilisé, décaissé, archivé).
     */
    APPROUVE,

    /**
     * Le circuit est interrompu consécutivement au rejet formulé par l'un des approbateurs
     * ou à un rejet automatique pour expiration de SLA.
     */
    REJETE,

    /**
     * Le circuit est momentanément suspendu suite à une demande de révision ou d'éléments complémentaires,
     * dans l'attente d'une correction apportée par l'initiateur.
     */
    SUSPENDU,

    /**
     * Le circuit a été annulé par l'initiateur ou par un administrateur autorisé avant son terme.
     */
    ANNULE
}
