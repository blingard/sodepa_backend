package com.sodepa.erp.share.erreurs;

import org.springframework.http.HttpStatus;

/**
 * L'appelant est authentifié mais ne détient pas l'habilitation requise.
 *
 * <p>403 et non 401 : renvoyer 401 ferait tenter au front un rafraîchissement
 * de jeton parfaitement inutile — le jeton est bon, ce sont les droits qui
 * manquent.</p>
 */
public class AccesRefuseException extends ApiException {

    public AccesRefuseException(String message) {
        super(HttpStatus.FORBIDDEN, "ACCES_REFUSE", message);
    }
}
