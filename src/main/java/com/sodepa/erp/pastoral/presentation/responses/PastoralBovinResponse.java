package com.sodepa.erp.pastoral.presentation.responses;

import com.sodepa.erp.pastoral.infrastructure.entities.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `PastoralBovinResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralBovinResponse {

    private UUID id;
    private String matriculeBoucle;
    private UUID ranchId;
    private String ranchNom;
    private String categorieCode;
    private SexeBovin sexe;
    private RaceBovine race;
    private LocalDate dateNaissance;
    private BigDecimal poidsActuelKg;
    private LocalDate dateDernierePesee;
    private String mereBoucle;
    private String pereBoucle;
    private EtatSanteBovin etatSante;
    private StatutBovin statut;
    private LocalDate dateEntree;
    private LocalDate dateSortie;
    private BigDecimal valeurComptableActuelle;

    public static PastoralBovinResponse fromEntity(PastoralBovinEntity entity) {
        if (entity == null) return null;
        return PastoralBovinResponse.builder()
                .id(entity.getId())
                .matriculeBoucle(entity.getMatriculeBoucle())
                .ranchId(entity.getRanch() != null ? entity.getRanch().getId() : null)
                .ranchNom(entity.getRanch() != null ? entity.getRanch().getNom() : null)
                .categorieCode(entity.getCategorieCode())
                .sexe(entity.getSexe())
                .race(entity.getRace())
                .dateNaissance(entity.getDateNaissance())
                .poidsActuelKg(entity.getPoidsActuelKg())
                .dateDernierePesee(entity.getDateDernierePesee())
                .mereBoucle(entity.getMereBoucle())
                .pereBoucle(entity.getPereBoucle())
                .etatSante(entity.getEtatSante())
                .statut(entity.getStatut())
                .dateEntree(entity.getDateEntree())
                .dateSortie(entity.getDateSortie())
                .valeurComptableActuelle(entity.getValeurComptableActuelle())
                .build();
    }
}
