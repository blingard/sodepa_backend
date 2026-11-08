package com.sodepa.erp.authorisation.infrastructure.entities;

import com.sodepa.erp.utils.Permissions;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Profil fonctionnel réutilisable SODEPA (REF-06 / F-REF-025 / CA-REF-025-02 & 03 / RG-REF-018).
 * Distinct du poste organique : toute modification des permissions d'un profil s'applique
 * dynamiquement à tous les utilisateurs qui en sont porteurs.
 */
@Entity
@Table(name = "iam_profils_fonctionnels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilFonctionnelEntity {

    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(nullable = false, unique = true, length = 60)
    private String code;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 180)
    private String libelle;

    /**
     * Description détaillée des caractéristiques.
     */
    @Column(length = 400)
    private String description;

    /**
     * Plafond monétaire maximal de validation intrinsèque au profil (CA-REF-025-02).
     */
    @Column(name = "plafond_engagement_fcfa", precision = 18, scale = 2)
    private BigDecimal plafondEngagementFcfa;

    /**
     * Indique si le profil est sensible et exige le MFA (CA-REF-028-02).
     */
    @Column(name = "mfa_requis", nullable = false)
    @Builder.Default
    private boolean mfaRequis = false;

    /**
     * Droit d'accès ou permission élémentaire.
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "iam_profil_permissions", joinColumns = @JoinColumn(name = "profil_id"))
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private Set<Permissions> permissions = new HashSet<>();

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
