package com.sodepa.erp.audit.infrastructure.repo;

import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.configuration.RequestTrackingEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Gestionnaire d'accès direct et d'écriture pour l'entrepôt de données ClickHouse via JDBC.
 * Crée les structures analytiques et fournit des requêtes préparées pour la réplication.
 */
@Component
@Slf4j
public class ClickHouseManager {

    @Value("${clickhouse.url}")
    private String clickhouseUrl;

    @Value("${clickhouse.username}")
    private String clickhouseUsername;

    @Value("${clickhouse.password}")
    private String clickhousePassword;

    private final static String ACTIVITY_TABLE = "clickhouse_activities";
    private final static String TRANSACTION_TABLE = "clickhouse_transactions";
    private final static String MAKER_CHECKER_TABLE = "clickhouse_maker_checker";
    private final static String CLICKHOUSE_DRIVE = "com.clickhouse.jdbc.ClickHouseDriver";

    @PostConstruct
    public void init() {
        try {
            // Charger le pilote JDBC ClickHouse
            Class.forName(CLICKHOUSE_DRIVE);

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                // Table pour l'archivage analytique des transactions
                stmt.execute("CREATE TABLE IF NOT EXISTS clickhouse_transactions (" +
                        "id UUID, compte_code varchar, debit Decimal(18,4), credit Decimal(18,4), libelle Nullable(String), " +
                        "date_comptable DateTime, section_id Nullable(UUID)) ENGINE = MergeTree() ORDER BY date_comptable");

                // Table pour l'audit à chaud de toutes les requêtes utilisateurs
                stmt.execute("CREATE TABLE IF NOT EXISTS clickhouse_activities (" +
                        "id UUID, correlationId Nullable(String), sessionId varchar, timestamp DateTime, httpMethod Nullable(String), " +
                        "uri varchar, queryString Nullable(String), remoteIp Nullable(String), username Nullable(String), userId Nullable(String), " +
                        "requestHeaders Nullable(String), requestBody Nullable(String), responseStatus Nullable(String), responseHeaders Nullable(String), " +
                        "responseBody Nullable(String), durationMs Nullable(Int64)) ENGINE = MergeTree() ORDER BY (timestamp, sessionId, uri)");

                // Table pour l'audit des événements Maker-Checker
                stmt.execute("CREATE TABLE IF NOT EXISTS clickhouse_maker_checker (" +
                        "id UUID, entity_name Nullable(String), entity_pk Nullable(String), maker_id Nullable(String), " +
                        "checker_id Nullable(String), payload Nullable(String), date DateTime) ENGINE = MergeTree() ORDER BY date");
            }
        } catch (Exception e) {
            // Log d'erreur silencieux au démarrage si ClickHouse n'est pas encore lancé
            log.error("Avertissement : Impossible d'initialiser ClickHouse au démarrage (le conteneur est peut-être arrêté) : " + e.getMessage());

        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(clickhouseUrl, clickhouseUsername, clickhousePassword);
    }

    /**
     * Enregistre une transaction comptable dans la table analytique ClickHouse.
     */
    public void saveTransaction(UUID id, String compteCode, BigDecimal debit, BigDecimal credit,
                                String libelle, LocalDate dateComptable, UUID sectionId) {
        String sql = "INSERT INTO clickhouse_transactions (id, compte_code, debit, credit, libelle, date_comptable, section_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, id);
            pstmt.setString(2, compteCode);
            pstmt.setBigDecimal(3, debit);
            pstmt.setBigDecimal(4, credit);
            pstmt.setString(5, libelle);
            pstmt.setDate(6, Date.valueOf(dateComptable));
            if (sectionId != null) {
                pstmt.setObject(7, sectionId);
            } else {
                pstmt.setNull(7, Types.OTHER);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erreur d'insertion dans ClickHouse (Transaction) : " + e.getMessage());
        }
    }

    /**
     * Enregistre une trace d'activité utilisateur dans la table analytique ClickHouse.
     */
    public void saveActivity(RequestTrackingEvent msg) {
        String sql = "INSERT INTO clickhouse_activities (id, correlationId, sessionId, timestamp, httpMethod, uri, " +
                "queryString, remoteIp, username, userId, requestHeaders, requestBody, responseStatus, responseHeaders, " +
                "responseBody, durationMs) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.randomUUID());
            pstmt.setString(2, msg.correlationId());
            pstmt.setString(3, msg.sessionId());
            pstmt.setTimestamp(4, Timestamp.from(msg.timestamp()));
            pstmt.setString(5, msg.httpMethod());
            pstmt.setString(6, msg.uri());
            pstmt.setString(7, msg.queryString());
            pstmt.setString(8, msg.remoteIp());
            pstmt.setString(9, msg.username());
            pstmt.setString(10, msg.userId());
            pstmt.setString(11, msg.requestHeaders());
            pstmt.setString(12, msg.requestBody());
            pstmt.setString(13, String.valueOf(msg.responseStatus()));
            pstmt.setString(14, msg.responseHeaders());
            pstmt.setString(15, msg.responseBody());
            pstmt.setLong(16, msg.durationMs());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erreur d'insertion dans ClickHouse (Activité) : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Enregistre un événement Maker-Checker dans la table analytique ClickHouse.
     */
    public void saveMakerChecker(MakerCheckerMessageInput input) {
        String sql = "INSERT INTO clickhouse_maker_checker (id, entity_name, entity_pk, payload, date, maker_id, checker_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, input.id());
            pstmt.setString(2, input.entityName());
            pstmt.setString(3, input.entityPk());
            pstmt.setObject(4, input.payload());
            pstmt.setTimestamp(5, Timestamp.valueOf(input.timestamp()));
            pstmt.setString(6, input.maker_id());
            pstmt.setString(7, input.checker_id());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erreur d'insertion dans ClickHouse (MakerChecker) : " + e.getMessage());
        }
    }
}
