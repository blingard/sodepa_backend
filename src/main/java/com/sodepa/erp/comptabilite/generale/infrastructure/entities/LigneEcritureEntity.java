package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.VentilationAnalytiqueEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représente une ligne d'une pièce d'écriture comptable.
 * Contient l'imputation par rapport à un compte général, un montant au débit ou au crédit, et éventuellement un tiers.
 */
@Entity
@Table(name = "lignes_ecriture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneEcritureEntity {

    /**
     * Identifiant unique de la ligne d'écriture (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Écriture (pièce comptable) parente à laquelle cette ligne est rattachée.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecriture_id", nullable = false)
    @JsonIgnore
    private EcritureEntity ecriture;

    /**
     * Code du compte général imputé (ex: '6011', '7011', '4111').
     */
    @Column(name = "compte_code", nullable = false, length = 20)
    private String compteCode;

    /**
     * Tiers associé pour le suivi en comptabilité auxiliaire (optionnel, pour les comptes collectifs).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tiers_id")
    private TiersEntity tiers;

    /**
     * Montant débité sur cette ligne (supérieur ou égal à 0).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal debit = BigDecimal.ZERO;

    /**
     * Montant crédité sur cette ligne (supérieur ou égal à 0).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal credit = BigDecimal.ZERO;

    /**
     * Libellé spécifique à cette ligne d'écriture.
     */
    @Column(name = "libelle_ligne", nullable = false, length = 255)
    private String libelleLigne;

    /**
     * Liste des ventilations analytiques associées à cette ligne.
     */
    @OneToMany(mappedBy = "ligneEcriture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<VentilationAnalytiqueEntity> ventilations = new ArrayList<>();

    /**
     * Ajoute une ventilation analytique de manière bidirectionnelle.
     * 
     * @param ventilation la ventilation analytique à associer
     */
    public void addVentilation(VentilationAnalytiqueEntity ventilation) {
        if (this.ventilations == null) {
            this.ventilations = new ArrayList<>();
        }
        this.ventilations.add(ventilation);
        ventilation.setLigneEcriture(this);
    }
}
