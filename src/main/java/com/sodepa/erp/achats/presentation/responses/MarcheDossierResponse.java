package com.sodepa.erp.achats.presentation.responses;

import com.sodepa.erp.achats.infrastructure.entities.MarcheDossierEntity;
import com.sodepa.erp.achats.infrastructure.entities.MarcheDossierLigneEntity;
import com.sodepa.erp.achats.infrastructure.entities.StatutMarcheDossier;
import com.sodepa.erp.achats.infrastructure.entities.TypeProcedureAchat;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Composant applicatif `MarcheDossierResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcheDossierResponse {

    private UUID id;
    private String numeroDossier;
    private TypeProcedureAchat typeProcedure;
    private String typeProcedureLibelle;
    private String objet;
    private UUID expressionBesoinId;
    private UUID fournisseurId;
    private String fournisseurNom;
    private BigDecimal montantHt;
    private BigDecimal montantTva;
    private BigDecimal montantTtc;
    private String ligneBudgetaireCode;
    private Integer delaiLivraisonJours;
    private LocalDate dateNotification;
    private LocalDate dateLimiteLivraison;
    private StatutMarcheDossier statut;
    private Integer proformasObtenuesCount;
    private boolean examenCipmRequis;
    private Instant dateCreation;
    private List<LigneDossierResponse> lignes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LigneDossierResponse {
        private UUID id;
        private Integer ordre;
        private String designation;
        private BigDecimal quantite;
        private String uniteMesure;
        private BigDecimal prixUnitaireHt;
        private BigDecimal montantHt;
        private BigDecimal tauxTva;
        private BigDecimal montantTtc;

        public static LigneDossierResponse fromEntity(MarcheDossierLigneEntity entity) {
            if (entity == null) return null;
            return LigneDossierResponse.builder()
                    .id(entity.getId())
                    .ordre(entity.getOrdre())
                    .designation(entity.getDesignation())
                    .quantite(entity.getQuantite())
                    .uniteMesure(entity.getUniteMesure())
                    .prixUnitaireHt(entity.getPrixUnitaireHt())
                    .montantHt(entity.getMontantHt())
                    .tauxTva(entity.getTauxTva())
                    .montantTtc(entity.getMontantTtc())
                    .build();
        }
    }

    public static MarcheDossierResponse fromEntity(MarcheDossierEntity entity) {
        if (entity == null) return null;
        return MarcheDossierResponse.builder()
                .id(entity.getId())
                .numeroDossier(entity.getNumeroDossier())
                .typeProcedure(entity.getTypeProcedure())
                .typeProcedureLibelle(entity.getTypeProcedure().name())
                .objet(entity.getObjet())
                .expressionBesoinId(entity.getExpressionBesoin() != null ? entity.getExpressionBesoin().getId() : null)
                .fournisseurId(entity.getFournisseur() != null ? entity.getFournisseur().getId() : null)
                .fournisseurNom(entity.getFournisseurNom())
                .montantHt(entity.getMontantHt())
                .montantTva(entity.getMontantTva())
                .montantTtc(entity.getMontantTtc())
                .ligneBudgetaireCode(entity.getLigneBudgetaireCode())
                .delaiLivraisonJours(entity.getDelaiLivraisonJours())
                .dateNotification(entity.getDateNotification())
                .dateLimiteLivraison(entity.getDateLimiteLivraison())
                .statut(entity.getStatut())
                .proformasObtenuesCount(entity.getProformasObtenuesCount())
                .examenCipmRequis(entity.isExamenCipmRequis())
                .dateCreation(entity.getDateCreation())
                .lignes(entity.getLignes() != null
                        ? entity.getLignes().stream().map(LigneDossierResponse::fromEntity).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }
}
