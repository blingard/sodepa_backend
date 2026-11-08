package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité de persistance représentant `MarcheDossierEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "marche_dossiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcheDossierEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ numeroDossier : numero dossier utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @Column(name = "numero_dossier", nullable = false, unique = true, length = 60)
    private String numeroDossier;

    /**
     * Champ typeProcedure : type procedure utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_procedure", nullable = false, length = 40)
    private TypeProcedureAchat typeProcedure;

    /**
     * Champ objet : objet utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @Column(nullable = false, length = 255)
    private String objet;

    /**
     * Champ expressionBesoin : expression besoin utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expression_besoin_id")
    private ExpressionBesoinEntity expressionBesoin;

    /**
     * Fournisseur partenaire ou adjudicataire de la commande dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private FournisseurEntity fournisseur;

    /**
     * Nom ou désignation textuelle de fournisseur pour l'affichage et la traçabilité.
     */
    @Column(name = "fournisseur_nom", length = 200)
    private String fournisseurNom;

    /**
     * Montant hors taxes en Francs CFA (XAF) dans le cadre des opérations SODEPA.
     */
    @Column(name = "montant_ht", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantHt = BigDecimal.ZERO;

    /**
     * Montant de la TVA appliquée en Francs CFA (XAF) dans le cadre des opérations SODEPA.
     */
    @Column(name = "montant_tva", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantTva = BigDecimal.ZERO;

    /**
     * Montant toutes taxes comprises en Francs CFA (XAF) dans le cadre des opérations SODEPA.
     */
    @Column(name = "montant_ttc", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantTtc = BigDecimal.ZERO;

    /**
     * Code normalisé unique identifiant ligneBudgetaireCode dans le référentiel SODEPA.
     */
    @Column(name = "ligne_budgetaire_code", length = 100)
    private String ligneBudgetaireCode;

    /**
     * Champ delaiLivraisonJours : delai livraison jours utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @Column(name = "delai_livraison_jours")
    @Builder.Default
    private Integer delaiLivraisonJours = 90;

    /**
     * Champ dateNotification : date notification utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @Column(name = "date_notification")
    private LocalDate dateNotification;

    /**
     * Champ dateLimiteLivraison : date limite livraison utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @Column(name = "date_limite_livraison")
    private LocalDate dateLimiteLivraison;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutMarcheDossier statut = StatutMarcheDossier.BROUILLON;

    /**
     * Champ proformasObtenuesCount : proformas obtenues count utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @Column(name = "proformas_obtenues_count")
    @Builder.Default
    private Integer proformasObtenuesCount = 0;

    /**
     * Champ examenCipmRequis : examen cipm requis utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @Column(name = "examen_cipm_requis", nullable = false)
    @Builder.Default
    private boolean examenCipmRequis = false;

    /**
     * Date et heure de création de l'enregistrement dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    /**
     * Champ lignes : lignes utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<MarcheDossierLigneEntity> lignes = new ArrayList<>();

    /**
     * Champ procesVerbaux : proces verbaux utilisé pour le pilotage et le suivi dans MarcheDossierEntity.
     */
    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MarcheReceptionPvEntity> procesVerbaux = new ArrayList<>();
}
