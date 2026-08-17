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
import java.util.*;

/**
 * Service applicatif (Use Case) gérant la comptabilité analytique multi-axes.
 * Permet de définir les dimensions analytiques (axes, sections) et de ventiler
 * les flux financiers (charges/produits) pour l'analyse de gestion.
 */
@Service
@RequiredArgsConstructor
public class AnalytiqueUseCase {

    /**
     * Dépôt de données pour les axes analytiques.
     */
    private final AxeAnalytiqueRepository axeAnalytiqueRepository;

    /**
     * Dépôt de données pour les sections analytiques.
     */
    private final SectionAnalytiqueRepository sectionAnalytiqueRepository;

    /**
     * Dépôt de données pour les lignes d'écritures comptables.
     */
    private final LigneEcritureRepository ligneEcritureRepository;

    /**
     * DTO pour la création ou modification d'un axe analytique.
     */
    @Data
    public static class AxeRequest {
        /**
         * Code unique identifiant l'axe (ex: 'PRJ', 'CC', 'DEP').
         */
        private String code;
        /**
         * Libellé descriptif de l'axe.
         */
        private String intitule;
    }

    /**
     * DTO pour la création ou modification d'une section analytique.
     */
    @Data
    public static class SectionRequest {
        /**
         * Code unique identifiant la section (ex: 'ALPHA', 'DIR_RD').
         */
        private String code;
        /**
         * Libellé descriptif de la section.
         */
        private String intitule;
    }

    /**
     * DTO représentant une ligne de ventilation analytique lors de la saisie.
     */
    @Data
    public static class VentilationRequest {
        /**
         * Identifiant unique de la section analytique destinataire.
         */
        private UUID sectionId;
        /**
         * Pourcentage de répartition affecté à cette section (ex: 40.00).
         */
        private BigDecimal pourcentage;
    }

    /**
     * Crée un nouvel axe analytique s'il n'existe pas déjà.
     * 
     * @param request les données de l'axe à créer
     * @return l'entité AxeAnalytiqueEntity créée
     * @throws IllegalArgumentException si un axe avec le même code existe déjà
     */
    @Transactional
    public AxeAnalytiqueEntity creerAxe(AxeRequest request) {
        if (axeAnalytiqueRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Un axe analytique avec le code " + request.getCode() + " existe déjà.");
        }
        AxeAnalytiqueEntity axe = AxeAnalytiqueEntity.builder()
                .code(request.getCode())
                .intitule(request.getIntitule())
                .actif(true)
                .build();
        return axeAnalytiqueRepository.save(axe);
    }

    /**
     * Récupère la liste de tous les axes analytiques enregistrés.
     * 
     * @return la liste des axes analytiques
     */
    @Transactional(readOnly = true)
    public List<AxeAnalytiqueEntity> listerAxes() {
        return axeAnalytiqueRepository.findAll();
    }

    /**
     * Active ou désactive un axe analytique.
     * 
     * @param id l'identifiant unique de l'axe
     * @param actif le nouvel état d'activation
     * @return l'axe analytique mis à jour
     * @throws IllegalArgumentException si l'axe n'existe pas
     */
    @Transactional
    public AxeAnalytiqueEntity modifierStatutAxe(UUID id, boolean actif) {
        AxeAnalytiqueEntity axe = axeAnalytiqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Axe analytique introuvable avec l'ID: " + id));
        axe.setActif(actif);
        return axeAnalytiqueRepository.save(axe);
    }

    /**
     * Crée une nouvelle section analytique rattachée à un axe parent.
     * 
     * @param axeId l'identifiant de l'axe parent
     * @param request les données de la section à créer
     * @return la section analytique créée
     * @throws IllegalArgumentException si l'axe n'existe pas ou si la section existe déjà pour cet axe
     */
    @Transactional
    public SectionAnalytiqueEntity creerSection(UUID axeId, SectionRequest request) {
        AxeAnalytiqueEntity axe = axeAnalytiqueRepository.findById(axeId)
                .orElseThrow(() -> new IllegalArgumentException("Axe analytique parent introuvable avec l'ID: " + axeId));

        if (sectionAnalytiqueRepository.findByAxeIdAndCode(axeId, request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Une section analytique avec le code " + request.getCode() + " existe déjà pour cet axe.");
        }

        SectionAnalytiqueEntity section = SectionAnalytiqueEntity.builder()
                .axe(axe)
                .code(request.getCode())
                .intitule(request.getIntitule())
                .actif(true)
                .build();
        return sectionAnalytiqueRepository.save(section);
    }

    /**
     * Récupère toutes les sections associées à un axe analytique donné.
     * 
     * @param axeId l'identifiant de l'axe
     * @return la liste des sections rattachées
     */
    @Transactional(readOnly = true)
    public List<SectionAnalytiqueEntity> listerSectionsParAxe(UUID axeId) {
        return sectionAnalytiqueRepository.findByAxeId(axeId);
    }

    /**
     * Active ou désactive une section analytique spécifique.
     * 
     * @param id l'identifiant unique de la section
     * @param actif le nouvel état d'activation
     * @return la section analytique mise à jour
     * @throws IllegalArgumentException si la section n'existe pas
     */
    @Transactional
    public SectionAnalytiqueEntity modifierStatutSection(UUID id, boolean actif) {
        SectionAnalytiqueEntity section = sectionAnalytiqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section analytique introuvable avec l'ID: " + id));
        section.setActif(actif);
        return sectionAnalytiqueRepository.save(section);
    }

    /**
     * Ventile le montant d'une ligne d'écriture comptable sur plusieurs sections analytiques.
     * Le total des pourcentages des sections pour un même axe doit être égal à 100%.
     * 
     * @param ligneId l'identifiant de la ligne d'écriture (charge ou produit)
     * @param ventilations la liste des requêtes de ventilation analytique
     * @return la ligne d'écriture mise à jour avec ses ventilations enregistrées
     * @throws IllegalArgumentException si la ligne n'existe pas, si elle ne concerne pas une charge/un produit (classe 6 ou 7),
     * ou si le total des pourcentages par axe est incorrect
     */
    @Transactional
    public LigneEcritureEntity ventilerLigne(UUID ligneId, List<VentilationRequest> ventilations) {
        LigneEcritureEntity ligne = ligneEcritureRepository.findById(ligneId)
                .orElseThrow(() -> new IllegalArgumentException("Ligne d'écriture introuvable avec l'ID: " + ligneId));

        String compteCode = ligne.getCompteCode();
        if (!compteCode.startsWith("6") && !compteCode.startsWith("7")) {
            throw new IllegalArgumentException("Seuls les comptes de charges (classe 6) ou de produits (classe 7) peuvent faire l'objet d'une ventilation analytique.");
        }

        BigDecimal montantTotalLigne = ligne.getDebit().compareTo(BigDecimal.ZERO) > 0 ? ligne.getDebit() : ligne.getCredit();
        if (montantTotalLigne.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant de la ligne à ventiler doit être strictement supérieur à zéro.");
        }

        // Regrouper par axe analytique pour s'assurer que le total par axe fait exactement 100%
        Map<UUID, BigDecimal> sommePourcentagesParAxe = new HashMap<>();
        List<VentilationAnalytiqueEntity> entitesVentilations = new ArrayList<>();

        for (VentilationRequest vr : ventilations) {
            SectionAnalytiqueEntity sec = sectionAnalytiqueRepository.findById(vr.getSectionId())
                    .orElseThrow(() -> new IllegalArgumentException("Section analytique introuvable avec l'ID: " + vr.getSectionId()));

            if (!sec.getActif() || !sec.getAxe().getActif()) {
                throw new IllegalArgumentException("La section ou l'axe associé est inactif et ne peut plus être imputé.");
            }

            UUID axeId = sec.getAxe().getId();
            BigDecimal pourcentageActuel = sommePourcentagesParAxe.getOrDefault(axeId, BigDecimal.ZERO);
            sommePourcentagesParAxe.put(axeId, pourcentageActuel.add(vr.getPourcentage()));

            BigDecimal montantVentile = montantTotalLigne.multiply(vr.getPourcentage())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            VentilationAnalytiqueEntity vent = VentilationAnalytiqueEntity.builder()
                    .ligneEcriture(ligne)
                    .section(sec)
                    .pourcentage(vr.getPourcentage())
                    .montant(montantVentile)
                    .build();

            entitesVentilations.add(vent);
        }

        // Validation du total des ventilations pour chaque axe impliqué (doit faire exactement 100%)
        for (Map.Entry<UUID, BigDecimal> entry : sommePourcentagesParAxe.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new IllegalArgumentException("Le total des ventilations pour l'axe d'ID " + entry.getKey() + " doit être exactement égal à 100% (actuel: " + entry.getValue() + "%).");
            }
        }

        // Remplacement complet des ventilations
        ligne.getVentilations().clear();
        for (VentilationAnalytiqueEntity v : entitesVentilations) {
            ligne.addVentilation(v);
        }

        return ligneEcritureRepository.save(ligne);
    }
}
