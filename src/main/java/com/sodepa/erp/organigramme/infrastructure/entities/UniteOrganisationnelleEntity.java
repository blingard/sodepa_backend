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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniteOrganisationnelleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String intitule;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_unite", nullable = false, length = 50)
    private TypeUniteOrganisationnelle typeUnite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_structure_id")
    private TypeStructureEntity typeStructure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private UniteOrganisationnelleEntity parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<UniteOrganisationnelleEntity> children = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "unite", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ObjectifUniteEntity> objectifs = new ArrayList<>();

    @Column(name = "materialized_path", nullable = false, length = 500)
    private String materializedPath;

    @Column(length = 100)
    private String localisation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutUnite statut = StatutUnite.ACTIF;

    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation;

    @Column(name = "date_fermeture")
    private LocalDate dateFermeture;
}
