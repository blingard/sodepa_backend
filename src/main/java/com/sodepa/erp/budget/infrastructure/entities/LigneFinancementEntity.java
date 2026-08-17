package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant une ligne de financement (prêt bancaire, leasing, emprunt obligataire).
 */
@Entity
@Table(name = "ligne_financement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneFinancementEntity {

    /**
     * Identifiant unique du financement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Banque partenaire ou créancier associé.
     */
    @Column(name = "banque_id", nullable = false)
    private UUID banqueId;

    /**
     * Intitulé descriptif du financement.
     */
    @Column(nullable = false, length = 255)
    private String intitule;

    /**
     * Type de financement : 'PRET', 'LEASING' ou 'OBLIGATION'.
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * Capital total emprunté ou valeur de base.
     */
    @Column(name = "capital_emprunte", nullable = false, precision = 19, scale = 4)
    private BigDecimal capitalEmprunte;

    /**
     * Taux d'intérêt nominal annuel appliqué (ex: 5.50 pour 5.5%).
     */
    @Column(name = "taux_nominal", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxNominal;

    /**
     * Date de mise en place ou d'effet du financement.
     */
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    /**
     * Durée totale du remboursement en mois (ex: 60).
     */
    @Column(name = "duree_mois", nullable = false)
    private Integer dureeMois;

    /**
     * Périodicité de remboursement : 'MENSUELLE', 'TRIMESTRIELLE', 'ANNUELLE'.
     */
    @Column(nullable = false, length = 50)
    private String periodicite;

    /**
     * Statut de la ligne : 'ACTIF', 'TERMINE'.
     */
    @Column(nullable = false, length = 50)
    private String statut;

    /**
     * Tableau d'amortissement (échéancier) associé.
     */
    @OneToMany(mappedBy = "ligneFinancement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<EcheanceFinancementEntity> echeances = new ArrayList<>();

    /**
     * Ajoute une échéance de remboursement au plan de manière bidirectionnelle.
     * 
     * @param echeance l'échéance à rajouter
     */
    public void addEcheance(EcheanceFinancementEntity echeance) {
        if (this.echeances == null) {
            this.echeances = new ArrayList<>();
        }
        this.echeances.add(echeance);
        echeance.setLigneFinancement(this);
    }
}
