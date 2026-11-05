package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "achat_fournisseurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FournisseurEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "raison_sociale", nullable = false, length = 200)
    private String raisonSociale;

    @Column(name = "numero_contribuable", length = 50)
    private String numeroContribuable;

    @Column(length = 50)
    private String rccm;

    @Column(name = "regime_fiscal", length = 50)
    @Builder.Default
    private String regimeFiscal = "REEL";

    @Column(length = 150)
    private String specialite;

    @Column(length = 255)
    private String adresse;

    @Column(length = 50)
    private String telephone;

    @Column(length = 100)
    private String email;

    @Column(name = "banque_nom", length = 100)
    private String banqueNom;

    @Column(name = "compte_bancaire_rib", length = 50)
    private String compteBancaireRib;

    @Column(name = "agree_sodepa", nullable = false)
    @Builder.Default
    private boolean agreeSodepa = true;

    @Column(name = "date_agrement")
    private LocalDate dateAgrement;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
