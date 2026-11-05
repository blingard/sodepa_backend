package com.sodepa.erp.tresorerie.regies.presentation.responses;

import com.sodepa.erp.tresorerie.regies.infrastructure.entities.TresorerieRegieEntity;
import com.sodepa.erp.tresorerie.regies.infrastructure.entities.TypeUniteRegie;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TresorerieRegieResponse {

    private UUID id;
    private String code;
    private String nom;
    private TypeUniteRegie typeUnite;
    private BigDecimal plafondAutoriseFcfa;
    private BigDecimal soldeActuelFcfa;
    private String regisseurNom;
    private String regisseurMatricule;
    private String banqueRattachement;
    private String ribCompteRegie;
    private boolean actif;

    public static TresorerieRegieResponse fromEntity(TresorerieRegieEntity entity) {
        if (entity == null) return null;
        return TresorerieRegieResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nom(entity.getNom())
                .typeUnite(entity.getTypeUnite())
                .plafondAutoriseFcfa(entity.getPlafondAutoriseFcfa())
                .soldeActuelFcfa(entity.getSoldeActuelFcfa())
                .regisseurNom(entity.getRegisseurNom())
                .regisseurMatricule(entity.getRegisseurMatricule())
                .banqueRattachement(entity.getBanqueRattachement())
                .ribCompteRegie(entity.getRibCompteRegie())
                .actif(entity.isActif())
                .build();
    }
}
