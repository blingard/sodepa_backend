package com.sodepa.erp.achats.presentation.responses;

import com.sodepa.erp.achats.infrastructure.entities.MarcheReceptionPvEntity;
import com.sodepa.erp.achats.infrastructure.entities.ResultatConformitePv;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `MarcheReceptionPvResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcheReceptionPvResponse {

    private UUID id;
    private String numeroPv;
    private UUID dossierId;
    private String dossierNumero;
    private String dossierObjet;
    private LocalDate dateReception;
    private String lieuReception;
    private ResultatConformitePv resultatConformite;
    private String observations;
    private String numeroFactureDefinitive;
    private BigDecimal montantFactureLiquide;
    private String fichierUrl;
    private boolean liquidationValidee;
    private Instant dateLiquidation;

    public static MarcheReceptionPvResponse fromEntity(MarcheReceptionPvEntity entity) {
        if (entity == null) return null;
        return MarcheReceptionPvResponse.builder()
                .id(entity.getId())
                .numeroPv(entity.getNumeroPv())
                .dossierId(entity.getDossier() != null ? entity.getDossier().getId() : null)
                .dossierNumero(entity.getDossier() != null ? entity.getDossier().getNumeroDossier() : null)
                .dossierObjet(entity.getDossier() != null ? entity.getDossier().getObjet() : null)
                .dateReception(entity.getDateReception())
                .lieuReception(entity.getLieuReception())
                .resultatConformite(entity.getResultatConformite())
                .observations(entity.getObservations())
                .numeroFactureDefinitive(entity.getNumeroFactureDefinitive())
                .montantFactureLiquide(entity.getMontantFactureLiquide())
                .fichierUrl(entity.getFichierUrl())
                .liquidationValidee(entity.isLiquidationValidee())
                .dateLiquidation(entity.getDateLiquidation())
                .build();
    }
}
