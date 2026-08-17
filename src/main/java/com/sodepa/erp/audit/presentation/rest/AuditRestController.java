package com.sodepa.erp.audit.presentation.rest;

import com.sodepa.erp.audit.infrastructure.repo.DuckDbAuditManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST de diagnostic pour consulter les logs d'activité directement depuis DuckDB
 * et les données analytiques archivées dans ClickHouse via les vues fédérées DuckDB.
 */
@RestController
@RequestMapping("/api/auth/audit")
@RequiredArgsConstructor
public class AuditRestController {

    private final DuckDbAuditManager duckDbAuditManager;

    /**
     * Récupère l'historique complet d'activité de l'utilisateur connecté stocké dans DuckDB.
     */
    @GetMapping("/activities")
    public ResponseEntity<List<Map<String, Object>>> getMyActivities(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<Map<String, Object>> activities = duckDbAuditManager.listActivities(userId);
        return ResponseEntity.ok(activities);
    }

    /**
     * Récupère les transactions comptables archivées dans ClickHouse via la vue fédérée DuckDB.
     *
     * @param limit nombre maximum de résultats (défaut 100)
     */
    @GetMapping("/clickhouse/transactions")
    public ResponseEntity<List<Map<String, Object>>> getClickHouseTransactions(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(duckDbAuditManager.listClickHouseTransactions(limit));
    }

    /**
     * Récupère les activités utilisateur archivées dans ClickHouse via la vue fédérée DuckDB.
     *
     * @param limit nombre maximum de résultats (défaut 100)
     */
    @GetMapping("/clickhouse/activities")
    public ResponseEntity<List<Map<String, Object>>> getClickHouseActivities(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(duckDbAuditManager.listClickHouseActivities(limit));
    }

    /**
     * Exécute une requête analytique ad-hoc sur DuckDB (incluant les vues fédérées ClickHouse).
     * Permet de combiner les données locales DuckDB avec les données ClickHouse.
     *
     * @param query la requête SQL DuckDB (ex: SELECT count(*) FROM ch_transactions)
     */
    @GetMapping("/analytics")
    public ResponseEntity<List<Map<String, Object>>> executeAnalyticalQuery(@RequestParam String query) {
        return ResponseEntity.ok(duckDbAuditManager.executeAnalyticalQuery(query));
    }
}
