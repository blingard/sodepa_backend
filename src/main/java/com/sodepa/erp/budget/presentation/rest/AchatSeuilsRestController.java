package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.infrastructure.entities.AchatSeuilConfigEntity;
import com.sodepa.erp.budget.infrastructure.repo.AchatSeuilConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/achats/seuils", "/api/v1/achats/seuils"})
@RequiredArgsConstructor
@Tag(name = "Seuils Marchés & Acquisitions SODEPA", description = "Configuration dynamique des plafonds et seuils de passation des commandes et marchés")
public class AchatSeuilsRestController {

    private final AchatSeuilConfigRepository seuilRepository;

    @GetMapping
    @Operation(summary = "Consulter les seuils de passation d'achats en vigueur (Achat direct, Bon de commande, Lettre commande, Marché CIPM)")
    public ResponseEntity<List<AchatSeuilConfigEntity>> getSeuils() {
        return ResponseEntity.ok(seuilRepository.findByActifTrueOrderByMontantMinTtcAsc());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un seuil de passation (montants min/max, nombre de proformas, commission requise)")
    public ResponseEntity<AchatSeuilConfigEntity> modifierSeuil(
            @PathVariable UUID id,
            @RequestBody ModifierSeuilRequest request) {
        AchatSeuilConfigEntity entity = seuilRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seuil introuvable avec l'ID: " + id));

        if (request.libelle() != null && !request.libelle().isBlank()) entity.setLibelle(request.libelle());
        if (request.montantMinTtc() != null) entity.setMontantMinTtc(request.montantMinTtc());
        entity.setMontantMaxTtc(request.montantMaxTtc());
        if (request.nbProformasRequis() != null) entity.setNbProformasRequis(request.nbProformasRequis());
        if (request.commissionRequise() != null) entity.setCommissionRequise(request.commissionRequise());
        if (request.actif() != null) entity.setActif(request.actif());

        return ResponseEntity.ok(seuilRepository.save(entity));
    }

    public record ModifierSeuilRequest(
            String libelle,
            BigDecimal montantMinTtc,
            BigDecimal montantMaxTtc,
            Integer nbProformasRequis,
            Boolean commissionRequise,
            Boolean actif
    ) {}
}
