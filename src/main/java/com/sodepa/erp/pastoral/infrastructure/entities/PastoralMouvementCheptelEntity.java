package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de persistance représentant `PastoralMouvementCheptelEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "pastoral_mouvements_cheptel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralMouvementCheptelEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ numeroMouvement : numero mouvement utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "numero_mouvement", nullable = false, unique = true, length = 60)
    private String numeroMouvement;

    /**
     * Champ bovin : bovin utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bovin_id", nullable = false)
    private PastoralBovinEntity bovin;

    /**
     * Champ ranchSource : ranch source utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranch_source_id", nullable = false)
    private PastoralRanchEntity ranchSource;

    /**
     * Champ ranchDestination : ranch destination utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranch_destination_id")
    private PastoralRanchEntity ranchDestination;

    /**
     * Champ typeMouvement : type mouvement utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false, length = 40)
    private TypeMouvementCheptel typeMouvement;

    /**
     * Champ dateMouvement : date mouvement utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "date_mouvement", nullable = false)
    private LocalDate dateMouvement;

    /**
     * Champ poidsKg : poids kg utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "poids_kg", precision = 8, scale = 2)
    private BigDecimal poidsKg;

    /**
     * Champ ancienneCategorie : ancienne categorie utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "ancienne_categorie", length = 40)
    private String ancienneCategorie;

    /**
     * Champ nouvelleCategorie : nouvelle categorie utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "nouvelle_categorie", length = 40)
    private String nouvelleCategorie;

    /**
     * Champ valeurMouvement : valeur mouvement utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "valeur_mouvement", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valeurMouvement = BigDecimal.ZERO;

    /**
     * Champ compteDebitSyscohada : compte debit syscohada utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "compte_debit_syscohada", length = 30)
    private String compteDebitSyscohada;

    /**
     * Champ compteCreditSyscohada : compte credit syscohada utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "compte_credit_syscohada", length = 30)
    private String compteCreditSyscohada;

    /**
     * Champ justificatifRef : justificatif ref utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "justificatif_ref", length = 100)
    private String justificatifRef;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(columnDefinition = "TEXT")
    private String observations;

    /**
     * Champ creePar : cree par utilisé pour le pilotage et le suivi dans PastoralMouvementCheptelEntity.
     */
    @Column(name = "cree_par", length = 100)
    private String creePar;

    /**
     * Date et heure de création de l'enregistrement dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
