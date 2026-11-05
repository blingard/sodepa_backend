package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.CreerObjectifInput;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutObjectif;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreerObjectifRequest(
        @NotBlank @Size(max = 150) String intitule,
        String description,
        String indicateur,
        String uniteMesure,
        @NotNull BigDecimal valeurCible,
        BigDecimal valeurRealisee,
        Integer anneeExercice,
        LocalDate dateEcheance,
        StatutObjectif statut,
        String responsableNom
) {
    public CreerObjectifInput toInput(UUID uniteId) {
        return CreerObjectifInput.builder()
                .uniteId(uniteId)
                .intitule(intitule)
                .description(description)
                .indicateur(indicateur)
                .uniteMesure(uniteMesure)
                .valeurCible(valeurCible)
                .valeurRealisee(valeurRealisee != null ? valeurRealisee : BigDecimal.ZERO)
                .anneeExercice(anneeExercice)
                .dateEcheance(dateEcheance)
                .statut(statut != null ? statut : StatutObjectif.NON_DEMARRE)
                .responsableNom(responsableNom)
                .build();
    }
}
