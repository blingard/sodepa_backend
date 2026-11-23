package com.sodepa.erp.comptabilite.analytique.presentation.rest;

import com.sodepa.erp.comptabilite.analytique.application.usecase.CleRepartitionUseCase;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.CleRepartitionEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST exposant les API d'administration et d'imputation automatique des clés de répartition analytique.
 */
@RestController
@RequestMapping("/api/comptabilite/analytique/cles")
@RequiredArgsConstructor
public class CleRepartitionRestController {

    /**
     * Cas d'usage pour la gestion des clés de répartition analytique.
     */
    private final CleRepartitionUseCase cleRepartitionUseCase;

    /**
     * Crée une nouvelle clé de répartition analytique préconfigurée.
     * 
     * @param request le DTO contenant le code, l'intitulé et les quote-parts de la clé
     * @return la réponse HTTP avec la clé créée ou un message d'erreur
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DAF', 'DG') or hasAnyAuthority('INIT_CREATE_ECRITURE', 'INIT_UPDATE_ECRITURE', 'CREATE_BUDGET', 'UPDATE_BUDGET', 'CREATE_BUDGET_PLAN')")
    public ResponseEntity<?> creerCle(@Valid @RequestBody CleRepartitionUseCase.CleRequest request) {
        try {
            CleRepartitionEntity cle = cleRepartitionUseCase.creerCle(request);
            return ResponseEntity.ok(cle);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Liste toutes les clés de répartition configurées.
     * 
     * @return la liste des clés de répartition
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DAF', 'DG') or hasAnyAuthority('VIEW_ECRITURE', 'VIEW_BUDGET', 'INIT_CREATE_ECRITURE', 'CREATE_BUDGET')")
    public ResponseEntity<List<CleRepartitionEntity>> listerCles() {
        return ResponseEntity.ok(cleRepartitionUseCase.listerCles());
    }

    /**
     * Applique une clé de répartition sur une ligne d'écriture comptable générale.
     * 
     * @param ligneId l'identifiant unique de la ligne d'écriture
     * @param cleId l'identifiant unique de la clé à appliquer
     * @return la ligne d'écriture ventilée ou un message d'erreur
     */
    @PostMapping("/lignes/{ligneId}/appliquer/{cleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DAF', 'DG') or hasAnyAuthority('INIT_CREATE_ECRITURE', 'INIT_UPDATE_ECRITURE', 'CREATE_BUDGET', 'UPDATE_BUDGET')")
    public ResponseEntity<?> appliquerCle(@PathVariable UUID ligneId, @PathVariable UUID cleId) {
        try {
            LigneEcritureEntity ligne = cleRepartitionUseCase.appliquerCle(ligneId, cleId);
            return ResponseEntity.ok(ligne);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
