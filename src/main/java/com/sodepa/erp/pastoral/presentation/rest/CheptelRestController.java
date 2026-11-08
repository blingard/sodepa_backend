package com.sodepa.erp.pastoral.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.pastoral.application.CheptelService;
import com.sodepa.erp.pastoral.infrastructure.entities.StatutBovin;
import com.sodepa.erp.pastoral.presentation.requests.*;
import com.sodepa.erp.pastoral.presentation.responses.*;
import com.sodepa.erp.share.RejectOrValidateSubmitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST exposant les points de terminaison pour `CheptelRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
@RestController
@RequestMapping({"/api/pastoral", "/api/v1/pastoral"})
@RequiredArgsConstructor
@Tag(name = "Cheptel, Ranches & Comptabilité Pastorale SODEPA", description = "Gestion zootechnique des bovins (Dumbo, Jakiri, Faro), mouvements et écritures SYSCOHADA (2191, 2496, 2462, 3880)")
public class CheptelRestController {

    private final CheptelService cheptelService;

    // ==========================================
    // RÉFÉRENTIELS RANCHES & CATÉGORIES
    // ==========================================

    @GetMapping("/ranches")
    @Operation(summary = "Lister les ranches pastoraux de la SODEPA (Dumbo, Jakiri, Faro)")
    public ResponseEntity<List<PastoralRanchResponse>> listerRanches() {
        return ResponseEntity.ok(cheptelService.listerRanches());
    }

    @GetMapping("/categories")
    @Operation(summary = "Lister les catégories zootechniques et comptes SYSCOHADA")
    public ResponseEntity<List<PastoralCategorieCheptelResponse>> listerCategories() {
        return ResponseEntity.ok(cheptelService.listerCategories());
    }

    // ==========================================
    // ZOOTECHNIE DU CHEPTEL
    // ==========================================

    @GetMapping("/bovins")
    @Operation(summary = "Lister les têtes de bétail avec filtres par ranch et statut")
    public ResponseEntity<Page<PastoralBovinResponse>> listerBovins(
            @RequestParam(required = false) UUID ranchId,
            @RequestParam(required = false) StatutBovin statut,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(cheptelService.listerBovins(ranchId, statut, pageable));
    }

    @GetMapping("/bovins/{id}")
    @Operation(summary = "Consulter la fiche zootechnique d'un bovin")
    public ResponseEntity<PastoralBovinResponse> obtenirBovin(@PathVariable UUID id) {
        return ResponseEntity.ok(cheptelService.obtenirBovin(id));
    }

    @PostMapping("/bovins/naissance")
    @Operation(summary = "Enregistrer une naissance au ranch (Débit 2191 / Crédit 7217 SYSCOHADA)")
    public ResponseEntity<PastoralBovinResponse> enregistrerNaissance(@Valid @RequestBody EnregistrerNaissanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cheptelService.enregistrerNaissance(request));
    }

    @PostMapping("/bovins/{id}/pesee")
    @Operation(summary = "Enregistrer une pesée de croissance et contrôle sanitaire")
    public ResponseEntity<PastoralBovinResponse> enregistrerPesee(
            @PathVariable UUID id,
            @Valid @RequestBody EnregistrerPeseeRequest request) {
        request.setBovinId(id);
        return ResponseEntity.ok(cheptelService.enregistrerPesee(request));
    }

    @PostMapping("/bovins/{id}/changement-categorie")
    @Operation(summary = "Reclassement de catégorie zootechnique (Transfer-Out / Transfer-In SYSCOHADA)")
    public ResponseEntity<PastoralBovinResponse> enregistrerChangementCategorie(
            @PathVariable UUID id,
            @Valid @RequestBody EnregistrerChangementCategorieRequest request) {
        request.setBovinId(id);
        return ResponseEntity.ok(cheptelService.enregistrerChangementCategorie(request));
    }

    @PostMapping("/bovins/{id}/transfert-ranch")
    @Operation(summary = "Enregistrer un transfert inter-ranches ou vers abattoir")
    public ResponseEntity<PastoralBovinResponse> enregistrerTransfertRanch(
            @PathVariable UUID id,
            @Valid @RequestBody EnregistrerTransfertRanchRequest request) {
        request.setBovinId(id);
        return ResponseEntity.ok(cheptelService.enregistrerTransfertRanch(request));
    }

    @PostMapping("/bovins/{id}/sortie")
    @Operation(summary = "Enregistrer une sortie de cheptel (Mortalité 8310, Vente/Abattage 7360/6542, Réforme 3880)")
    public ResponseEntity<PastoralBovinResponse> enregistrerSortie(
            @PathVariable UUID id,
            @Valid @RequestBody EnregistrerSortieCheptelRequest request) {
        request.setBovinId(id);
        return ResponseEntity.ok(cheptelService.enregistrerSortie(request));
    }

    @PutMapping("/bovins/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une opération sur bovin (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectBovin(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        cheptelService.validateOrRejectBovin(input);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // HERD COMPOSITION & TABLEAU DE BORD
    // ==========================================

    @GetMapping("/herd-compositions")
    @Operation(summary = "Consulter les fiches d'inventaire Herd Composition de fin d'exercice")
    public ResponseEntity<List<PastoralHerdCompositionResponse>> listerHerdCompositions(
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(cheptelService.listerHerdCompositions(annee));
    }

    @PostMapping("/herd-compositions/generer")
    @Operation(summary = "Générer et clôturer le Herd Composition d'un ranch pour un exercice")
    public ResponseEntity<PastoralHerdCompositionResponse> genererHerdComposition(
            @RequestParam UUID ranchId,
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(cheptelService.genererHerdComposition(ranchId, annee));
    }

    @PutMapping("/herd-compositions/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter un inventaire Herd Composition (Maker-Checker)")
    public ResponseEntity<Void> validateOrRejectHerdComposition(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateSubmitRequest request) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        cheptelService.validateOrRejectHerdComposition(input);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tableau-bord")
    @Operation(summary = "Tableau de bord de synthèse du cheptel pastoral SODEPA")
    public ResponseEntity<PastoralTableauBordResponse> obtenirTableauBord() {
        return ResponseEntity.ok(cheptelService.obtenirTableauBord());
    }
}
