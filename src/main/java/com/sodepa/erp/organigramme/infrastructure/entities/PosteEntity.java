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
/**
 * Entité de persistance représentant `PosteEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosteEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ codePoste : code poste utilisé pour le pilotage et le suivi dans PosteEntity.
     */
    @Column(name = "code_poste", nullable = false, unique = true, length = 50)
    private String codePoste;

    /**
     * Intitulé officiel ou désignation dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String intitule;

    /**
     * Champ uniteOrganisationnelle : unite organisationnelle utilisé pour le pilotage et le suivi dans PosteEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_id", nullable = false)
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    /**
     * Champ posteSuperieur : poste superieur utilisé pour le pilotage et le suivi dans PosteEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_superieur_id")
    private PosteEntity posteSuperieur;

    /**
     * Champ effectifAutorise : effectif autorise utilisé pour le pilotage et le suivi dans PosteEntity.
     */
    @Column(name = "effectif_autorise", nullable = false)
    @Builder.Default
    private Integer effectifAutorise = 1;

    /**
     * Champ effectifActuel : effectif actuel utilisé pour le pilotage et le suivi dans PosteEntity.
     */
    @Column(name = "effectif_actuel", nullable = false)
    @Builder.Default
    private Integer effectifActuel = 0;

    /**
     * Champ categorieConvention : categorie convention utilisé pour le pilotage et le suivi dans PosteEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_convention", nullable = false, length = 50)
    private CategorieConvention categorieConvention;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutPoste statut = StatutPoste.OUVERT;

    /**
     * Champ descriptionMission : description mission utilisé pour le pilotage et le suivi dans PosteEntity.
     */
    @Column(name = "description_mission", columnDefinition = "TEXT")
    private String descriptionMission;

    /**
     * Ensemble des permissions d'accès et droits attribués dans le cadre des opérations SODEPA.
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "poste_permissions", joinColumns = @JoinColumn(name = "poste_id"))
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private Set<Permissions> permissions = new HashSet<>();
}
