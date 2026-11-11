package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Référentiel Sanitaire et Vétérinaire SODEPA (REF-03 / F-REF-015 / CA-REF-015-01) :
 * pathologies, protocoles de traitement, calendriers de prophylaxie/vaccination et délais d'attente avant abattage.
 */
@Entity
@Table(name = "ref_sanitaire_veterinaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferentielSanitaireVeterinaireEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "RSV";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Intitule dans le contexte du traitement ReferentielSanitaireVeterinaireEntity.
     */
    @Column(nullable = false, length = 200)
    private String intitule;

    /**
     * Type : MALADIE_CONTAGIEUSE, PROTOCOLE_TRAITEMENT, CALENDRIER_VACCINATION, ACTE_VETERINAIRE, MOTIF_QUARANTAINE.
     */
    @Column(name = "type_element", nullable = false, length = 50)
    private String typeElement;

    /**
     * Code mnémonique du espèce animale du référentiel zootechnique (Bovins, Porcins, Caprins, etc.).
     */
    @Column(name = "code_espece", nullable = false, length = 30)
    @Builder.Default
    private String codeEspece = "BOVINE";

    /**
     * Vaccin ou produit prophylactique administré au cheptel.
     */
    @Column(name = "agent_pathogene_ou_vaccin", length = 180)
    private String agentPathogeneOuVaccin;

    /**
     * Posologie recommandee dans le contexte du traitement ReferentielSanitaireVeterinaireEntity.
     */
    @Column(name = "posologie_recommandee", length = 250)
    private String posologieRecommandee;

    /**
     * Délai d'attente sanitaire obligatoire (en jours) entre le traitement/vaccin et l'abattage pour consommation humaine.
     */
    @Column(name = "delai_attente_abattage_jours")
    @Builder.Default
    private Integer delaiAttenteAbattageJours = 0;

    /**
     * Periodicite rappel mois dans le contexte du traitement ReferentielSanitaireVeterinaireEntity.
     */
    @Column(name = "periodicite_rappel_mois")
    private Integer periodiciteRappelMois;

    /**
     * Indicateur booléen précisant si decleéaration obligatoire minepia est effectif ou validé.
     */
    @Column(name = "decleéaration_obligatoire_minepia", nullable = false)
    @Builder.Default
    private boolean decleéarationObligatoireMinepia = false;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
