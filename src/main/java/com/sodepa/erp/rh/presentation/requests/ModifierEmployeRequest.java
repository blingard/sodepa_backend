package com.sodepa.erp.rh.presentation.requests;

import com.sodepa.erp.rh.infrastructure.entities.SituationMatrimoniale;
import com.sodepa.erp.rh.infrastructure.entities.StatutActivite;
import com.sodepa.erp.rh.infrastructure.entities.TypeContrat;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierEmployeRequest {
    private String nom;
    private String prenom;
    private SituationMatrimoniale situationMatrimoniale;
    private Integer nombreEnfantsACharge;
    private String email;
    private String telephone;
    private String adresse;
    private String numeroCnps;
    private String compteBancaireRib;
    private String banqueNom;
    private TypeContrat typeContrat;
    private StatutActivite statutActivite;
    private LocalDate dateFinEssai;
    private LocalDate dateFinContrat;
    private UUID uniteOrganisationnelleId;
    private UUID posteId;
    private Integer categorie;
    private String echelon;
}
