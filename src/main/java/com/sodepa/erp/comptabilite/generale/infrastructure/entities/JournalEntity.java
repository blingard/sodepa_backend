package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import com.sodepa.erp.utils.CodeJournal;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Représente un journal comptable (ex : Achats, Ventes, Banque, Opérations Diverses).
 * Permet de catégoriser les pièces d'écriture comptable saisies.
 */
@Entity
@Table(name = "journaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntity {

    /**
     * Identifiant unique du journal (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Code abrégé unique du journal (ex: 'HA', 'VT', 'BQ', 'OD').
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private CodeJournal code;

    /**
     * Libellé explicite du journal.
     */
    @Column(nullable = false, length = 255)
    private String intitule;

    /**
     * Type de journal (ex: ACHATS, VENTES, BANQUE, OD).
     */
    @Column(name = "type_journal", nullable = false, length = 50)
    private String typeJournal;

    /**
     * Statut du journal indiquant s'il est actif et accepte des écritures.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
