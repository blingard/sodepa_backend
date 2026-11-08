package com.sodepa.erp.tresorerie.regies.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité de persistance représentant `TresorerieRegieEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "tresorerie_regies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TresorerieRegieEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Code de référence unique normalisé dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /**
     * Nom officiel dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String nom;

    /**
     * Champ typeUnite : type unite utilisé pour le pilotage et le suivi dans TresorerieRegieEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_unite", nullable = false, length = 30)
    private TypeUniteRegie typeUnite;

    /**
     * Champ plafondAutoriseFcfa : plafond autorise fcfa utilisé pour le pilotage et le suivi dans TresorerieRegieEntity.
     */
    @Column(name = "plafond_autorise_fcfa", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal plafondAutoriseFcfa = BigDecimal.valueOf(10_000_000);

    /**
     * Champ soldeActuelFcfa : solde actuel fcfa utilisé pour le pilotage et le suivi dans TresorerieRegieEntity.
     */
    @Column(name = "solde_actuel_fcfa", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeActuelFcfa = BigDecimal.ZERO;

    /**
     * Nom ou désignation textuelle de regisseur pour l'affichage et la traçabilité.
     */
    @Column(name = "regisseur_nom", length = 150)
    private String regisseurNom;

    /**
     * Champ regisseurMatricule : regisseur matricule utilisé pour le pilotage et le suivi dans TresorerieRegieEntity.
     */
    @Column(name = "regisseur_matricule", length = 50)
    private String regisseurMatricule;

    /**
     * Champ banqueRattachement : banque rattachement utilisé pour le pilotage et le suivi dans TresorerieRegieEntity.
     */
    @Column(name = "banque_rattachement", length = 100)
    private String banqueRattachement;

    /**
     * Champ ribCompteRegie : rib compte regie utilisé pour le pilotage et le suivi dans TresorerieRegieEntity.
     */
    @Column(name = "rib_compte_regie", length = 50)
    private String ribCompteRegie;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
