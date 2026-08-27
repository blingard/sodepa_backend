package com.sodepa.erp.share.erreurs;

import org.springframework.http.HttpStatus;

/** La requête est malformée ou viole une règle métier : le client doit la corriger. */
public class RequeteInvalideException extends ApiException {

    public RequeteInvalideException(String message) {
        super(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE", message);
    }
}
