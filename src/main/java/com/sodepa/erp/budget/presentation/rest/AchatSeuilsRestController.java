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

/**
 * Service de parametrage et de gouvernance des Seuils Reglementaires de la Commande Publique (Module 02 - BUD / Achats).
 * <p>
 * Ce service definit les regles et contraintes applicables aux engagements selon le montant prevu de la depense :
 * <ul>
 *   <li><b>Achat direct sur caisse (Menue dépense)</b> : Moins de 500 000 FCFA TTC, sans bon de commande formel.</li>
 *   <li><b>Bon de commande d'unité</b> : De 500 000 FCFA à 5 000 000 FCFA, exigeant au minimum 3 devis proforma contradictoires.</li>
 *   <li><b>Lettre-commande / Marché de gré à gré</b> : De 5 000 000 FCFA à 50 000 000 FCFA, validation Direction Générale.</li>
 *   <li><b>Marché public formel (Appel d'offres CIPM)</b> : Au-delà de 50 000 000 FCFA, passage obligatoire en Commission Interne de Passation des Marchés.</li>
 * </ul>
 */
@RestController
@RequestMapping({"/api/achats/seuils", "/api/v1/achats/seuils"})
@RequiredArgsConstructor
@Tag(name = "Seuils Marchés & Acquisitions SODEPA", description = "Configuration dynamique des plafonds et règles de passation des commandes")
public class AchatSeuilsRestController {

    private final AchatSeuilConfigRepository seuilRepository;

    /**
     * Restitue la grille complete des seuils de passation d'achats actuellement en vigueur a la SODEPA.
     * <p>
     * Permet au moteur de regles budgétaires d'adapter automatiquement le circuit de visa et les pieces requises.
     *
     * @return la liste des seuils ordonnee par montant minimal croissant
     */
    @GetMapping
    @Operation(summary = "Consulter les seuils de passation d'achats en vigueur")
    public ResponseEntity<List<AchatSeuilConfigEntity>> getSeuils() {
        return ResponseEntity.ok(seuilRepository.findByActifTrueOrderByMontantMinTtcAsc());
    }

    /**
     * Met a jour les parametres reglementaires d'un seuil de passation (montants limites, nombre de devis, commission).
     * <p>
     * Utilise lors de la revision annuelle des plafonds ou d'une modification des textes reglementaires sur les marches publics.
     *
     * @param id identifiant unique du palier de seuil a modifier
     * @param request nouvelles valeurs (plages de montants, nombre de proformas imposes, commission requise)
     * @return le seuil actualise
     * @throws IllegalArgumentException si l'identifiant du seuil n'existe pas
     */
    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un seuil de passation des achats et commandes")
    public ResponseEntity<AchatSeuilConfigEntity> modifierSeuil(
            @PathVariable UUID id,
            @RequestBody ModifierSeuilRequest request) {
        AchatSeuilConfigEntity entity = seuilRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seuil introuvable avec l'ID: " + id));

        // 1. Mise a jour des montants plancher et plafond
        if (request.libelle() != null && !request.libelle().isBlank()) entity.setLibelle(request.libelle());
        if (request.montantMinTtc() != null) entity.setMontantMinTtc(request.montantMinTtc());
        entity.setMontantMaxTtc(request.montantMaxTtc());

        // 2. Mise a jour des contraintes documentaires et d'approbation
        if (request.nbProformasRequis() != null) entity.setNbProformasRequis(request.nbProformasRequis());
        if (request.commissionRequise() != null) entity.setCommissionRequise(request.commissionRequise());
        if (request.actif() != null) entity.setActif(request.actif());

        // 3. Sauvegarde et application immediate
        return ResponseEntity.ok(seuilRepository.save(entity));
    }

    /**
     * DTO de transmission pour la mise a jour d'un palier de seuil.
     */
    public record ModifierSeuilRequest(
            String libelle,
            BigDecimal montantMinTtc,
            BigDecimal montantMaxTtc,
            Integer nbProformasRequis,
            Boolean commissionRequise,
            Boolean actif
    ) {}
}
