package com.sodepa.erp.achats.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "marche_receptions_pv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcheReceptionPvEntity {

    @Id
    private UUID id;

    @Column(name = "numero_pv", nullable = false, unique = true, length = 60)
    private String numeroPv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private MarcheDossierEntity dossier;

    @Column(name = "date_reception", nullable = false)
    private LocalDate dateReception;

    @Column(name = "lieu_reception", length = 150)
    private String lieuReception;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat_conformite", nullable = false, length = 40)
    @Builder.Default
    private ResultatConformitePv resultatConformite = ResultatConformitePv.CONFORME_SANS_RESERVE;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "numero_facture_definitive", length = 100)
    private String numeroFactureDefinitive;

    @Column(name = "montant_facture_liquide", precision = 18, scale = 2)
    private BigDecimal montantFactureLiquide;

    @Column(name = "fichier_url", length = 500)
    private String fichierUrl;

    @Column(name = "liquidation_validee", nullable = false)
    @Builder.Default
    private boolean liquidationValidee = false;

    @Column(name = "date_liquidation")
    private Instant dateLiquidation;
}
