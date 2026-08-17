package com.sodepa.erp.comptabilite.generale.infrastructure.entities;

import com.sodepa.erp.utils.TypeTiers;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Représente un tiers (compte auxiliaire externe tel qu'un client, un fournisseur, ou le personnel).
 * Cette entité permet de suivre les balances individuelles des tiers associées à un compte collectif.
 */
@Entity
@Table(name = "tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TiersEntity {

    /**
     * Identifiant unique du tiers (UUID).
     */
    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Code unique du tiers (ex: 'CLI0001', 'FOU0042').
     */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String raisonSociale;       // nom / raison sociale

    @Column(length = 255)
    private String adresse;

    @Column(length = 20)
    private String telephone;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_tiers", nullable = false, length = 30)
    private TypeTiers typeTiers;         // CLIENT, FOURNISSEUR, PERSONNEL, ORGANISME

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    /**
     * Code du compte collectif de rattachement du plan comptable (ex: '4111', '4011').
     */
    @Column(name = "compte_collectif_code", nullable = false, length = 20)
    private String compteCollectifCode;
}
