package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pastoral_mouvements_cheptel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralMouvementCheptelEntity {

    @Id
    private UUID id;

    @Column(name = "numero_mouvement", nullable = false, unique = true, length = 60)
    private String numeroMouvement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bovin_id", nullable = false)
    private PastoralBovinEntity bovin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranch_source_id", nullable = false)
    private PastoralRanchEntity ranchSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranch_destination_id")
    private PastoralRanchEntity ranchDestination;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false, length = 40)
    private TypeMouvementCheptel typeMouvement;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDate dateMouvement;

    @Column(name = "poids_kg", precision = 8, scale = 2)
    private BigDecimal poidsKg;

    @Column(name = "ancienne_categorie", length = 40)
    private String ancienneCategorie;

    @Column(name = "nouvelle_categorie", length = 40)
    private String nouvelleCategorie;

    @Column(name = "valeur_mouvement", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valeurMouvement = BigDecimal.ZERO;

    @Column(name = "compte_debit_syscohada", length = 30)
    private String compteDebitSyscohada;

    @Column(name = "compte_credit_syscohada", length = 30)
    private String compteCreditSyscohada;

    @Column(name = "justificatif_ref", length = 100)
    private String justificatifRef;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "cree_par", length = 100)
    private String creePar;

    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
