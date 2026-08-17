package com.sodepa.erp.comptabilite.analytique.infrastructure.entities;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité matérialisant la ventilation d'une ligne d'écriture générale sur une section analytique.
 * Permet de répartir le montant d'une charge ou d'un produit (classe 6 ou 7) sur un ou plusieurs axes.
 */
@Entity
@Table(name = "ventilation_analytique")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentilationAnalytiqueEntity {

    /**
     * Identifiant unique de la ventilation analytique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Ligne d'écriture générale d'origine faisant l'objet de cette ventilation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_ecriture_id", nullable = false)
    private LigneEcritureEntity ligneEcriture;

    /**
     * Section analytique de destination affectée par cette ventilation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private SectionAnalytiqueEntity section;

    /**
     * Pourcentage de répartition du montant affecté à cette section (ex: 40.00 pour 40%).
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentage;

    /**
     * Montant calculé affecté à cette section en devise locale (pourcentage * montant ligne).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;
}
