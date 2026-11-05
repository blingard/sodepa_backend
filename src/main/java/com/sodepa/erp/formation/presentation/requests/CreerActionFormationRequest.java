package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerActionFormationRequest {

    @NotNull(message = "L'identifiant du plan est obligatoire")
    private UUID planId;

    @NotBlank(message = "Le thème de formation est obligatoire")
    private String theme;

    @NotBlank(message = "Le domaine est obligatoire")
    private String domaine;

    private String publicCible;

    private String organismeFormateur;

    private String typeFormateur;

    private String lieu;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private Integer dureeHeures;

    private BigDecimal coutEstimatifFcfa;

    private boolean donneDroitReclassement;
}
