package com.sodepa.erp.rh.presentation.responses;

import com.sodepa.erp.rh.infrastructure.entities.Sexe;
import com.sodepa.erp.rh.infrastructure.entities.SituationMatrimoniale;
import com.sodepa.erp.rh.infrastructure.entities.StatutActivite;
import com.sodepa.erp.rh.infrastructure.entities.TypeContrat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeResponse {
    private UUID id;
    private String matricule;
    private String nom;
    private String prenom;
    private String nomComplet;
    private Sexe sexe;
    private LocalDate dateNaissance;
    private String lieuNaissance;
    private SituationMatrimoniale situationMatrimoniale;
    private Integer nombreEnfantsACharge;
    private String email;
    private String telephone;
    private String adresse;
    private String numeroCnps;
    private String compteBancaireRib;
    private String banqueNom;
    private TypeContrat typeContrat;
    private LocalDate dateEmbauche;
    private LocalDate dateFinEssai;
    private LocalDate dateFinContrat;
    private StatutActivite statutActivite;

    // Rattachement Organigramme
    private UUID uniteId;
    private String uniteCode;
    private String uniteIntitule;
    private UUID posteId;
    private String posteCode;
    private String posteIntitule;

    // Rémunération & Statut
    private UUID grilleId;
    private Integer categorie;
    private String echelon;
    private Integer groupe;
    private BigDecimal salaireBase;
    private BigDecimal tauxHoraire;

    private String userId;
    private UUID utilisateurId;
    private String username;
    private LocalDateTime dateCreation;
}
