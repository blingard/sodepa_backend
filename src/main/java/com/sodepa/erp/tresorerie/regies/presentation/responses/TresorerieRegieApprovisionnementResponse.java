package com.sodepa.erp.tresorerie.regies.presentation.responses;

import com.sodepa.erp.tresorerie.regies.infrastructure.entities.StatutApprovisionnementRegie;
import com.sodepa.erp.tresorerie.regies.infrastructure.entities.TresorerieRegieApprovisionnementEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TresorerieRegieApprovisionnementResponse {

    private UUID id;
    private String numeroDemande;
    private UUID regieId;
    private String regieNom;
    private BigDecimal montantDemandeFcfa;
    private BigDecimal soldeAvantApprov;
    private String motifReapprovisionnement;
    private String etatJustificatifsDepenses;
    private BigDecimal montantJustificatifsValides;
    private LocalDate dateDemande;
    private StatutApprovisionnementRegie statut;
    private boolean signatureDaf;
    private Instant dateSignatureDaf;
    private boolean signatureDg;
    private Instant dateSignatureDg;
    private String referenceVirementBanque;
    private LocalDate dateEffetDeblocage;

    public static TresorerieRegieApprovisionnementResponse fromEntity(TresorerieRegieApprovisionnementEntity entity) {
        if (entity == null) return null;
        return TresorerieRegieApprovisionnementResponse.builder()
                .id(entity.getId())
                .numeroDemande(entity.getNumeroDemande())
                .regieId(entity.getRegie() != null ? entity.getRegie().getId() : null)
                .regieNom(entity.getRegie() != null ? entity.getRegie().getNom() : null)
                .montantDemandeFcfa(entity.getMontantDemandeFcfa())
                .soldeAvantApprov(entity.getSoldeAvantApprov())
                .motifReapprovisionnement(entity.getMotifReapprovisionnement())
                .etatJustificatifsDepenses(entity.getEtatJustificatifsDepenses())
                .montantJustificatifsValides(entity.getMontantJustificatifsValides())
                .dateDemande(entity.getDateDemande())
                .statut(entity.getStatut())
                .signatureDaf(entity.isSignatureDaf())
                .dateSignatureDaf(entity.getDateSignatureDaf())
                .signatureDg(entity.isSignatureDg())
                .dateSignatureDg(entity.getDateSignatureDg())
                .referenceVirementBanque(entity.getReferenceVirementBanque())
                .dateEffetDeblocage(entity.getDateEffetDeblocage())
                .build();
    }
}
