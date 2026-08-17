package com.sodepa.erp.comptabilite.generale.application.inputs;

import com.sodepa.erp.utils.Devise;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Entrée pour la saisie d'une écriture comptable.
 */
@Builder
public record SaisieEcritureInput(
        UUID journalId,
        String numeroPiece,
        String libelle,
        LocalDate dateComptable,
        Devise typeDevise,
        BigDecimal tauxChange,
        List<LigneInput> lignes
) {}
