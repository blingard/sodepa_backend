package com.sodepa.erp.achats.presentation.responses;

import com.sodepa.erp.achats.infrastructure.entities.ExpressionBesoinEntity;
import com.sodepa.erp.achats.infrastructure.entities.ExpressionBesoinLigneEntity;
import com.sodepa.erp.achats.infrastructure.entities.StatutExpressionBesoin;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpressionBesoinResponse {

    private UUID id;
    private String numero;
    private UUID demandeurId;
    private String demandeurNom;
    private UUID uniteOrganisationnelleId;
    private String structureNom;
    private String motif;
    private LocalDate dateExpression;
    private BigDecimal montantEstimeTotal;
    private boolean stockDisponible;
    private boolean attestationCarenceEtablie;
    private boolean budgetDisponible;
    private String ligneBudgetaireImputation;
    private StatutExpressionBesoin statut;
    private Instant dateCreation;
    private List<LigneExpressionBesoinResponse> lignes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LigneExpressionBesoinResponse {
        private UUID id;
        private Integer ordre;
        private String designation;
        private String specificationsTechniques;
        private BigDecimal quantite;
        private String uniteMesure;
        private BigDecimal prixUnitaireEstime;
        private BigDecimal montantTotalEstime;

        public static LigneExpressionBesoinResponse fromEntity(ExpressionBesoinLigneEntity e) {
            if (e == null) return null;
            return LigneExpressionBesoinResponse.builder()
                    .id(e.getId())
                    .ordre(e.getOrdre())
                    .designation(e.getDesignation())
                    .specificationsTechniques(e.getSpecificationsTechniques())
                    .quantite(e.getQuantite())
                    .uniteMesure(e.getUniteMesure())
                    .prixUnitaireEstime(e.getPrixUnitaireEstime())
                    .montantTotalEstime(e.getMontantTotalEstime())
                    .build();
        }
    }

    public static ExpressionBesoinResponse fromEntity(ExpressionBesoinEntity entity) {
        if (entity == null) return null;
        return ExpressionBesoinResponse.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .demandeurId(entity.getDemandeur() != null ? entity.getDemandeur().getId() : null)
                .demandeurNom(entity.getDemandeurNom())
                .uniteOrganisationnelleId(entity.getUniteOrganisationnelle() != null ? entity.getUniteOrganisationnelle().getId() : null)
                .structureNom(entity.getStructureNom())
                .motif(entity.getMotif())
                .dateExpression(entity.getDateExpression())
                .montantEstimeTotal(entity.getMontantEstimeTotal())
                .stockDisponible(entity.isStockDisponible())
                .attestationCarenceEtablie(entity.isAttestationCarenceEtablie())
                .budgetDisponible(entity.isBudgetDisponible())
                .ligneBudgetaireImputation(entity.getLigneBudgetaireImputation())
                .statut(entity.getStatut())
                .dateCreation(entity.getDateCreation())
                .lignes(entity.getLignes() != null
                        ? entity.getLignes().stream().map(LigneExpressionBesoinResponse::fromEntity).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }
}
