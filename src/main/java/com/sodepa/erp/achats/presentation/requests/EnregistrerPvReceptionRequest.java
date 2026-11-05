package com.sodepa.erp.achats.presentation.requests;

import com.sodepa.erp.achats.infrastructure.entities.ResultatConformitePv;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerPvReceptionRequest {

    @NotNull(message = "DOSSIER_ID_REQUIRED")
    private UUID dossierId;

    @NotNull(message = "RECEPTION_DATE_REQUIRED")
    @PastOrPresent(message = "RECEPTION_DATE_PAST_OR_PRESENT")
    private LocalDate dateReception;

    @NotNull(message = "RECEPTION_LIEU_REQUIRED")
    @Size(max = 255, message = "MAX_LENGTH_255")
    private String lieuReception;

    @NotNull(message = "RESULTAT_CONFORMITE_REQUIRED")
    private ResultatConformitePv resultatConformite;

    @Size(max = 255, message = "MAX_LENGTH_255")
    private String observations;

    @Size(max = 100, message = "MAX_LENGTH_100")
    private String numeroFactureDefinitive;

    @Positive(message = "AMOUNT_POSITIVE")
    private BigDecimal montantFactureLiquide;

    @NotNull(message = "FICHIER_PV_REQUIRED")
    private MultipartFile fichierPv;
}
