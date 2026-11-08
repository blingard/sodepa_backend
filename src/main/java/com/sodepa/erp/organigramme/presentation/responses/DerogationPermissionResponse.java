package com.sodepa.erp.organigramme.presentation.responses;

import com.sodepa.erp.organigramme.infrastructure.entities.StatutDerogation;
import com.sodepa.erp.utils.Permissions;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Composant applicatif `DerogationPermissionResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DerogationPermissionResponse {
    private UUID id;
    private UUID utilisateurId;
    private String username;
    private String nomComplet;
    private Permissions permission;
    private Instant dateDebut;
    private Instant dateFin;
    private StatutDerogation statut;
    private String motif;
    private String referenceDecision;
    private String accordeParNom;
    private Instant dateCreation;
}
