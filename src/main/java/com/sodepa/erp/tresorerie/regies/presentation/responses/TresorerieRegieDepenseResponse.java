package com.sodepa.erp.tresorerie.regies.presentation.responses;

import com.sodepa.erp.tresorerie.regies.infrastructure.entities.TresorerieRegieDepenseEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `TresorerieRegieDepenseResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TresorerieRegieDepenseResponse {

    private UUID id;
    private String numeroPiece;
    private UUID regieId;
    private String regieNom;
    private LocalDate dateDepense;
    private String beneficiaire;
    private String motif;
    private String imputationBudgetaire;
    private BigDecimal montantDepenseFcfa;
    private String justificatifRef;
    private boolean justifie;

    public static TresorerieRegieDepenseResponse fromEntity(TresorerieRegieDepenseEntity entity) {
        if (entity == null) return null;
        return TresorerieRegieDepenseResponse.builder()
                .id(entity.getId())
                .numeroPiece(entity.getNumeroPiece())
                .regieId(entity.getRegie() != null ? entity.getRegie().getId() : null)
                .regieNom(entity.getRegie() != null ? entity.getRegie().getNom() : null)
                .dateDepense(entity.getDateDepense())
                .beneficiaire(entity.getBeneficiaire())
                .motif(entity.getMotif())
                .imputationBudgetaire(entity.getImputationBudgetaire())
                .montantDepenseFcfa(entity.getMontantDepenseFcfa())
                .justificatifRef(entity.getJustificatifRef())
                .justifie(entity.isJustifie())
                .build();
    }
}
