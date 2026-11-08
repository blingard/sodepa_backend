package com.sodepa.erp.budget.application.outputs;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Composant applicatif `AuditTrailOutput` du système ERP SODEPA.
 */
public record AuditTrailOutput(
    UUID id,
    String entiteNom,
    UUID entiteId,
    String action,
    String details,
    LocalDateTime timestamp,
    UUID utilisateur
) {}
