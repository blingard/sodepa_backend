package com.sodepa.erp.audit.infrastructure.repo;

import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.application.inputs.WorkflowEventMessageInput;
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
    private final static String WORKFLOW_TABLE = "clickhouse_workflow_events";
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

                // Table pour l'audit asynchrone des événements et actions de Workflow
                stmt.execute("CREATE TABLE IF NOT EXISTS clickhouse_workflow_events (" +
                        "id UUID, instance_id Nullable(UUID), metier_code Nullable(String), definition_id Nullable(UUID), " +
                        "etape_id Nullable(UUID), tache_id Nullable(UUID), acteur_id Nullable(UUID), employe_id Nullable(UUID), " +
                        "matricule Nullable(String), user_id Nullable(String), username Nullable(String), unite_id Nullable(UUID), " +
                        "action_type varchar, decision Nullable(String), commentaire Nullable(String), payload Nullable(String), " +
                        "timestamp DateTime) ENGINE = MergeTree() ORDER BY (timestamp, action_type)");
                createWorkflowEntityStatesTable(stmt);
                workflowStatesTableReady = true;
            }
        } catch (Exception e) {
            // Log d'erreur silencieux au démarrage si ClickHouse n'est pas encore lancé
            log.error("Avertissement : Impossible d'initialiser ClickHouse au démarrage (le conteneur est peut-être arrêté) : " + e.getMessage());
        }
    }

    private volatile boolean workflowStatesTableReady = false;
    private volatile long circuitOpenUntilMillis = 0L;
    private volatile com.zaxxer.hikari.HikariDataSource clickhousePool;
    private static final long CIRCUIT_OPEN_COOLDOWN_MS = 15_000L;

    public boolean isCircuitOpen() {
        return System.currentTimeMillis() < circuitOpenUntilMillis;
    }

    private void openCircuitBreaker(String context, Exception e) {
        workflowStatesTableReady = false;
        boolean newlyOpened = !isCircuitOpen();
        if (newlyOpened) {
            circuitOpenUntilMillis = System.currentTimeMillis() + CIRCUIT_OPEN_COOLDOWN_MS;
            log.warn("Circuit breaker ClickHouse OUVERT pour 15s ({}) : {}", context, e.getMessage());
        }
        evictBrokenPoolConnections();
    }

    private void resetCircuitBreaker() {
        circuitOpenUntilMillis = 0L;
    }

    private void evictBrokenPoolConnections() {
        com.zaxxer.hikari.HikariDataSource pool = clickhousePool;
        if (pool != null && !pool.isClosed()) {
            try {
                if (pool.getHikariPoolMXBean() != null) {
                    pool.getHikariPoolMXBean().softEvictConnections();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private com.zaxxer.hikari.HikariDataSource getOrCreatePool() throws SQLException {
        if (clickhouseUrl == null || clickhouseUrl.isBlank()) {
            throw new SQLException("ClickHouse URL non configurée");
        }
        com.zaxxer.hikari.HikariDataSource existing = clickhousePool;
        if (existing != null && !existing.isClosed()) {
            return existing;
        }
        synchronized (this) {
            if (clickhousePool == null || clickhousePool.isClosed()) {
                com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource();
                ds.setPoolName("sodepa-clickhouse-pool");
                ds.setJdbcUrl(clickhouseUrl);
                ds.setUsername(clickhouseUsername != null ? clickhouseUsername : "default");
                ds.setPassword(clickhousePassword != null ? clickhousePassword : "");
                ds.setMaximumPoolSize(10);
                ds.setMinimumIdle(0);
                ds.setConnectionTimeout(2000L);
                ds.setValidationTimeout(1500L);
                ds.setIdleTimeout(30_000L);
                ds.setMaxLifetime(300_000L);
                ds.setInitializationFailTimeout(-1L);
                ds.setConnectionTestQuery("SELECT 1");
                clickhousePool = ds;
            }
            return clickhousePool;
        }
    }

    @jakarta.annotation.PreDestroy
    public void closePool() {
        com.zaxxer.hikari.HikariDataSource pool = clickhousePool;
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    private static final java.time.format.DateTimeFormatter CH_DATETIME64_UTC =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(java.time.ZoneOffset.UTC);

    public Connection getConnection() throws SQLException {
        return getOrCreatePool().getConnection();
    }

    public boolean isAvailable() {
        if (isCircuitOpen()) {
            return false;
        }
        return probePing("ping", true);
    }

    /**
     * Sonde activement la connectivité ClickHouse pour le rejeu Outbox afin de fermer le circuit breaker
     * dès la reprise du service sans allonger la fenêtre d'ouverture en cas d'échec persistant.
     */
    public boolean probeAvailability() {
        return probePing("outbox-probe", false);
    }

    private boolean probePing(String context, boolean openBreakerOnFailure) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT 1")) {
            resetCircuitBreaker();
            return rs.next();
        } catch (Exception e) {
            if (openBreakerOnFailure) {
                openCircuitBreaker(context, e);
            } else {
                evictBrokenPoolConnections();
            }
            return false;
        }
    }

    private void createWorkflowEntityStatesTable(Statement stmt) throws SQLException {
        stmt.execute("CREATE TABLE IF NOT EXISTS clickhouse_workflow_entity_states (" +
                "id UUID, " +
                "instance_id UUID, " +
                "version UInt32, " +
                "event_type String, " +
                "entity_name String, " +
                "entity_pk Nullable(String), " +
                "operation_type String, " +
                "definition_code Nullable(String), " +
                "etape_ordre Nullable(Int32), " +
                "etape_code Nullable(String), " +
                "task_id Nullable(UUID), " +
                "acteur_user_id Nullable(String), " +
                "acteur_username Nullable(String), " +
                "acteur_poste Nullable(String), " +
                "patch_json Nullable(String), " +
                "full_snapshot_json String, " +
                "attachments_json Nullable(String), " +
                "commentaire Nullable(String), " +
                "created_at DateTime64(3, 'UTC')" +
                ") ENGINE = MergeTree() ORDER BY (instance_id, version, created_at)");
    }

    private void ensureWorkflowEntityStatesTable(Connection conn) throws SQLException {
        if (!workflowStatesTableReady) {
            try (Statement stmt = conn.createStatement()) {
                createWorkflowEntityStatesTable(stmt);
                workflowStatesTableReady = true;
            }
        }
    }

    /**
     * Persiste de manière synchrone un instantané versionné d'instance de workflow dans ClickHouse.
     * Propage {@link SQLException} en cas d'indisponibilité afin de déclencher le repli Outbox PostgreSQL.
     */
    public void saveWorkflowEntityState(com.sodepa.erp.audit.application.inputs.WorkflowEntityStateSnapshot state)
            throws SQLException {
        if (isCircuitOpen()) {
            throw new SQLException("Circuit breaker ClickHouse actif : écriture différée vers Outbox.");
        }
        String sql = "INSERT INTO clickhouse_workflow_entity_states (" +
                "id, instance_id, version, event_type, entity_name, entity_pk, operation_type, " +
                "definition_code, etape_ordre, etape_code, task_id, acteur_user_id, acteur_username, acteur_poste, " +
                "patch_json, full_snapshot_json, attachments_json, commentaire, created_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            ensureWorkflowEntityStatesTable(conn);
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                java.util.UUID rowId = state.getId() != null ? state.getId() : java.util.UUID.randomUUID();
                java.time.Instant createdAt = state.getCreatedAt() != null ? state.getCreatedAt() : java.time.Instant.now();

                ps.setString(1, rowId.toString());
                ps.setString(2, state.getInstanceId().toString());
                ps.setInt(3, state.getVersion());
                ps.setString(4, state.getEventType());
                ps.setString(5, state.getEntityName());
                setNullableString(ps, 6, state.getEntityPk());
                ps.setString(7, state.getOperationType() != null ? state.getOperationType() : "WORKFLOW");
                setNullableString(ps, 8, state.getDefinitionCode());
                if (state.getEtapeOrdre() == null) {
                    ps.setNull(9, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(9, state.getEtapeOrdre());
                }
                setNullableString(ps, 10, state.getEtapeCode());
                setNullableString(ps, 11, state.getTaskId() != null ? state.getTaskId().toString() : null);
                setNullableString(ps, 12, state.getActeurUserId());
                setNullableString(ps, 13, state.getActeurUsername());
                setNullableString(ps, 14, state.getActeurPoste());
                setNullableString(ps, 15, state.getPatchJson());
                ps.setString(16, state.getFullSnapshotJson() != null ? state.getFullSnapshotJson() : "{}");
                setNullableString(ps, 17, state.getAttachmentsJson());
                setNullableString(ps, 18, state.getCommentaire());
                ps.setString(19, CH_DATETIME64_UTC.format(createdAt));
                ps.executeUpdate();
                resetCircuitBreaker();
            }
        } catch (SQLException e) {
            openCircuitBreaker("saveWorkflowEntityState", e);
            throw e;
        }
    }

    public boolean existsWorkflowEntityState(java.util.UUID id) throws SQLException {
        if (id == null) {
            return false;
        }
        if (isCircuitOpen()) {
            throw new SQLException("Circuit breaker ClickHouse actif");
        }
        String sql = "SELECT count() FROM clickhouse_workflow_entity_states WHERE id = ?";
        try (Connection conn = getConnection()) {
            ensureWorkflowEntityStatesTable(conn);
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id.toString());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    resetCircuitBreaker();
                    return rs.next() && rs.getLong(1) > 0;
                }
            }
        } catch (SQLException e) {
            openCircuitBreaker("existsWorkflowEntityState", e);
            throw e;
        }
    }

    public java.util.List<com.sodepa.erp.audit.application.inputs.WorkflowEntityStateSnapshot> findWorkflowEntityStates(
            java.util.UUID instanceId) throws SQLException {
        if (isCircuitOpen()) {
            throw new SQLException("Circuit breaker ClickHouse actif");
        }
        String sql = "SELECT id, instance_id, version, event_type, entity_name, entity_pk, operation_type, " +
                "definition_code, etape_ordre, etape_code, task_id, acteur_user_id, acteur_username, acteur_poste, " +
                "patch_json, full_snapshot_json, attachments_json, commentaire, " +
                "toUnixTimestamp64Milli(created_at) AS created_at_ms " +
                "FROM clickhouse_workflow_entity_states WHERE instance_id = ? " +
                "ORDER BY version ASC, created_at ASC";
        java.util.List<com.sodepa.erp.audit.application.inputs.WorkflowEntityStateSnapshot> results = new java.util.ArrayList<>();
        try (Connection conn = getConnection()) {
            ensureWorkflowEntityStatesTable(conn);
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, instanceId.toString());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapWorkflowEntityStateRow(rs));
                    }
                }
            }
            resetCircuitBreaker();
            return results;
        } catch (SQLException e) {
            openCircuitBreaker("findWorkflowEntityStates", e);
            throw e;
        }
    }

    public java.util.Optional<com.sodepa.erp.audit.application.inputs.WorkflowEntityStateSnapshot> findLatestWorkflowEntityState(
            java.util.UUID instanceId) throws SQLException {
        if (isCircuitOpen()) {
            throw new SQLException("Circuit breaker ClickHouse actif");
        }
        String sql = "SELECT id, instance_id, version, event_type, entity_name, entity_pk, operation_type, " +
                "definition_code, etape_ordre, etape_code, task_id, acteur_user_id, acteur_username, acteur_poste, " +
                "patch_json, full_snapshot_json, attachments_json, commentaire, " +
                "toUnixTimestamp64Milli(created_at) AS created_at_ms " +
                "FROM clickhouse_workflow_entity_states WHERE instance_id = ? " +
                "ORDER BY version DESC, created_at DESC LIMIT 1";
        try (Connection conn = getConnection()) {
            ensureWorkflowEntityStatesTable(conn);
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, instanceId.toString());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    resetCircuitBreaker();
                    if (rs.next()) {
                        return java.util.Optional.of(mapWorkflowEntityStateRow(rs));
                    }
                    return java.util.Optional.empty();
                }
            }
        } catch (SQLException e) {
            openCircuitBreaker("findLatestWorkflowEntityState", e);
            throw e;
        }
    }

    private com.sodepa.erp.audit.application.inputs.WorkflowEntityStateSnapshot mapWorkflowEntityStateRow(
            java.sql.ResultSet rs) throws SQLException {
        String taskIdStr = rs.getString("task_id");
        int etapeOrdreRaw = rs.getInt("etape_ordre");
        Integer etapeOrdre = rs.wasNull() ? null : etapeOrdreRaw;
        return com.sodepa.erp.audit.application.inputs.WorkflowEntityStateSnapshot.builder()
                .id(java.util.UUID.fromString(rs.getString("id")))
                .instanceId(java.util.UUID.fromString(rs.getString("instance_id")))
                .version(rs.getInt("version"))
                .eventType(rs.getString("event_type"))
                .entityName(rs.getString("entity_name"))
                .entityPk(rs.getString("entity_pk"))
                .operationType(rs.getString("operation_type"))
                .definitionCode(rs.getString("definition_code"))
                .etapeOrdre(etapeOrdre)
                .etapeCode(rs.getString("etape_code"))
                .taskId(taskIdStr != null && !taskIdStr.isBlank() ? java.util.UUID.fromString(taskIdStr) : null)
                .acteurUserId(rs.getString("acteur_user_id"))
                .acteurUsername(rs.getString("acteur_username"))
                .acteurPoste(rs.getString("acteur_poste"))
                .patchJson(rs.getString("patch_json"))
                .fullSnapshotJson(rs.getString("full_snapshot_json"))
                .attachmentsJson(rs.getString("attachments_json"))
                .commentaire(rs.getString("commentaire"))
                .createdAt(java.time.Instant.ofEpochMilli(rs.getLong("created_at_ms")))
                .storageSource("CLICKHOUSE")
                .build();
    }

    private void setNullableString(java.sql.PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    /**
     * Vide intégralement les données analytiques de ClickHouse.
     */
    public void truncateAllTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE IF EXISTS clickhouse_transactions");
            stmt.execute("TRUNCATE TABLE IF EXISTS clickhouse_activities");
            stmt.execute("TRUNCATE TABLE IF EXISTS clickhouse_maker_checker");
            stmt.execute("TRUNCATE TABLE IF EXISTS clickhouse_workflow_events");
            log.info("Tables ClickHouse purgées avec succès.");
        } catch (Exception e) {
            log.warn("Impossible de vider ClickHouse (le conteneur est peut-être arrêté) : {}", e.getMessage());
        }
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
            resetCircuitBreaker();
        } catch (SQLException e) {
            openCircuitBreaker("saveTransaction", e);
            log.error("Erreur d'insertion dans ClickHouse (Transaction) : " + e.getMessage());
            throw new IllegalStateException("Erreur d'insertion ClickHouse (Transaction)", e);
        }
    }

    /**
     * Enregistre une trace d'activité utilisateur dans la table analytique ClickHouse.
     */
    public void saveActivity(RequestTrackingEvent msg) {
        if (isCircuitOpen()) {
            throw new IllegalStateException("Circuit breaker ClickHouse actif : écriture activité différée");
        }
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
            resetCircuitBreaker();
        } catch (SQLException e) {
            openCircuitBreaker("saveActivity", e);
            log.warn("Erreur d'insertion dans ClickHouse (Activité) : {}", e.getMessage());
            throw new IllegalStateException("Erreur d'insertion ClickHouse (Activité)", e);
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
            resetCircuitBreaker();
        } catch (SQLException e) {
            openCircuitBreaker("saveMakerChecker", e);
            log.error("Erreur d'insertion dans ClickHouse (MakerChecker) : " + e.getMessage());
            throw new IllegalStateException("Erreur d'insertion ClickHouse (MakerChecker)", e);
        }
    }

    /**
     * Enregistre un événement de workflow de manière directe dans ClickHouse.
     */
    public void saveWorkflowEvent(WorkflowEventMessageInput event) {
        if (isCircuitOpen()) {
            return;
        }
        String sql = "INSERT INTO clickhouse_workflow_events (id, instance_id, metier_code, definition_id, " +
                "etape_id, tache_id, acteur_id, employe_id, matricule, user_id, username, unite_id, action_type, " +
                "decision, commentaire, payload, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, event.id() != null ? event.id() : UUID.randomUUID());
            pstmt.setObject(2, event.instanceId());
            pstmt.setString(3, event.metierCode());
            pstmt.setObject(4, event.definitionId());
            pstmt.setObject(5, event.etapeId());
            pstmt.setObject(6, event.tacheId());
            pstmt.setObject(7, event.acteurId());
            pstmt.setObject(8, event.employeId());
            pstmt.setString(9, event.matricule());
            pstmt.setString(10, event.userId());
            pstmt.setString(11, event.username());
            pstmt.setObject(12, event.uniteId());
            pstmt.setString(13, event.actionType());
            pstmt.setString(14, event.decision());
            pstmt.setString(15, event.commentaire());
            pstmt.setString(16, event.payload());
            pstmt.setTimestamp(17, event.timestamp() != null ? Timestamp.valueOf(event.timestamp()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            pstmt.executeUpdate();
            resetCircuitBreaker();
        } catch (SQLException e) {
            openCircuitBreaker("saveWorkflowEvent", e);
            log.error("Erreur d'insertion dans ClickHouse (WorkflowEvent) : " + e.getMessage());
        }
    }

    /**
     * Interroge directement ClickHouse avec pushdown SQL {@code ORDER BY ... DESC LIMIT ?}
     * pour éviter tout téléchargement complet de table via DuckDB {@code read_csv_auto}.
     */
    public java.util.List<java.util.Map<String, Object>> listTransactions(int limit) {
        return queryRecentRows("SELECT * FROM " + TRANSACTION_TABLE + " ORDER BY date_comptable DESC LIMIT ?", limit);
    }

    /**
     * Interroge directement ClickHouse avec pushdown SQL {@code ORDER BY ... DESC LIMIT ?}.
     */
    public java.util.List<java.util.Map<String, Object>> listActivities(int limit) {
        return queryRecentRows("SELECT * FROM " + ACTIVITY_TABLE + " ORDER BY timestamp DESC LIMIT ?", limit);
    }

    /**
     * Interroge les activités d'un utilisateur donné directement depuis ClickHouse (partagé multi-instances).
     */
    public java.util.List<java.util.Map<String, Object>> listActivitiesForUser(String userId, int limit) {
        if (isCircuitOpen() || userId == null || userId.isBlank()) {
            return java.util.List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 500));
        String sql = "SELECT id, userId AS user_id, username, sessionId AS session_id, " +
                "concat('HTTP_', coalesce(responseStatus, '200')) AS action, httpMethod AS method, uri, " +
                "correlationId AS idempotency_key, timestamp, remoteIp AS ip_address " +
                "FROM " + ACTIVITY_TABLE + " WHERE userId = ? ORDER BY timestamp DESC LIMIT ?";
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        Object val = rs.getObject(i);
                        row.put(meta.getColumnName(i), val instanceof UUID ? val.toString() : val);
                    }
                    rows.add(row);
                }
            }
            resetCircuitBreaker();
        } catch (SQLException e) {
            openCircuitBreaker("listActivitiesForUser", e);
            log.warn("Lecture ClickHouse activités utilisateur indisponible : {}", e.getMessage());
        }
        return rows;
    }

    private java.util.List<java.util.Map<String, Object>> queryRecentRows(String sql, int limit) {
        if (isCircuitOpen()) {
            return java.util.List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 500));
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        Object val = rs.getObject(i);
                        row.put(meta.getColumnName(i), val instanceof UUID ? val.toString() : val);
                    }
                    rows.add(row);
                }
            }
            resetCircuitBreaker();
        } catch (SQLException e) {
            openCircuitBreaker("queryRecentRows", e);
            log.warn("Lecture ClickHouse indisponible (LIMIT {}) : {}", safeLimit, e.getMessage());
        }
        return rows;
    }
}
