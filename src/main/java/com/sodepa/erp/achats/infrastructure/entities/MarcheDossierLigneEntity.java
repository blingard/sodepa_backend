package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "marche_dossier_lignes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcheDossierLigneEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private MarcheDossierEntity dossier;

    @Column(nullable = false)
    private Integer ordre;

    @Column(nullable = false, length = 255)
    private String designation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantite;

    @Column(name = "unite_mesure", length = 30)
    @Builder.Default
    private String uniteMesure = "U";

    @Column(name = "prix_unitaire_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal prixUnitaireHt;

    @Column(name = "montant_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantHt;

    @Column(name = "taux_tva", precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal tauxTva = new BigDecimal("0.1925");

    @Column(name = "montant_ttc", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantTtc;
}
