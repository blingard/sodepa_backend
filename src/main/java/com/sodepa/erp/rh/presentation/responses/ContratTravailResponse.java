package com.sodepa.erp.rh.presentation.responses;

import com.sodepa.erp.rh.infrastructure.entities.StatutContrat;
import com.sodepa.erp.rh.infrastructure.entities.TypeContrat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratTravailResponse {
    private UUID id;
    private String numeroContrat;
    private UUID employeId;
    private String employeMatricule;
    private String employeNomComplet;
    private TypeContrat typeContrat;
    private StatutContrat statut;
    private LocalDate dateDebut;
    private LocalDate dateFinEssai;
    private LocalDate dateFinContrat;
    private Integer categorie;
    private String echelon;
    private BigDecimal salaireBase;
    private String referenceNoteServiceDg;
    private LocalDate dateSignatureDg;
    private Boolean signeParDg;
    private UUID workflowInstanceId;
    private UUID makerCheckerTicketId;
    private UUID uniteId;
    private String uniteIntitule;
    private UUID posteId;
    private String posteIntitule;
    private LocalDateTime dateCreation;
}
