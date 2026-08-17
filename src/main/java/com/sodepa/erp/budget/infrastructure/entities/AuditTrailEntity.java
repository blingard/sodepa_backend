package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité de journalisation pour l'audit trail et l'historisation des actions budgétaires et financières.
 */
@Entity
@Table(name = "audit_trail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditTrailEntity {

    /**
     * Identifiant unique du log d'audit.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Nom simple ou de la classe de l'entité auditée (ex: 'BudgetPlanEntity').
     */
    @Column(name = "entite_nom", nullable = false, length = 100)
    private String entiteNom;

    /**
     * Identifiant de l'enregistrement de l'entité concernée.
     */
    @Column(name = "entite_id", nullable = false)
    private UUID entiteId;

    /**
     * Action effectuée (ex: 'CREATE', 'UPDATE', 'APPROVE', 'REALLOCATE').
     */
    @Column(nullable = false, length = 100)
    private String action;

    /**
     * Détails textuels ou format JSON décrivant les modifications (diffs).
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * Horodatage de l'action.
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * Identifiant de l'utilisateur ayant déclenché l'action.
     */
    @Column(name = "utilisateur")
    private UUID utilisateur;
}
