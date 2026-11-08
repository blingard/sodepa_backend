package com.sodepa.erp.fiscalite.application;

import com.sodepa.erp.fiscalite.infrastructure.entities.RegimeFiscalEntity;
import com.sodepa.erp.fiscalite.infrastructure.entities.SeuilGestionEntity;
import com.sodepa.erp.fiscalite.infrastructure.entities.TaxeImpotsEntity;
import com.sodepa.erp.fiscalite.infrastructure.repo.RegimeFiscalRepository;
import com.sodepa.erp.fiscalite.infrastructure.repo.SeuilGestionRepository;
import com.sodepa.erp.fiscalite.infrastructure.repo.TaxeImpotsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service applicatif du Référentiel Fiscal et de la Table Unique des Seuils de Gestion (REF-04 / F-REF-020).
 * <p>
 * Garantit :
 * <ul>
 *   <li>La gestion des taxes et impôts (TVA, AIR, CAC, TSR, taxes vétérinaires) avec {@code dateEffet} et historisation ({@code CA-REF-020-01}, {@code RG-REF-007}).</li>
 *   <li>La table unique transversale des seuils partagée entre Budget, Achats, Trésorerie/Caisses et Comptabilité ({@code CA-REF-020-02}).</li>
 *   <li>L'application stricte aux valeurs bornes et à la date d'effet sans écrasement rétroactif des opérations antérieures ({@code CA-REF-020-03}).</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscaliteService {

    private final TaxeImpotsRepository taxeRepository;
    private final RegimeFiscalRepository regimeRepository;
    private final SeuilGestionRepository seuilRepository;

    // ==========================================
    // TAXES & IMPÔTS (CA-REF-020-01)
    // ==========================================

    /**
     * Liste toutes les taxes, impôts et retenues à la source actifs.
     *
     * @return Liste triée des entités {@link TaxeImpotsEntity}.
     */
    @Transactional(readOnly = true)
    public List<TaxeImpotsEntity> listerTaxesActives() {
        return taxeRepository.findByActifTrueOrderByCodeAscDateEffetDesc();
    }

    /**
     * Consulte l'historique complet des versions d'une taxe ou d'un impôt par son code.
     *
     * @param code Code de la taxe (ex: {@code TVA_STD}, {@code AIR_REEL}).
     * @return Liste chronologique décroissante des versions de la taxe.
     */
    @Transactional(readOnly = true)
    public List<TaxeImpotsEntity> consulterHistoriqueTaxe(String code) {
        return taxeRepository.findByCodeIgnoreCaseOrderByDateEffetDesc(code);
    }

    /**
     * Résout la règle fiscale applicable à une date d'opération précise ({@code CA-REF-020-01}, {@code RG-REF-007}).
     *
     * @param code          Code de la taxe.
     * @param dateOperation Date de l'opération comptable ou commerciale.
     * @return La version {@link TaxeImpotsEntity} en vigueur à cette date.
     * @throws IllegalArgumentException Si aucune version active n'existe pour cette date.
     */
    @Transactional(readOnly = true)
    public TaxeImpotsEntity resoudreTaxeApplicable(String code, LocalDate dateOperation) {
        LocalDate dateRef = dateOperation != null ? dateOperation : LocalDate.now();
        return taxeRepository.findApplicableAtDate(code, dateRef).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune règle fiscale active trouvée pour le code [" + code + "] à la date du " + dateRef));
    }

    /**
     * Crée ou publie une nouvelle version datée d'une taxe/impôt ({@code CA-REF-020-01} / {@code RG-REF-007}).
     * Clôture automatiquement la version précédente à {@code dateEffet.minusDays(1)} sans l'écraser.
     *
     * @param nouvelleTaxe Données de la nouvelle version de taxe.
     * @return L'entité {@link TaxeImpotsEntity} enregistrée.
     */
    @Transactional
    public TaxeImpotsEntity enregistréerOuRevisionnerTaxe(TaxeImpotsEntity nouvelleTaxe) {
        if (nouvelleTaxe.getDateEffet() == null) {
            nouvelleTaxe.setDateEffet(LocalDate.now());
        }
        taxeRepository.findFirstByCodeIgnoreCaseAndActifTrueAndDateFinIsNullOrderByDateEffetDesc(nouvelleTaxe.getCode())
                .ifPresent(ancienne -> {
                    if (nouvelleTaxe.getDateEffet().isAfter(ancienne.getDateEffet())) {
                        ancienne.setDateFin(nouvelleTaxe.getDateEffet().minusDays(1));
                        taxeRepository.save(ancienne);
                        log.info("Historisation fiscale (RG-REF-007) : ancienne version de la taxe {} cleéôturée au {}",
                                ancienne.getCode(), ancienne.getDateFin());
                    }
                });
        nouvelleTaxe.setActif(true);
        return taxeRepository.save(nouvelleTaxe);
    }

    /**
     * Calcule la TVA et la retenue à la source (AIR) applicables à une opération selon la date et le régime fiscal.
     *
     * @param montantHt        Montant hors taxes en FCFA.
     * @param codeRegimeFiscal Code du régime fiscal du tiers (ex: {@code REEL}, {@code SIMPLIFIE}).
     * @param dateOperation    Date de l'opération.
     * @return Dictionnaire détaillant le montant HT, la TVA, le TTC, la retenue AIR et le net à payer.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> simulerCalculFiscal(BigDecimal montantHt, String codeRegimeFiscal, LocalDate dateOperation) {
        LocalDate dateRef = dateOperation != null ? dateOperation : LocalDate.now();
        TaxeImpotsEntity tva = taxeRepository.findApplicableAtDate("TVA_STD", dateRef).stream()
                .findFirst()
                .orElse(TaxeImpotsEntity.builder().code("TVA_STD").tauxPourcentage(BigDecimal.valueOf(19.25)).build());

        RegimeFiscalEntity regime = (codeRegimeFiscal != null && !codeRegimeFiscal.isBlank())
                ? regimeRepository.findByCodeIgnoreCase(codeRegimeFiscal).orElse(null)
                : null;

        BigDecimal tauxTva = (regime == null || regime.isAssujettiTva())
                ? tva.getTauxPourcentage()
                : BigDecimal.ZERO;
        BigDecimal tauxAir = regime != null ? regime.getTauxAirApplicable() : BigDecimal.valueOf(2.20);

        BigDecimal montantTva = montantHt.multiply(tauxTva).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal montantAir = montantHt.multiply(tauxAir).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal montantTtc = montantHt.add(montantTva);
        BigDecimal netAPayerFournisseur = montantHt.subtract(montantAir);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dateOperation", dateRef);
        result.put("montantHtFcfa", montantHt);
        result.put("tauxTvaApplique", tauxTva);
        result.put("montantTvaFcfa", montantTva);
        result.put("montantTtcFcfa", montantTtc);
        result.put("regimeFiscal", regime != null ? regime.getCode() : "REEL");
        result.put("tauxAirApplique", tauxAir);
        result.put("retenueSourceAirFcfa", montantAir);
        result.put("netAPayerTiersFcfa", netAPayerFournisseur);
        return result;
    }

    // ==========================================
    // RÉGIMES FISCAUX
    // ==========================================

    /**
     * Liste tous les régimes fiscaux actifs (Réel, Simplifié, Libératoire, Exonéré).
     *
     * @return Liste des régimes fiscaux {@link RegimeFiscalEntity}.
     */
    @Transactional(readOnly = true)
    public List<RegimeFiscalEntity> listerRegimesFiscaux() {
        return regimeRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Crée ou met à jour un régime fiscal.
     *
     * @param regime Entité du régime fiscal.
     * @return Le régime {@link RegimeFiscalEntity} persisté.
     */
    @Transactional
    public RegimeFiscalEntity creerRegimeFiscal(RegimeFiscalEntity regime) {
        return regimeRepository.save(regime);
    }

    // ==========================================
    // TABLE UNIQUE DES SEUILS DE GESTION (CA-REF-020-02 & CA-REF-020-03)
    // ==========================================

    /**
     * Liste les seuils de gestion actifs, optionnellement filtrés par domaine ({@code ACHATS}, {@code TRESORERIE}, {@code BUDGET}).
     *
     * @param domaine Domaine fonctionnel optionnel.
     * @return Liste des seuils {@link SeuilGestionEntity}.
     */
    @Transactional(readOnly = true)
    public List<SeuilGestionEntity> listerSeuilsActifs(String domaine) {
        if (domaine != null && !domaine.isBlank()) {
            return seuilRepository.findByDomaineIgnoreCaseAndActifTrueOrderByMontantMinFcfaAsc(domaine.trim());
        }
        return seuilRepository.findByActifTrueOrderByDomaineAscMontantMinFcfaAsc();
    }

    /**
     * Consulte l'historique complet des révisions d'un seuil de gestion.
     *
     * @param code Code du seuil de gestion.
     * @return Liste historique des versions du seuil.
     */
    @Transactional(readOnly = true)
    public List<SeuilGestionEntity> consulterHistoriqueSeuil(String code) {
        return seuilRepository.findByCodeIgnoreCaseOrderByDateEffetDesc(code);
    }

    /**
     * Résout un seuil de gestion par son code et sa date d'effet ({@code CA-REF-020-03}).
     *
     * @param code          Code du seuil.
     * @param dateOperation Date de référence.
     * @return Le seuil {@link SeuilGestionEntity} applicable à cette date.
     */
    @Transactional(readOnly = true)
    public SeuilGestionEntity resoudreSeuilParCodeEtDate(String code, LocalDate dateOperation) {
        LocalDate dateRef = dateOperation != null ? dateOperation : LocalDate.now();
        return seuilRepository.findSeuilApplicableParCodeEtDate(code, dateRef).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun seuil de gestion actif trouvé pour [" + code + "] à la date du " + dateRef));
    }

    /**
     * Détermine la procédure et l'autorité d'approbation applicables pour un domaine, un montant et une date ({@code CA-REF-020-02} &amp; {@code 03}).
     *
     * @param domaine       Domaine fonctionnel.
     * @param montantFcfa   Montant de l'opération en FCFA.
     * @param dateOperation Date de l'opération.
     * @return Le seuil {@link SeuilGestionEntity} correspondant à la tranche de montant et à la date.
     */
    @Transactional(readOnly = true)
    public SeuilGestionEntity resoudreSeuilPourDomaineEtMontant(String domaine, BigDecimal montantFcfa, LocalDate dateOperation) {
        LocalDate dateRef = dateOperation != null ? dateOperation : LocalDate.now();
        return seuilRepository.findSeuilPourDomaineMontantEtDate(domaine, montantFcfa, dateRef).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun seuil applicable trouvé pour le domaine [" + domaine + "], montant " + montantFcfa + " FCFA au " + dateRef));
    }

    /**
     * Enregistre un nouveau seuil ou une révision à date d'effet ({@code CA-REF-020-03} / {@code RG-REF-007}).
     * Clôture automatiquement la version précédente à {@code dateEffet.minusDays(1)} afin qu'une opération
     * datée du 31/12/N conserve l'ancien seuil tandis qu'une opération du 01/01/N+1 applique le nouveau.
     *
     * @param nouveauSeuil Données du seuil à enregistréer.
     * @return Le seuil {@link SeuilGestionEntity} persisté.
     */
    @Transactional
    public SeuilGestionEntity enregistréerOuRevisionnerSeuil(SeuilGestionEntity nouveauSeuil) {
        if (nouveauSeuil.getDateEffet() == null) {
            nouveauSeuil.setDateEffet(LocalDate.now());
        }
        seuilRepository.findFirstByCodeIgnoreCaseAndActifTrueAndDateFinIsNullOrderByDateEffetDesc(nouveauSeuil.getCode())
                .ifPresent(ancien -> {
                    if (nouveauSeuil.getDateEffet().isAfter(ancien.getDateEffet())) {
                        ancien.setDateFin(nouveauSeuil.getDateEffet().minusDays(1));
                        seuilRepository.save(ancien);
                        log.info("Historisation seuil de gestion (CA-REF-020-03) : ancien seuil {} cleéôturé au {}",
                                ancien.getCode(), ancien.getDateFin());
                    }
                });
        nouveauSeuil.setActif(true);
        return seuilRepository.save(nouveauSeuil);
    }
}
