package com.sodepa.erp.user.infrastructure.entities;

import com.sodepa.erp.utils.Permissions;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entité représentant un utilisateur du système ERP SODEPA.
 * <p>
 * Centralise les identifiants IAM, le rattachement organisationnel,
 * l'état d'activation du compte, le statut MFA et les habilitations de sécurité.
 * </p>
 */
@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilisateurEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "USR";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant technique unique de l'utilisateur.
     */
    @Id
    private UUID id;

    /**
     * Identifiant de connexion unique (nom d'utilisateur ou matricule).
     */
    @Column(unique = true, nullable = false, updatable = false)
    private String username;

    /**
     * Nom de famille de l'utilisateur.
     */
    @Column(nullable = false)
    private String nom;

    /**
     * Prénom de l'utilisateur.
     */
    @Column(nullable = false)
    private String prenom;

    /**
     * Adresse électronique professionnelle de notification et d'authentification.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Identifiant du compte dans le fournisseur d'identité externe (Keycloak / IAM).
     */
    @Column(unique = true, nullable = false)
    private UUID iam;

    /**
     * Chemin d'accès ou URL vers la photo de profil de l'utilisateur.
     */
    @Column(name = "photo_profile")
    private String photoProfile;

    /**
     * Indicateur d'activation du compte (true si actif, false si désactivé ou révoqué).
     */
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean actif = true;

    /**
     * Identifiant de l'unité organisationnelle de rattachement principal (Direction, Service, Unité).
     */
    @Column(name = "unite_organisationnelle_id")
    private UUID uniteOrganisationnelleId;

    /**
     * Indicateur d'activation de l'authentification multifacteur (MFA / OTP).
     */
    @Builder.Default
    @Column(name = "mfa_active", nullable = false, columnDefinition = "boolean default false")
    private boolean mfaActive = false;

    /**
     * Date d'expiration du compte temporaire ou du prestataire externe.
     */
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    /**
     * Statut de cycle de vie du compte utilisateur (ACTIF, SUSPENDU, EXPIRE, DESACTIVE_SORTIE_RH).
     */
    @Builder.Default
    @Column(name = "statut_compte", length = 50, columnDefinition = "varchar(50) default 'ACTIF'")
    private String statutCompte = "ACTIF";

    /**
     * Numéros de téléphone de contact et d'envoi des codes SMS OTP.
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "utilisateur_telephones", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "telephone")
    private Set<String> telephones = new HashSet<>();

    /**
     * Ensemble des permissions unitaires attribuées à l'utilisateur.
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "utilisateur_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private Set<Permissions> permissions = new HashSet<>();
}
