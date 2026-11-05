package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "marche_dossiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcheDossierEntity {

    @Id
    private UUID id;

    @Column(name = "numero_dossier", nullable = false, unique = true, length = 60)
    private String numeroDossier;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_procedure", nullable = false, length = 40)
    private TypeProcedureAchat typeProcedure;

    @Column(nullable = false, length = 255)
    private String objet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expression_besoin_id")
    private ExpressionBesoinEntity expressionBesoin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private FournisseurEntity fournisseur;

    @Column(name = "fournisseur_nom", length = 200)
    private String fournisseurNom;

    @Column(name = "montant_ht", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantHt = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantTva = BigDecimal.ZERO;

    @Column(name = "montant_ttc", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantTtc = BigDecimal.ZERO;

    @Column(name = "ligne_budgetaire_code", length = 100)
    private String ligneBudgetaireCode;

    @Column(name = "delai_livraison_jours")
    @Builder.Default
    private Integer delaiLivraisonJours = 90;

    @Column(name = "date_notification")
    private LocalDate dateNotification;

    @Column(name = "date_limite_livraison")
    private LocalDate dateLimiteLivraison;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutMarcheDossier statut = StatutMarcheDossier.BROUILLON;

    @Column(name = "proformas_obtenues_count")
    @Builder.Default
    private Integer proformasObtenuesCount = 0;

    @Column(name = "examen_cipm_requis", nullable = false)
    @Builder.Default
    private boolean examenCipmRequis = false;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<MarcheDossierLigneEntity> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MarcheReceptionPvEntity> procesVerbaux = new ArrayList<>();
}
