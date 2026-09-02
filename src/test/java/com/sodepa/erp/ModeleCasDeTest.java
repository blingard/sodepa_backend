package com.sodepa.erp;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * MODÈLE À COPIER pour écrire un nouveau cas de test.
 *
 * <p>Marche à suivre : dupliquer ce fichier dans le package du module concerné,
 * le renommer en {@code <CeQuiEstTesté>Test.java}, retirer {@code @Disabled},
 * puis remplir les annotations et le corps du test.</p>
 *
 * <p>Tant que le test reste {@code @Disabled}, il apparaît en « skipped » dans le
 * rapport : c'est la façon recommandée de déclarer un cas de test identifié mais
 * pas encore automatisé, sans faire échouer la CI.</p>
 *
 * <p>Convention de tags JUnit :</p>
 * <ul>
 *   <li>{@code @Tag("unit")} — aucune infrastructure requise, tourne en CI ;</li>
 *   <li>{@code @Tag("integration")} — nécessite PostgreSQL / Kafka / Redis / Keycloak,
 *       exclu de la CI par {@code -DexcludedGroups=integration}.</li>
 * </ul>
 */
@Tag("unit")
@Epic("À remplacer : le domaine métier — Comptabilité générale, Budget, Trésorerie, Audit…")
@Feature("À remplacer : la fonctionnalité — Écritures comptables, Engagements de dépenses…")
@Owner("à-remplacer-par-le-trigramme-ou-le-nom")
@DisplayName("À remplacer : intitulé lisible du regroupement de cas de test")
class ModeleCasDeTest {

    @Test
    @Disabled("Modèle — retirer cette ligne une fois le cas de test écrit")
    @Story("À remplacer : le comportement attendu, formulé du point de vue métier")
    @Severity(SeverityLevel.NORMAL) // BLOCKER > CRITICAL > NORMAL > MINOR > TRIVIAL
    @TmsLink("SODEPA-000")
    @DisplayName("À remplacer : ce que le test vérifie, en une phrase")
    void nommerLeTestAvecUnePhraseQuiDecritLeComportement() {
        // ── ÉTANT DONNÉ ──────────────────────────────────────────────────────
        preparerLesDonnees("jeu de données de départ");

        // ── QUAND ────────────────────────────────────────────────────────────
        executerLAction("action métier déclenchée");

        // ── ALORS ────────────────────────────────────────────────────────────
        verifierLeResultat("résultat attendu");

        // Trace libre dans le rapport (utile pour un contexte non structuré).
        Allure.parameter("Exercice comptable", "2026");
        Allure.description("Explication détaillée, visible dans l'onglet du cas de test.");
    }

    @Step("Étant donné : {contexte}")
    private void preparerLesDonnees(String contexte) {
        // Construire les objets d'entrée ici.
    }

    @Step("Quand : {action}")
    private void executerLAction(String action) {
        // Appeler le use case / la méthode sous test ici.
    }

    @Step("Alors : {attendu}")
    private void verifierLeResultat(String attendu) {
        // Assertions AssertJ ici : assertThat(...).isEqualTo(...)
    }
}
