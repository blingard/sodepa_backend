package com.sodepa.erp.organigramme.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant un type de structure (ex: Direction Générale, Direction, Ranch, Abattoir, Service, etc.).
 * Les types peuvent être configurés avec ou sans type parent.
 * Lorsqu'un type a un type parent, toute unité de ce type doit obligatoirement être rattachée
 * sous une unité du type parent configuré.
 */
@Entity
@Table(name = "types_structures", indexes = {
    @Index(name = "idx_type_struct_code", columnList = "code", unique = true),
    @Index(name = "idx_type_struct_parent", columnList = "type_parent_id")
})
/**
 * Entité de persistance représentant `TypeStructureEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeStructureEntity {

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
     * Libellé descriptif officiel dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 100)
    private String libelle;

    /**
     * Description détaillée et informations contextuelles dans le cadre des opérations SODEPA.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Champ typeParent : type parent utilisé pour le pilotage et le suivi dans TypeStructureEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_parent_id")
    private TypeStructureEntity typeParent;

    /**
     * Champ sousTypes : sous types utilisé pour le pilotage et le suivi dans TypeStructureEntity.
     */
    @Builder.Default
    @OneToMany(mappedBy = "typeParent", cascade = CascadeType.ALL)
    private List<TypeStructureEntity> sousTypes = new ArrayList<>();

    /**
     * Champ niveauHierarchique : niveau hierarchique utilisé pour le pilotage et le suivi dans TypeStructureEntity.
     */
    @Column(name = "niveau_hierarchique", nullable = false)
    @Builder.Default
    private Integer niveauHierarchique = 1;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    /**
     * Champ couleur : couleur utilisé pour le pilotage et le suivi dans TypeStructureEntity.
     */
    @Column(length = 20)
    @Builder.Default
    private String couleur = "#4f46e5";

    /**
     * Champ icone : icone utilisé pour le pilotage et le suivi dans TypeStructureEntity.
     */
    @Column(length = 50)
    @Builder.Default
    private String icone = "solar:buildings-2-line-duotone";

    /**
     * Champ missionsTypes : missions types utilisé pour le pilotage et le suivi dans TypeStructureEntity.
     */
    @Column(name = "missions_types", columnDefinition = "TEXT")
    private String missionsTypes;

    /**
     * Champ createdAt : created at utilisé pour le pilotage et le suivi dans TypeStructureEntity.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Champ updatedAt : updated at utilisé pour le pilotage et le suivi dans TypeStructureEntity.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
