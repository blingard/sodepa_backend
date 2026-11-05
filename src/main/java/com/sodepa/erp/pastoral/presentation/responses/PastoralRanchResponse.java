package com.sodepa.erp.pastoral.presentation.responses;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralRanchEntity;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralRanchResponse {

    private UUID id;
    private String code;
    private String nom;
    private String region;
    private BigDecimal superficieHectares;
    private Integer capaciteTetes;
    private String responsableNom;
    private BigDecimal regiePlafondFcfa;
    private boolean actif;

    public static PastoralRanchResponse fromEntity(PastoralRanchEntity entity) {
        if (entity == null) return null;
        return PastoralRanchResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nom(entity.getNom())
                .region(entity.getRegion())
                .superficieHectares(entity.getSuperficieHectares())
                .capaciteTetes(entity.getCapaciteTetes())
                .responsableNom(entity.getResponsableNom())
                .regiePlafondFcfa(entity.getRegiePlafondFcfa())
                .actif(entity.isActif())
                .build();
    }
}
