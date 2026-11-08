package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de persistance représentant `PastoralBovinEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "pastoral_bovins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralBovinEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ matriculeBoucle : matricule boucle utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Column(name = "matricule_boucle", nullable = false, unique = true, length = 50)
    private String matriculeBoucle;

    /**
     * Ranch d'élevage ou station pastorale de la SODEPA dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranch_id", nullable = false)
    private PastoralRanchEntity ranch;

    /**
     * Code normalisé unique identifiant categorieCode dans le référentiel SODEPA.
     */
    @Column(name = "categorie_code", nullable = false, length = 40)
    private String categorieCode;

    /**
     * Sexe de l'animal (Mâle, Femelle) dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SexeBovin sexe;

    /**
     * Race animale élevée ou abattue (Goudali, Mbororo, Charolais, etc.) dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RaceBovine race = RaceBovine.GUDALI;

    /**
     * Champ dateNaissance : date naissance utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    /**
     * Champ poidsActuelKg : poids actuel kg utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Column(name = "poids_actuel_kg", precision = 8, scale = 2)
    private BigDecimal poidsActuelKg;

    /**
     * Champ dateDernierePesee : date derniere pesee utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Column(name = "date_derniere_pesee")
    private LocalDate dateDernierePesee;

    /**
     * Champ mereBoucle : mere boucle utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Column(name = "mere_boucle", length = 50)
    private String mereBoucle;

    /**
     * Champ pereBoucle : pere boucle utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Column(name = "pere_boucle", length = 50)
    private String pereBoucle;

    /**
     * Champ etatSante : etat sante utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "etat_sante", nullable = false, length = 50)
    @Builder.Default
    private EtatSanteBovin etatSante = EtatSanteBovin.BON;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutBovin statut = StatutBovin.VIVANT_AU_RANCH;

    /**
     * Champ dateEntree : date entree utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Column(name = "date_entree", nullable = false)
    private LocalDate dateEntree;

    /**
     * Champ dateSortie : date sortie utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Column(name = "date_sortie")
    private LocalDate dateSortie;

    /**
     * Champ valeurComptableActuelle : valeur comptable actuelle utilisé pour le pilotage et le suivi dans PastoralBovinEntity.
     */
    @Column(name = "valeur_comptable_actuelle", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valeurComptableActuelle = BigDecimal.ZERO;
}
