package com.sodepa.erp.comptabilite.analytique.presentation.rest;

import com.sodepa.erp.comptabilite.analytique.application.usecase.ReportingAnalytiqueUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST exposant les API d'extraction des rapports de comptabilité analytique et de suivi budgétaire.
 */
@RestController
@RequestMapping("/api/comptabilite/analytique/reporting")
@RequiredArgsConstructor
public class ReportingAnalytiqueRestController {

    /**
     * Cas d'usage gérant la génération des rapports analytiques et budgétaires.
     */
    private final ReportingAnalytiqueUseCase reportingAnalytiqueUseCase;

    /**
     * Génère et extrait le Grand Livre Analytique pour une période donnée.
     * 
     * @param debut la date de début de période (format yyyy-MM-dd)
     * @param fin la date de fin de période (format yyyy-MM-dd)
     * @return la structure détaillée du grand livre analytique par section
     */
    @GetMapping("/grand-livre")
    public ResponseEntity<List<ReportingAnalytiqueUseCase.GrandLivreAnalytiqueSection>> genererGrandLivre(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(reportingAnalytiqueUseCase.genererGrandLivreAnalytique(debut, fin));
    }

    /**
     * Génère la Balance Analytique globale du système pour une période donnée.
     * 
     * @param debut la date de début de période (format yyyy-MM-dd)
     * @param fin la date de fin de période (format yyyy-MM-dd)
     * @return la balance analytique récapitulative
     */
    @GetMapping("/balance")
    public ResponseEntity<List<ReportingAnalytiqueUseCase.BalanceAnalytiqueLine>> genererBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(reportingAnalytiqueUseCase.genererBalanceAnalytique(debut, fin));
    }

    /**
     * Extrait le compte de résultat analytique d'une section pour un exercice fiscal donné.
     * 
     * @param sectionId l'identifiant unique de la section analytique
     * @param annee l'exercice comptable (ex: 2026)
     * @return le rapport de rentabilité de la section
     */
    @GetMapping("/sections/{sectionId}/resultat/{annee}")
    public ResponseEntity<?> genererCompteResultat(
            @PathVariable UUID sectionId,
            @PathVariable int annee) {
        try {
            return ResponseEntity.ok(reportingAnalytiqueUseCase.genererCompteResultatAnalytique(sectionId, annee));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Génère l'état comparatif Budgets vs Réels (Suivi budgétaire) d'une section analytique.
     * 
     * @param sectionId l'identifiant unique de la section analytique
     * @param annee l'année d'exercice budgétaire
     * @return le rapport comparatif de suivi budgétaire
     */
    @GetMapping("/sections/{sectionId}/suivi-budgetaire/{annee}")
    public ResponseEntity<?> genererSuiviBudgetaire(
            @PathVariable UUID sectionId,
            @PathVariable int annee) {
        try {
            return ResponseEntity.ok(reportingAnalytiqueUseCase.genererSuiviBudgetaire(sectionId, annee));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
