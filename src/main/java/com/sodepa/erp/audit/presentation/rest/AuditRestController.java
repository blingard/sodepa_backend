package com.sodepa.erp.audit.presentation.rest;

import com.sodepa.erp.audit.infrastructure.repo.DuckDbAuditManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Service de Diagnostic et d'Exploration Analytique des Traces d'Audit (Module Audit & Supervision).
 * <p>
 * Ce service offre aux auditeurs internes et aux administrateurs une vision rapide sur les activites du systeme :
 * <ul>
 *   <li>Consultation des actions recentes realisees par l'utilisateur connecte a partir du moteur analytique DuckDB.</li>
 *   <li>Interrogation des transactions financieres et comptables archivees a long terme dans ClickHouse.</li>
 *   <li>Suivi des journaux d'activite et des connexions a des fins de conformite et de securite.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth/audit")
@RequiredArgsConstructor
@Tag(name = "Audit & Logs Analytiques", description = "Consultation des historiques d'activité utilisateurs et des transactions archivées dans ClickHouse/DuckDB")
public class AuditRestController {

    private final DuckDbAuditManager duckDbAuditManager;

    /**
     * Restitue l'ensemble des actions et operations recentes effectuees par l'utilisateur connecte.
     * <p>
     * Permet a chaque collaborateur de visualiser sa propre piste d'activite et de verifier l'historique
     * de ses interventions dans le systeme ERP.
     *
     * @param jwt jeton de securite contenant l'identite numerique de l'agent
     * @return la liste ordonnee des activites recentes de l'utilisateur
     */
    @GetMapping("/activities")
    @Operation(summary = "Consulter mon historique d'activité personnel")
    public ResponseEntity<List<Map<String, Object>>> getMyActivities(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<Map<String, Object>> activities = duckDbAuditManager.listActivities(userId);
        return ResponseEntity.ok(activities);
    }

    /**
     * Recherche et affiche les transactions financieres et comptables archivees dans le lac de donnees analytique.
     * <p>
     * Utilise les vues federees de DuckDB pour interroger directement le stockage haute performance ClickHouse,
     * garantissant une vitesse d'analyse elevee meme sur des millions d'ecritures comptables.
     *
     * @param limit nombre maximal de lignes a retourner (100 par defaut)
     * @return la liste des ecritures financieres archivees
     */
    @GetMapping("/clickhouse/transactions")
    @Operation(summary = "Consulter les transactions financières archivées dans le lac de données")
    public ResponseEntity<List<Map<String, Object>>> getClickHouseTransactions(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(duckDbAuditManager.listClickHouseTransactions(limit));
    }

    /**
     * Restitue les journaux globaux d'activites et de navigation des utilisateurs archives dans ClickHouse.
     * <p>
     * Outil privilegie des equipes de controle et de la Cellule Informatique pour les investigations d'audit forensique.
     *
     * @param limit nombre maximal d'enregistrements a restituer (100 par defaut)
     * @return la chronologie des evenements d'activite archivés
     */
    @GetMapping("/clickhouse/activities")
    @Operation(summary = "Consulter les journaux d'activité transverses archivés")
    public ResponseEntity<List<Map<String, Object>>> getClickHouseActivities(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(duckDbAuditManager.listClickHouseActivities(limit));
    }
}
