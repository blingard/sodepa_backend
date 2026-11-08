package com.sodepa.erp.pastoral.infrastructure.seeder;

import com.sodepa.erp.pastoral.infrastructure.entities.*;
import com.sodepa.erp.pastoral.infrastructure.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeder officiel du sous-domaine REF-03 & F-REF-012 :
 * - 15 unités de mesure officielles SODEPA et règles de conversion (F-REF-016)
 * - 5 espèces animales et 8 races bovines de référence (F-REF-013)
 * - Catalogue des produits et sous-produits animaux (F-REF-014)
 * - Référentiel sanitaire/vétérinaire et normes d'abattage/transformation (F-REF-015)
 * - Référentiel géographique camerounais et sites d'exploitation SODEPA (F-REF-012)
 */
@Component
@Order(25)
public class Ref03TechniqueEtGeoSeeder implements CommandLineRunner {

    private final UniteMesureRepository uniteMesureRepository;
    private final RegleConversionUniteRepository regleConversionRepository;
    private final EspeceAnimaleRepository especeRepository;
    private final RaceAnimaleRepository raceRepository;
    private final CatalogueProduitAnimalRepository produitRepository;
    private final ReferentielSanitaireVeterinaireRepository sanitaireRepository;
    private final ReferentielAbattageTransformationRepository abattageRepository;
    private final LocaliteGeographiqueRepository localiteRepository;
    private final SiteExploitationRepository siteRepository;

    public Ref03TechniqueEtGeoSeeder(
            UniteMesureRepository uniteMesureRepository,
            RegleConversionUniteRepository regleConversionRepository,
            EspeceAnimaleRepository especeRepository,
            RaceAnimaleRepository raceRepository,
            CatalogueProduitAnimalRepository produitRepository,
            ReferentielSanitaireVeterinaireRepository sanitaireRepository,
            ReferentielAbattageTransformationRepository abattageRepository,
            LocaliteGeographiqueRepository localiteRepository,
            SiteExploitationRepository siteRepository) {
        this.uniteMesureRepository = uniteMesureRepository;
        this.regleConversionRepository = regleConversionRepository;
        this.especeRepository = especeRepository;
        this.raceRepository = raceRepository;
        this.produitRepository = produitRepository;
        this.sanitaireRepository = sanitaireRepository;
        this.abattageRepository = abattageRepository;
        this.localiteRepository = localiteRepository;
        this.siteRepository = siteRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUnitesEtConversions();
        seedEspecesEtRaces();
        seedCatalogueProduits();
        seedSanitaireEtAbattage();
        seedGeographieEtSitesSodepa();
    }

    private void seedUnitesEtConversions() {
        creerUniteSiAbsente("KG", "Kilogramme", "kg", "MASSE_POIDS", 3);
        creerUniteSiAbsente("T", "Tonne", "t", "MASSE_POIDS", 3);
        creerUniteSiAbsente("G", "Gramme", "g", "MASSE_POIDS", 2);
        creerUniteSiAbsente("L", "Litre", "L", "VOLUME", 2);
        creerUniteSiAbsente("UNITE", "Unité / Pièce", "u", "EFFECTIF_UNITE", 0);
        creerUniteSiAbsente("TETE", "Tête de bétail", "tête", "EFFECTIF_UNITE", 0);
        creerUniteSiAbsente("SAC", "Sac standard (50 kg)", "sac", "CONDITIONNEMENT", 0);
        creerUniteSiAbsente("CARTON", "Carton frigorifique (20 kg)", "carton", "CONDITIONNEMENT", 0);
        creerUniteSiAbsente("CAISSE", "Caisse de manutention", "caisse", "CONDITIONNEMENT", 0);
        creerUniteSiAbsente("HA", "Hectare", "ha", "SUPERFICIE", 2);
        creerUniteSiAbsente("M2", "Mètre carré", "m²", "SUPERFICIE", 2);
        creerUniteSiAbsente("M3", "Mètre cube", "m³", "VOLUME", 3);
        creerUniteSiAbsente("H", "Heure", "h", "TEMPS", 2);
        creerUniteSiAbsente("JOUR", "Jour", "j", "TEMPS", 1);
        creerUniteSiAbsente("KWH", "Kilowattheure", "kWh", "ENERGIE", 2);

        creerConversionSiAbsente("T", "KG", new BigDecimal("1000.000000"), null);
        creerConversionSiAbsente("KG", "G", new BigDecimal("1000.000000"), null);
        creerConversionSiAbsente("SAC", "KG", new BigDecimal("50.000000"), null);
        creerConversionSiAbsente("CARTON", "KG", new BigDecimal("20.000000"), "PRD-CARC-BOV");
        creerConversionSiAbsente("HA", "M2", new BigDecimal("10000.000000"), null);
    }

    private void creerUniteSiAbsente(String code, String libelle, String symbole, String dimension, int decimales) {
        if (!uniteMesureRepository.existsByCodeIgnoreCase(code)) {
            UniteMesureEntity u = new UniteMesureEntity();
            u.setCode(code);
            u.setLibelle(libelle);
            u.setSymbole(symbole);
            u.setDimensionPhysique(dimension);
            u.setDecimalesAutorisees(decimales);
            u.setActif(true);
            uniteMesureRepository.save(u);
        }
    }

    private void creerConversionSiAbsente(String source, String cible, BigDecimal facteur, String codeProduit) {
        if (regleConversionRepository
                .findByCodeUniteSourceIgnoreCaseAndCodeUniteCibleIgnoreCaseAndActifTrue(source, cible)
                .isEmpty()) {
            RegleConversionUniteEntity r = new RegleConversionUniteEntity();
            r.setCodeUniteSource(source);
            r.setCodeUniteCible(cible);
            r.setFacteurMultiplication(facteur);
            r.setCodeProduitSpecifique(codeProduit);
            r.setActif(true);
            regleConversionRepository.save(r);
        }
    }

    private void seedEspecesEtRaces() {
        creerEspeceSiAbsente("BOVINE", "Espèce Bovine", "Bos taurus / Bos indicus", 283, 7);
        creerEspeceSiAbsente("OVINE", "Espèce Ovine", "Ovis aries", 150, 3);
        creerEspeceSiAbsente("CAPRINE", "Espèce Caprine", "Capra hircus", 150, 3);
        creerEspeceSiAbsente("PORCINE", "Espèce Porcine", "Sus scrofa domesticus", 114, 2);
        creerEspeceSiAbsente("AVICOLE", "Volailles", "Gallus gallus", 21, 1);

        creerRaceSiAbsente("GOUDALI", "BOVINE", "Zébu Goudali de l'Adamaoua", "Adamaoua (Cameroun)", "MIXTE",
                new BigDecimal("450.00"), new BigDecimal("52.50"), "Pur ou croisement améliorateur Charolais/Brahman");
        creerRaceSiAbsente("MBORORO", "BOVINE", "Zébu Mbororo (Red Fulani)", "Nord / Adamaoua / Est", "BOUCHERIE_VIANDE",
                new BigDecimal("400.00"), new BigDecimal("50.00"), "Rustique pastoral");
        creerRaceSiAbsente("WHITE_FULANI", "BOVINE", "Zébu Aku / White Fulani", "Zone soudano-sahélienne", "MIXTE",
                new BigDecimal("380.00"), new BigDecimal("49.50"), "Mixte lait-viande");
        creerRaceSiAbsente("BRAHMAN", "BOVINE", "Zébu Brahman amélioré", "Importé / Ranches SODEPA", "BOUCHERIE_VIANDE",
                new BigDecimal("550.00"), new BigDecimal("55.00"), "Croisement d'amélioration bouchère");
        creerRaceSiAbsente("HOLSTEIN", "BOVINE", "Pie Noire Holstein", "Importé / Hauts Plateaux", "LAITIERE",
                new BigDecimal("600.00"), new BigDecimal("48.00"), "Croisement laitier F1 Goudali x Holstein");
        creerRaceSiAbsente("CHAROLAISE", "BOVINE", "Charolaise", "Europe / Semence IA", "BOUCHERIE_VIANDE",
                new BigDecimal("700.00"), new BigDecimal("58.00"), "Croisement terminal boucher");
        creerRaceSiAbsente("SIMMENTAL", "BOVINE", "Simmental", "Europe / Semence IA", "MIXTE",
                new BigDecimal("650.00"), new BigDecimal("56.00"), "Croisement mixte viande-lait");
        creerRaceSiAbsente("CROISE_F1", "BOVINE", "Croisé F1 SODEPA (Goudali x Exotique)", "Ranches SODEPA", "CROISEMENT_AMELIORATEUR",
                new BigDecimal("520.00"), new BigDecimal("54.50"), "Schéma génétique F1 SODEPA");
    }

    private void creerEspeceSiAbsente(String code, String libelle, String nomScientifique, int gestationJours, int sevrageMois) {
        if (!especeRepository.existsByCodeIgnoreCase(code)) {
            EspeceAnimaleEntity e = new EspeceAnimaleEntity();
            e.setCode(code);
            e.setLibelle(libelle);
            e.setNomScientifique(nomScientifique);
            e.setDureeGestationJours(gestationJours);
            e.setAgeSevrageMois(sevrageMois);
            e.setActif(true);
            especeRepository.save(e);
        }
    }

    private void creerRaceSiAbsente(String code, String codeEspece, String nom, String origine, String aptitude,
                                   BigDecimal poidsMoyen, BigDecimal rendement, String schema) {
        if (!raceRepository.existsByCodeIgnoreCase(code)) {
            RaceAnimaleEntity r = new RaceAnimaleEntity();
            r.setCode(code);
            r.setCodeEspece(codeEspece);
            r.setNom(nom);
            r.setOrigineGeographique(origine);
            r.setAptitudePrincipale(aptitude);
            r.setPoidsMoyenAdulteKg(poidsMoyen);
            r.setRendementCarcasseReferencePct(rendement);
            r.setSchemaCroisementAutorise(schema);
            r.setActif(true);
            raceRepository.save(r);
        }
    }

    private void seedCatalogueProduits() {
        creerProduitSiAbsent("PRD-BOV-VIF", "Bovin sur pied destiné à l'abattage ou l'élevage",
                "ANIMAL_VIF", "BOVINE", "TETE", "SUR_PIED", 365, new BigDecimal("350000.00"), "701100");
        creerProduitSiAbsent("PRD-CARC-BOV", "Carcasse bovine entière réfrigérée",
                "CARCASSE", "BOVINE", "KG", "FRAIS_REFRIGERE_0_4C", 14, new BigDecimal("2800.00"), "701200");
        creerProduitSiAbsent("PRD-DEMI-CARC", "Demi-carcasse bovine réfrigérée",
                "CARCASSE", "BOVINE", "KG", "FRAIS_REFRIGERE_0_4C", 14, new BigDecimal("2850.00"), "701210");
        creerProduitSiAbsent("PRD-DECOUPE-FILET", "Filet de boeuf SODEPA conditionné",
                "DECOUPE_VIANDE", "BOVINE", "KG", "FRAIS_REFRIGERE_0_4C", 10, new BigDecimal("4500.00"), "701300");
        creerProduitSiAbsent("PRD-ABATS-ROUGES", "Abats rouges bovins (foie, coeur, rognons)",
                "ABAT_CINQUIEME_QUARTIER", "BOVINE", "KG", "FRAIS_REFRIGERE_0_4C", 5, new BigDecimal("2200.00"), "701400");
        creerProduitSiAbsent("PRD-CUIR-BRUT", "Cuir et peau brute de bovin",
                "SOUS_PRODUIT_CUIR_PEAU", "BOVINE", "UNITE", "AMBIANT", 90, new BigDecimal("15000.00"), "701500");
        creerProduitSiAbsent("PRD-LAIT-CRU", "Lait cru frais de vache",
                "PRODUIT_LAITIER", "BOVINE", "L", "FRAIS_REFRIGERE_0_4C", 3, new BigDecimal("600.00"), "701600");
    }

    private void creerProduitSiAbsent(String code, String designation, String categorie, String codeEspece,
                                      String unite, String conservation, int dureeJours, BigDecimal prixRef, String compteProduit) {
        if (!produitRepository.existsByCodeIgnoreCase(code)
                && !produitRepository.existsByDesignationIgnoreCase(designation)) {
            CatalogueProduitAnimalEntity p = new CatalogueProduitAnimalEntity();
            p.setCode(code);
            p.setDesignation(designation);
            p.setCategorieProduit(categorie);
            p.setCodeEspece(codeEspece);
            p.setCodeUniteMesure(unite);
            p.setModeConservation(conservation);
            p.setDureeConservationJours(dureeJours);
            p.setPrixReferenceHtFcfa(prixRef);
            p.setCompteProduitSyscohada(compteProduit);
            p.setStatutCommercial("COMMERCIALISABLE");
            p.setActif(true);
            produitRepository.save(p);
        }
    }

    private void seedSanitaireEtAbattage() {
        creerSanitaireSiAbsent("VET-MAL-PPCB", "Péripneumonie Contagieuse Bovine (PPCB)",
                "MALADIE_CONTAGIEUSE", "BOVINE", "Mycoplasma mycoides / Vaccin T1/44",
                "1 dose sous-cutanée annuelle + quarantaine stricte", 21, 12, true);
        creerSanitaireSiAbsent("VET-MAL-FA", "Fièvre Aphteuse",
                "MALADIE_CONTAGIEUSE", "BOVINE", "Aphtovirus O/A/SAT2",
                "Vaccination trivalente + interdiction de mouvement", 14, 6, true);
        creerSanitaireSiAbsent("VET-PRO-DEPARASITAGE", "Protocole de déparasitage interne/externe",
                "PROTOCOLE_TRAITEMENT", "BOVINE", "Ivermectine 1% / Albendazole",
                "1 ml / 50 kg poids vif en sous-cutanée", 28, 3, false);
        creerSanitaireSiAbsent("VET-CAL-PROPHYLAXIE", "Calendrier annuel de prophylaxie des ranches SODEPA",
                "CALENDRIER_VACCINATION", "BOVINE", "PPCB + Symptovax + Anthravax + Pastovax",
                "Campagne pré-saison sèche et pré-saison des pluies", 21, 6, true);

        creerNormeAbattageSiAbsente("ABT-REND-GOUDALI", "Norme de rendement carcasse Zébu Goudali",
                "NORME_RENDEMENT_CARCASSE", "BOVINE", "GOUDALI",
                new BigDecimal("49.00"), new BigDecimal("52.50"), new BigDecimal("56.00"), new BigDecimal("2.00"), new BigDecimal("2.00"));
        creerNormeAbattageSiAbsente("ABT-REND-CHAROLAIS", "Norme de rendement carcasse Croisé Charolais",
                "NORME_RENDEMENT_CARCASSE", "BOVINE", "CHAROLAISE",
                new BigDecimal("54.00"), new BigDecimal("58.00"), new BigDecimal("62.00"), new BigDecimal("1.80"), new BigDecimal("2.00"));
        creerNormeAbattageSiAbsente("ABT-CHAINE-BOV", "Chaîne d'abattage bovin industrielle (AIY/AID/AING)",
                "CHAINE_ABATTAGE", "BOVINE", null,
                new BigDecimal("48.00"), new BigDecimal("53.00"), new BigDecimal("60.00"), new BigDecimal("2.00"), new BigDecimal("2.00"));
    }

    private void creerSanitaireSiAbsent(String code, String intitule, String type, String espece,
                                        String agentOuVaccin, String posologie, int delaiJours,
                                        int rappelMois, boolean decleéarationMinepia) {
        if (!sanitaireRepository.existsByCodeIgnoreCase(code)) {
            ReferentielSanitaireVeterinaireEntity s = new ReferentielSanitaireVeterinaireEntity();
            s.setCode(code);
            s.setIntitule(intitule);
            s.setTypeElement(type);
            s.setCodeEspece(espece);
            s.setAgentPathogeneOuVaccin(agentOuVaccin);
            s.setPosologieRecommandee(posologie);
            s.setDelaiAttenteAbattageJours(delaiJours);
            s.setPeriodiciteRappelMois(rappelMois);
            s.setDecleéarationObligatoireMinepia(decleéarationMinepia);
            s.setActif(true);
            sanitaireRepository.save(s);
        }
    }

    private void creerNormeAbattageSiAbsente(String code, String libelle, String type, String espece, String codeRace,
                                             BigDecimal minPct, BigDecimal ciblePct, BigDecimal maxPct,
                                             BigDecimal perteMaxPct, BigDecimal tempCelsius) {
        if (!abattageRepository.existsByCodeIgnoreCase(code)) {
            ReferentielAbattageTransformationEntity a = new ReferentielAbattageTransformationEntity();
            a.setCode(code);
            a.setLibelle(libelle);
            a.setTypeNorme(type);
            a.setCodeEspece(espece);
            a.setCodeRace(codeRace);
            a.setRendementMinPct(minPct);
            a.setRendementCiblePct(ciblePct);
            a.setRendementMaxPct(maxPct);
            a.setPerteMaxRessuyagePct(perteMaxPct);
            a.setTemperatureConsigneCelsius(tempCelsius);
            a.setActif(true);
            abattageRepository.save(a);
        }
    }

    private void seedGeographieEtSitesSodepa() {
        creerLocaliteSiAbsente("LOC-CM-CE-YDE", "Yaoundé (Mfoundi - Centre)", "LOCALITE", "REG-CE", "FORET_BIMODALE");
        creerLocaliteSiAbsente("LOC-CM-LT-DLA", "Douala (Wouri - Littoral)", "LOCALITE", "REG-LT", "FORET_MONOMODALE");
        creerLocaliteSiAbsente("LOC-CM-AD-NGD", "Ngaoundéré / Faro (Adamaoua)", "LOCALITE", "REG-AD", "HAUTES_SAVANES_GUINEENNES");
        creerLocaliteSiAbsente("LOC-CM-NW-DUM", "Dumbo / Jakiri (Nord-Ouest)", "LOCALITE", "REG-NW", "HAUTS_PLATEAUX_OUEST");
        creerLocaliteSiAbsente("LOC-CM-ES-NDK", "Ndokayo (Lom-et-Djérem - Est)", "LOCALITE", "REG-ES", "FORET_BIMODALE");

        creerSiteSiAbsent("SITE-DG-YDE", "Siège Direction Générale SODEPA Yaoundé", "SIEGE_DIRECTION", "LOC-CM-CE-YDE", new BigDecimal("2.50"), new BigDecimal("0.00"));
        creerSiteSiAbsent("SITE-RFD", "Station d'Élevage / Ranch de Dumbo (RFD)", "RANCH_PASTORAL", "LOC-CM-NW-DUM", new BigDecimal("12000.00"), new BigDecimal("50.00"));
        creerSiteSiAbsent("SITE-RFJ", "Station d'Élevage / Ranch de Jakiri (RFJ)", "RANCH_PASTORAL", "LOC-CM-NW-DUM", new BigDecimal("6000.00"), new BigDecimal("30.00"));
        creerSiteSiAbsent("SITE-RFF", "Station d'Élevage / Ranch du Faro (RFF)", "RANCH_PASTORAL", "LOC-CM-AD-NGD", new BigDecimal("15000.00"), new BigDecimal("60.00"));
        creerSiteSiAbsent("SITE-RFN", "Station d'Élevage / Ranch de Ndokayo (RFN)", "RANCH_PASTORAL", "LOC-CM-ES-NDK", new BigDecimal("10000.00"), new BigDecimal("40.00"));
        creerSiteSiAbsent("SITE-AIY", "Abattoir Industriel de Yaoundé (AIY)", "ABATTOIR_INDUSTRIEL", "LOC-CM-CE-YDE", new BigDecimal("15.00"), new BigDecimal("350.00"));
        creerSiteSiAbsent("SITE-AID", "Abattoir Industriel de Douala (AID)", "ABATTOIR_INDUSTRIEL", "LOC-CM-LT-DLA", new BigDecimal("18.00"), new BigDecimal("450.00"));
        creerSiteSiAbsent("SITE-AING", "Abattoir Industriel de Ngaoundéré (AING)", "ABATTOIR_INDUSTRIEL", "LOC-CM-AD-NGD", new BigDecimal("12.00"), new BigDecimal("250.00"));
    }

    private void creerLocaliteSiAbsente(String code, String nom, String niveauAdmin, String parentCode, String zoneAgro) {
        if (!localiteRepository.existsByCodeIgnoreCase(code)) {
            LocaliteGeographiqueEntity l = new LocaliteGeographiqueEntity();
            l.setCode(code);
            l.setNom(nom);
            l.setNiveauAdministratif(niveauAdmin);
            l.setParentCode(parentCode);
            l.setZoneAgroEcologique(zoneAgro);
            l.setActif(true);
            localiteRepository.save(l);
        }
    }

    private void creerSiteSiAbsent(String code, String nom, String typeSite, String codeLocalite,
                                   BigDecimal superficieHa, BigDecimal capaciteTonnes) {
        if (!siteRepository.existsByCodeIgnoreCase(code)) {
            SiteExploitationEntity s = new SiteExploitationEntity();
            s.setCode(code);
            s.setNom(nom);
            s.setTypeSite(typeSite);
            s.setCodeLocalite(codeLocalite);
            s.setSuperficieHectares(superficieHa);
            s.setCapaciteStockageTonnes(capaciteTonnes);
            s.setProposeParUniteLocale(false);
            s.setStatutValidation("VALIDE_CENTRAL");
            s.setActif(true);
            siteRepository.save(s);
        }
    }
}
