package com.sodepa.erp.budgetaire.commun;

import com.sodepa.erp.share.erreurs.RequeteInvalideException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Forme des codes des référentiels budgétaires (nomenclature, natures,
 * sources de financement).
 *
 * <p>
 * Ce n'est pas un masque de codification : le plan réel de la SODEPA n'est pas
 * encore connu (Q-01, H-01). On écarte seulement ce qui casserait un export,
 * un import ou une recherche (espaces, accents, barres obliques), et on met le
 * code en majuscules pour que « p01 » et « P01 » soient le même code.
 * </p>
 */
public final class CodeReferentiel {

    private static final Pattern FORME = Pattern.compile("[A-Z0-9][A-Z0-9._-]{0,29}");

    private CodeReferentiel() {
    }

    /**
     * @return le code sans espaces autour, en majuscules
     * @throws RequeteInvalideException {@code CODE_OBLIGATOIRE} ou {@code CODE_INVALIDE}
     */
    public static String valide(String code) {
        if (code == null || code.isBlank()) {
            throw new RequeteInvalideException("CODE_OBLIGATOIRE", "Indiquez le code.");
        }
        String normalise = code.strip().toUpperCase(Locale.ROOT);
        if (!FORME.matcher(normalise).matches()) {
            throw new RequeteInvalideException("CODE_INVALIDE",
                    "Le code ne doit contenir que des lettres sans accent, des chiffres et les séparateurs "
                            + "« - », « . » ou « _ », sans espace, et 30 caractères au plus.");
        }
        return normalise;
    }
}
