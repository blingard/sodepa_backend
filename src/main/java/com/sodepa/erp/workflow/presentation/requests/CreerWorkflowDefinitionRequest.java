package com.sodepa.erp.workflow.presentation.requests;

import com.sodepa.erp.workflow.infrastructure.entities.ActionDepassementSla;
import com.sodepa.erp.workflow.infrastructure.entities.ModeActionValidationEtape;
import com.sodepa.erp.workflow.infrastructure.entities.PolitiqueValidation;
import com.sodepa.erp.workflow.infrastructure.entities.StrategieAssignation;
import com.sodepa.erp.workflow.infrastructure.entities.TypeEtapeWorkflow;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Requête pour créer une nouvelle définition de workflow avec ses étapes et validateurs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerWorkflowDefinitionRequest {

    /**
     * Code de référence unique normalisé dans le cadre des opérations SODEPA.
     */
    @NotBlank
    private String code;

    /**
     * Intitulé officiel ou désignation dans le cadre des opérations SODEPA.
     */
    @NotBlank
    private String intitule;

    /**
     * Description détaillée et informations contextuelles dans le cadre des opérations SODEPA.
     */
    private String description;

    /**
     * Champ domaine : domaine utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
    @NotNull
    private WorkflowDomaine domaine;

    /**
     * Identifiant unique technique (UUID) de l'élément metier associé.
     */
    private UUID metierId;
    /**
     * Code normalisé unique identifiant metierCode dans le référentiel SODEPA.
     */
    private String metierCode;

    /**
     * Identifiant unique technique (UUID) de l'élément uniteorganisationnelle associé.
     */
    private UUID uniteOrganisationnelleId;

    /**
     * Champ rolesInitiateurs : roles initiateurs utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
    private String rolesInitiateurs;
    /**
     * Champ postesInitiateurs : postes initiateurs utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
    private String postesInitiateurs;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
    @Builder.Default
    private Boolean actif = true;

    /**
     * Champ etapes : etapes utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
    private List<CreerEtapeDefinitionRequest> etapes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreerEtapeDefinitionRequest {
    /**
     * Champ ordre : ordre utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        @NotNull
        private Integer ordre;

    /**
     * Champ codeEtape : code etape utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        @NotBlank
        private String codeEtape;

    /**
     * Intitulé officiel ou désignation dans le cadre des opérations SODEPA.
     */
        @NotBlank
        private String intitule;

    /**
     * Champ typeEtape : type etape utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        @NotNull
        private TypeEtapeWorkflow typeEtape;

    /**
     * Champ politiqueValidation : politique validation utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        @Builder.Default
        private PolitiqueValidation politiqueValidation = PolitiqueValidation.UN_SEUL_SUFFIT;

    /**
     * Champ strategieAssignation : strategie assignation utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        private StrategieAssignation strategieAssignation;
    /**
     * Champ valeurAssignation : valeur assignation utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        private String valeurAssignation;
    /**
     * Champ delaiSlaHeures : delai sla heures utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        private Integer delaiSlaHeures;
        /**
         * Mode d'action exigé pour la validation de cette étape (fichiers/champs/rien/optionnel).
         */
        @Builder.Default
        private ModeActionValidationEtape modeActionValidation = ModeActionValidationEtape.OPTIONNEL_FICHIER_OU_CHAMPS;

        /**
         * Autorisation d'ajouter un document justificatif (GED).
         */
        @Builder.Default
        private Boolean autoriserAjoutFichier = true;

        /**
         * Obligation de joindre un fichier pour valider.
         */
        @Builder.Default
        private Boolean fichierValidationObligatoire = false;

        /**
         * Autorisation de modifier des champs du dossier.
         */
        @Builder.Default
        private Boolean autoriserModificationChamps = true;

        /**
         * Obligation de modifier des champs du dossier pour valider.
         */
        @Builder.Default
        private Boolean modificationChampsObligatoire = false;

        /**
         * Liste des champs modifiables autorisés séparés par des virgules.
         */
        private String champsModifiablesAutorises;

    /**
     * Champ actionDepassementSla : action depassement sla utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        @Builder.Default
        private ActionDepassementSla actionDepassementSla = ActionDepassementSla.RELANCE_EMAIL;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
        @Builder.Default
        private Boolean actif = true;

    /**
     * Champ acteurs : acteurs utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        private List<CreerActeurDefinitionRequest> acteurs;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreerActeurDefinitionRequest {
    /**
     * Champ intituleAction : intitule action utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        @NotBlank
        private String intituleAction;

    /**
     * Identifiant unique de l'employé concerné dans le cadre des opérations SODEPA.
     */
        private UUID employeId;
    /**
     * Identifiant unique technique (UUID) de l'élément utilisateur associé.
     */
        private UUID utilisateurId;
    /**
     * Identifiant unique technique (UUID) de l'élément uniteorganisationnelle associé.
     */
        private UUID uniteOrganisationnelleId;

    /**
     * Champ strategieAssignation : strategie assignation utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        @NotNull
        private StrategieAssignation strategieAssignation;

    /**
     * Champ valeurAssignation : valeur assignation utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        private String valeurAssignation;

    /**
     * Champ obligatoire : obligatoire utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        @Builder.Default
        private Boolean obligatoire = true;

    /**
     * Champ delaiSlaHeures : delai sla heures utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        private Integer delaiSlaHeures;
        /**
         * Mode d'action exigé pour la validation de cette étape (fichiers/champs/rien/optionnel).
         */
        @Builder.Default
        private ModeActionValidationEtape modeActionValidation = ModeActionValidationEtape.OPTIONNEL_FICHIER_OU_CHAMPS;

        /**
         * Autorisation d'ajouter un document justificatif (GED).
         */
        @Builder.Default
        private Boolean autoriserAjoutFichier = true;

        /**
         * Obligation de joindre un fichier pour valider.
         */
        @Builder.Default
        private Boolean fichierValidationObligatoire = false;

        /**
         * Autorisation de modifier des champs du dossier.
         */
        @Builder.Default
        private Boolean autoriserModificationChamps = true;

        /**
         * Obligation de modifier des champs du dossier pour valider.
         */
        @Builder.Default
        private Boolean modificationChampsObligatoire = false;

        /**
         * Liste des champs modifiables autorisés séparés par des virgules.
         */
        private String champsModifiablesAutorises;

    /**
     * Champ actionDepassementSla : action depassement sla utilisé pour le pilotage et le suivi dans CreerWorkflowDefinitionRequest.
     */
        @Builder.Default
        private ActionDepassementSla actionDepassementSla = ActionDepassementSla.RELANCE_EMAIL;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
        @Builder.Default
        private Boolean actif = true;
    }
}
