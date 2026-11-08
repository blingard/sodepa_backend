package com.sodepa.erp.comptabilite.generale.infrastructure.event;

import java.util.UUID;

/**
 * Composant applicatif `BankEventInput` du système ERP SODEPA.
 */
public record BankEventInput(
        UUID id,
        String code,
        String nom,
        String compteComptableCode,
        String logo,
        boolean status,
        String userId
) {
}
