package com.sodepa.erp.budget.presentation.rest;

import com.sodepa.erp.budget.application.inputs.CreerFinancementInput;
import com.sodepa.erp.budget.application.inputs.CreerHorsBilanInput;
import com.sodepa.erp.budget.application.inputs.RechercheFinancementInput;
import com.sodepa.erp.budget.application.outputs.EcheanceOutput;
import com.sodepa.erp.budget.application.outputs.EngagementHorsBilanOutput;
import com.sodepa.erp.budget.application.outputs.FinancementOutput;
import com.sodepa.erp.budget.application.outputs.FinancementSmartOutput;
import com.sodepa.erp.budget.application.outputs.KpiReportOutput;
import com.sodepa.erp.budget.application.usecase.CalculerKpisPerformancesUseCase;
import com.sodepa.erp.budget.application.usecase.GetFinancementByIdUseCase;
import com.sodepa.erp.budget.application.usecase.GetPageFinancementsUseCase;
import com.sodepa.erp.budget.application.usecase.EnregistrerEngagementHorsBilanUseCase;
import com.sodepa.erp.budget.application.usecase.EnregistrerFinancementUseCase;
import com.sodepa.erp.budget.application.usecase.EnregistrerReglementEcheanceUseCase;
import com.sodepa.erp.budget.application.usecase.GenererReportingHorsBilanUseCase;
import com.sodepa.erp.budget.application.usecase.SimulerAmortissementUseCase;
import com.sodepa.erp.budget.presentation.requests.CreerFinancementRequest;
import com.sodepa.erp.budget.presentation.requests.CreerHorsBilanRequest;
import com.sodepa.erp.utils.PageRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des Financements Long Terme, Emprunts et Engagements Hors-Bilan (Module 02 - BUD / Finance).
 * <p>
 * Ce service assure la couverture financière des investissements stratégiques de la SODEPA :
 * <ul>
 *   <li>Souscription d'emprunts bancaires et conventions de financement avec les partenaires au développement.</li>
 *   <li>Génération et simulation des tableaux d'amortissement financier (capital, intérêts, échéances périodiques).</li>
 *   <li>Comptabilisation automatique lors de l'encaissement des fonds et du règlement des échéances de la dette.</li>
 *   <li>Suivi et reporting des engagements hors-bilan conformément à la réglementation OHADA (cautions bancaires, garanties, avals).</li>
 *   <li>Indicateurs de structure financière et de solvabilité (ROE, ROA, ratios de liquidité générale).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/financement")
@RequiredArgsConstructor
@Tag(name = "Financements & Dettes Long Terme", description = "Emprunts bancaires, tableaux d'amortissement, engagements hors-bilan OHADA et ratios")
public class FinancementRestController {

    private final EnregistrerFinancementUseCase enregistrerFinancementUseCase;
    private final SimulerAmortissementUseCase simulerAmortissementUseCase;
    private final EnregistrerReglementEcheanceUseCase enregistrerReglementEcheanceUseCase;
    private final EnregistrerEngagementHorsBilanUseCase enregistrerEngagementHorsBilanUseCase;
    private final GenererReportingHorsBilanUseCase genererReportingHorsBilanUseCase;
    private final CalculerKpisPerformancesUseCase calculerKpisPerformancesUseCase;
    private final GetPageFinancementsUseCase getPageFinancementsUseCase;
    private final GetFinancementByIdUseCase getFinancementByIdUseCase;

    /**
     * Recherche et liste les dossiers d'emprunts et de financements souscrits par la SODEPA.
     * <p>
     * Renvoie une synthèse synthétique des contrats actifs avec pagination.
     *
     * @param pageable paramètres de pagination
     * @param banqueId filtre optionnel pour cibler un établissement bancaire prêteur
     * @param type nature du financement (EMPRUNT_BANCAIRE, LIGNE_CREDIT, SUBVENTION_INVESTISSEMENT)
     * @return la liste paginée des financements correspondants
     */
    @GetMapping
    @Operation(summary = "Lister les dossiers d'emprunt et de financement")
    public PageRecord<FinancementSmartOutput> listerFinancements(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) UUID banqueId,
            @RequestParam(required = false) String type
    ) {
        return getPageFinancementsUseCase.execute(new RechercheFinancementInput(pageable, banqueId, type));
    }

    /**
     * Consulte le détail complet d'un dossier de financement, incluant l'ensemble de son échéancier d'amortissement.
     *
     * @param id identifiant unique du financement
     * @return les informations détaillées du prêt et la liste de toutes ses échéances
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consulter un financement et son tableau d'amortissement complet")
    public FinancementOutput getFinancement(@PathVariable UUID id) {
        return getFinancementByIdUseCase.execute(id);
    }

    /**
     * Enregistre un nouveau contrat de prêt ou de financement.
     * <p>
     * Calcule automatiquement le tableau d'amortissement complet et passe l'écriture comptable d'entrée de fonds.
     *
     * @param request caractéristiques de l'emprunt (montant du capital, taux d'intérêt nominal, durée, périodicité)
     * @return le dossier de financement créé et son échéancier validé
     */
    @PostMapping
    @Operation(summary = "Souscrire et enregistrer un nouveau financement")
    public FinancementOutput enregistrerFinancement(@Valid @RequestBody CreerFinancementRequest request) {
        CreerFinancementInput input = new CreerFinancementInput(
                request.banqueId(), request.intitule(), request.type(),
                request.capital(), request.tauxNominal(),
                request.dateEffet(), request.dureeMois(),
                request.periodicite(), request.utilisateurId()
        );
        return enregistrerFinancementUseCase.execute(input);
    }

    /**
     * Simule un plan d'amortissement prévisionnel sans modifier la base de données.
     * <p>
     * Outil d'aide à la négociation permettant à la DAF de comparer différentes offres de prêt
     * (impact du taux, de la maturité et de la périodicité sur les annuités).
     *
     * @param capital montant envisagé du capital emprunté
     * @param tauxNominal taux d'intérêt annuel en pourcentage (ex: 6.5)
     * @param dureeMois durée de remboursement en mois
     * @param periodicite fréquence des remboursements (MENSUELLE, TRIMESTRIELLE, ANNUELLE)
     * @param dateEffet date prévisionnelle de premier déblocage
     * @return la projection de l'échéancier avec ventilation capital / intérêts
     */
    @GetMapping("/simuler")
    @Operation(summary = "Simuler un tableau d'amortissement financier sans engagement")
    public List<EcheanceOutput> simulerAmortissement(
            @RequestParam BigDecimal capital,
            @RequestParam BigDecimal tauxNominal,
            @RequestParam int dureeMois,
            @RequestParam String periodicite,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEffet) {
        CreerFinancementInput input = new CreerFinancementInput(null, null, null, capital, tauxNominal, dateEffet, dureeMois, periodicite, null);
        return simulerAmortissementUseCase.execute(input);
    }

    /**
     * Constate et valide le paiement d'une échéance de remboursement de la dette.
     * <p>
     * Met à jour le capital restant dû et déclenche les écritures comptables d'extinction de dette et de charge d'intérêts.
     *
     * @param echeanceId identifiant unique de l'échéance réglée
     * @param userId identifiant du trésorier ou comptable effectuant la saisie
     */
    @PostMapping("/echeances/{echeanceId}/payer")
    @Operation(summary = "Enregistrer le règlement d'une échéance de prêt")
    public void payerEcheance(@PathVariable UUID echeanceId, @RequestParam UUID userId) {
        enregistrerReglementEcheanceUseCase.execute(new EnregistrerReglementEcheanceUseCase.Input(echeanceId, userId));
    }

    /**
     * Enregistre un engagement hors-bilan (cautionnement bancaire, garantie d'achèvement, aval).
     * <p>
     * Obligation de transparence financière requise par les normes comptables OHADA (article 34-1).
     *
     * @param request caractéristiques de l'engagement (type de garantie, tiers bénéficiaire, montant garanti, date d'échéance)
     * @return l'engagement hors-bilan officialisé
     */
    @PostMapping("/hors-bilan")
    @Operation(summary = "Enregistrer un engagement hors-bilan (caution, garantie, aval)")
    public EngagementHorsBilanOutput enregistrerHorsBilan(@Valid @RequestBody CreerHorsBilanRequest request) {
        CreerHorsBilanInput input = new CreerHorsBilanInput(
                request.type(), request.intitule(), request.tiersId(),
                request.montant(), request.dateEffet(), request.dateEcheance()
        );
        return enregistrerEngagementHorsBilanUseCase.execute(input);
    }

    /**
     * Génère l'état récapitulatif annuel des engagements hors-bilan conformément à l'Acte Uniforme OHADA.
     *
     * @return le rapport d'annexe financière des engagements hors-bilan
     */
    @GetMapping("/reporting/hors-bilan")
    @Operation(summary = "Produire l'état financier des engagements hors-bilan (Annexe OHADA)")
    public List<EngagementHorsBilanOutput> genererReportingHorsBilan() {
        return genererReportingHorsBilanUseCase.execute(null);
    }

    /**
     * Calcule et synthétise les grands ratios financiers de performance de la SODEPA.
     * <p>
     * Fournit le Return on Equity (ROE), le Return on Assets (ROA) et le ratio de liquidité générale.
     *
     * @return l'ensemble des ratios financiers consolidés
     */
    @GetMapping("/reporting/kpis")
    @Operation(summary = "Consulter les ratios financiers de performance (Rentabilité, Liquidité, Solvabilité)")
    public KpiReportOutput genererReportingKpis() {
        return calculerKpisPerformancesUseCase.execute(null);
    }
}
