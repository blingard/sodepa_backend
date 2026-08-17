package com.sodepa.erp.share;

import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "maker_checker_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MakerCheckerRequestEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "entity_name", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private MakerCheckerEntityName entityName;

    @Column(name = "entity_pk", nullable = false, length = 100)
    private String entityPk;

    @Column(name = "maker_id", nullable = false, length = 100)
    private String makerId;

    @Column(name = "checker_id", length = 100)
    private String checkerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MakerCheckerStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applied_patch", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expiration_date", nullable = false, updatable = false)
    private Instant expiredAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    MakerCheckerOperationType checkerOperationType;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
