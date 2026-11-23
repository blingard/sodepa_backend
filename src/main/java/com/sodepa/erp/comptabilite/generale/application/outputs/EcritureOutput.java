package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import com.sodepa.erp.utils.Devise;
import com.sodepa.erp.utils.StatutEcriture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Composant applicatif `EcritureOutput` du système ERP SODEPA.
 */
public record EcritureOutput(
        UUID id,
        JournalOutput journal,
        String numeroPiece,
        String libelle,

        LocalDate dateComptable,
        LocalDateTime dateSaisie,
        boolean valide,
        StatutEcriture statut,
        UserRecordSmartOutput validePar,
        LocalDateTime dateValidation,
        Devise typeDevise,
        BigDecimal tauxChange,
        List<LigneEcritureEntity> lignes,
        boolean enCoursDeModification,
        boolean workflowEnCours,
        UUID workflowInstanceId,
        String workflowStatut,
        String workflowOperationType,
        List<String> piecesJointesWorkflow
) {
    public EcritureOutput(
            UUID id,
            JournalOutput journal,
            String numeroPiece,
            String libelle,
            LocalDate dateComptable,
            LocalDateTime dateSaisie,
            boolean valide,
            StatutEcriture statut,
            UserRecordSmartOutput validePar,
            LocalDateTime dateValidation,
            Devise typeDevise,
            BigDecimal tauxChange,
            List<LigneEcritureEntity> lignes
    ) {
        this(id, journal, numeroPiece, libelle, dateComptable, dateSaisie, valide, statut,
                validePar, dateValidation, typeDevise, tauxChange, lignes,
                false, false, null, null, null, null);
    }
}
