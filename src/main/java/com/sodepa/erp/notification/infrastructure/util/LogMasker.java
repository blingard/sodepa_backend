package com.sodepa.erp.notification.infrastructure.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilitaire de masquage des données personnelles (PII) et d'assainissement des traces de log.
 */
public final class LogMasker {

    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9\\-._~+/]+=*");
    private static final Pattern SECRET_PARAM_PATTERN = Pattern.compile("(?i)(apiKey|api_key|token|password|secret)=[^&\\s]+");

    private LogMasker() {
    }

    /**
     * Masque une adresse e-mail tout en conservant le domaine pour le diagnostic.
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "[absent]";
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + domain.toLowerCase(Locale.ROOT);
    }

    /**
     * Masque un numéro de téléphone en ne conservant que l'indicatif et les 4 derniers chiffres.
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "[absent]";
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 4) {
            return "***";
        }
        String prefix = trimmed.startsWith("+") && trimmed.length() >= 7 ? trimmed.substring(0, 4) : "";
        String suffix = trimmed.substring(trimmed.length() - 4);
        return prefix + "***" + suffix;
    }

    /**
     * Tronque un message d'erreur technique et masque d'éventuels jetons secrets.
     */
    public static String sanitizeError(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }
        String cleaned = BEARER_PATTERN.matcher(error).replaceAll("Bearer ***");
        cleaned = SECRET_PARAM_PATTERN.matcher(cleaned).replaceAll("$1=***");
        cleaned = cleaned.replaceAll("[\\r\\n]+", " ").trim();
        return cleaned.length() > 480 ? cleaned.substring(0, 480) : cleaned;
    }
}
