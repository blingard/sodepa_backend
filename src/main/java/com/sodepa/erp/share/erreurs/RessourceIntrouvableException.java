package com.sodepa.erp.share.erreurs;

import org.springframework.http.HttpStatus;

/** L'entité désignée n'existe pas. */
public class RessourceIntrouvableException extends ApiException {

    public RessourceIntrouvableException(String message) {
        super(HttpStatus.NOT_FOUND, "RESSOURCE_INTROUVABLE", message);
    }
}
