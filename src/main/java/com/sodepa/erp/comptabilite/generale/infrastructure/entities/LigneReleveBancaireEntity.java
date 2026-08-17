package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Représente une ligne de transaction extraite d'un relevé bancaire.
 * Stocke le montant (positif pour un encaissement, négatif pour un décaissement) et le statut de rapprochement.
 */
@Entity
@Table(name = "lignes_releve_bancaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneReleveBancaireEntity {

    /**
     * Identifiant unique de la ligne de relevé (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Relevé bancaire parent auquel cette ligne est rattachée.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "releve_id", nullable = false)
    @JsonIgnore
    private ReleveBancaireEntity releve;

    /**
     * Date de la transaction bancaire.
     */
    @Column(name = "date_transaction", nullable = false)
    private LocalDate dateTransaction;

    /**
     * Libellé d'opération provenant de la banque.
     */
    @Column(nullable = false, length = 255)
    private String libelle;

    /**
     * Montant de l'opération (valeur positive pour un crédit bancaire, négative pour un débit bancaire).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    /**
     * Indique si cette ligne de relevé a été rapprochée avec une écriture du grand livre.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean rapproche = false;
}
