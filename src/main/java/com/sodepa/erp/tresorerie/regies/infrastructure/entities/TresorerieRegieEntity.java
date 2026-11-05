package com.sodepa.erp.tresorerie.regies.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tresorerie_regies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TresorerieRegieEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_unite", nullable = false, length = 30)
    private TypeUniteRegie typeUnite;

    @Column(name = "plafond_autorise_fcfa", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal plafondAutoriseFcfa = BigDecimal.valueOf(10_000_000);

    @Column(name = "solde_actuel_fcfa", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeActuelFcfa = BigDecimal.ZERO;

    @Column(name = "regisseur_nom", length = 150)
    private String regisseurNom;

    @Column(name = "regisseur_matricule", length = 50)
    private String regisseurMatricule;

    @Column(name = "banque_rattachement", length = 100)
    private String banqueRattachement;

    @Column(name = "rib_compte_regie", length = 50)
    private String ribCompteRegie;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
