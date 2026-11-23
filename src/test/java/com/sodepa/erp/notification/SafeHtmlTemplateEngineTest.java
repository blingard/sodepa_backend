package com.sodepa.erp.notification;

import com.sodepa.erp.notification.application.template.SafeHtmlTemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SafeHtmlTemplateEngineTest {

    private final SafeHtmlTemplateEngine engine = new SafeHtmlTemplateEngine();

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<div><SCRIPT src='https://evil.test/x.js'></script></div>",
            "<iframe src='https://evil.test'></iframe>",
            "<p onclick='alert(1)'>Click</p>",
            "<img src=x onerror=alert(1)>",
            "<a href='javascript:alert(1)'>Voir</a>",
            "<div style='width:expression(alert(1))'>Test</div>",
            "<svg onload='alert(1)'></svg>"
    })
    @DisplayName("Anti-XSS : rejette toute balise dangereuse, handler on* ou URI javascript:")
    void shouldRejectXssPayloadsOnSave(String maliciousHtml) {
        assertThrows(IllegalArgumentException.class, () ->
                engine.validateTemplate("[Sujet]", "Texte {{message}}", maliciousHtml, "message"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "${7*7}",
            "#{T(java.lang.Runtime).getRuntime().exec('id')}",
            "*{username}",
            "<div th:text='${secret}'></div>",
            "[[${evil}]]"
    })
    @DisplayName("Anti-SSTI : rejette toute syntaxe SpEL / Thymeleaf / OGNL dans les modèles")
    void shouldRejectSstiProbesOnSave(String probe) {
        assertThrows(IllegalArgumentException.class, () ->
                engine.validateTemplate("Sujet", "Bonjour " + probe, "<p>{{message}}</p>", "message"));
    }

    @Test
    @DisplayName("Whitelist placeholders : rejette tout placeholder non autorisé")
    void shouldRejectUnauthorizedPlaceholder() {
        assertThrows(IllegalArgumentException.class, () ->
                engine.validateTemplate("Sujet", "Bonjour {{passwordSecret}}", "<p>{{passwordSecret}}</p>", "destinataireNom,message"));
    }

    @Test
    @DisplayName("Rendu HTML sûr : échappe les variables utilisateur en une passe unique sans ré-évaluation")
    void shouldEscapeUserInputInSinglePassHtmlRender() {
        String htmlTpl = "<div class=\"card\"><p>Bonjour <strong>{{destinataireNom}}</strong>,</p><p>{{motif}}</p></div>";
        Map<String, String> vars = Map.of(
                "destinataireNom", "Jean <script>alert(1)</script>",
                "motif", "{{entityId}} & \"guillemets\"",
                "entityId", "INJECTED-SHOULD-NOT-EXPAND"
        );

        String rendered = engine.renderHtml(htmlTpl, vars, "destinataireNom,motif,entityId");

        assertFalse(rendered.contains("<script>"));
        assertTrue(rendered.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(rendered.contains("{{entityId}} &amp; &quot;guillemets&quot;"));
        assertFalse(rendered.contains("INJECTED-SHOULD-NOT-EXPAND"));
    }

    @Test
    @DisplayName("Extraction ordonnée des paramètres WhatsApp HSM")
    void shouldExtractOrderedWhatsAppParams() {
        List<String> params = engine.extractOrderedWhatsAppParams(
                "destinataireNom, entityName, etapeCode, motif",
                Map.of("destinataireNom", "Aminata", "entityName", "DA", "motif", "Budget insuffisant")
        );

        assertEquals(List.of("Aminata", "DA", "", "Budget insuffisant"), params);
    }

    @Test
    @DisplayName("CSS inline sûr : max-width et color hex ne doivent pas être bloqués par erreur")
    void shouldAllowSafeCssWidthAndColorsInSeededHtmlTemplates() {
        String seededHtml = "<div style=\"font-family:Arial,sans-serif;max-width:600px;color:#1e293b;\">"
                + "<p>Bonjour <strong>{{destinataireNom}}</strong> - {{message}}</p></div>";
        assertDoesNotThrow(() -> engine.validateTemplate("Sujet", "Msg", seededHtml, "destinataireNom,message"));
        String rendered = engine.renderHtml(seededHtml, Map.of("destinataireNom", "Flora", "message", "<b>OK</b>"), "destinataireNom,message");
        assertTrue(rendered.contains("max-width:600px"));
        assertTrue(rendered.contains("&lt;b&gt;OK&lt;/b&gt;"));
    }
}
