package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Politique d'enrichissement et d'action attendue de l'employé validateur lors du traitement d'une étape de workflow.
 * <p>
 * Conformément au paramétrage de chaque étape du circuit de validation, ce mode détermine ce que l'employé
 * doit ou peut fournir au moment de soumettre sa décision de validation :
 * <ul>
 *   <li>{@link #RIEN_A_AJOUTER} : Validation simple par visa/signature et commentaire (aucun ajout de fichier ni modification de champ requis ou permis).</li>
 *   <li>{@link #AJOUT_FICHIER_OBLIGATOIRE} : L'employé doit obligatoirement joindre un document justificatif (GED ou URI de pièce jointe) pour valider l'étape.</li>
 *   <li>{@link #MODIFICATION_CHAMPS_OBLIGATOIRE} : L'employé doit obligatoirement renseigner ou modifiéer la valeur d'un ou plusieurs champs autorisés du dossier.</li>
 *   <li>{@link #FICHIER_ET_CHAMPS_OBLIGATOIRES} : L'employé doit obligatoirement joindre un fichier justificatif ET modifiéer au moins un champ autorisé du dossier.</li>
 *   <li>{@link #OPTIONNEL_FICHIER_OU_CHAMPS} : L'employé peut librement ajouter un fichier, modifiéer un ou plusieurs champs autorisés, ou valider sans rien ajouter.</li>
 * </ul>
 * </p>
 */
public enum ModeActionValidationEtape {
    /** Validation simple sans ajout de pièce ni modification de champ. */
    RIEN_A_AJOUTER,
    /** Obligation stricte de joindre un fichier justificatif lors de la validation de l'étape. */
    AJOUT_FICHIER_OBLIGATOIRE,
    /** Obligation stricte de modifiéer ou compléter un ou plusieurs champs du dossier lors de la validation de l'étape. */
    MODIFICATION_CHAMPS_OBLIGATOIRE,
    /** Obligation stricte de joindre un fichier ET de modifiéer un ou plusieurs champs du dossier lors de la validation. */
    FICHIER_ET_CHAMPS_OBLIGATOIRES,
    /** Liberté laissée au validateur d'ajouter un fichier, de modifiéer des champs autorisés, ou de ne rien ajouter. */
    OPTIONNEL_FICHIER_OU_CHAMPS
}
