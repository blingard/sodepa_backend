package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CreerVersionOrganigrammeInput(
        String numeroVersion,
        String libelle,
        String description,
        LocalDate dateEffet
) {}
