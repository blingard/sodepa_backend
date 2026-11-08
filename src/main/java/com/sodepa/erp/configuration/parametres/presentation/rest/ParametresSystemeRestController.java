package com.sodepa.erp.configuration.parametres.presentation.rest;

import com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService;
import com.sodepa.erp.configuration.parametres.infrastructure.entities.ParametreSystemeEntity;
import com.sodepa.erp.configuration.parametres.presentation.requests.UpdateParametreRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST exposant les points de terminaison pour `ParametresSystemeRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
@RestController
@RequestMapping("/api/parametres")
@RequiredArgsConstructor
@Tag(name = "Paramètres Système & Configurabilité Dynamique", description = "Administration à chaud des seuils, barèmes fiscaux, taux CNPS et règles SODEPA sans redémarrage")
public class ParametresSystemeRestController {

    private final ParametreConfigurationService configurationService;

    @GetMapping
    @Operation(summary = "Lister tous les paramètres système")
    public ResponseEntity<List<ParametreSystemeEntity>> listerTous(
            @RequestParam(required = false) String categorie) {
        if (categorie != null && !categorie.isBlank()) {
            return ResponseEntity.ok(configurationService.listerParCategorie(categorie.toUpperCase()));
        }
        return ResponseEntity.ok(configurationService.listerTous());
    }

    @PutMapping("/{cle}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DAF', 'DRH')")
    @Operation(summary = "Mettre à jour un paramètre système à chaud (prise d'effet immédiate)")
    public ResponseEntity<ParametreSystemeEntity> modifierParametre(
            @PathVariable String cle,
            @Valid @RequestBody UpdateParametreRequest request) {
        return ResponseEntity.ok(configurationService.modifierParametre(cle, request.getValeur(), request.getModifiePar()));
    }

    @PostMapping("/cache/purge")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Purger le cache local des paramètres pour forcer la synchronisation BDD")
    public ResponseEntity<Void> purgerCache() {
        configurationService.viderCache();
        return ResponseEntity.noContent().build();
    }
}
