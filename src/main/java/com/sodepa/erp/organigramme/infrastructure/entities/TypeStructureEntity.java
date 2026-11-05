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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeStructureEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_parent_id")
    private TypeStructureEntity typeParent;

    @Builder.Default
    @OneToMany(mappedBy = "typeParent", cascade = CascadeType.ALL)
    private List<TypeStructureEntity> sousTypes = new ArrayList<>();

    @Column(name = "niveau_hierarchique", nullable = false)
    @Builder.Default
    private Integer niveauHierarchique = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(length = 20)
    @Builder.Default
    private String couleur = "#4f46e5";

    @Column(length = 50)
    @Builder.Default
    private String icone = "solar:buildings-2-line-duotone";

    @Column(name = "missions_types", columnDefinition = "TEXT")
    private String missionsTypes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

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
