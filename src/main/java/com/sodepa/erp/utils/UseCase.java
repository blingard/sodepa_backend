package com.sodepa.erp.utils;

@FunctionalInterface
public interface UseCase<I, O> {
    O execute(I input);
}