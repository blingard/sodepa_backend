package com.sodepa.erp.share;

import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary DTO for Maker-Checker records.
 *
 * <p>
 * {@code id} est l'identifiant à passer à {@code validate_or_reject} ;
 * {@code entityPk} désigne l'entité concernée. Les deux sont nécessaires : le
 * premier pour trancher, le second pour afficher de quoi il retourne.
 * </p>
 *
 * <p>
 * {@code makerId} et {@code createdAt} ne sont pas décoratifs : un checker doit
 * savoir qui a soumis et depuis quand, et {@code expiredAt} lui dit ce qui
 * tombera en péremption s'il ne fait rien.
 * </p>
 */
public record MakerCheckerSmartOutput(
        UUID id,
        MakerCheckerEntityName entityName,
        String entityPk,
        MakerCheckerStatus status,
        MakerCheckerOperationType checkerOperationType,
        String makerId,
        Instant createdAt,
        Instant expiredAt
) {
}
