package com.sodepa.erp.organigramme.infrastructure.entities;

import com.sodepa.erp.utils.Permissions;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entité représentant un poste budgété dans l'organigramme SODEPA.
 */
@Entity
@Table(name = "postes", indexes = {
    @Index(name = "idx_poste_code", columnList = "code_poste", unique = true),
    @Index(name = "idx_poste_unite", columnList = "unite_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosteEntity {

    @Id
    private UUID id;

    @Column(name = "code_poste", nullable = false, unique = true, length = 50)
    private String codePoste;

    @Column(nullable = false, length = 150)
    private String intitule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_id", nullable = false)
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_superieur_id")
    private PosteEntity posteSuperieur;

    @Column(name = "effectif_autorise", nullable = false)
    @Builder.Default
    private Integer effectifAutorise = 1;

    @Column(name = "effectif_actuel", nullable = false)
    @Builder.Default
    private Integer effectifActuel = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_convention", nullable = false, length = 50)
    private CategorieConvention categorieConvention;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutPoste statut = StatutPoste.OUVERT;

    @Column(name = "description_mission", columnDefinition = "TEXT")
    private String descriptionMission;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "poste_permissions", joinColumns = @JoinColumn(name = "poste_id"))
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private Set<Permissions> permissions = new HashSet<>();
}
