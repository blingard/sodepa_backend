package com.sodepa.erp.achats.presentation.responses;

import com.sodepa.erp.achats.infrastructure.entities.FournisseurEntity;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FournisseurResponse {

    private UUID id;
    private String code;
    private String raisonSociale;
    private String numeroContribuable;
    private String rccm;
    private String regimeFiscal;
    private String specialite;
    private String adresse;
    private String telephone;
    private String email;
    private String banqueNom;
    private String compteBancaireRib;
    private boolean agreeSodepa;
    private LocalDate dateAgrement;
    private boolean actif;

    public static FournisseurResponse fromEntity(FournisseurEntity entity) {
        if (entity == null) return null;
        return FournisseurResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .raisonSociale(entity.getRaisonSociale())
                .numeroContribuable(entity.getNumeroContribuable())
                .rccm(entity.getRccm())
                .regimeFiscal(entity.getRegimeFiscal())
                .specialite(entity.getSpecialite())
                .adresse(entity.getAdresse())
                .telephone(entity.getTelephone())
                .email(entity.getEmail())
                .banqueNom(entity.getBanqueNom())
                .compteBancaireRib(entity.getCompteBancaireRib())
                .agreeSodepa(entity.isAgreeSodepa())
                .dateAgrement(entity.getDateAgrement())
                .actif(entity.isActif())
                .build();
    }
}
