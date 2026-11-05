package com.sodepa.erp.paie.presentation.responses;

import com.sodepa.erp.paie.infrastructure.entities.SensRubrique;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinLigneResponse {

    private UUID id;
    private Integer ordre;
    private String codeRubrique;
    private String libelle;
    private SensRubrique sens;
    private BigDecimal baseCalcul;
    private BigDecimal taux;
    private BigDecimal gain;
    private BigDecimal retenue;
    private BigDecimal chargePatronale;
}
