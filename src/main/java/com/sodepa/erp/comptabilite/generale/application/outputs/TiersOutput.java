package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sodepa.erp.utils.TypeTiers;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TiersOutput(
        @NotNull UUID id,
        @NotNull @NotEmpty @NotBlank String code,
        @NotNull @NotEmpty @NotBlank String raisonSociale,
        @NotNull @NotEmpty @NotBlank String adresse,
        @NotNull @NotEmpty @NotBlank String telephone,
        @Email String email,
        @NotNull TypeTiers typeTiers,
        @NotNull Boolean actif,
        @NotNull @NotEmpty @NotBlank String compteCollectifCode,
        boolean enCoursDeModification,
        boolean workflowEnCours,
        UUID workflowInstanceId,
        String workflowStatut,
        String workflowOperationType,
        List<String> piecesJointesWorkflow
) implements Serializable {
    public TiersOutput(
            UUID id,
            String code,
            String raisonSociale,
            String adresse,
            String telephone,
            String email,
            TypeTiers typeTiers,
            Boolean actif,
            String compteCollectifCode
    ) {
        this(id, code, raisonSociale, adresse, telephone, email, typeTiers, actif, compteCollectifCode,
                false, false, null, null, null, null);
    }
}