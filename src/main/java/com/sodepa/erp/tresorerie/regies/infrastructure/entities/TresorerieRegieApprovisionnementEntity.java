package com.sodepa.erp.tresorerie.regies.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de persistance représentant `TresorerieRegieApprovisionnementEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "tresorerie_regie_approvisionnements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TresorerieRegieApprovisionnementEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "TRA";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ numeroDemande : numero demande utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "numero_demande", nullable = false, unique = true, length = 60)
    private String numeroDemande;

    /**
     * Champ regie : regie utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regie_id", nullable = false)
    private TresorerieRegieEntity regie;

    /**
     * Valeur financière (montant demande fcfa) exprimée en Francs CFA (XAF).
     */
    @Column(name = "montant_demande_fcfa", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDemandeFcfa;

    /**
     * Champ soldeAvantApprov : solde avant approv utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "solde_avant_approv", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeAvantApprov = BigDecimal.ZERO;

    /**
     * Champ motifReapprovisionnement : motif reapprovisionnement utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "motif_reapprovisionnement", nullable = false, length = 255)
    private String motifReapprovisionnement;

    /**
     * Champ etatJustificatifsDepenses : etat justificatifs depenses utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "etat_justificatifs_depenses", columnDefinition = "TEXT")
    private String etatJustificatifsDepenses;

    /**
     * Valeur financière (montant justificatifs valides) exprimée en Francs CFA (XAF).
     */
    @Column(name = "montant_justificatifs_valides", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantJustificatifsValides = BigDecimal.ZERO;

    /**
     * Champ dateDemande : date demande utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "date_demande", nullable = false)
    private LocalDate dateDemande;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private StatutApprovisionnementRegie statut = StatutApprovisionnementRegie.EN_ATTENTE_DAF;

    /**
     * Champ signatureDaf : signature daf utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "signature_daf", nullable = false)
    @Builder.Default
    private boolean signatureDaf = false;

    /**
     * Champ dateSignatureDaf : date signature daf utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "date_signature_daf")
    private Instant dateSignatureDaf;

    /**
     * Champ signatureDg : signature dg utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "signature_dg", nullable = false)
    @Builder.Default
    private boolean signatureDg = false;

    /**
     * Champ dateSignatureDg : date signature dg utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "date_signature_dg")
    private Instant dateSignatureDg;

    /**
     * Champ referenceVirementBanque : reference virement banque utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "reference_virement_banque", length = 100)
    private String referenceVirementBanque;

    /**
     * Champ dateEffetDeblocage : date effet deblocage utilisé pour le pilotage et le suivi dans TresorerieRegieApprovisionnementEntity.
     */
    @Column(name = "date_effet_deblocage")
    private LocalDate dateEffetDeblocage;
}
