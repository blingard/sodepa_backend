package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Représente un établissement bancaire partenaire de l'entreprise.
 * 
 * <p><b>Rôle dans le système :</b></p>
 * Cette entité permet d'identifier de manière unique chaque banque physique
 * où l'entreprise possède un compte courant, et de la rattacher au compte de trésorerie
 * correspondant dans la comptabilité générale (ex : compte 521).
 */
@Entity
@Table(name = "banques")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BanqueEntity {

    /**
     * Identifiant unique de la banque (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Code abrégé unique de la banque (ex: 'BOA', 'SG', 'ECO').
     */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /**
     * Nom officiel ou raison sociale de la banque.
     */
    @Column(nullable = false, length = 255)
    private String nom;

    /**
     * Code du compte comptable associé dans le plan de comptes (ex: '52110000').
     */
    @Column(name = "compte_comptable_code", nullable = false, length = 20)
    private String compteComptableCode;

    /**
     * Logo de la banque (ex: http:www.example.com).
     */
    @Column(name = "logo", nullable = false, length = 250)
    private String logo;

    private boolean status;
}
