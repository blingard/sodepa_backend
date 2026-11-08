package com.sodepa.erp.pastoral.presentation.responses;

import com.sodepa.erp.pastoral.infrastructure.entities.PastoralCategorieCheptelEntity;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Composant applicatif `PastoralCategorieCheptelResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralCategorieCheptelResponse {

    private UUID id;
    private String code;
    private String libelle;
    private Integer ageMinMois;
    private Integer ageMaxMois;
    private String compteActifSyscohada;
    private String compteVariationSyscohada;
    private BigDecimal valeurForfaitaireBareme;

    public static PastoralCategorieCheptelResponse fromEntity(PastoralCategorieCheptelEntity entity) {
        if (entity == null) return null;
        return PastoralCategorieCheptelResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .libelle(entity.getLibelle())
                .ageMinMois(entity.getAgeMinMois())
                .ageMaxMois(entity.getAgeMaxMois())
                .compteActifSyscohada(entity.getCompteActifSyscohada())
                .compteVariationSyscohada(entity.getCompteVariationSyscohada())
                .valeurForfaitaireBareme(entity.getValeurForfaitaireBareme())
                .build();
    }
}
