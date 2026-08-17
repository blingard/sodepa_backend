package com.sodepa.erp.comptabilite.generale.presentation.requests;

import com.sodepa.erp.utils.Devise;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Requête pour la saisie d'une écriture comptable.
 */
public record SaisieEcritureRequest(
        @NotNull UUID journalId,
        @NotBlank String numeroPiece,
        @NotBlank String libelle,
        @NotNull LocalDate dateComptable,
        Devise typeDevise,
        BigDecimal tauxChange,
        @NotEmpty List<LigneRequest> lignes
) {}
