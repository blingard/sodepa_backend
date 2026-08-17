package com.sodepa.erp.comptabilite.analytique.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Entité représentant une section analytique spécifique (ex: 'R&D', 'PROJET_BETA', 'DIRECTION_TECHNIQUE').
 * Chaque section appartient à un axe analytique parent et reçoit une quote-part des charges ou produits ventilés.
 */
@Entity
@Table(name = "sections_analytiques")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionAnalytiqueEntity {

    /**
     * Identifiant unique de la section analytique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Axe analytique parent auquel est rattachée la section.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "axe_id", nullable = false)
    private AxeAnalytiqueEntity axe;

    /**
     * Code abrégé de la section (ex: 'RD', 'PROJ_BETA', 'DIR_TECH').
     */
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * Libellé ou intitulé complet de la section.
     */
    @Column(nullable = false)
    private String intitule;

    /**
     * Statut d'activation de la section. Une section inactive ne peut plus recevoir de nouvelles ventilations.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
