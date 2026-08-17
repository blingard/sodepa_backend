package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

public record ValuationCouvertureReportOutput(
    String referenceContrat,
    String deviseCible,
    BigDecimal montantDevise,
    BigDecimal coursGaranti,
    BigDecimal coursSpotActuel,
    BigDecimal valeurGarantie,
    BigDecimal valeurMarcheEstimee,
    BigDecimal ecartChangeLatent,
    String typeEcart
) {}
