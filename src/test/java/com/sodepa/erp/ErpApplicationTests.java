package com.sodepa.erp;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Démarrage complet du contexte Spring.
 *
 * <p>Marqué {@code @Tag("integration")} : ce test exige PostgreSQL, Kafka, Redis,
 * Keycloak et MinIO. Il est donc exclu de la CI publique
 * ({@code mvn test -DexcludedGroups=integration}) et se lance en local avec
 * {@code docker compose up -d} suivi de {@code mvn test}.</p>
 */
@Tag("integration")
@SpringBootTest
@Epic("Plateforme")
@Feature("Démarrage de l'application")
@DisplayName("Contexte applicatif Spring Boot")
class ErpApplicationTests {

    @Test
    @Story("Le contexte Spring démarre avec toutes ses dépendances")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Le contexte applicatif se charge sans erreur")
    void contextLoads() {
    }

}
