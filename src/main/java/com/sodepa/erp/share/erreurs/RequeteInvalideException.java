package com.sodepa.erp.share.erreurs;

import org.springframework.http.HttpStatus;

/** La requête est malformée ou viole une règle métier : le client doit la corriger. */
public class RequeteInvalideException extends ApiException {

    public RequeteInvalideException(String message) {
        super(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE", message);
    }

    /**
     * Refus dont le client doit pouvoir distinguer la cause par son code.
     *
     * @param code    code stable, en majuscules (par exemple {@code ANNEE_HORS_BORNES})
     * @param message message en français destiné à l'utilisateur
     */
    public RequeteInvalideException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
