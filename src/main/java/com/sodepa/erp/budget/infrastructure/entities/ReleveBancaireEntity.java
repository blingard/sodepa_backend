package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant un relevé bancaire importé pour le rapprochement.
 */
@Entity(name = "BudgetReleveBancaireEntity")
@Table(name = "releve_bancaire")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleveBancaireEntity {

    /**
     * Identifiant unique du relevé.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Identifiant de la banque partenaire émettrice.
     */
    @Column(name = "banque_id", nullable = false)
    private UUID banqueId;

    /**
     * Date et heure de l'import du relevé bancaire dans le système.
     */
    @Column(name = "date_import", nullable = false)
    private LocalDateTime dateImport;

    /**
     * Numéro de référence unique du relevé (ex: extrait mensuel, code MT940).
     */
    @Column(name = "reference_releve", nullable = false, length = 100)
    private String referenceReleve;

    /**
     * Solde initial du compte au début du relevé.
     */
    @Column(name = "solde_debut", nullable = false, precision = 19, scale = 4)
    private BigDecimal soldeDebut;

    /**
     * Solde final théorique du compte après prise en compte des lignes du relevé.
     */
    @Column(name = "solde_fin", nullable = false, precision = 19, scale = 4)
    private BigDecimal soldeFin;

    /**
     * Liste des lignes de transactions incluses dans le relevé.
     */
    @OneToMany(mappedBy = "releveBancaire", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    @ToString.Exclude
    private List<LigneReleveBancaireEntity> lignes = new ArrayList<>();

    /**
     * Helper pour ajouter une ligne de relevé et maintenir la relation bidirectionnelle.
     * 
     * @param ligne la ligne de relevé à ajouter
     */
    public void addLigne(LigneReleveBancaireEntity ligne) {
        lignes.add(ligne);
        ligne.setReleveBancaire(this);
    }
}
