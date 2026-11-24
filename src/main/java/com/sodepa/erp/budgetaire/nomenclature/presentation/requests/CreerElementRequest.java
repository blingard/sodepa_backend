package com.sodepa.erp.budgetaire.nomenclature.presentation.requests;

import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.NiveauProgrammatique;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Corps de {@code POST /api/budget/nomenclature/init_create}.
 *
 * <p>La forme du code et les règles de rattachement sont contrôlées par
 * l'adaptateur, qui rend un code d'erreur stable pour chaque refus.</p>
 *
 * @param niveau        {@code PROGRAMME} ou {@code ACTION}
 * @param code          code du plan de codification, mis en majuscules par le serveur
 * @param libelle       libellé
 * @param description   description facultative
 * @param parentId      programme de rattachement d'une action ; absent pour un programme
 * @param responsableId utilisateur responsable ; obligatoire pour un programme
 */
public record CreerElementRequest(
        @Schema(example = "PROGRAMME")
        @NotNull(message = "Indiquez le niveau de l'élément.")
        NiveauProgrammatique niveau,
        @Schema(example = "P01")
        @NotBlank(message = "Indiquez le code de l'élément.")
        @Size(max = 30, message = "Le code ne doit pas dépasser 30 caractères.")
        String code,
        @Schema(example = "Production animale")
        @NotBlank(message = "Indiquez le libellé de l'élément.")
        @Size(max = 255, message = "Le libellé ne doit pas dépasser 255 caractères.")
        String libelle,
        @Size(max = 1000, message = "La description ne doit pas dépasser 1 000 caractères.")
        String description,
        UUID parentId,
        UUID responsableId
) {
}
