package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

public record SimulationResultOutput(
    BigDecimal baseEbitda,
    BigDecimal simulatedEbitda,
    BigDecimal ebitdaEcart,
    BigDecimal baseBfr,
    BigDecimal simulatedBfr,
    BigDecimal bfrEcart,
    BigDecimal baseCashFlow,
    BigDecimal simulatedCashFlow,
    BigDecimal cashFlowEcart
) {}
