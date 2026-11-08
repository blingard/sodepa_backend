package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.TiersPartenaireUnifieService;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.PieceConformiteTiersEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity;
import com.sodepa.erp.utils.TypeTiers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur REST du Référentiel Unifié des Tiers et Partenaires SODEPA (REF-05 / F-REF-021 à F-REF-023).
 * <p>
 * Expose les opérations de gestion des fiches tiers unifiées (Clients, Fournisseurs, Bailleurs, Éleveurs),
 * de suivi des pièces administratives et fiscales obligatoires, et de détection des expirations à J-15
 * avec notifications Email et SMS.
 * </p>
 */
@RestController
@RequestMapping({"/api/tiers-unifies", "/api/v1/tiers-unifies"})
@RequiredArgsConstructor
@Tag(name = "Référentiel Unifié des Tiers & Partenaires MDM (REF-05)",
     description = "Fiches Clients, Fournisseurs et Partenaires unifiées, contrôle NIU/RIB, plafonds de crédit et alertes d'expiration des pièces à J-15")
/**
 * Contrôleur REST exposant les points de terminaison pour `TiersPartenaireUnifieRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
public class TiersPartenaireUnifieRestController {

    private final TiersPartenaireUnifieService tiersUnifieService;

    /**
     * Liste les fiches tiers unifiées selon le type de tiers et le filtre d'activité.
     *
     * @param typeTiers       Catégorie optionnelle de tiers.
     * @param actifsSeulement Indique si seuls les tiers actifs sont retournés (par défaut {@code true}).
     * @return Réponse HTTP 200 contenant la liste des tiers.
     */
    @GetMapping
    @Operation(summary = "Lister les fiches tiers unifiées (Clients, Fournisseurs, Bailleurs, Éleveurs, Boucheries)")
    public ResponseEntity<List<TiersEntity>> listerTiersUnifies(
            @RequestParam(required = false) TypeTiers typeTiers,
            @RequestParam(required = false, defaultValue = "true") boolean actifsSeulement) {
        return ResponseEntity.ok(tiersUnifieService.listerTiersUnifies(typeTiers, actifsSeulement));
    }

    /**
     * Crée une nouvelle fiche Client, Fournisseur ou Partenaire dans le référentiel unifié avec contrôles NIU et RIB.
     *
     * @param request Corps de la requête contenant les informations du tiers.
     * @return Réponse HTTP 201 contenant la fiche crée.
     */
    @PostMapping
    @Operation(summary = "Créer une fiche Client, Fournisseur ou Partenaire avec segment, plafond de crédit et contrôle NIU/RIB (CA-REF-021-01 & CA-REF-022-01)")
    public ResponseEntity<TiersEntity> creerTiersUnifie(@Valid @RequestBody TiersEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiersUnifieService.creerFicheTiersUnifiee(request));
    }

    /**
     * Liste toutes les pièces administratives et fiscales obligatoires rattachées à un tiers.
     *
     * @param tiersId Identifiant UUID du tiers.
     * @return Réponse HTTP 200 contenant la liste des pièces de conformité.
     */
    @GetMapping("/{tiersId}/pieces-conformite")
    @Operation(summary = "Lister les pièces administratives et fiscales obligatoires d'un tiers")
    public ResponseEntity<List<PieceConformiteTiersEntity>> listerPiecesConformite(@PathVariable UUID tiersId) {
        return ResponseEntity.ok(tiersUnifieService.listerPiecesTiers(tiersId));
    }

    /**
     * Enregistre une nouvelle pièce obligatoire avec sa date d'expiration pour un tiers donné.
     *
     * @param tiersId Identifiant UUID du tiers.
     * @param request Données de la pièce de conformité.
     * @return Réponse HTTP 201 contenant la pièce enregistrée.
     */
    @PostMapping("/{tiersId}/pieces-conformite")
    @Operation(summary = "Enregistrer une pièce obligatoire d'un tiers avec sa date d'expiration (CA-REF-023-01)")
    public ResponseEntity<PieceConformiteTiersEntity> enregistréerPieceConformite(
            @PathVariable UUID tiersId,
            @Valid @RequestBody PieceConformiteTiersEntity request) {
        request.setTiersId(tiersId);
        return ResponseEntity.status(HttpStatus.CREATED).body(tiersUnifieService.enregistréerPieceConformite(request));
    }

    /**
     * Détecte les pièces obligatoires de tiers expirées ou arrivant à échéance sous {@code delaiJours} jours (J-15 par défaut)
     * et décleéenche les notifications Email et SMS correspondantes.
     *
     * @param delaiJours Horizon d'alerte en jours (par défaut 15).
     * @return Réponse HTTP 200 contenant la liste des alertes d'expiration.
     */
    @GetMapping("/alertes-expiration-pieces")
    @Operation(summary = "Détecter les pièces obligatoires de tiers expirées ou arrivant à échéance à J-15 (CA-REF-023-01)")
    public ResponseEntity<List<Map<String, Object>>> detecterAlertesExpirationPieces(
            @RequestParam(required = false, defaultValue = "15") int delaiJours) {
        return ResponseEntity.ok(tiersUnifieService.detecterAlertesExpirationPieces(delaiJours));
    }
}
