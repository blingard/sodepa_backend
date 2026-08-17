package com.sodepa.erp.comptabilite.analytique.presentation.rest;

import com.sodepa.erp.comptabilite.analytique.application.usecase.AnalytiqueUseCase;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.AxeAnalytiqueEntity;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.SectionAnalytiqueEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST exposant les API de configuration et d'imputation de la comptabilité analytique.
 */
@RestController
@RequestMapping("/api/comptabilite/analytique")
@RequiredArgsConstructor
public class AnalytiqueRestController {

    /**
     * Cas d'usage de la comptabilité analytique multi-axes.
     */
    private final AnalytiqueUseCase analytiqueUseCase;

    /**
     * Crée un nouvel axe analytique.
     * 
     * @param request le DTO contenant le code et l'intitulé de l'axe
     * @return la réponse HTTP contenant l'axe analytique créé ou un message d'erreur
     */
    @PostMapping("/axes")
    public ResponseEntity<?> createAxe(@RequestBody AnalytiqueUseCase.AxeRequest request) {
        try {
            AxeAnalytiqueEntity axe = analytiqueUseCase.creerAxe(request);
            return ResponseEntity.ok(axe);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Liste tous les axes analytiques configurés dans le système.
     * 
     * @return la liste des axes analytiques
     */
    @GetMapping("/axes")
    public ResponseEntity<List<AxeAnalytiqueEntity>> listerAxes() {
        return ResponseEntity.ok(analytiqueUseCase.listerAxes());
    }

    /**
     * Active ou désactive un axe analytique.
     * 
     * @param id l'identifiant unique de l'axe
     * @param actif l'état d'activation (true/false)
     * @return la réponse HTTP contenant l'axe mis à jour ou une erreur
     */
    @PutMapping("/axes/{id}/statut")
    public ResponseEntity<?> modifierStatutAxe(@PathVariable UUID id, @RequestParam boolean actif) {
        try {
            AxeAnalytiqueEntity axe = analytiqueUseCase.modifierStatutAxe(id, actif);
            return ResponseEntity.ok(axe);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Crée une nouvelle section analytique rattachée à un axe.
     * 
     * @param axeId l'identifiant unique de l'axe analytique parent
     * @param request le DTO contenant le code et l'intitulé de la section
     * @return la section analytique créée ou une erreur
     */
    @PostMapping("/axes/{axeId}/sections")
    public ResponseEntity<?> creerSection(@PathVariable UUID axeId, @RequestBody AnalytiqueUseCase.SectionRequest request) {
        try {
            SectionAnalytiqueEntity section = analytiqueUseCase.creerSection(axeId, request);
            return ResponseEntity.ok(section);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Liste toutes les sections analytiques rattachées à un axe.
     * 
     * @param axeId l'identifiant unique de l'axe
     * @return la liste des sections rattachées
     */
    @GetMapping("/axes/{axeId}/sections")
    public ResponseEntity<List<SectionAnalytiqueEntity>> listerSectionsParAxe(@PathVariable UUID axeId) {
        return ResponseEntity.ok(analytiqueUseCase.listerSectionsParAxe(axeId));
    }

    /**
     * Active ou désactive une section analytique spécifique.
     * 
     * @param id l'identifiant unique de la section
     * @param actif l'état d'activation (true/false)
     * @return la section analytique mise à jour ou une erreur
     */
    @PutMapping("/sections/{id}/statut")
    public ResponseEntity<?> modifierStatutSection(@PathVariable UUID id, @RequestParam boolean actif) {
        try {
            SectionAnalytiqueEntity section = analytiqueUseCase.modifierStatutSection(id, actif);
            return ResponseEntity.ok(section);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Ventile une charge ou un produit sur différentes sections analytiques.
     * 
     * @param ligneId l'identifiant unique de la ligne d'écriture comptable d'origine
     * @param request la liste des pourcentages de répartition par section
     * @return la ligne d'écriture ventilée ou une erreur
     */
    @PostMapping("/lignes/{ligneId}/ventiler")
    public ResponseEntity<?> ventilerLigne(@PathVariable UUID ligneId, @RequestBody List<AnalytiqueUseCase.VentilationRequest> request) {
        try {
            LigneEcritureEntity ligne = analytiqueUseCase.ventilerLigne(ligneId, request);
            return ResponseEntity.ok(ligne);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
