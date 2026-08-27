package com.sodepa.erp.share.erreurs;

import org.springframework.http.HttpStatus;

/**
 * Le fournisseur d'identité est injoignable.
 *
 * <p>503 et non 500 : la distinction dit au client que réessayer a un sens, ce
 * qui n'est pas le cas d'un défaut applicatif.</p>
 */
public class AuthentificationIndisponibleException extends ApiException {

    public AuthentificationIndisponibleException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_INDISPONIBLE",
                "Service d'authentification indisponible.");
    }
}
