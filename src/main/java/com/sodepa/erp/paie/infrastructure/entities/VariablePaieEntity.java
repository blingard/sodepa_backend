package com.sodepa.erp.paie.infrastructure.entities;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité de persistance représentant `VariablePaieEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "paie_variables_mensuelles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariablePaieEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ periode : periode utilisé pour le pilotage et le suivi dans VariablePaieEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periode_id", nullable = false)
    private PeriodePaieEntity periode;

    /**
     * Employé ou collaborateur SODEPA concerné dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    /**
     * Champ codeRubrique : code rubrique utilisé pour le pilotage et le suivi dans VariablePaieEntity.
     */
    @Column(name = "code_rubrique", nullable = false, length = 30)
    private String codeRubrique;

    /**
     * Libellé descriptif officiel dans le cadre des opérations SODEPA.
     */
    @Column(length = 150)
    private String libelle;

    /**
     * Montant financier exprimé en Francs CFA (XAF) dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    /**
     * Quantité ou volume exprimé dans l'unité de mesure de référence dans le cadre des opérations SODEPA.
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantite = BigDecimal.ONE;

    /**
     * Commentaires et observations opérationnelles dans le cadre des opérations SODEPA.
     */
    @Column(length = 255)
    private String commentaire;
}
