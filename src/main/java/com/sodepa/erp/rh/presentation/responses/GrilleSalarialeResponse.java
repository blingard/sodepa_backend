package com.sodepa.erp.rh.presentation.responses;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Composant applicatif `GrilleSalarialeResponse` du système ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrilleSalarialeResponse {
    private UUID id;
    private Integer categorie;
    private String echelon;
    private Integer groupe;
    private BigDecimal salaireBase;
    private BigDecimal tauxHoraire;
    private Boolean actif;
}
