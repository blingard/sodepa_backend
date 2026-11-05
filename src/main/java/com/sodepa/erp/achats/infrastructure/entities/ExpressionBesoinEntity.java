package com.sodepa.erp.achats.infrastructure.entities;

import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "achat_expressions_besoin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpressionBesoinEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id")
    private EmployeEntity demandeur;

    @Column(name = "demandeur_nom", nullable = false, length = 150)
    private String demandeurNom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_organisationnelle_id")
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    @Column(name = "structure_nom", length = 150)
    private String structureNom;

    @Column(nullable = false, length = 255)
    private String motif;

    @Column(name = "date_expression", nullable = false)
    private LocalDate dateExpression;

    @Column(name = "montant_estime_total", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantEstimeTotal = BigDecimal.ZERO;

    @Column(name = "stock_disponible", nullable = false)
    @Builder.Default
    private boolean stockDisponible = false;

    @Column(name = "attestation_carence_etablie", nullable = false)
    @Builder.Default
    private boolean attestationCarenceEtablie = true;

    @Column(name = "budget_disponible", nullable = false)
    @Builder.Default
    private boolean budgetDisponible = true;

    @Column(name = "ligne_budgetaire_imputation", length = 100)
    private String ligneBudgetaireImputation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutExpressionBesoin statut = StatutExpressionBesoin.BROUILLON;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @OneToMany(mappedBy = "expressionBesoin", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<ExpressionBesoinLigneEntity> lignes = new ArrayList<>();
}
