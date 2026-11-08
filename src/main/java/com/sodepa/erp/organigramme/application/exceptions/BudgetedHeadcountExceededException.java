package com.sodepa.erp.organigramme.application.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Composant applicatif `BudgetedHeadcountExceededException` du système ERP SODEPA.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BudgetedHeadcountExceededException extends RuntimeException {
    public BudgetedHeadcountExceededException(String message) {
        super(message);
    }
}
