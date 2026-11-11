package com.sodepa.erp.utils;

/**
 * Classe utilitaire pour la génération de codes et de références métier au sein du système ERP SODEPA.
 * <p>
 * Permet de produire des codes séquentiels normés et standardisés sous le format :
 * {@code <PREFIXE>-<SEQUENCE_ZERO_PADDEE>} (ex: {@code DG-00000001}, {@code RH-00000125}, {@code ACH-00000003}).
 * </p>
 */
public final class CodeGenerator {

    /** Longueur par défaut du remplissage par des zéros (8 chiffres). */
    public static final int DEFAULT_PADDING = 8;

    /** Séparateur standard utilisé entre le préfixe et le numéro séquentiel. */
    public static final String DEFAULT_SEPARATOR = "-";

    private CodeGenerator() {
        throw new UnsupportedOperationException("Cette classe utilitaire ne doit pas être instanciée.");
    }

    /**
     * Génère un code normé au format {@code PREFIXE-00000001} avec 8 chiffres.
     *
     * @param prefixe  Préfixe textuel (ex: "DG", "RH", "ACH", "WF").
     * @param sequence Numéro séquentiel numérique (ex: 1, 42, 1000).
     * @return Le code formaté (ex: {@code "DG-00000001"}).
     * @throws IllegalArgumentException si le préfixe est null/vide ou si la séquence est négative.
     */
    public static String generate(String prefixe, long sequence) {
        return Formatter.format(prefixe, sequence, DEFAULT_PADDING, DEFAULT_SEPARATOR);
    }

    /**
     * Génère un code normé avec un nombre personnalisé de zéros de remplissage.
     *
     * @param prefixe  Préfixe textuel (ex: "DG").
     * @param sequence Numéro séquentiel numérique.
     * @param padding  Nombre total minimal de chiffres (ex: 8).
     * @return Le code formaté (ex: {@code "DG-00000001"}).
     */
    public static String generate(String prefixe, long sequence, int padding) {
        return Formatter.format(prefixe, sequence, padding, DEFAULT_SEPARATOR);
    }

    /**
     * Classe statique interne dédiée au formatage, à la validation et à la normalisation des codes.
     */
    public static final class Formatter {

        private Formatter() {
            throw new UnsupportedOperationException("Cette classe utilitaire interne ne doit pas être instanciée.");
        }

        /**
         * Formate un préfixe et un numéro séquentiel avec un padding et un séparateur configurables.
         *
         * @param prefixe    Préfixe textuel (ex: "DG").
         * @param sequence   Numéro séquentiel positif ou nul (ex: 1).
         * @param padding    Largeur minimale du bloc numérique (remplissage par des zéros à gauche).
         * @param separateur Caractère ou chaîne de séparation (ex: "-").
         * @return Le code final généré et normalisé en majuscules (ex: {@code "DG-00000001"}).
         * @throws IllegalArgumentException si les paramètres sont invalides.
         */
        public static String format(String prefixe, long sequence, int padding, String separateur) {
            if (prefixe == null || prefixe.isBlank()) {
                throw new IllegalArgumentException("Le préfixe ne peut pas être null ou vide.");
            }
            if (sequence < 0) {
                throw new IllegalArgumentException("Le numéro séquentiel doit être positif ou nul. Valeur fournie : " + sequence);
            }
            if (padding < 1) {
                throw new IllegalArgumentException("La largeur (padding) doit être au minimum de 1 chiffre. Valeur fournie : " + padding);
            }

            String sep = (separateur != null) ? separateur : "";
            String cleanPrefix = prefixe.trim().toUpperCase();
            String formatPattern = "%s" + sep + "%0" + padding + "d";

            return String.format(formatPattern, cleanPrefix, sequence);
        }
    }
}
