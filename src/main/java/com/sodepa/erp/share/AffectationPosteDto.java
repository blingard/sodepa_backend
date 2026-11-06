package com.sodepa.erp.share;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutAffectation;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeAffectation;
import com.sodepa.erp.utils.UserOutput;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for {@link com.sodepa.erp.organigramme.infrastructure.entities.AffectationPosteEntity}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AffectationPosteDto(UUID id, UserOutput utilisateur, PosteDto poste, TypeAffectation typeAffectation,
                                  LocalDate dateDebut, LocalDate dateFin, String referenceDecision, String motifFin,
                                  StatutAffectation statut) implements Serializable {
}