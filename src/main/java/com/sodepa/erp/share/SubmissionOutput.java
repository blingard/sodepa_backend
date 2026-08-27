package com.sodepa.erp.share;

import java.util.UUID;

/**
 * Accusé de réception d'une soumission maker-checker.
 *
 * <p>
 * Les points d'entrée {@code init_*} n'appliquent rien : ils déposent une
 * demande qu'un checker tranchera. Sans cet accusé, le client repartait avec
 * un 200 vide et ignorait tout des deux identifiants dont il a besoin ensuite
 * — {@code requestId} pour appeler {@code validate_or_reject}, {@code entityId}
 * pour retrouver l'entité une fois la demande acceptée.
 * </p>
 *
 * @param requestId identifiant de la demande, à passer à {@code validate_or_reject}
 * @param entityId  identifiant que portera l'entité si la demande est acceptée
 */
public record SubmissionOutput(UUID requestId, UUID entityId) {
}
