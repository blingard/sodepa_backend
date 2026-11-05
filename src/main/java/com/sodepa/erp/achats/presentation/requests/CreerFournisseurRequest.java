package com.sodepa.erp.achats.presentation.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerFournisseurRequest {

    @NotBlank(message = "RAISON_SOCIALE_REQUIRED")
    @Size(max = 100, message = "MAX_LENGTH_100")
    private String raisonSociale;

    @Size(max = 100, min = 9, message = "MAX_CONTRIBUABLE_LENGTH_100")
    private String numeroContribuable;

    @Size(max = 100, min = 9, message = "MAX_RCCM_LENGTH_100")
    private String rccm;

    @Size(max = 100, min = 9, message = "MAX_REGIME_FISCALE_LENGTH_100")
    private String regimeFiscal;

    private String specialite;

    @Size(max = 100, min = 9, message = "MAX_ADDRESS_LENGTH_100")
    private String adresse;

    @Size(max = 25, min = 9, message = "MAX_PHONE_LENGTH_100")
    private String telephone;

    @Size(max = 100, min = 9, message = "MAX_EMAIL_LENGTH_100")
    @Email(message = "EMAIL_INVALID")
    private String email;

    @Size(max = 100, min = 9, message = "MAX_BANK_NAME_LENGTH_100")
    private String banqueNom;

    @Size(max = 100, min = 9, message = "MAX_RIB_LENGTH_100")
    private String compteBancaireRib;
}
