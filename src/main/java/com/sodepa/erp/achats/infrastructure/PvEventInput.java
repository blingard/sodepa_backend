package com.sodepa.erp.achats.infrastructure;

import com.sodepa.erp.achats.infrastructure.entities.ResultatConformitePv;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `PvEventInput` du système ERP SODEPA.
 */
@Builder
public record PvEventInput (
        UUID id,
        String numeroPv,
        UUID dossierId,
        LocalDate dateReception,
        String lieuReception,
        ResultatConformitePv resultatConformite,
        String observations,
        String numeroFactureDefinitive,
        BigDecimal montantFactureLiquide,
        String fichierUrl
) {
}
