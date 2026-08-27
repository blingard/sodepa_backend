package com.sodepa.erp.share.erreurs;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Erreur applicative portant son propre statut HTTP.
 *
 * <p>
 * Le projet ne disposait d'aucun gestionnaire d'exceptions : toute
 * {@code RuntimeException} devenait un 500. Un refus de droits, un identifiant
 * déjà pris et une panne réelle arrivaient au client sous la même forme, et
 * aucun front ne pouvait les distinguer.
 * </p>
 *
 * <p>
 * Le {@code code} est destiné au programme appelant — stable, non traduit — là
 * où le message s'adresse à l'utilisateur.
 * </p>
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
