package com.sodepa.erp.workflow.infrastructure.entities;

import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA modélisant une opération ou activité métier dans l'ERP SODEPA (ex: RECRUITMENT, SERVICE_MEMO).
 * <p>
 * Remplace tout code de workflow en dur : le code applicatif fait référence à une opération métier
 * et le moteur résout dynamiquement le workflow configuré pour l'unité organisationnelle de l'employé.
 * </p>
 */
@Entity
@Table(name = "workflows_metiers", indexes = {
    @Index(name = "idx_wf_metier_code", columnList = "code", unique = true),
    @Index(name = "idx_wf_metier_domain", columnList = "domain"),
    @Index(name = "idx_wf_metier_unit", columnList = "authorized_unit_id")
})
/**
 * Entité de persistance représentant `WorkflowMetierEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowMetierEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "WM";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique universel (UUID) de l'opération métier.
     */
    @Id
    private UUID id;

    /**
     * Code fonctionnel unique en anglais (ex: "RECRUITMENT", "BUDGET_COMMITMENT", "SERVICE_MEMO", "ACCOUNTING_ENTRY").
     */
    @Column(nullable = false, unique = true, length = 60)
    private String code;

    /**
     * Intitulé lisible de l'opération métier (ex: "Recrutement d'un Salarié", "Rédaction d'une Note de Service").
     */
    @Column(nullable = false, length = 150)
    private String title;

    /**
     * Description détaillée de l'activité métier et de ses impacts.
     */
    @Column(columnDefinition = "TEXT", length = 1000)
    private String description;

    /**
     * Domaine d'application métier (RH, BUDGET, COMPTABILITE, FINANCE, ACHATS, GOUVERNANCE, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkflowDomaine domain;

    /**
     * Indicateur de transversalité :
     * <ul>
     *   <li>{@code true} : Métier transversal (ex: Note de service, Demande de congés). Chaque unité de la SODEPA
     *       peut configurer son propre workflow pour cette opération.</li>
     *   <li>{@code false} : Métier exclusif réservé à une unité spécifique ({@link #authorizedUnit}).</li>
     * </ul>
     */
    @Column(name = "is_transversal", nullable = false)
    @Builder.Default
    private boolean isTransversal = false;

    /**
     * Unité organisationnelle habilitée de façon exclusive (si le métier n'est pas transversal).
     * Exemple : La Cellule Comptabilité pour les écritures comptables.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorized_unit_id")
    private UniteOrganisationnelleEntity authorizedUnit;

    /**
     * Indicateur d'état actif de l'opération métier.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /**
     * Horodatage de création de la définition de l'opération métier.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public boolean isTransversal() {
        return this.isTransversal;
    }

    public Boolean getIsTransversal() {
        return this.isTransversal;
    }

    public void setIsTransversal(Boolean isTransversal) {
        this.isTransversal = isTransversal != null ? isTransversal : false;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public Boolean getIsActive() {
        return this.isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }
}
