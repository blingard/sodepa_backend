package com.sodepa.erp.comptabilite.analytique.application.usecase;

import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.*;
import com.sodepa.erp.comptabilite.analytique.infrastructure.repo.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.LigneEcritureRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service applicatif (Use Case) gérant les clés de répartition analytique préconfigurées.
 * Permet d'automatiser les imputations analytiques complexes lors de la saisie des écritures comptables.
 */
@Service
@RequiredArgsConstructor
public class CleRepartitionUseCase {

    /**
     * Dépôt de données pour les clés de répartition.
     */
    private final CleRepartitionRepository cleRepartitionRepository;

    /**
     * Dépôt de données pour les sections analytiques.
     */
    private final SectionAnalytiqueRepository sectionAnalytiqueRepository;

    /**
     * Dépôt de données pour les lignes d'écritures générales.
     */
    private final LigneEcritureRepository ligneEcritureRepository;

    /**
     * DTO représentant une requête de création ou de mise à jour d'une clé de répartition.
     */
    @Data
    public static class CleRequest {
        /**
         * Code unique identifiant la clé de répartition (ex: 'FRAIS_SIEGE').
         */
        private String code;
        /**
         * Libellé décrivant la clé de répartition (ex: 'Ventilation frais de siège').
         */
        private String intitule;
        /**
         * Liste des quote-parts (sections et pourcentages) associées.
         */
        private List<DetailRequest> details;
    }

    /**
     * DTO représentant une ligne de quote-part dans le DTO global de la clé.
     */
    @Data
    public static class DetailRequest {
        /**
         * Identifiant unique de la section analytique ciblée.
         */
        private UUID sectionId;
        /**
         * Pourcentage à affecter (ex: 25.00).
         */
        private BigDecimal pourcentage;
    }

    /**
     * Crée une nouvelle clé de répartition préconfigurée après avoir vérifié que le total
     * des pourcentages des sections associées fait exactement 100%.
     * 
     * @param request la structure contenant l'en-tête de la clé et ses détails
     * @return la clé de répartition créée et persistée en base
     * @throws IllegalArgumentException si la clé existe déjà ou si le total des pourcentages est différent de 100%
     */
    @Transactional
    public CleRepartitionEntity creerCle(CleRequest request) {
        if (cleRepartitionRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Une clé de répartition avec le code " + request.getCode() + " existe déjà.");
        }

        // Vérification du total des pourcentages (doit faire exactement 100%)
        BigDecimal totalPourcentage = BigDecimal.ZERO;
        for (DetailRequest dr : request.getDetails()) {
            totalPourcentage = totalPourcentage.add(dr.getPourcentage());
        }
        if (totalPourcentage.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("Le total des pourcentages de la clé de répartition doit faire exactement 100% (actuel: " + totalPourcentage + "%).");
        }

        CleRepartitionEntity cle = CleRepartitionEntity.builder()
                .code(request.getCode())
                .intitule(request.getIntitule())
                .actif(true)
                .build();

        List<DetailCleRepartitionEntity> detailsEntites = new ArrayList<>();
        for (DetailRequest dr : request.getDetails()) {
            SectionAnalytiqueEntity sec = sectionAnalytiqueRepository.findById(dr.getSectionId())
                    .orElseThrow(() -> new IllegalArgumentException("Section analytique introuvable avec l'ID: " + dr.getSectionId()));

            DetailCleRepartitionEntity det = DetailCleRepartitionEntity.builder()
                    .cle(cle)
                    .section(sec)
                    .pourcentage(dr.getPourcentage())
                    .build();
            detailsEntites.add(det);
        }

        cle.setDetails(detailsEntites);
        return cleRepartitionRepository.save(cle);
    }

    /**
     * Récupère la liste de toutes les clés de répartition configurées.
     * 
     * @return la liste des clés de répartition
     */
    @Transactional(readOnly = true)
    public List<CleRepartitionEntity> listerCles() {
        return cleRepartitionRepository.findAll();
    }

    /**
     * Applique une clé de répartition préconfigurée sur une ligne d'écriture de charge ou de produit.
     * Calcule automatiquement les montants pour chaque section et met à jour les ventilations.
     * 
     * @param ligneId l'identifiant unique de la ligne d'écriture d'origine
     * @param cleId l'identifiant unique de la clé de répartition à appliquer
     * @return la ligne d'écriture mise à jour avec ses nouvelles ventilations analytiques
     * @throws IllegalArgumentException si la ligne ou la clé n'existe pas, si la clé est inactive,
     * ou si le compte n'est pas une charge/un produit
     */
    @Transactional
    public LigneEcritureEntity appliquerCle(UUID ligneId, UUID cleId) {
        LigneEcritureEntity ligne = ligneEcritureRepository.findById(ligneId)
                .orElseThrow(() -> new IllegalArgumentException("Ligne d'écriture introuvable avec l'ID: " + ligneId));

        CleRepartitionEntity cle = cleRepartitionRepository.findById(cleId)
                .orElseThrow(() -> new IllegalArgumentException("Clé de répartition introuvable avec l'ID: " + cleId));

        if (!cle.getActif()) {
            throw new IllegalArgumentException("La clé de répartition " + cle.getCode() + " est inactive.");
        }

        String compteCode = ligne.getCompteCode();
        if (!compteCode.startsWith("6") && !compteCode.startsWith("7")) {
            throw new IllegalArgumentException("L'affectation analytique par clé n'est autorisée que sur les comptes de charges ou produits.");
        }

        BigDecimal montantLigne = ligne.getDebit().compareTo(BigDecimal.ZERO) > 0 ? ligne.getDebit() : ligne.getCredit();
        if (montantLigne.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant de la ligne doit être supérieur à zéro pour être ventilé.");
        }

        // Effacer les ventilations existantes
        ligne.getVentilations().clear();

        // Appliquer les répartitions de la clé
        for (DetailCleRepartitionEntity det : cle.getDetails()) {
            BigDecimal montantVentile = montantLigne.multiply(det.getPourcentage())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            VentilationAnalytiqueEntity vent = VentilationAnalytiqueEntity.builder()
                    .ligneEcriture(ligne)
                    .section(det.getSection())
                    .pourcentage(det.getPourcentage())
                    .montant(montantVentile)
                    .build();

            ligne.addVentilation(vent);
        }

        return ligneEcritureRepository.save(ligne);
    }
}
