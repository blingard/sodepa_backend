package com.sodepa.erp.share.erreurs;

import org.springframework.http.HttpStatus;

/**
 * Le fournisseur d'identité a refusé les identifiants présentés.
 *
 * <p>Le message reste volontairement générique : distinguer « ce compte
 * n'existe pas » de « mot de passe incorrect » indiquerait à un attaquant
 * quels identifiants existent.</p>
 */
public class IdentifiantsRefusesException extends ApiException {

    public IdentifiantsRefusesException() {
        super(HttpStatus.UNAUTHORIZED, "IDENTIFIANTS_REFUSES", "Identifiants incorrects.");
    }
}
