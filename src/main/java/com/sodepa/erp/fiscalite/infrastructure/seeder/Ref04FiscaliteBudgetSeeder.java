package com.sodepa.erp.fiscalite.infrastructure.seeder;

import com.sodepa.erp.budget.infrastructure.entities.*;
import com.sodepa.erp.budget.infrastructure.repo.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ExerciceComptableService;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.SchemaEcritureComptableEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.ExerciceComptableRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.SchemaEcritureComptableRepository;
import com.sodepa.erp.fiscalite.infrastructure.entities.RegimeFiscalEntity;
import com.sodepa.erp.fiscalite.infrastructure.entities.SeuilGestionEntity;
import com.sodepa.erp.fiscalite.infrastructure.entities.TaxeImpotsEntity;
import com.sodepa.erp.fiscalite.infrastructure.repo.RegimeFiscalRepository;
import com.sodepa.erp.fiscalite.infrastructure.repo.SeuilGestionRepository;
import com.sodepa.erp.fiscalite.infrastructure.repo.TaxeImpotsRepository;
import com.sodepa.erp.tresorerie.regies.infrastructure.entities.MoyenPaiementEntity;
import com.sodepa.erp.tresorerie.regies.infrastructure.repo.MoyenPaiementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeder transversal du sous-domaine REF-04 (Fiscalité, Comptabilité, Budget & Trésorerie — F-REF-017 à F-REF-020).
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class Ref04FiscaliteBudgetSeeder implements CommandLineRunner {

    private final TaxeImpotsRepository taxeRepository;
    private final RegimeFiscalRepository regimeRepository;
    private final SeuilGestionRepository seuilRepository;
    private final ExerciceComptableRepository exerciceRepository;
    private final ExerciceComptableService exerciceService;
    private final SchemaEcritureComptableRepository schemaRepository;
    private final BudgetProgrammeRepository programmeRepository;
    private final BudgetActiviteRepository activiteRepository;
    private final BudgetSourceFinancementRepository sourceRepository;
    private final BudgetNatureDepenseRepository natureRepository;
    private final MoyenPaiementRepository moyenPaiementRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedFiscaliteEtRegimes();
        seedTableUniqueSeuils();
        seedExerciceEtSchemasComptables();
        seedNomencleéatureBudgetaire();
        seedMoyensPaiementEtMobileMoney();
    }

    private void seedFiscaliteEtRegimes() {
        if (regimeRepository.count() == 0) {
            regimeRepository.saveAll(List.of(
                    RegimeFiscalEntity.builder().code("REEL").libelle("Régime du Réel (DGI Cameroun)")
                            .tauxAirApplicable(new BigDecimal("2.20")).assujettiTva(true)
                            .description("Entreprises au régime du réel : retenue AIR 2,2 % et assujettissement TVA 19,25 %").actif(true).build(),
                    RegimeFiscalEntity.builder().code("SIMPLIFIE").libelle("Régime Simplifié d'Imposition (RSI)")
                            .tauxAirApplicable(new BigDecimal("5.50")).assujettiTva(false)
                            .description("Contribuables au RSI : retenue AIR 5,5 % hors TVA").actif(true).build(),
                    RegimeFiscalEntity.builder().code("LIBERATOIRE").libelle("Impôt Libératoire")
                            .tauxAirApplicable(new BigDecimal("10.00")).assujettiTva(false)
                            .description("Petits prestataires à l'impôt libératoire").actif(true).build(),
                    RegimeFiscalEntity.builder().code("EXONERE_PUBLIC").libelle("Organisme Public / Exonéré")
                            .tauxAirApplicable(BigDecimal.ZERO).assujettiTva(false)
                            .description("État, collectivités et conventions exonérées").actif(true).build()
            ));
        }

        if (taxeRepository.count() == 0) {
            LocalDate dateEffetLoiFinances = LocalDate.of(2026, 1, 1);
            taxeRepository.saveAll(List.of(
                    TaxeImpotsEntity.builder().code("TVA_STD").libelle("TVA Cameroun au taux général (17,5 % + CAC 10 % = 19,25 %)")
                            .categorieFiscale("TVA").tauxPourcentage(new BigDecimal("19.25")).baseCalcul("HT")
                            .compteComptableCollecte("4431").compteComptableDeductible("4452")
                            .periodiciteDecleéaration("MENSUELLE_J15").dateEffet(dateEffetLoiFinances)
                            .referenceTexteLoi("CGI Cameroun Art. 125 & Loi de Finances 2026").actif(true).build(),
                    TaxeImpotsEntity.builder().code("AIR_REEL").libelle("Acompte sur Impôt sur le Revenu — Régime du Réel (2,2 %)")
                            .categorieFiscale("RETENUE_SOURCE_AIR").tauxPourcentage(new BigDecimal("2.20")).baseCalcul("HT")
                            .compteComptableCollecte("4472").periodiciteDecleéaration("MENSUELLE_J15")
                            .dateEffet(dateEffetLoiFinances).referenceTexteLoi("CGI Cameroun Art. 21").actif(true).build(),
                    TaxeImpotsEntity.builder().code("AIR_SIMPLIFIE").libelle("Acompte sur Impôt sur le Revenu — Régime Simplifié (5,5 %)")
                            .categorieFiscale("RETENUE_SOURCE_AIR").tauxPourcentage(new BigDecimal("5.50")).baseCalcul("HT")
                            .compteComptableCollecte("4472").periodiciteDecleéaration("MENSUELLE_J15")
                            .dateEffet(dateEffetLoiFinances).referenceTexteLoi("CGI Cameroun Art. 21").actif(true).build(),
                    TaxeImpotsEntity.builder().code("TSR_EXT").libelle("Taxe Spéciale sur les Revenus servis à l'étranger (15 %)")
                            .categorieFiscale("RETENUE_SOURCE_AIR").tauxPourcentage(new BigDecimal("15.00")).baseCalcul("HT")
                            .compteComptableCollecte("4478").periodiciteDecleéaration("MENSUELLE_J15")
                            .dateEffet(dateEffetLoiFinances).referenceTexteLoi("CGI Cameroun Art. 225").actif(true).build(),
                    TaxeImpotsEntity.builder().code("INSPECTION_VET_BOVIN").libelle("Taxe d'inspection sanitaire vétérinaire à l'abattage bovin")
                            .categorieFiscale("TAXE_PARAFISCALE_PASTORALE").montantFixeFcfa(new BigDecimal("1000"))
                            .baseCalcul("TETE_ABATTUE").compteComptableCollecte("4462")
                            .periodiciteDecleéaration("MENSUELLE_J15").dateEffet(dateEffetLoiFinances)
                            .referenceTexteLoi("Fiscalité pastorale MINEPIA").actif(true).build()
            ));
            log.info("Référentiel fiscal DGI Cameroun initialisé (F-REF-020 / CA-REF-020-01).");
        }
    }

    private void seedTableUniqueSeuils() {
        if (seuilRepository.count() == 0) {
            LocalDate dateEffet = LocalDate.of(2026, 1, 1);
            seuilRepository.saveAll(List.of(
                    SeuilGestionEntity.builder().code("ACHAT_DIRECT_CAISSE").domaine("ACHATS")
                            .libelle("Menues dépenses / Achat direct sur caisse (< 500 000 FCFA)")
                            .montantMinFcfa(BigDecimal.ZERO).montantMaxFcfa(new BigDecimal("499999"))
                            .nbProformasRequis(1).commissionRequise(false).autoriteValidation("CHEF_UNITE")
                            .dateEffet(dateEffet).actif(true).build(),
                    SeuilGestionEntity.builder().code("BON_COMMANDE").domaine("ACHATS")
                            .libelle("Bon de Commande administratif (500 000 à 4 999 999 FCFA — 3 proformas requis)")
                            .montantMinFcfa(new BigDecimal("500000")).montantMaxFcfa(new BigDecimal("4999999"))
                            .nbProformasRequis(3).commissionRequise(false).autoriteValidation("DG")
                            .dateEffet(dateEffet).actif(true).build(),
                    SeuilGestionEntity.builder().code("DEMANDE_COTATION_LETTRE_COMMANDE").domaine("ACHATS")
                            .libelle("Demande de cotation / Lettre-Commande (5 000 000 à 49 999 999 FCFA)")
                            .montantMinFcfa(new BigDecimal("5000000")).montantMaxFcfa(new BigDecimal("49999999"))
                            .nbProformasRequis(3).commissionRequise(true).autoriteValidation("DG_COMITE_ACHAT")
                            .dateEffet(dateEffet).actif(true).build(),
                    SeuilGestionEntity.builder().code("MARCHE_PUBLIC_CIPM").domaine("ACHATS")
                            .libelle("Appel d'offres / Marché Public CIPM (>= 50 000 000 FCFA)")
                            .montantMinFcfa(new BigDecimal("50000000")).montantMaxFcfa(null)
                            .nbProformasRequis(3).commissionRequise(true).autoriteValidation("CIPM_DG")
                            .dateEffet(dateEffet).actif(true).build(),
                    SeuilGestionEntity.builder().code("PLAFOND_MENUE_DEPENSE_REGIE").domaine("TRESORERIE_CAISSE")
                            .libelle("Plafond unitaire d'un bon de caisse en régie d'avance")
                            .montantMinFcfa(BigDecimal.ZERO).montantMaxFcfa(new BigDecimal("100000"))
                            .nbProformasRequis(1).commissionRequise(false).autoriteValidation("CSFR_CHEF_UNITE")
                            .dateEffet(dateEffet).actif(true).build(),
                    SeuilGestionEntity.builder().code("PLAFOND_ENCAISSE_REGIE_RANCH").domaine("TRESORERIE_CAISSE")
                            .libelle("Plafond d'encaisse autorisé pour une caisse de Ranch")
                            .montantMinFcfa(BigDecimal.ZERO).montantMaxFcfa(new BigDecimal("10000000"))
                            .nbProformasRequis(0).commissionRequise(false).autoriteValidation("DAF_DG")
                            .dateEffet(dateEffet).actif(true).build(),
                    SeuilGestionEntity.builder().code("SEUIL_VIREMENT_CREDIT_BUDGET").domaine("BUDGET")
                            .libelle("Seuil de réallocation / virement de crédits budgétaires soumis à visa DAF + DG")
                            .montantMinFcfa(BigDecimal.ZERO).montantMaxFcfa(new BigDecimal("25000000"))
                            .nbProformasRequis(0).commissionRequise(false).autoriteValidation("DAF_DG")
                            .dateEffet(dateEffet).actif(true).build()
            ));
            log.info("Table unique des seuils de gestion initialisée (CA-REF-020-02 & CA-REF-020-03).");
        }
    }

    private void seedExerciceEtSchemasComptables() {
        int anneeCourante = LocalDate.now().getYear();
        if (exerciceRepository.findByAnnee(anneeCourante).isEmpty()) {
            exerciceService.ouvrirExercice(anneeCourante, "Exercice Comptable SODEPA " + anneeCourante,
                    "ACHAT,VENTE,BANQUE,CAISSE,OD,ANOUVEAU");
        }

        if (schemaRepository.count() == 0) {
            schemaRepository.saveAll(List.of(
                    SchemaEcritureComptableEntity.builder().code("SCH_FACTURE_ACHAT").libelle("Comptabilisation Facture Fournisseur avec TVA et AIR")
                            .codeJournal("ACHAT").typeOperation("FACTURE_ACHAT")
                            .compteDebitCode("6011").compteCreditCode("4011")
                            .compteTaxeCode("4452").compteRetenueAirCode("4472").actif(true).build(),
                    SchemaEcritureComptableEntity.builder().code("SCH_VENTE_BOVIN").libelle("Vente de bovins sur pied ou carcasses")
                            .codeJournal("VENTE").typeOperation("VENTE_BOVIN")
                            .compteDebitCode("4111").compteCreditCode("7011")
                            .compteTaxeCode("4431").actif(true).build(),
                    SchemaEcritureComptableEntity.builder().code("SCH_NAISSANCE_CHEPTEL").libelle("Croît biologique — Naissance de veau en ranch")
                            .codeJournal("OD").typeOperation("NAISSANCE_BOVIN")
                            .compteDebitCode("2191").compteCreditCode("7217").actif(true).build(),
                    SchemaEcritureComptableEntity.builder().code("SCH_APPRO_REGIE").libelle("Approvisionnement de caisse de régie (Ranch/Abattoir)")
                            .codeJournal("BANQUE").typeOperation("APPRO_REGIE")
                            .compteDebitCode("5711").compteCreditCode("5211").actif(true).build()
            ));
        }
    }

    private void seedNomencleéatureBudgetaire() {
        if (programmeRepository.count() == 0) {
            programmeRepository.saveAll(List.of(
                    BudgetProgrammeEntity.builder().code("PROG-01").intitule("Développement de la Production Pastorale et Génétique Bovine")
                            .objectifStrategique("Accroître le cheptel bovin et améliorer le potentiel génétique dans les ranches de Dumbo, Jakiri, Faro et Ndokayo")
                            .responsableProgramme("Directeur de la Production et de la Commercialisation (DPC)").actif(true).build(),
                    BudgetProgrammeEntity.builder().code("PROG-02").intitule("Modernisation de l'Abattage Industriel et de la Chaîne du Froid")
                            .objectifStrategique("Optimiser le rendement d'abattage et la conservation frigorifique à Yaoundé, Douala et Ngaoundéré")
                            .responsableProgramme("Directeur de la Production et de la Commercialisation (DPC)").actif(true).build(),
                    BudgetProgrammeEntity.builder().code("PROG-03").intitule("Gouvernance, Pilotage Administratif, Financier et Transformation Numérique")
                            .objectifStrategique("Assurer l'équilibre financier, la maîtrise des risques et la digitalisation intégrée ERP SODEPA")
                            .responsableProgramme("Directeur Administratif et Financier (DAF)").actif(true).build()
            ));
        }

        if (activiteRepository.count() == 0) {
            activiteRepository.saveAll(List.of(
                    BudgetActiviteEntity.builder().code("ACT-01-01").intitule("Suivi sanitaire, prophylaxie et alimentation du cheptel")
                            .programmeCode("PROG-01").actif(true).build(),
                    BudgetActiviteEntity.builder().code("ACT-01-02").intitule("Aménagement des pâturages et hydraulique pastorale")
                            .programmeCode("PROG-01").actif(true).build(),
                    BudgetActiviteEntity.builder().code("ACT-02-01").intitule("Exploitation et maintenance des chaînes d'abattage et chambres froides")
                            .programmeCode("PROG-02").actif(true).build(),
                    BudgetActiviteEntity.builder().code("ACT-03-01").intitule("Administration générale, RH, Finance et Systèmes d'Information")
                            .programmeCode("PROG-03").actif(true).build()
            ));
        }

        if (sourceRepository.count() == 0) {
            sourceRepository.saveAll(List.of(
                    BudgetSourceFinancementEntity.builder().code("FONDS_PROPRES_SODEPA").intitule("Recettes propres d'exploitation SODEPA")
                            .typeSource("FONDS_PROPRES").bailleurNom("SODEPA").devise("XAF").actif(true).build(),
                    BudgetSourceFinancementEntity.builder().code("SUBVENTION_MINEPIA_BIP").intitule("Subvention d'investissement de l'État (MINEPIA / BIP)")
                            .typeSource("SUBVENTION_ETAT").bailleurNom("État du Cameroun — MINEPIA").devise("XAF").actif(true).build(),
                    BudgetSourceFinancementEntity.builder().code("FIN_EXT_BANQUE_MONDIALE").intitule("Financement extérieur projets d'élevage (PRODEL / BM)")
                            .typeSource("FINANCEMENT_EXTERIEUR").bailleurNom("Banque Mondiale / MINEPIA").devise("XAF").actif(true).build()
            ));
        }

        if (natureRepository.count() == 0) {
            natureRepository.saveAll(List.of(
                    BudgetNatureDepenseEntity.builder().code("NAT_INTRANTS_VET").intitule("Achats de produits vétérinaires, vaccins et aliments bétail")
                            .categorie("FONCTIONNEMENT").prefixeCompteSyscohada("60").actif(true).build(),
                    BudgetNatureDepenseEntity.builder().code("NAT_SERVICES_EXT").intitule("Transports, entretien, maintenance et services extérieurs")
                            .categorie("FONCTIONNEMENT").prefixeCompteSyscohada("61").actif(true).build(),
                    BudgetNatureDepenseEntity.builder().code("NAT_AUTRES_SERVICES").intitule("Honoraires, télécommunications, missions et prestations externes")
                            .categorie("FONCTIONNEMENT").prefixeCompteSyscohada("62").actif(true).build(),
                    BudgetNatureDepenseEntity.builder().code("NAT_CHARGES_PERSONNEL").intitule("Salaires, primes et charges sociales du personnel SODEPA")
                            .categorie("FONCTIONNEMENT").prefixeCompteSyscohada("66").actif(true).build(),
                    BudgetNatureDepenseEntity.builder().code("NAT_INV_EQUIPEMENT").intitule("Acquisitions d'équipements pastoraux, frigorifiques et industriels")
                            .categorie("INVESTISSEMENT").prefixeCompteSyscohada("24").actif(true).build(),
                    BudgetNatureDepenseEntity.builder().code("NAT_INV_INFRA").intitule("Bâtiments, parcs de contention, forages et infrastructures")
                            .categorie("INVESTISSEMENT").prefixeCompteSyscohada("23").actif(true).build()
            ));
            log.info("Nomencleéature budgétaire SODEPA initialisée (F-REF-018 / CA-REF-018-01 & 02).");
        }
    }

    private void seedMoyensPaiementEtMobileMoney() {
        if (moyenPaiementRepository.count() == 0) {
            moyenPaiementRepository.saveAll(List.of(
                    MoyenPaiementEntity.builder().code("ESPECES_CAISSE").libelle("Espèces en Francs CFA (Caisses SODEPA)")
                            .typeMoyen("ESPECES").operateur("CAISSE_INTERNE").compteComptableAssocie("571100")
                            .plafondTransactionFcfa(new BigDecimal("100000")).fraisPourcentage(BigDecimal.ZERO).actif(true).build(),
                    MoyenPaiementEntity.builder().code("VIREMENT_CEMAC").libelle("Virement Bancaire SYGMA / SYSTAC (Zone CEMAC)")
                            .typeMoyen("VIREMENT_BANCAIRE").operateur("SYGMA_SYSTAC_BEAC").compteComptableAssocie("521100")
                            .plafondTransactionFcfa(null).fraisPourcentage(BigDecimal.ZERO).actif(true).build(),
                    MoyenPaiementEntity.builder().code("CHEQUE_BANCAIRE").libelle("Chèque Bancaire Compensable")
                            .typeMoyen("CHEQUE").operateur("COMPENSATION_BEAC").compteComptableAssocie("521100")
                            .plafondTransactionFcfa(new BigDecimal("50000000")).fraisPourcentage(BigDecimal.ZERO).actif(true).build(),
                    MoyenPaiementEntity.builder().code("MTN_MOMO").libelle("MTN Mobile Money Cameroun (Marchand SODEPA)")
                            .typeMoyen("MOBILE_MONEY").operateur("MTN_CAMEROON").numeroMarchandOuShortcode("*126# / 670000000")
                            .compteComptableAssocie("558100").plafondTransactionFcfa(new BigDecimal("2000000"))
                            .fraisPourcentage(new BigDecimal("1.000")).actif(true).build(),
                    MoyenPaiementEntity.builder().code("ORANGE_MONEY").libelle("Orange Money Cameroun (Marchand SODEPA)")
                            .typeMoyen("MOBILE_MONEY").operateur("ORANGE_CAMEROON").numeroMarchandOuShortcode("#150# / 690000000")
                            .compteComptableAssocie("558200").plafondTransactionFcfa(new BigDecimal("2000000"))
                            .fraisPourcentage(new BigDecimal("1.000")).actif(true).build()
            ));
            log.info("Moyens de paiement et opérateurs Mobile Money initialisés (F-REF-019 / CA-REF-019-02).");
        }
    }
}
