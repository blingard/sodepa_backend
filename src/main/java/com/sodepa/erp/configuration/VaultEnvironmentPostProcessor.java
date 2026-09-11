package com.sodepa.erp.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge les secrets depuis HashiCorp Vault de manière sécurisée et légère.
 * S'exécute très tôt dans le cycle de vie de Spring Boot (avant le démarrage du contexte).
 * Évite les conflits de classpath ou de dépendance avec Spring Cloud Vault / Spring Vault Core.
 */
public class VaultEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(VaultEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String vaultUri = environment.getProperty("VAULT_URI", "http://localhost:8200");
        String vaultToken = environment.getProperty("VAULT_TOKEN", "sodepa-root-token");
        String secretPath = environment.getProperty("VAULT_SECRET_PATH", "secret/data/erp");

        log.info("🔐 Récupération des secrets depuis Vault : {} (path: {})", vaultUri, secretPath);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(vaultUri + "/v1/" + secretPath))
                    .header("X-Vault-Token", vaultToken)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                JsonNode dataNode = root.path("data");
                
                // Gestion KV v2 vs KV v1
                JsonNode secretsNode = dataNode.has("data") ? dataNode.path("data") : dataNode;

                Map<String, Object> vaultProperties = new HashMap<>();
                secretsNode.fields().forEachRemaining(entry -> {
                    vaultProperties.put(entry.getKey(), entry.getValue().asText());
                    // Masquer la valeur pour éviter de la logger en clair
                    log.info("   -> Secret chargé : {}", entry.getKey());
                });

                if (!vaultProperties.isEmpty()) {
                    MapPropertySource vaultPropertySource = new MapPropertySource("vaultProperties", vaultProperties);
                    environment.getPropertySources().addFirst(vaultPropertySource);
                    log.info("✅ {} secrets injectés avec succès depuis Vault", vaultProperties.size());
                }
            } else if (response.statusCode() == 404 && secretPath.contains("/data/")) {
                // Fallback KV v1
                String fallbackPath = secretPath.replace("/data/", "/");
                log.warn("⚠️ Path Vault KV v2 non trouvé (404), tentative avec le format KV v1 : {}", fallbackPath);
                
                HttpRequest fallbackRequest = HttpRequest.newBuilder()
                        .uri(URI.create(vaultUri + "/v1/" + fallbackPath))
                        .header("X-Vault-Token", vaultToken)
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                
                HttpResponse<String> fallbackResponse = client.send(fallbackRequest, HttpResponse.BodyHandlers.ofString());
                if (fallbackResponse.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(fallbackResponse.body());
                    JsonNode dataNode = root.path("data");
                    Map<String, Object> vaultProperties = new HashMap<>();
                    dataNode.fields().forEachRemaining(entry -> {
                        vaultProperties.put(entry.getKey(), entry.getValue().asText());
                        log.info("   -> Secret chargé (KV v1) : {}", entry.getKey());
                    });
                    if (!vaultProperties.isEmpty()) {
                        MapPropertySource vaultPropertySource = new MapPropertySource("vaultProperties", vaultProperties);
                        environment.getPropertySources().addFirst(vaultPropertySource);
                        log.info("✅ {} secrets injectés avec succès depuis Vault (KV v1)", vaultProperties.size());
                    }
                } else {
                    log.error("❌ Échec du fallback KV v1. Status: {}", fallbackResponse.statusCode());
                }
            } else {
                log.error("❌ Impossible de charger les secrets depuis Vault. HTTP Status: {}, Body: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("❌ Impossible de joindre HashiCorp Vault. L'application démarrera avec les valeurs par défaut. Erreur : {}", e.getMessage());
        }
    }
}
