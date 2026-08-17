package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import com.sodepa.erp.utils.Devise;
import com.sodepa.erp.utils.NatureLigne;
import com.sodepa.erp.utils.StatutEcriture;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représente l'en-tête d'une pièce d'écriture comptable.
 * Regroupe les métadonnées globales de la transaction (date, libellé, journal) et contient la liste des lignes.
 */
@Entity
@Table(name = "ecritures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcritureEntity {

    /**
     * Identifiant unique de la pièce d'écriture (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Journal comptable dans lequel la pièce est enregistrée.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private JournalEntity journal;

    /**
     * Numéro de pièce comptable unique pour l'exercice (ex: référence facture).
     */
    @Column(name = "numero_piece", nullable = false, length = 50)
    private String numeroPiece;

    /**
     * Libellé explicatif global de la pièce comptable.
     */
    @Column(nullable = false, length = 255)
    private String libelle;

    /**
     * Date d'effet comptable de la pièce.
     */
    @Column(name = "date_comptable", nullable = false)
    private LocalDate dateComptable;

    /**
     * Date et heure système de la saisie de l'écriture.
     */
    @CreationTimestamp
    @Column(name = "date_saisie", nullable = false, updatable = false)
    private LocalDateTime dateSaisie;

    /**
     * Indique si l'écriture est définitivement validée (bloquée pour modification).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean valide = false;

    /**
     * Statut de l'écriture au sein du workflow de validation (BROUILLON, SOUMIS, VALIDE, REJETE).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutEcriture statut = StatutEcriture.BROUILLON;

    /**
     * Identifiant de l'utilisateur ayant validé l'écriture.
     */
    @Column(name = "valide_par")
    private UUID validePar;

    /**
     * Date et heure à laquelle l'écriture a été validée.
     */
    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    /**
     * Devise dans laquelle la pièce a été émise (par défaut XAF).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_devise", nullable = false, length = 10)
    @Builder.Default
    private Devise typeDevise = Devise.XAF;

    /**
     * Taux de change appliqué pour la conversion en monnaie nationale.
     */
    @Column(name = "taux_change", nullable = false, precision = 15, scale = 6)
    @Builder.Default
    private BigDecimal tauxChange = BigDecimal.ONE;

    /**
     * Liste des lignes de débit/crédit rattachées à cette pièce comptable.
     */
    @OneToMany(mappedBy = "ecriture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneEcritureEntity> lignes = new ArrayList<>();

    /**
     * Ajoute de manière bidirectionnelle une ligne d'écriture à cette pièce.
     * @param ligne la ligne d'écriture de débit/crédit à ajouter.
     */
    public void addLigne(LigneEcritureEntity ligne) {
        lignes.add(ligne);
        ligne.setEcriture(this);
    }

    /** Validation d’équilibre – appelée avant transition VALIDATED */
    public void validateEquilibre() {
        BigDecimal totalDebit = lignes.stream()
                .map(LigneEcritureEntity::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lignes.stream()
                .map(LigneEcritureEntity::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalStateException(
                    String.format("Écriture non équilibrée – débit=%s, crédit=%s", totalDebit, totalCredit));
        }
    }
}
