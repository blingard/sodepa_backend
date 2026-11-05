package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pastoral_bovins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralBovinEntity {

    @Id
    private UUID id;

    @Column(name = "matricule_boucle", nullable = false, unique = true, length = 50)
    private String matriculeBoucle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranch_id", nullable = false)
    private PastoralRanchEntity ranch;

    @Column(name = "categorie_code", nullable = false, length = 40)
    private String categorieCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SexeBovin sexe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RaceBovine race = RaceBovine.GUDALI;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "poids_actuel_kg", precision = 8, scale = 2)
    private BigDecimal poidsActuelKg;

    @Column(name = "date_derniere_pesee")
    private LocalDate dateDernierePesee;

    @Column(name = "mere_boucle", length = 50)
    private String mereBoucle;

    @Column(name = "pere_boucle", length = 50)
    private String pereBoucle;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_sante", nullable = false, length = 50)
    @Builder.Default
    private EtatSanteBovin etatSante = EtatSanteBovin.BON;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutBovin statut = StatutBovin.VIVANT_AU_RANCH;

    @Column(name = "date_entree", nullable = false)
    private LocalDate dateEntree;

    @Column(name = "date_sortie")
    private LocalDate dateSortie;

    @Column(name = "valeur_comptable_actuelle", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valeurComptableActuelle = BigDecimal.ZERO;
}
