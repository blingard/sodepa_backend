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

    /**
     * Conflit dont le client doit pouvoir distinguer la cause sans lire le
     * message, qui s'adresse à l'utilisateur et peut changer de formulation.
     *
     * @param code    code stable, en majuscules (par exemple {@code EXERCICE_EXISTANT})
     * @param message message en français destiné à l'utilisateur
     */
    public ConflitMetierException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
