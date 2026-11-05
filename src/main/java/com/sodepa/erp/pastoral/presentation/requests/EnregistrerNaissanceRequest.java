package com.sodepa.erp.pastoral.presentation.requests;

import com.sodepa.erp.pastoral.infrastructure.entities.RaceBovine;
import com.sodepa.erp.pastoral.infrastructure.entities.SexeBovin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerNaissanceRequest {

    @NotNull(message = "L'identifiant du ranch est obligatoire")
    private UUID ranchId;

    @NotNull(message = "Le sexe du veau est obligatoire")
    private SexeBovin sexe;

    private RaceBovine race;

    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate dateNaissance;

    private BigDecimal poidsNaissanceKg;

    private String mereBoucle;

    private String pereBoucle;

    private String observations;
}
