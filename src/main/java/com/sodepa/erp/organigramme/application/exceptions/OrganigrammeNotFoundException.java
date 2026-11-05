package com.sodepa.erp.organigramme.application.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrganigrammeNotFoundException extends RuntimeException {
    public OrganigrammeNotFoundException(String message) {
        super(message);
    }
}
