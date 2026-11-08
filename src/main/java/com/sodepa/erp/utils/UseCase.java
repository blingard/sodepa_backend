package com.sodepa.erp.utils;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `UseCase` au sein de la SODEPA.
 */
@FunctionalInterface
public interface UseCase<I, O> {
    O execute(I input);
}