package com.sodepa.erp.authorisation.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Affectation multidimensionnelle d'un profil fonctionnel à un utilisateur
 * par unité, magasin/site, exercice et seuil (REF-06 / F-REF-025 / CA-REF-025-01 & 02 / CT-REF-04).
 */
@Entity
@Table(name = "iam_affectations_profils")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationProfilUtilisateurEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "APU";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Identifiant unique.
     */
    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    /**
     * Profil dans le contexte du traitement AffectationProfilUtilisateurEntity.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profil_id", nullable = false)
    private ProfilFonctionnelEntity profil;

    /**
     * Restriction de périmètre à une unité organisationnelle spécifique (null = transversal).
     */
    @Column(name = "unite_organisationnelle_id")
    private UUID uniteOrganisationnelleId;

    /**
     * Restriction à un magasin, ranch ou abattoir spécifique (CA-REF-025-01).
     */
    @Column(name = "code_magasin_ou_site", length = 60)
    private String codeMagasinOuSite;

    /**
     * Restriction éventuelle à un exercice comptable/budgétaire.
     */
    @Column(name = "exercice_autorise")
    private Integer exerciceAutorise;

    /**
     * Plafond d'engagement spécifique accordé à l'agent (CA-REF-025-02).
     */
    @Column(name = "plafond_specifique_fcfa", precision = 18, scale = 2)
    private BigDecimal plafondSpecifiqueFcfa;

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_debut", nullable = false)
    @Builder.Default
    private LocalDate dateDebut = LocalDate.now();

    /**
     * Date de l'opération ou de l'échéance.
     */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
