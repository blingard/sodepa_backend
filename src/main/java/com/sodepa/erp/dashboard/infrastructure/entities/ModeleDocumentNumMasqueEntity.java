package com.sodepa.erp.dashboard.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Modèle officiel de document SODEPA et masque de numérotation paramétrable par unité et par exercice
 * (REF-07 / F-REF-029 / CA-REF-029-01 & 02 / CT-REF-06).
 */
@Entity
@Table(name = "ref_modeles_documents_masques")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeleDocumentNumMasqueEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "MDNM";

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
    @Column(name = "code_modele", nullable = false, unique = true, length = 50)
    private String codeModele;

    /**
     * Intitule dans le contexte du traitement ModeleDocumentNumMasqueEntity.
     */
    @Column(nullable = false, length = 180)
    private String intitule;

    /**
     * Type de document : BON_ENTREE, BON_SORTIE, BON_COMMANDE, FACTURE, CERTIFICAT_SANITAIRE, ORDRE_MISSION, DECISION_DG.
     */
    @Column(name = "type_document", nullable = false, length = 50)
    private String typeDocument;

    /**
     * Masque paramétrable (CA-REF-029-02) : ex. BE-{UO}-{AAAA}-{00000}.
     */
    @Column(name = "masque_numerotation", nullable = false, length = 80)
    private String masqueNumerotation;

    /**
     * Entete officielle sodepa dans le contexte du traitement ModeleDocumentNumMasqueEntity.
     */
    @Column(name = "entete_officielle_sodepa", length = 500)
    @Builder.Default
    private String enteteOfficielleSodepa = "RÉPUBLIQUE DU CAMEROUN - Paix-Travail-Patrie - SODEPA (Société de Développement et d'Exploitation des Productions Animales)";

    /**
     * Variables dynamiques dans le contexte du traitement ModeleDocumentNumMasqueEntity.
     */
    @Column(name = "variables_dynamiques", length = 500)
    @Builder.Default
    private String variablesDynamiques = "{NUMERO_DOC},{DATE_JOUR},{UNITE_EMETTRICE},{MONTANT_FCFA},{SIGNATAIRE}";

    /**
     * Compte comptable conforme au plan comptable OHADA révisé.
     */
    @Column(name = "compteur_courant", nullable = false)
    @Builder.Default
    private Long compteurCourant = 0L;

    /**
     * Indicateur booléen précisant si archivage ged automatique est effectif ou validé.
     */
    @Column(name = "archivage_ged_automatique", nullable = false)
    @Builder.Default
    private boolean archivageGedAutomatique = true;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
