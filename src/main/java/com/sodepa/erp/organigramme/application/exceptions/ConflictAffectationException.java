package com.sodepa.erp.organigramme.application.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictAffectationException extends RuntimeException {
    public ConflictAffectationException(String message) {
        super(message);
    }
}
