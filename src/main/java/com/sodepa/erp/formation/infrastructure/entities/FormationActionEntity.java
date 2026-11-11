package com.sodepa.erp.formation.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité de persistance représentant `FormationActionEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "rh_formation_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationActionEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "FAC";

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
     * Champ plan : plan utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private FormationPlanEntity plan;

    /**
     * Code de référence unique normalisé dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Champ theme : theme utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @Column(nullable = false, length = 255)
    private String theme;

    /**
     * Champ domaine : domaine utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @Column(nullable = false, length = 100)
    private String domaine;

    /**
     * Champ publicCible : public cible utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @Column(name = "public_cible", length = 200)
    private String publicCible;

    /**
     * Champ organismeFormateur : organisme formateur utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @Column(name = "organisme_formateur", length = 150)
    private String organismeFormateur;

    /**
     * Champ typeFormateur : type formateur utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @Column(name = "type_formateur", length = 30)
    @Builder.Default
    private String typeFormateur = "EXTERNE";

    /**
     * Champ lieu : lieu utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @Column(length = 150)
    private String lieu;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_debut")
    private LocalDate dateDebut;

    /**
     * Date de fin d'effet ou échéance dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    /**
     * Champ dureeHeures : duree heures utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @Column(name = "duree_heures")
    private Integer dureeHeures;

    /**
     * Valeur financière (cout estimatif fcfa) exprimée en Francs CFA (XAF).
     */
    @Column(name = "cout_estimatif_fcfa", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal coutEstimatifFcfa = BigDecimal.ZERO;

    /**
     * Champ donneDroitReclassement : donne droit reclassement utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @Column(name = "donne_droit_reclassement", nullable = false)
    @Builder.Default
    private boolean donneDroitReclassement = false;

    /**
     * Champ participations : participations utilisé pour le pilotage et le suivi dans FormationActionEntity.
     */
    @OneToMany(mappedBy = "action", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FormationParticipationEntity> participations = new ArrayList<>();
}
