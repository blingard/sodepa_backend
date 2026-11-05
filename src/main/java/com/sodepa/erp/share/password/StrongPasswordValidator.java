package com.sodepa.erp.share.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, StrongPasswordSubject> {

    @Override
    public boolean isValid(StrongPasswordSubject value, ConstraintValidatorContext context) {
        String reason = "PASSWORD_INVALID_NULL_OR_EMPTY";
        if (value == null || value.password() == null || value.password().isEmpty() || value.password().isBlank()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(reason)
                    .addPropertyNode("password")
                    .addConstraintViolation();
            return false;
        }
        reason = findWeakness(value.password());
        if (reason == null) {
            return true;
        }else {

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(reason)
                    .addPropertyNode("password")
                    .addConstraintViolation();
            return false;
        }
    }

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    private String findWeakness(String password) {
        if (password.length() < MIN_LENGTH) {
            return "PASSWORD_TOO_SHORT";
        }
        if (password.length() > MAX_LENGTH) {
            return "PASSWORD_TOO_LONG";
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            return "PASSWORD_CONTAINS_WHITESPACE";
        }
        if (password.matches("\\d+")) {
            return "PASSWORD_ONLY_DIGITS";
        }
        if (!password.matches(".*[a-z].*")) {
            return "PASSWORD_NO_LOWERCASE";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "PASSWORD_NO_UPPERCASE";
        }
        if (!password.matches(".*\\d.*")) {
            return "PASSWORD_NO_DIGIT";
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "PASSWORD_NO_SPECIAL_CHAR";
        }
        if (password.matches(".*(.)\\1{2,}.*")) {
            return "PASSWORD_REPEATED_CHAR";
        }
        return null;
    }
}
