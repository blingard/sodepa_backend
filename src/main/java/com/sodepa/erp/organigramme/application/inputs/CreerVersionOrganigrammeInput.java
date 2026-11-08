package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.time.LocalDate;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerVersionOrganigrammeInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Builder
public record CreerVersionOrganigrammeInput(
        String numeroVersion,
        String libelle,
        String description,
        LocalDate dateEffet
) {}
