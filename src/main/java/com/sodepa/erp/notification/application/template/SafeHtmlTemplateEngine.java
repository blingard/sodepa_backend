package com.sodepa.erp.notification.application.template;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Moteur de rendu et de validation stricte des modèles de notification en base de données.
 * <p>
 * Zéro évaluation Thymeleaf/SpEL : seuls des placeholders {@code {{cle}}} appartenant à une whitelist explicite
 * sont substitués en une passe unique, avec échappement HTML systématique pour le rendu HTML et validation
 * des balises/attributs HTML à l'enregistrement (protection Anti-SSTI et Anti-XSS).
 * </p>
 */
@Component
public class SafeHtmlTemplateEngine {

    public static final Set<String> DEFAULT_ALLOWED_PLACEHOLDERS = Set.of(
            "destinataireNom",
            "entityName",
            "entityId",
            "etapeCode",
            "motif",
            "referenceDocument",
            "dateEcheance",
            "dateExpiration",
            "joursRestants",
            "typePiece",
            "username",
            "sujet",
            "message"
    );

    private static final Set<String> ALLOWED_HTML_TAGS = Set.of(
            "div", "p", "span", "strong", "b", "em", "i", "u", "br", "hr",
            "h1", "h2", "h3", "h4", "ul", "ol", "li", "table", "thead", "tbody",
            "tr", "th", "td", "a", "code", "pre"
    );

    private static final Set<String> ALLOWED_HTML_ATTRIBUTES = Set.of(
            "style", "class", "href", "target", "rel", "colspan", "rowspan"
    );

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");
    private static final Pattern SSTI_PROBE_PATTERN = Pattern.compile("(\\$\\{|<#|<%|\\*\\{|#\\{|\\bth:[a-zA-Z]+|\\[\\[)");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<\\s*(/?)\\s*([a-zA-Z0-9:-]+)([^>]*)>", Pattern.DOTALL);
    private static final Pattern HTML_COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern ATTR_PATTERN = Pattern.compile(
            "([a-zA-Z0-9:-]+)\\s*(?:=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+)))?"
    );
    private static final Pattern EVENT_HANDLER_ATTR_PATTERN = Pattern.compile("(?i)^on[a-z0-9_-]*$");
    private static final Pattern DANGEROUS_URI_PATTERN = Pattern.compile("(?i)^\\s*(javascript|vbscript|data\\s*:\\s*text/html)\\s*:");
    private static final Pattern DANGEROUS_STYLE_PATTERN = Pattern.compile("(?i)(expression\\s*\\(|javascript:|vbscript:|behavior\\s*:|url\\s*\\()");

    /**
     * Valide un modèle complet avant persistance en base (sujet, texte brut, HTML facultatif, liste de placeholders).
     */
    public void validateTemplate(
            String sujetTemplate,
            String bodyTextTemplate,
            String bodyHtmlTemplate,
            String allowedPlaceholdersCsv) {
        Set<String> whitelist = parseAllowedPlaceholders(allowedPlaceholdersCsv);

        validateNoSsti(sujetTemplate, "sujetTemplate");
        validateNoSsti(bodyTextTemplate, "bodyTextTemplate");
        validatePlaceholdersAllowed(sujetTemplate, whitelist, "sujetTemplate");
        validatePlaceholdersAllowed(bodyTextTemplate, whitelist, "bodyTextTemplate");

        if (bodyHtmlTemplate != null && !bodyHtmlTemplate.isBlank()) {
            validateNoSsti(bodyHtmlTemplate, "bodyHtmlTemplate");
            validatePlaceholdersAllowed(bodyHtmlTemplate, whitelist, "bodyHtmlTemplate");
            validateSafeHtmlMarkup(bodyHtmlTemplate);
        }
    }

    /**
     * Valide et assainit un fragment HTML administrateur selon une liste blanche stricte de balises et d'attributs.
     */
    public void validateSafeHtmlMarkup(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return;
        }
        String withoutComments = HTML_COMMENT_PATTERN.matcher(rawHtml).replaceAll("");
        if (withoutComments.contains("<!") || withoutComments.contains("<?")) {
            throw new IllegalArgumentException("Instructions XML/DOCTYPE interdites dans les templates HTML.");
        }

        Matcher tagMatcher = HTML_TAG_PATTERN.matcher(withoutComments);
        while (tagMatcher.find()) {
            boolean closing = "/".equals(tagMatcher.group(1));
            String tagName = tagMatcher.group(2).toLowerCase(Locale.ROOT);
            String attrsRaw = tagMatcher.group(3);

            if (!ALLOWED_HTML_TAGS.contains(tagName)) {
                throw new IllegalArgumentException("Balise HTML interdite dans un template : <" + tagName + ">");
            }
            if (closing) {
                continue;
            }
            validateTagAttributes(tagName, attrsRaw);
        }
    }

    /**
     * Rend un texte brut en une passe unique sans évaluation d'expressions ni ré-interprétation des valeurs injectées.
     */
    public String renderPlain(String template, Map<String, String> variables, String allowedPlaceholdersCsv) {
        if (template == null) {
            return "";
        }
        Set<String> whitelist = parseAllowedPlaceholders(allowedPlaceholdersCsv);
        return substitutePlaceholders(template, variables, whitelist, false);
    }

    /**
     * Rend un corps HTML de manière sûre : vérifie le markup, échappe chaque variable substituée via {@link HtmlUtils#htmlEscape}.
     */
    public String renderHtml(String htmlTemplate, Map<String, String> variables, String allowedPlaceholdersCsv) {
        if (htmlTemplate == null || htmlTemplate.isBlank()) {
            return null;
        }
        validateNoSsti(htmlTemplate, "bodyHtmlTemplate");
        validateSafeHtmlMarkup(htmlTemplate);
        Set<String> whitelist = parseAllowedPlaceholders(allowedPlaceholdersCsv);
        return substitutePlaceholders(htmlTemplate, variables, whitelist, true);
    }

    /**
     * Extrait la liste ordonnée des paramètres WhatsApp HSM selon {@code whatsappParamOrder}.
     */
    public List<String> extractOrderedWhatsAppParams(String paramOrderCsv, Map<String, String> variables) {
        if (paramOrderCsv == null || paramOrderCsv.isBlank()) {
            return List.of();
        }
        List<String> params = new ArrayList<>();
        Map<String, String> safeVars = variables != null ? variables : Map.of();
        for (String rawKey : paramOrderCsv.split(",")) {
            String key = rawKey.trim();
            if (!key.isEmpty()) {
                params.add(safeVars.getOrDefault(key, ""));
            }
        }
        return params;
    }

    public Set<String> parseAllowedPlaceholders(String csv) {
        if (csv == null || csv.isBlank()) {
            return DEFAULT_ALLOWED_PLACEHOLDERS;
        }
        Set<String> set = new LinkedHashSet<>();
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(set::add);
        return set.isEmpty() ? DEFAULT_ALLOWED_PLACEHOLDERS : set;
    }

    private String substitutePlaceholders(String template, Map<String, String> variables, Set<String> whitelist, boolean htmlEscapeValues) {
        Map<String, String> safeVars = variables != null ? variables : Map.of();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = "";
            if (whitelist.contains(key)) {
                String rawValue = safeVars.getOrDefault(key, "");
                replacement = htmlEscapeValues ? HtmlUtils.htmlEscape(rawValue) : rawValue;
                if (htmlEscapeValues) {
                    replacement = replacement.replace("\n", "<br/>");
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void validateNoSsti(String text, String fieldName) {
        if (text != null && SSTI_PROBE_PATTERN.matcher(text).find()) {
            throw new IllegalArgumentException(
                    "Syntaxe d'expression dynamique non autorisée dans " + fieldName + " (Anti-SSTI : utilisez uniquement {{placeholder}})."
            );
        }
    }

    private void validatePlaceholdersAllowed(String text, Set<String> whitelist, String fieldName) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher m = PLACEHOLDER_PATTERN.matcher(text);
        while (m.find()) {
            String key = m.group(1);
            if (!whitelist.contains(key)) {
                throw new IllegalArgumentException(
                        "Placeholder non autorisé '{{" + key + "}}' dans " + fieldName + " (autorisés : " + whitelist + ")."
                );
            }
        }
    }

    private void validateTagAttributes(String tagName, String attrsRaw) {
        if (attrsRaw == null || attrsRaw.isBlank()) {
            return;
        }
        String trimmed = attrsRaw.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        if (trimmed.isEmpty()) {
            return;
        }

        Matcher attrMatcher = ATTR_PATTERN.matcher(trimmed);
        while (attrMatcher.find()) {
            String attrName = attrMatcher.group(1).toLowerCase(Locale.ROOT);
            String attrVal = firstNonNull(attrMatcher.group(2), attrMatcher.group(3), attrMatcher.group(4));
            if (EVENT_HANDLER_ATTR_PATTERN.matcher(attrName).matches()) {
                throw new IllegalArgumentException("Attribut événementiel XSS interdit : " + attrName);
            }
            if (!ALLOWED_HTML_ATTRIBUTES.contains(attrName)) {
                throw new IllegalArgumentException("Attribut HTML non autorisé sur <" + tagName + "> : " + attrName);
            }
            if ("href".equals(attrName)) {
                if (!"a".equals(tagName)) {
                    throw new IllegalArgumentException("Attribut href autorisé uniquement sur <a>.");
                }
                if (attrVal != null && (DANGEROUS_URI_PATTERN.matcher(attrVal).find()
                        || (!attrVal.startsWith("https://") && !attrVal.startsWith("http://")
                        && !attrVal.startsWith("mailto:") && !attrVal.startsWith("/")))) {
                    throw new IllegalArgumentException("URL href non autorisée dans le template HTML.");
                }
            }
            if ("style".equals(attrName) && attrVal != null && DANGEROUS_STYLE_PATTERN.matcher(attrVal).find()) {
                throw new IllegalArgumentException("Directive CSS dangereuse détectée dans l'attribut style.");
            }
        }
    }

    private String firstNonNull(String a, String b, String c) {
        if (a != null) return a;
        if (b != null) return b;
        return c;
    }
}
