package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représente un relevé bancaire importé ou saisi manuellement.
 * Permet de stocker les lignes de transactions d'un compte financier pour effectuer le rapprochement bancaire.
 */
@Entity
@Table(name = "releves_bancaires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleveBancaireEntity {

    /**
     * Identifiant unique du relevé bancaire (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Banque émettrice et détentrice du compte de ce relevé.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id")
    private BanqueEntity banque;

    /**
     * Date de clôture ou d'émission du relevé bancaire.
     */
    @Column(name = "date_releve", nullable = false)
    private LocalDate dateReleve;

    /**
     * Solde de départ du compte au début de la période du relevé.
     */
    @Column(name = "solde_initial", nullable = false, precision = 19, scale = 4)
    private BigDecimal soldeInitial;

    /**
     * Solde de clôture du compte à la fin de la période du relevé.
     */
    @Column(name = "solde_final", nullable = false, precision = 19, scale = 4)
    private BigDecimal soldeFinal;

    /**
     * Indique si le relevé est entièrement rapproché et validé.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean valide = false;

    /**
     * Liste des lignes de transactions bancaires contenues dans le relevé.
     */
    @OneToMany(mappedBy = "releve", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneReleveBancaireEntity> lignes = new ArrayList<>();

    /**
     * Ajoute de manière bidirectionnelle une ligne de relevé à ce relevé bancaire.
     * @param ligne la ligne de relevé bancaire à ajouter.
     */
    public void addLigne(LigneReleveBancaireEntity ligne) {
        lignes.add(ligne);
        ligne.setReleve(this);
    }
}
