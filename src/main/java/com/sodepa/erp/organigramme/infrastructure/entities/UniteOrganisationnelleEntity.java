package com.sodepa.erp.organigramme.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant une unité organisationnelle (Direction, Ranch, Abattoir, Magasin).
 * Utilise un chemin matérialisé (materializedPath) pour optimiser les requêtes hiérarchiques.
 */
@Entity
@Table(name = "unites_organisationnelles", indexes = {
    @Index(name = "idx_unite_code", columnList = "code", unique = true),
    @Index(name = "idx_unite_mat_path", columnList = "materialized_path")
})
/**
 * Entité de persistance représentant `UniteOrganisationnelleEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniteOrganisationnelleEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Code de référence unique normalisé dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Intitulé officiel ou désignation dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String intitule;

    /**
     * Champ typeUnite : type unite utilisé pour le pilotage et le suivi dans UniteOrganisationnelleEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_unite", nullable = false, length = 50)
    private TypeUniteOrganisationnelle typeUnite;

    /**
     * Champ typeStructure : type structure utilisé pour le pilotage et le suivi dans UniteOrganisationnelleEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_structure_id")
    private TypeStructureEntity typeStructure;

    /**
     * Champ parent : parent utilisé pour le pilotage et le suivi dans UniteOrganisationnelleEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private UniteOrganisationnelleEntity parent;

    /**
     * Champ children : children utilisé pour le pilotage et le suivi dans UniteOrganisationnelleEntity.
     */
    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<UniteOrganisationnelleEntity> children = new ArrayList<>();

    /**
     * Champ objectifs : objectifs utilisé pour le pilotage et le suivi dans UniteOrganisationnelleEntity.
     */
    @Builder.Default
    @OneToMany(mappedBy = "unite", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ObjectifUniteEntity> objectifs = new ArrayList<>();

    /**
     * Champ materializedPath : materialized path utilisé pour le pilotage et le suivi dans UniteOrganisationnelleEntity.
     */
    @Column(name = "materialized_path", nullable = false, length = 500)
    private String materializedPath;

    /**
     * Champ localisation : localisation utilisé pour le pilotage et le suivi dans UniteOrganisationnelleEntity.
     */
    @Column(length = 100)
    private String localisation;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutUnite statut = StatutUnite.ACTIF;

    /**
     * Date et heure de création de l'enregistrement dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation;

    /**
     * Champ dateFermeture : date fermeture utilisé pour le pilotage et le suivi dans UniteOrganisationnelleEntity.
     */
    @Column(name = "date_fermeture")
    private LocalDate dateFermeture;
}
