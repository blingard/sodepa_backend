package com.sodepa.erp.share.erreurs;

import org.springframework.http.HttpStatus;

/**
 * La demande est bien formée, mais l'état du système s'y oppose.
 *
 * <p>Séparation maker-checker non respectée, demande déjà tranchée, doublon
 * d'identifiant. 409 plutôt que 400 : rien ne cloche dans le corps de la
 * requête, et le renvoyer corrigé ne changerait rien.</p>
 */
public class ConflitMetierException extends ApiException {

    public ConflitMetierException(String message) {
        super(HttpStatus.CONFLICT, "CONFLIT_METIER", message);
    }
}
