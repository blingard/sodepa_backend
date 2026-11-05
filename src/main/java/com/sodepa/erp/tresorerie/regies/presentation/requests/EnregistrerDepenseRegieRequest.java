package com.sodepa.erp.tresorerie.regies.presentation.requests;

import jakarta.validation.constraints.DecimalMax;
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
public class EnregistrerDepenseRegieRequest {

    @NotNull(message = "L'identifiant de la régie est obligatoire")
    private UUID regieId;

    private LocalDate dateDepense;

    @NotBlank(message = "Le bénéficiaire est obligatoire")
    private String beneficiaire;

    @NotBlank(message = "Le motif de la dépense est obligatoire")
    private String motif;

    private String imputationBudgetaire;

    @NotNull(message = "Le montant de la dépense est obligatoire")
    @DecimalMax(value = "100000.00", message = "Les menues dépenses de régie sont plafonnées à 100 000 FCFA par opération")
    private BigDecimal montantDepenseFcfa;

    private String justificatifRef;
}
