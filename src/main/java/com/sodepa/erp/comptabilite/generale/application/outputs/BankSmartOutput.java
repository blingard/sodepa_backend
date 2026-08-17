package com.sodepa.erp.comptabilite.generale.application.outputs;

import java.util.UUID;

public record BankSmartOutput(
        UUID id,
        String code,
        String nom,
        String logo
) {
}
