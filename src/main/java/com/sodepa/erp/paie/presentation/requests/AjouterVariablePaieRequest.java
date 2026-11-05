package com.sodepa.erp.paie.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjouterVariablePaieRequest {

    @NotNull(message = "L'employé est obligatoire")
    private UUID employeId;

    @NotBlank(message = "Le code de la rubrique est obligatoire")
    private String codeRubrique;

    private String libelle;

    @NotNull(message = "Le montant est obligatoire")
    private BigDecimal montant;

    private BigDecimal quantite;

    private String commentaire;
}
