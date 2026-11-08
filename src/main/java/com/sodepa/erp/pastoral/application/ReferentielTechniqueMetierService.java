package com.sodepa.erp.pastoral.application;

import com.sodepa.erp.pastoral.infrastructure.entities.*;
import com.sodepa.erp.pastoral.infrastructure.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service applicatif de gestion des Référentiels Métiers, Techniques &amp; Géographiques
 * ({@code REF-03} &amp; {@code F-REF-012} à {@code F-REF-016}).
 * <p>
 * Couvre :
 * <ul>
 *   <li><b>{@code F-REF-012}</b> : Référentiel géographique (Régions, Départements, Arrondissements) et sites
 *       d'exploitation (Ranchs, Abattoirs, Frigorifiques, Points de vente) avec validation centrale obligatoire
 *       des propositions locales ({@code RG-REF-028}).</li>
 *   <li><b>{@code F-REF-013}</b> : Référentiel zootechnique (Espèces animales, Races locales et exotiques, catégories d'âge/sexe).</li>
 *   <li><b>{@code F-REF-014}</b> : Catalogue des produits et sous-produits animaux avec détection de doublon insensible à la casse ({@code RG-REF-002}).</li>
 *   <li><b>{@code F-REF-015}</b> : Référentiel sanitaire/vétérinaire (pathologies, vaccins, délais d'attente avant abattage)
 *       et référentiel d'abattage/transformation (types de découpe, grilles de cleéassement, rendements cibles).</li>
 *   <li><b>{@code F-REF-016}</b> : Unités de mesure et règles de conversion avec contrôle de cohérence dimensionnelle.</li>
 * </ul>
 * </p>
 */
@Service
public class ReferentielTechniqueMetierService {

    private static final Set<String> DIMENSIONS_CONDITIONNABLES = Set.of(
            "MASSE_POIDS", "VOLUME", "EFFECTIF_UNITE", "CONDITIONNEMENT"
    );

    private final LocaliteGeographiqueRepository localiteRepository;
    private final SiteExploitationRepository siteRepository;
    private final EspeceAnimaleRepository especeRepository;
    private final RaceAnimaleRepository raceRepository;
    private final CatalogueProduitAnimalRepository produitRepository;
    private final ReferentielSanitaireVeterinaireRepository sanitaireRepository;
    private final ReferentielAbattageTransformationRepository abattageRepository;
    private final UniteMesureRepository uniteMesureRepository;
    private final RegleConversionUniteRepository regleConversionRepository;

    /**
     * Constructeur avec injection des 9 dépôts du référentiel technique et géographique.
     *
     * @param localiteRepository        Dépôt des localités géographiques.
     * @param siteRepository            Dépôt des sites d'exploitation.
     * @param especeRepository          Dépôt des espèces animales.
     * @param raceRepository            Dépôt des races animales.
     * @param produitRepository         Dépôt du catalogue des produits et sous-produits animaux.
     * @param sanitaireRepository       Dépôt du référentiel sanitaire et vétérinaire.
     * @param abattageRepository        Dépôt des normes d'abattage et de transformation.
     * @param uniteMesureRepository     Dépôt des unités de mesure.
     * @param regleConversionRepository Dépôt des règles de conversion d'unités.
     */
    public ReferentielTechniqueMetierService(
            LocaliteGeographiqueRepository localiteRepository,
            SiteExploitationRepository siteRepository,
            EspeceAnimaleRepository especeRepository,
            RaceAnimaleRepository raceRepository,
            CatalogueProduitAnimalRepository produitRepository,
            ReferentielSanitaireVeterinaireRepository sanitaireRepository,
            ReferentielAbattageTransformationRepository abattageRepository,
            UniteMesureRepository uniteMesureRepository,
            RegleConversionUniteRepository regleConversionRepository) {
        this.localiteRepository = localiteRepository;
        this.siteRepository = siteRepository;
        this.especeRepository = especeRepository;
        this.raceRepository = raceRepository;
        this.produitRepository = produitRepository;
        this.sanitaireRepository = sanitaireRepository;
        this.abattageRepository = abattageRepository;
        this.uniteMesureRepository = uniteMesureRepository;
        this.regleConversionRepository = regleConversionRepository;
    }

    // =========================================================================
    // F-REF-012 : Référentiel géographique et sites d'exploitation (RG-REF-028)
    // =========================================================================

    /**
     * Liste toutes les localités géographiques actives du référentiel territorial camerounais.
     *
     * @return Liste triée par code des entités {@link LocaliteGeographiqueEntity}.
     */
    public List<LocaliteGeographiqueEntity> listerLocalites() {
        return localiteRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Crée une localité géographique (Pays, Région, Département, Arrondissement, Ville) si son code n'existe pas déjà.
     *
     * @param localite Entité de la localité à créer.
     * @return La localité {@link LocaliteGeographiqueEntity} enregistrée.
     * @throws IllegalArgumentException Si le code est vide.
     */
    @Transactional
    public LocaliteGeographiqueEntity creerLocalite(LocaliteGeographiqueEntity localite) {
        if (localite.getCode() == null || localite.getCode().isBlank()) {
            throw new IllegalArgumentException("Le code de la localité géographique est obligatoire.");
        }
        return localiteRepository.findByCodeIgnoreCase(localite.getCode().trim())
                .orElseGet(() -> localiteRepository.save(localite));
    }

    /**
     * Liste tous les sites d'exploitation actifs et validés centralement.
     *
     * @return Liste des sites actifs {@link SiteExploitationEntity}.
     */
    public List<SiteExploitationEntity> listerSitesActifs() {
        return siteRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Liste les sites d'exploitation selon leur statut de validation ({@code PROPOSE_LOCALEMENT} ou {@code VALIDE_CENTRAL}).
     *
     * @param statutValidation Filtre sur le statut de validation.
     * @return Liste des sites correspondants.
     */
    public List<SiteExploitationEntity> listerSitesParStatutValidation(String statutValidation) {
        if (statutValidation == null || statutValidation.isBlank()) {
            return siteRepository.findAll();
        }
        return siteRepository.findByStatutValidationIgnoreCaseOrderByCodeAsc(statutValidation.trim());
    }

    /**
     * Crée ou propose un site d'exploitation / point de vente ({@code CA-REF-012-01} &amp; {@code CA-REF-012-02}, {@code RG-REF-028}).
     * Si le site est proposé par une unité opérationnelle locale ({@code proposeParUniteLocale = true}),
     * il est placé en statut {@code PROPOSE_LOCALEMENT} et reste inactif jusqu'à validation centrale.
     *
     * @param site Données du site d'exploitation.
     * @return Le site {@link SiteExploitationEntity} enregistré.
     * @throws IllegalArgumentException Si le code ou la localité de rattachement est manquant.
     */
    @Transactional
    public SiteExploitationEntity proposerOuCreerSite(SiteExploitationEntity site) {
        if (site.getCode() == null || site.getCode().isBlank()) {
            throw new IllegalArgumentException("Le code du site d'exploitation est obligatoire.");
        }
        if (site.getCodeLocalite() == null || site.getCodeLocalite().isBlank()) {
            throw new IllegalArgumentException("Le rattachement géographique (codeLocalite) du site est obligatoire (CA-REF-012-01).");
        }
        if (site.isProposeParUniteLocale()) {
            site.setStatutValidation("PROPOSE_LOCALEMENT");
            site.setActif(false);
        } else {
            site.setStatutValidation("VALIDE_CENTRAL");
            site.setActif(true);
        }
        return siteRepository.save(site);
    }

    /**
     * Valide centralement un site d'exploitation proposé par une unité locale et l'active ({@code CA-REF-012-02}, {@code RG-REF-028}).
     *
     * @param siteId Identifiant UUID du site à valider.
     * @return Le site {@link SiteExploitationEntity} validé et activé.
     */
    @Transactional
    public SiteExploitationEntity validerSiteCentral(UUID siteId) {
        SiteExploitationEntity site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site d'exploitation introuvable : " + siteId));
        site.setStatutValidation("VALIDE_CENTRAL");
        site.setActif(true);
        return siteRepository.save(site);
    }

    // =========================================================================
    // F-REF-013 : Référentiel zootechnique (Espèces & Races animales)
    // =========================================================================

    /**
     * Liste toutes les espèces animales actives du référentiel zootechnique.
     *
     * @return Liste triée par code des espèces {@link EspeceAnimaleEntity}.
     */
    public List<EspeceAnimaleEntity> listerEspeces() {
        return especeRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Crée une espèce animale dans le référentiel zootechnique.
     *
     * @param espece Données de l'espèce animale.
     * @return L'espèce {@link EspeceAnimaleEntity} enregistrée.
     */
    @Transactional
    public EspeceAnimaleEntity creerEspece(EspeceAnimaleEntity espece) {
        if (espece.getCode() == null || espece.getCode().isBlank()) {
            throw new IllegalArgumentException("Le code de l'espèce animale est obligatoire.");
        }
        return especeRepository.findByCodeIgnoreCase(espece.getCode().trim())
                .orElseGet(() -> especeRepository.save(espece));
    }

    /**
     * Liste les races animales actives, optionnellement filtrées par code d'espèce.
     *
     * @param codeEspece Code optionnel de l'espèce animale (ex: {@code BOVIN}).
     * @return Liste des races {@link RaceAnimaleEntity}.
     */
    public List<RaceAnimaleEntity> listerRaces(String codeEspece) {
        if (codeEspece != null && !codeEspece.isBlank()) {
            return raceRepository.findByCodeEspeceIgnoreCaseAndActifTrueOrderByCodeAsc(codeEspece.trim());
        }
        return raceRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Crée une race animale rattachée obligatoirement à une espèce validée ({@code CA-REF-013-01} &amp; {@code CA-REF-013-02}).
     *
     * @param race Données de la race animale.
     * @return La race {@link RaceAnimaleEntity} sauvegardée.
     * @throws IllegalArgumentException Si l'espèce de rattachement est absente ou inconnue.
     */
    @Transactional
    public RaceAnimaleEntity creerRace(RaceAnimaleEntity race) {
        if (race.getCodeEspece() == null || race.getCodeEspece().isBlank()
                || !especeRepository.existsByCodeIgnoreCase(race.getCodeEspece().trim())) {
            throw new IllegalArgumentException(
                    "Création refusée (CA-REF-013-02) : toute race animale doit obligatoirement être rattachée à une espèce validée du référentiel zootechnique.");
        }
        race.setCodeEspece(race.getCodeEspece().trim().toUpperCase());
        return raceRepository.save(race);
    }

    // =========================================================================
    // F-REF-014 : Catalogue des produits et sous-produits animaux (RG-REF-002)
    // =========================================================================

    /**
     * Liste tous les articleées actifs du catalogue des produits et sous-produits animaux.
     *
     * @return Liste des produits {@link CatalogueProduitAnimalEntity}.
     */
    public List<CatalogueProduitAnimalEntity> listerProduits() {
        return produitRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Crée un produit ou sous-produit animal avec détection de doublon insensible à la casse ({@code CA-REF-014-02}, {@code RG-REF-002}).
     *
     * @param produit Données du produit animal.
     * @return Le produit {@link CatalogueProduitAnimalEntity} enregistré.
     * @throws IllegalArgumentException Si la désignation ou le code existe déjà ou si l'unité de mesure est absente.
     */
    @Transactional
    public CatalogueProduitAnimalEntity creerProduit(CatalogueProduitAnimalEntity produit) {
        if (produit.getDesignation() == null || produit.getDesignation().isBlank()) {
            throw new IllegalArgumentException("La désignation du produit animal est obligatoire.");
        }
        String designationNormalisee = produit.getDesignation().trim();
        if (produitRepository.existsByDesignationIgnoreCase(designationNormalisee)) {
            throw new IllegalArgumentException(
                    "Création refusée (CA-REF-014-02 / RG-REF-002) : un produit ou sous-produit portant la désignation '"
                            + designationNormalisee + "' existe déjà dans le catalogue (contrôle insensible à la casse).");
        }
        if (produit.getCode() != null && produitRepository.existsByCodeIgnoreCase(produit.getCode().trim())) {
            throw new IllegalArgumentException(
                    "Un produit portant le code '" + produit.getCode().trim() + "' existe déjà dans le catalogue.");
        }
        if (produit.getCodeUniteMesure() == null || produit.getCodeUniteMesure().isBlank()) {
            throw new IllegalArgumentException("L'unité de mesure de base est obligatoire pour tout produit du catalogue (CA-REF-014-01).");
        }
        produit.setDesignation(designationNormalisee);
        return produitRepository.save(produit);
    }

    // =========================================================================
    // F-REF-015 : Référentiels sanitaire/vétérinaire et abattage/transformation
    // =========================================================================

    /**
     * Liste les éléments du référentiel sanitaire et vétérinaire, optionnellement filtrés par type
     * ({@code PATHOLOGIE}, {@code VACCIN_PROPHYLAXIE}, {@code TRAITEMENT_VETERINAIRE}, {@code MOTIF_SAISIE_SANTE}).
     *
     * @param typeElement Type optionnel d'élément sanitaire.
     * @return Liste des éléments {@link ReferentielSanitaireVeterinaireEntity}.
     */
    public List<ReferentielSanitaireVeterinaireEntity> listerElementsSanitaires(String typeElement) {
        if (typeElement != null && !typeElement.isBlank()) {
            return sanitaireRepository.findByTypeElementIgnoreCaseAndActifTrueOrderByCodeAsc(typeElement.trim());
        }
        return sanitaireRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Crée un élément dans le référentiel sanitaire et vétérinaire en contrôlant le délai d'attente avant abattage.
     *
     * @param element Données de l'élément sanitaire.
     * @return L'élément {@link ReferentielSanitaireVeterinaireEntity} enregistré.
     */
    @Transactional
    public ReferentielSanitaireVeterinaireEntity creerElementSanitaire(ReferentielSanitaireVeterinaireEntity element) {
        if (element.getDelaiAttenteAbattageJours() != null && element.getDelaiAttenteAbattageJours() < 0) {
            throw new IllegalArgumentException("Le délai d'attente avant abattage ne peut pas être négatif.");
        }
        return sanitaireRepository.save(element);
    }

    /**
     * Liste les normes du référentiel d'abattage et de transformation, optionnellement filtrées par type de norme.
     *
     * @param typeNorme Type optionnel de norme ({@code TYPE_DECOUPE}, {@code GRILLE_QUALITE_CARCASSE}, {@code RENDEMENT_CIBLE}).
     * @return Liste des normes {@link ReferentielAbattageTransformationEntity}.
     */
    public List<ReferentielAbattageTransformationEntity> listerNormesAbattage(String typeNorme) {
        if (typeNorme != null && !typeNorme.isBlank()) {
            return abattageRepository.findByTypeNormeIgnoreCaseAndActifTrueOrderByCodeAsc(typeNorme.trim());
        }
        return abattageRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Crée une norme technique d'abattage ou de transformation.
     *
     * @param norme Données de la norme d'abattage.
     * @return La norme {@link ReferentielAbattageTransformationEntity} sauvegardée.
     */
    @Transactional
    public ReferentielAbattageTransformationEntity creerNormeAbattage(ReferentielAbattageTransformationEntity norme) {
        return abattageRepository.save(norme);
    }

    // =========================================================================
    // F-REF-016 : Unités de mesure et règles de conversion contrôlées
    // =========================================================================

    /**
     * Liste toutes les unités de mesure actives du référentiel.
     *
     * @return Liste des unités {@link UniteMesureEntity}.
     */
    public List<UniteMesureEntity> listerUnitesMesure() {
        return uniteMesureRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Crée une unité de mesure si son code n'existe pas déjà.
     *
     * @param unite Données de l'unité de mesure.
     * @return L'unité {@link UniteMesureEntity} enregistrée.
     */
    @Transactional
    public UniteMesureEntity creerUniteMesure(UniteMesureEntity unite) {
        if (unite.getCode() == null || unite.getCode().isBlank()) {
            throw new IllegalArgumentException("Le code de l'unité de mesure est obligatoire.");
        }
        return uniteMesureRepository.findByCodeIgnoreCase(unite.getCode().trim())
                .orElseGet(() -> uniteMesureRepository.save(unite));
    }

    /**
     * Liste toutes les règles de conversion d'unités actives.
     *
     * @return Liste des règles {@link RegleConversionUniteEntity}.
     */
    public List<RegleConversionUniteEntity> listerReglesConversion() {
        return regleConversionRepository.findByActifTrue();
    }

    /**
     * Crée une règle de conversion entre deux unités en vérifiant la cohérence dimensionnelle ({@code CA-REF-016-01} &amp; {@code CA-REF-016-02}).
     * Rejette toute conversion directe entre dimensions physiques incompatibles (ex. Litre &lt;-&gt; Tête sans règle métier,
     * ou Hectare &lt;-&gt; Kilogramme).
     *
     * @param regle Données de la règle de conversion.
     * @return La règle {@link RegleConversionUniteEntity} persistée.
     * @throws IllegalArgumentException Si les dimensions physiques sont incompatibles ou si le facteur est négatif/nul.
     */
    @Transactional
    public RegleConversionUniteEntity creerRegleConversion(RegleConversionUniteEntity regle) {
        if (regle.getFacteurMultiplication() == null || regle.getFacteurMultiplication().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le facteur multiplicatif de conversion doit être strictement positif.");
        }
        UniteMesureEntity source = uniteMesureRepository.findByCodeIgnoreCase(regle.getCodeUniteSource().trim())
                .orElseThrow(() -> new IllegalArgumentException("Unité source introuvable : " + regle.getCodeUniteSource()));
        UniteMesureEntity cible = uniteMesureRepository.findByCodeIgnoreCase(regle.getCodeUniteCible().trim())
                .orElseThrow(() -> new IllegalArgumentException("Unité cible introuvable : " + regle.getCodeUniteCible()));

        String dimSource = source.getDimensionPhysique().toUpperCase();
        String dimCible = cible.getDimensionPhysique().toUpperCase();

        boolean memeDimension = dimSource.equals(dimCible);
        boolean conversionConditionnementAutorisee =
                (dimSource.equals("CONDITIONNEMENT") && DIMENSIONS_CONDITIONNABLES.contains(dimCible))
                || (dimCible.equals("CONDITIONNEMENT") && DIMENSIONS_CONDITIONNABLES.contains(dimSource));

        if (!memeDimension && !conversionConditionnementAutorisee) {
            throw new IllegalArgumentException(
                    "Conversion incohérente refusée (CA-REF-016-02) : impossible de définir une règle de conversion directe entre '"
                            + source.getCode() + "' (" + dimSource + ") et '"
                            + cible.getCode() + "' (" + dimCible + ") car leurs dimensions physiques sont incompatibles.");
        }

        regle.setCodeUniteSource(source.getCode());
        regle.setCodeUniteCible(cible.getCode());
        return regleConversionRepository.save(regle);
    }

    /**
     * Convertit une quantité d'une unité source vers une unité cible ({@code CA-REF-016-01} &amp; {@code CA-REF-016-02}).
     *
     * @param quantite        Quantité numérique à convertir.
     * @param codeUniteSource Code de l'unité de départ.
     * @param codeUniteCible  Code de l'unité d'arrivée.
     * @return Quantité convertie arrondie à 4 décimales.
     * @throws IllegalArgumentException Si aucune règle de conversion directe ou inverse n'est définie.
     */
    public BigDecimal convertirQuantite(BigDecimal quantite, String codeUniteSource, String codeUniteCible) {
        if (quantite == null) {
            throw new IllegalArgumentException("La quantité à convertir est obligatoire.");
        }
        if (codeUniteSource.trim().equalsIgnoreCase(codeUniteCible.trim())) {
            return quantite.setScale(4, RoundingMode.HALF_UP);
        }

        return regleConversionRepository
                .findByCodeUniteSourceIgnoreCaseAndCodeUniteCibleIgnoreCaseAndActifTrue(
                        codeUniteSource.trim(), codeUniteCible.trim())
                .map(r -> quantite.multiply(r.getFacteurMultiplication()).setScale(4, RoundingMode.HALF_UP))
                .or(() -> regleConversionRepository
                        .findByCodeUniteSourceIgnoreCaseAndCodeUniteCibleIgnoreCaseAndActifTrue(
                                codeUniteCible.trim(), codeUniteSource.trim())
                        .map(inverse -> quantite.divide(inverse.getFacteurMultiplication(), 4, RoundingMode.HALF_UP)))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conversion refusée (CA-REF-016-02) : aucune règle de conversion validée n'est définie entre '"
                                + codeUniteSource + "' et '" + codeUniteCible + "'."));
    }
}
