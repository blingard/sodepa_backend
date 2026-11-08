package com.sodepa.erp.comptabilite.generale.application.outputs;

import java.util.UUID;

/**
 * Composant applicatif `BankSmartOutput` du système ERP SODEPA.
 */
public record BankSmartOutput(
        UUID id,
        String code,
        String nom,
        String logo
) {
}
