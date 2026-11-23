package com.sodepa.erp.notification.infrastructure.util;

/**
 * Normalisation centralisée des numéros de téléphone (Cameroun / zone CEMAC et format international E.164).
 */
public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {
    }

    /**
     * Normalise un numéro au format E.164 (par défaut {@code +237}) ou retourne {@code null} s'il est vide/invalide.
     */
    public static String normalize(String rawPhone, String defaultCountryCode) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String digits = rawPhone.replaceAll("[\\s\\-.()]", "").trim();
        if (digits.isBlank()) {
            return null;
        }
        if (digits.startsWith("00")) {
            digits = "+" + digits.substring(2);
        }
        if (digits.startsWith("+")) {
            String body = digits.substring(1).replaceAll("\\D", "");
            return body.length() >= 8 && body.length() <= 15 ? "+" + body : null;
        }
        String onlyDigits = digits.replaceAll("\\D", "");
        if (onlyDigits.isEmpty()) {
            return null;
        }
        String ccDigits = (defaultCountryCode == null || defaultCountryCode.isBlank())
                ? "237"
                : defaultCountryCode.replaceAll("\\D", "");
        if (onlyDigits.startsWith(ccDigits) && onlyDigits.length() >= ccDigits.length() + 7) {
            return "+" + onlyDigits;
        }
        if (onlyDigits.length() >= 8 && onlyDigits.length() <= 10) {
            return "+" + ccDigits + onlyDigits;
        }
        return onlyDigits.length() >= 8 && onlyDigits.length() <= 15 ? "+" + onlyDigits : null;
    }

    /**
     * Format attendu par Meta WhatsApp Cloud API (E.164 sans le préfixe '+').
     */
    public static String toMetaWhatsAppNumber(String e164Phone) {
        String normalized = normalize(e164Phone, "+237");
        if (normalized == null) {
            return null;
        }
        return normalized.startsWith("+") ? normalized.substring(1) : normalized;
    }
}
