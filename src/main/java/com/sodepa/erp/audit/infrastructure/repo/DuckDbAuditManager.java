package com.sodepa.erp.audit.infrastructure.repo;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestionnaire d'accès et d'écriture programmé pour la base de données embarquée DuckDB.
 * Permet de stocker les clés d'idempotence et les traces d'activité utilisateur hors de PostgreSQL.
 */
@Component
@DependsOn("clickHouseManager")
public class DuckDbAuditManager {

    private static final String CONNECTION_URL = "jdbc:duckdb:sodepa_audit.db";

    @Value("${clickhouse.url-http:http://localhost:8123}")
    private String clickhouseHttpUrl;

    @Value("${clickhouse.username:default}")
    private String clickhouseUser;

    @Value("${clickhouse.password:}")
    private String clickhousePass;

    @PostConstruct
    public void init() {
        try {
            // Charger le pilote DuckDB
            Class.forName("org.duckdb.DuckDBDriver");

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                // Table pour l'idempotence des requêtes
                stmt.execute("CREATE TABLE IF NOT EXISTS idempotency_keys (" +
                        "id VARCHAR PRIMARY KEY, " +
                        "statut VARCHAR, " +
                        "response_status INTEGER, " +
                        "response_body VARCHAR, " +
                        "created_at TIMESTAMP)");

                // Table pour le suivi d'activité utilisateur (audit trail)
                stmt.execute("CREATE TABLE IF NOT EXISTS user_activities (" +
                        "id VARCHAR PRIMARY KEY, " +
                        "user_id VARCHAR, " +
                        "username VARCHAR, " +
                        "session_id VARCHAR, " +
                        "action VARCHAR, " +
                        "method VARCHAR, " +
                        "uri VARCHAR, " +
                        "idempotency_key VARCHAR, " +
                        "timestamp TIMESTAMP, " +
                        "ip_address VARCHAR)");

                // Installer et charger l'extension httpfs pour la fédération avec ClickHouse
                stmt.execute("INSTALL httpfs");
                stmt.execute("LOAD httpfs");

                // Créer les vues fédérées vers les tables ClickHouse via l'API HTTP
                createClickHouseView(stmt, "ch_transactions",
                        "SELECT * FROM clickhouse_transactions");
                createClickHouseView(stmt, "ch_activities",
                        "SELECT * FROM clickhouse_activities");
                createClickHouseView(stmt, "ch_maker_checker",
                        "SELECT * FROM clickhouse_maker_checker");
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'initialiser la base de données DuckDB : " + e.getMessage(), e);
        }
    }

    /**
     * Crée ou remplace une vue DuckDB fédérée qui lit une table ClickHouse
     * via l'API HTTP de ClickHouse au format CSVWithNames.
     *
     * @param stmt      le statement JDBC DuckDB
     * @param viewName  le nom de la vue locale DuckDB
     * @param chQuery   la requête SELECT ClickHouse
     */
    private void createClickHouseView(Statement stmt, String viewName, String chQuery) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(chQuery + " FORMAT CSVWithNames", "UTF-8");
            String authParam = "";
            if (clickhouseUser != null && !clickhouseUser.isEmpty()) {
                authParam = "&user=" + java.net.URLEncoder.encode(clickhouseUser, "UTF-8");
                if (clickhousePass != null && !clickhousePass.isEmpty()) {
                    authParam += "&password=" + java.net.URLEncoder.encode(clickhousePass, "UTF-8");
                }
            }
            String fullUrl = clickhouseHttpUrl + "/?query=" + encodedQuery + authParam;
            stmt.execute("CREATE OR REPLACE VIEW " + viewName +
                    " AS SELECT * FROM read_csv_auto('" + fullUrl + "')");
        } catch (Exception e) {
            System.err.println("Avertissement : Impossible de créer la vue fédérée '" + viewName +
                    "' vers ClickHouse (le conteneur est peut-être arrêté) : " + e.getMessage());
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECTION_URL);
    }

    /**
     * Enregistre une nouvelle clé d'idempotence à l'état IN_PROGRESS.
     * 
     * @param key la clé d'idempotence
     * @param status le statut initial (ex: 'IN_PROGRESS')
     */
    public synchronized void saveIdempotencyKey(String key, String status) {
        String sql = "INSERT INTO idempotency_keys (id, statut, response_status, response_body, created_at) VALUES (?, ?, null, null, CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Échec de la sauvegarde de la clé d'idempotence dans DuckDB", e);
        }
    }

    /**
     * Recherche une clé d'idempotence et renvoie ses données.
     * 
     * @param key la clé d'idempotence
     * @return un dictionnaire contenant les valeurs ou null si non trouvée
     */
    public synchronized Map<String, Object> getIdempotencyKey(String key) {
        String sql = "SELECT statut, response_status, response_body FROM idempotency_keys WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("statut", rs.getString("statut"));
                    map.put("response_status", rs.getObject("response_status"));
                    map.put("response_body", rs.getString("response_body"));
                    return map;
                }
            }
        } catch (SQLException e) {
            // Ignoré, retourne null
        }
        return null;
    }

    /**
     * Met à jour une clé d'idempotence avec la réponse HTTP finale capturée.
     */
    public synchronized void updateIdempotencyKey(String key, String status, int responseStatus, String responseBody) {
        String sql = "UPDATE idempotency_keys SET statut = ?, response_status = ?, response_body = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, responseStatus);
            pstmt.setString(3, responseBody);
            pstmt.setString(4, key);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Échec de la mise à jour de la clé d'idempotence dans DuckDB", e);
        }
    }

    /**
     * Supprime une clé d'idempotence (en cas d'erreur réseau / applicative pour permettre le rejeu).
     */
    public synchronized void deleteIdempotencyKey(String key) {
        String sql = "DELETE FROM idempotency_keys WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Échec de la suppression de la clé d'idempotence dans DuckDB", e);
        }
    }

    /**
     * Enregistre une trace d'activité utilisateur dans la table DuckDB.
     */
    public synchronized void saveUserActivity(String userId, String username, String sessionId,
                                             String action, String method, String uri,
                                             String idempotencyKey, String ipAddress) {
        String sql = "INSERT INTO user_activities (id, user_id, username, session_id, action, method, uri, idempotency_key, timestamp, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, userId != null ? userId : "ANONYMOUS");
            pstmt.setString(3, username != null ? username : "ANONYMOUS");
            pstmt.setString(4, sessionId != null ? sessionId : "NO_SESSION");
            pstmt.setString(5, action);
            pstmt.setString(6, method);
            pstmt.setString(7, uri);
            pstmt.setString(8, idempotencyKey);
            pstmt.setString(9, ipAddress);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Log d'erreur silencieux pour ne pas bloquer la transaction métier principale en cas de pépin d'audit
        }
    }

    /**
     * Récupère l'historique complet d'activité pour un utilisateur à partir de DuckDB.
     */
    public synchronized List<Map<String, Object>> listActivities(String userId) {
        String sql = "SELECT id, user_id, username, session_id, action, method, uri, idempotency_key, timestamp, ip_address FROM user_activities WHERE user_id = ? ORDER BY timestamp DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getString("id"));
                    map.put("user_id", rs.getString("user_id"));
                    map.put("username", rs.getString("username"));
                    map.put("session_id", rs.getString("session_id"));
                    map.put("action", rs.getString("action"));
                    map.put("method", rs.getString("method"));
                    map.put("uri", rs.getString("uri"));
                    map.put("idempotency_key", rs.getString("idempotency_key"));
                    map.put("timestamp", rs.getTimestamp("timestamp").toString());
                    map.put("ip_address", rs.getString("ip_address"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            // Retourne la liste vide en cas d'erreur
        }
        return list;
    }

    /**
     * Récupère les transactions comptables archivées dans ClickHouse via la vue fédérée DuckDB.
     *
     * @param limit le nombre maximum de lignes à retourner
     * @return la liste des transactions sous forme de dictionnaires
     */
    public synchronized List<Map<String, Object>> listClickHouseTransactions(int limit) {
        String sql = "SELECT * FROM ch_transactions ORDER BY date_comptable DESC LIMIT ?";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur de lecture fédérée ch_transactions : " + e.getMessage());
        }
        return list;
    }

    /**
     * Récupère les activités utilisateur archivées dans ClickHouse via la vue fédérée DuckDB.
     *
     * @param limit le nombre maximum de lignes à retourner
     * @return la liste des activités sous forme de dictionnaires
     */
    public synchronized List<Map<String, Object>> listClickHouseActivities(int limit) {
        String sql = "SELECT * FROM ch_activities ORDER BY timestamp DESC LIMIT ?";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur de lecture fédérée ch_activities : " + e.getMessage());
        }
        return list;
    }

    /**
     * Exécute une requête SQL libre sur DuckDB, incluant les vues fédérées ClickHouse.
     * Utile pour des rapports analytiques ad-hoc combinant données locales et ClickHouse.
     *
     * @param query la requête SQL DuckDB
     * @return la liste des résultats sous forme de dictionnaires
     */
    public synchronized List<Map<String, Object>> executeAnalyticalQuery(String query) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                list.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur d'exécution de la requête analytique DuckDB : " + e.getMessage(), e);
        }
        return list;
    }
}
