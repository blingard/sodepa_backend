package com.sodepa.erp.pastoral.presentation.rest;

import com.sodepa.erp.pastoral.application.ReferentielTechniqueMetierService;
import com.sodepa.erp.pastoral.infrastructure.entities.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur REST des Référentiels Métiers, Techniques &amp; Géographiques ({@code REF-03} &amp; {@code F-REF-012} à {@code F-REF-016}).
 */
@RestController
@RequestMapping({"/api/referentiel-technique", "/api/v1/referentiel-technique"})
@Tag(name = "Référentiels Métiers, Techniques & Géographiques (REF-03 & F-REF-012 à 016)",
     description = "Localités, sites d'exploitation, espèces et races animales, catalogue produits, référentiel sanitaire/vétérinaire, abattage et conversions d'unités")
/**
 * Contrôleur REST exposant les points de terminaison pour `ReferentielTechniqueMetierRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
public class ReferentielTechniqueMetierRestController {

    private final ReferentielTechniqueMetierService service;

    /**
     * Constructeur du contrôleur des référentiels techniques et métiers.
     *
     * @param service Service applicatif associé.
     */
    public ReferentielTechniqueMetierRestController(ReferentielTechniqueMetierService service) {
        this.service = service;
    }

    // --- F-REF-012 : Localités & Sites d'exploitation ---

    /**
     * Liste toutes les localités géographiques actives.
     *
     * @return Réponse HTTP 200 contenant les localités.
     */
    @GetMapping("/localites")
    @Operation(summary = "Lister les localités du découpage administratif camerounais (F-REF-012)")
    public ResponseEntity<List<LocaliteGeographiqueEntity>> listerLocalites() {
        return ResponseEntity.ok(service.listerLocalites());
    }

    /**
     * Crée une nouvelle localité géographique.
     *
     * @param localite Données de la localité.
     * @return Réponse HTTP 201 contenant la localité crée.
     */
    @PostMapping("/localites")
    @Operation(summary = "Créer une localité géographique (Région, Département, Arrondissement, Ville)")
    public ResponseEntity<LocaliteGeographiqueEntity> creerLocalite(@RequestBody LocaliteGeographiqueEntity localite) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerLocalite(localite));
    }

    /**
     * Liste les sites d'exploitation SODEPA selon leur statut de validation.
     *
     * @param statutValidation Filtre optionnel sur le statut de validation ({@code PROPOSE_LOCALEMENT} ou {@code VALIDE_CENTRAL}).
     * @return Réponse HTTP 200 contenant les sites d'exploitation.
     */
    @GetMapping("/sites")
    @Operation(summary = "Lister les sites d'exploitation (Ranchs, Abattoirs, Frigorifiques, Points de vente — F-REF-012)")
    public ResponseEntity<List<SiteExploitationEntity>> listerSites(
            @RequestParam(required = false) String statutValidation) {
        if (statutValidation != null && !statutValidation.isBlank()) {
            return ResponseEntity.ok(service.listerSitesParStatutValidation(statutValidation));
        }
        return ResponseEntity.ok(service.listerSitesActifs());
    }

    /**
     * Propose localement ou crée centralement un site d'exploitation ({@code CA-REF-012-01} &amp; {@code 02}, {@code RG-REF-028}).
     *
     * @param site Données du site d'exploitation.
     * @return Réponse HTTP 201 contenant le site cré.
     */
    @PostMapping("/sites")
    @Operation(summary = "Proposer ou créer un site d'exploitation avec rattachement géographique obligatoire (CA-REF-012-01 & 02)")
    public ResponseEntity<SiteExploitationEntity> proposerOuCreerSite(@RequestBody SiteExploitationEntity site) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.proposerOuCreerSite(site));
    }

    /**
     * Valide centralement un site d'exploitation proposé par une unité opérationnelle locale.
     *
     * @param siteId Identifiant UUID du site.
     * @return Réponse HTTP 200 contenant le site validé et activé.
     */
    @PostMapping("/sites/{siteId}/valider-central")
    @Operation(summary = "Valider centralement un site ou point de vente proposé par une unité locale (CA-REF-012-02 / RG-REF-028)")
    public ResponseEntity<SiteExploitationEntity> validerSiteCentral(@PathVariable UUID siteId) {
        return ResponseEntity.ok(service.validerSiteCentral(siteId));
    }

    // --- F-REF-013 : Espèces & Races animales ---

    /**
     * Liste toutes les espèces animales actives du référentiel zootechnique.
     *
     * @return Réponse HTTP 200 contenant la liste des espèces.
     */
    @GetMapping("/especes")
    @Operation(summary = "Lister les espèces animales du référentiel zootechnique (F-REF-013)")
    public ResponseEntity<List<EspeceAnimaleEntity>> listerEspeces() {
        return ResponseEntity.ok(service.listerEspeces());
    }

    /**
     * Crée une espèce animale dans le référentiel zootechnique.
     *
     * @param espece Données de l'espèce animale.
     * @return Réponse HTTP 201 contenant l'espèce crée.
     */
    @PostMapping("/especes")
    @Operation(summary = "Créer une espèce animale dans le référentiel zootechnique")
    public ResponseEntity<EspeceAnimaleEntity> creerEspece(@RequestBody EspeceAnimaleEntity espece) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerEspece(espece));
    }

    /**
     * Liste les races animales actives, optionnellement filtrées par code d'espèce.
     *
     * @param codeEspece Code optionnel de l'espèce animale.
     * @return Réponse HTTP 200 contenant la liste des races.
     */
    @GetMapping("/races")
    @Operation(summary = "Lister les races animales (Goudali, Mbororo, Red Fulani, Charolaise, Brahman, etc. — F-REF-013)")
    public ResponseEntity<List<RaceAnimaleEntity>> listerRaces(@RequestParam(required = false) String codeEspece) {
        return ResponseEntity.ok(service.listerRaces(codeEspece));
    }

    /**
     * Crée une race animale rattachée obligatoirement à une espèce validée ({@code CA-REF-013-02}).
     *
     * @param race Données de la race animale.
     * @return Réponse HTTP 201 contenant la race crée.
     */
    @PostMapping("/races")
    @Operation(summary = "Créer une race animale rattachée obligatoirement à une espèce validée (CA-REF-013-01 & 02)")
    public ResponseEntity<RaceAnimaleEntity> creerRace(@RequestBody RaceAnimaleEntity race) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerRace(race));
    }

    // --- F-REF-014 : Catalogue des produits et sous-produits animaux ---

    /**
     * Liste les produits et sous-produits animaux actifs du catalogue.
     *
     * @return Réponse HTTP 200 contenant le catalogue des produits.
     */
    @GetMapping("/produits")
    @Operation(summary = "Consulter le catalogue des produits et sous-produits animaux (F-REF-014)")
    public ResponseEntity<List<CatalogueProduitAnimalEntity>> listerProduits() {
        return ResponseEntity.ok(service.listerProduits());
    }

    /**
     * Crée un produit ou sous-produit animal avec contrôle anti-doublon insensible à la casse ({@code CA-REF-014-02}).
     *
     * @param produit Données du produit animal.
     * @return Réponse HTTP 201 contenant le produit cré.
     */
    @PostMapping("/produits")
    @Operation(summary = "Créer un produit ou sous-produit animal avec contrôle anti-doublon insensible à la casse (CA-REF-014-02)")
    public ResponseEntity<CatalogueProduitAnimalEntity> creerProduit(@RequestBody CatalogueProduitAnimalEntity produit) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerProduit(produit));
    }

    // --- F-REF-015 : Référentiel sanitaire/vétérinaire & Abattage/transformation ---

    /**
     * Liste les éléments du référentiel sanitaire et vétérinaire.
     *
     * @param typeElement Filtre optionnel sur le type d'élément sanitaire.
     * @return Réponse HTTP 200 contenant la liste des éléments sanitaires.
     */
    @GetMapping("/sanitaire")
    @Operation(summary = "Lister les pathologies, vaccins, traitements vétérinaires et délais d'attente avant abattage (F-REF-015)")
    public ResponseEntity<List<ReferentielSanitaireVeterinaireEntity>> listerElementsSanitaires(
            @RequestParam(required = false) String typeElement) {
        return ResponseEntity.ok(service.listerElementsSanitaires(typeElement));
    }

    /**
     * Crée un élément dans le référentiel sanitaire et vétérinaire.
     *
     * @param element Données de l'élément sanitaire.
     * @return Réponse HTTP 201 contenant l'élément cré.
     */
    @PostMapping("/sanitaire")
    @Operation(summary = "Créer un élément sanitaire/vétérinaire avec délai d'attente avant abattage (CA-REF-015-01)")
    public ResponseEntity<ReferentielSanitaireVeterinaireEntity> creerElementSanitaire(
            @RequestBody ReferentielSanitaireVeterinaireEntity element) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerElementSanitaire(element));
    }

    /**
     * Liste les normes techniques d'abattage et de transformation.
     *
     * @param typeNorme Filtre optionnel sur le type de norme.
     * @return Réponse HTTP 200 contenant les normes d'abattage.
     */
    @GetMapping("/abattage")
    @Operation(summary = "Lister les normes d'abattage, types de découpe, grilles de cleéassement et rendements cibles (F-REF-015)")
    public ResponseEntity<List<ReferentielAbattageTransformationEntity>> listerNormesAbattage(
            @RequestParam(required = false) String typeNorme) {
        return ResponseEntity.ok(service.listerNormesAbattage(typeNorme));
    }

    /**
     * Crée une norme technique d'abattage ou de transformation.
     *
     * @param norme Données de la norme.
     * @return Réponse HTTP 201 contenant la norme crée.
     */
    @PostMapping("/abattage")
    @Operation(summary = "Créer une norme d'abattage ou de transformation (CA-REF-015-02)")
    public ResponseEntity<ReferentielAbattageTransformationEntity> creerNormeAbattage(
            @RequestBody ReferentielAbattageTransformationEntity norme) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerNormeAbattage(norme));
    }

    // --- F-REF-016 : Unités de mesure & Règles de conversion ---

    /**
     * Liste toutes les unités de mesure officielles SODEPA.
     *
     * @return Réponse HTTP 200 contenant les unités de mesure.
     */
    @GetMapping("/unites-mesure")
    @Operation(summary = "Lister les unités de mesure officielles SODEPA (F-REF-016)")
    public ResponseEntity<List<UniteMesureEntity>> listerUnitesMesure() {
        return ResponseEntity.ok(service.listerUnitesMesure());
    }

    /**
     * Crée une nouvelle unité de mesure.
     *
     * @param unite Données de l'unité de mesure.
     * @return Réponse HTTP 201 contenant l'unité crée.
     */
    @PostMapping("/unites-mesure")
    @Operation(summary = "Créer une unité de mesure avec sa dimension physique")
    public ResponseEntity<UniteMesureEntity> creerUniteMesure(@RequestBody UniteMesureEntity unite) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerUniteMesure(unite));
    }

    /**
     * Liste toutes les règles actives de conversion entre unités de mesure.
     *
     * @return Réponse HTTP 200 contenant les règles de conversion.
     */
    @GetMapping("/conversions")
    @Operation(summary = "Lister les règles de conversion d'unités actives (F-REF-016)")
    public ResponseEntity<List<RegleConversionUniteEntity>> listerReglesConversion() {
        return ResponseEntity.ok(service.listerReglesConversion());
    }

    /**
     * Crée une règle de conversion entre deux unités après contrôle de compatibilité dimensionnelle ({@code CA-REF-016-02}).
     *
     * @param regle Données de la règle de conversion.
     * @return Réponse HTTP 201 contenant la règle crée.
     */
    @PostMapping("/conversions")
    @Operation(summary = "Créer une règle de conversion avec contrôle de cohérence dimensionnelle (CA-REF-016-01 & 02)")
    public ResponseEntity<RegleConversionUniteEntity> creerRegleConversion(
            @RequestBody RegleConversionUniteEntity regle) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerRegleConversion(regle));
    }

    /**
     * Convertit une quantité d'une unité source vers une unité cible.
     *
     * @param quantite    Quantité à convertir.
     * @param uniteSource Code de l'unité source.
     * @param uniteCible  Code de l'unité cible.
     * @return Réponse HTTP 200 contenant le résultat de la conversion.
     */
    @GetMapping("/conversions/calculer")
    @Operation(summary = "Convertir une quantité entre deux unités compatibles (CA-REF-016-01 & 02)")
    public ResponseEntity<Map<String, Object>> convertirQuantite(
            @RequestParam BigDecimal quantite,
            @RequestParam String uniteSource,
            @RequestParam String uniteCible) {
        BigDecimal resultat = service.convertirQuantite(quantite, uniteSource, uniteCible);
        return ResponseEntity.ok(Map.of(
                "quantiteSource", quantite,
                "uniteSource", uniteSource.toUpperCase(),
                "quantiteConvertie", resultat,
                "uniteCible", uniteCible.toUpperCase()
        ));
    }
}
