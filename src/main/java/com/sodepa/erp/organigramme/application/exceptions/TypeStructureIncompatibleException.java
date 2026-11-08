package com.sodepa.erp.organigramme.application.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Composant applicatif `TypeStructureIncompatibleException` du système ERP SODEPA.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TypeStructureIncompatibleException extends RuntimeException {
    public TypeStructureIncompatibleException(String message) {
        super(message);
    }
}
