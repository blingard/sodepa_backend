package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.JournalEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import com.sodepa.erp.utils.Devise;
import com.sodepa.erp.utils.StatutEcriture;
import jakarta.persistence.*;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        List<LigneEcritureEntity> lignes
) {
}
