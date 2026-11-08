package com.sodepa.erp.comptabilite.generale.application.outputs;

import jakarta.persistence.Column;

import java.util.UUID;

/**
 * Composant applicatif `BankOutput` du système ERP SODEPA.
 */
public record BankOutput(
        UUID id,
        String code,
        String nom,
        String accountingCode,
        String logo,
        boolean status
) {
}
