package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.utils.Permissions;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccorderDerogationRequest {

    @NotNull(message = "L'utilisateur bénéficiaire est obligatoire")
    private UUID utilisateurId;

    @NotNull(message = "L'habilitation à déroger est obligatoire")
    private Permissions permission;

    @NotNull(message = "La date de début est obligatoire")
    private Instant dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private Instant dateFin;

    private String motif;
    private String referenceDecision;
}
