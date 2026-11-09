package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Politique d'enrichissement et d'action attendue de l'employé validateur lors du traitement d'une étape de workflow.
 * <p>
 * Conformément au paramétrage de chaque étape du circuit de validation, ce mode détermine ce que l'employé
 * doit ou peut fournir au moment de soumettre sa décision de validation :
 * <ul>
 *   <li>{@link #DO_NOTHING} : Validation simple par visa/signature et commentaire (aucun ajout de fichier ni modification de champ requis ou permis).</li>
 *   <li>{@link #ADD_FILE} : L'employé doit obligatoirement joindre un document justificatif (GED ou URI de pièce jointe) pour valider l'étape.</li>
 *   <li>{@link #SET_ATTRIBUTE} : L'employé doit obligatoirement renseigner ou modifier la valeur d'un ou plusieurs champs autorisés du dossier.</li>
 *   <li>{@link #FILE_AND_ATTRIBUTE} : L'employé doit obligatoirement joindre un fichier justificatif ET modifier au moins un champ autorisé du dossier.</li>
 *   <li>{@link #OPTIONAL_FILE_OR_ATTRIBUTE} : L'employé peut librement ajouter un fichier, modifier un ou plusieurs champs autorisés, ou valider sans rien ajouter.</li>
 * </ul>
 * </p>
 */
public enum ValidationStepActionMode {
    /** Validation simple sans ajout de pièce ni modification de champ. */
    DO_NOTHING,
    /** Obligation stricte de joindre un fichier justificatif lors de la validation de l'étape. */
    ADD_FILE,
    /** Obligation stricte de modifier ou compléter un ou plusieurs champs du dossier lors de la validation de l'étape. */
    SET_ATTRIBUTE,
    /** Obligation conjointe d'ajouter un fichier et de modifier un ou des champs. */
    FILE_AND_ATTRIBUTE,
    /** Optionnel : liberté laissée au validateur de joindre un fichier ou modifier des champs. */
    OPTIONAL_FILE_OR_ATTRIBUTE

}
