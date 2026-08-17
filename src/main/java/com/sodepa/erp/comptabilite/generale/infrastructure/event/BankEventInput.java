package com.sodepa.erp.comptabilite.generale.infrastructure.event;

import java.util.UUID;

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
