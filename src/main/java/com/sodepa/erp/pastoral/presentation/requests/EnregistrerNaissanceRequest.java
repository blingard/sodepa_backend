package com.sodepa.erp.pastoral.presentation.requests;

import com.sodepa.erp.pastoral.infrastructure.entities.RaceBovine;
import com.sodepa.erp.pastoral.infrastructure.entities.SexeBovin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `EnregistrerNaissanceRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerNaissanceRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément Ranch d'élevage ou station pastorale de la SODEPA associé.
     */
    @NotNull(message = "L'identifiant du ranch est obligatoire")
    private UUID ranchId;

    /**
     * Sexe de l'animal (Mâle, Femelle) dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "Le sexe du veau est obligatoire")
    private SexeBovin sexe;

    /**
     * Race animale élevée ou abattue (Goudali, Mbororo, Charolais, etc.) dans le cadre des opérations SODEPA.
     */
    private RaceBovine race;

    /**
     * Champ dateNaissance : date naissance utilisé pour le pilotage et le suivi dans EnregistrerNaissanceRequest.
     */
    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate dateNaissance;

    /**
     * Champ poidsNaissanceKg : poids naissance kg utilisé pour le pilotage et le suivi dans EnregistrerNaissanceRequest.
     */
    private BigDecimal poidsNaissanceKg;

    /**
     * Champ mereBoucle : mere boucle utilisé pour le pilotage et le suivi dans EnregistrerNaissanceRequest.
     */
    private String mereBoucle;

    /**
     * Champ pereBoucle : pere boucle utilisé pour le pilotage et le suivi dans EnregistrerNaissanceRequest.
     */
    private String pereBoucle;

    /**
     * Champ observations : observations utilisé pour le pilotage et le suivi dans EnregistrerNaissanceRequest.
     */
    private String observations;
}
