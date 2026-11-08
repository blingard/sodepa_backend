package com.sodepa.erp.dashboard.infrastructure.seeder;

import com.sodepa.erp.dashboard.infrastructure.entities.FamilleReferentielEntity;
import com.sodepa.erp.dashboard.infrastructure.entities.ModeleDocumentNumMasqueEntity;
import com.sodepa.erp.dashboard.infrastructure.repo.FamilleReferentielRepository;
import com.sodepa.erp.dashboard.infrastructure.repo.ModeleDocumentNumMasqueRepository;
import com.sodepa.erp.organigramme.infrastructure.entities.EvolutionStructureOrganigrammeEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.EvolutionStructureOrganigrammeRepository;
import com.sodepa.erp.utils.StatutCycleVieReferentiel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Seeder officiel des sous-domaines REF-01 (Familles MDM), REF-02 (Fiches enrichies d'unités opérationnelles
 * conformes à la Résolution n° 11/CA/SODEPA/2018) et REF-07 (Modèles de documents officiels SODEPA et masques).
 */
@Component
@Order(30)
public class Ref01Ref02MdmSeeder implements CommandLineRunner {

    private final FamilleReferentielRepository familleRepository;
    private final ModeleDocumentNumMasqueRepository modeleDocumentRepository;
    private final EvolutionStructureOrganigrammeRepository evolutionStructureRepository;

    public Ref01Ref02MdmSeeder(
            FamilleReferentielRepository familleRepository,
            ModeleDocumentNumMasqueRepository modeleDocumentRepository,
            EvolutionStructureOrganigrammeRepository evolutionStructureRepository) {
        this.familleRepository = familleRepository;
        this.modeleDocumentRepository = modeleDocumentRepository;
        this.evolutionStructureRepository = evolutionStructureRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedFamillesMdm();
        seedFichesUnitesOperationnelles();
        seedModelesDocumentsEtMasques();
    }

    private void seedFamillesMdm() {
        creerFamilleSiAbsente("FAM-ORG", "Référentiel des structures organisationnelles et opérationnelles",
                "REF-02", "DG", null, "UO-{0000}", "^UO-[0-9]{4}$");
        creerFamilleSiAbsente("FAM-GEO", "Référentiel géographique et découpage administratif",
                "REF-02", "DG", null, "LOC-{00000}", "^LOC-[A-Z0-9-]{3,20}$");
        creerFamilleSiAbsente("FAM-SITE", "Sites d'exploitation, ranches, abattoirs et points de vente",
                "REF-02", "DPA", "FAM-GEO", "SITE-{0000}", "^SITE-[A-Z0-9-]{2,15}$");
        creerFamilleSiAbsente("FAM-ZOO-ESP", "Référentiel zootechnique — Espèces animales",
                "REF-03", "DPA", null, "ESP-{000}", "^[A-Z_]{3,20}$");
        creerFamilleSiAbsente("FAM-ZOO-RAC", "Référentiel zootechnique — Races animales",
                "REF-03", "DPA", "FAM-ZOO-ESP", "RAC-{0000}", "^[A-Z0-9_]{3,30}$");
        creerFamilleSiAbsente("FAM-PRD", "Catalogue des produits, articleées et sous-produits animaux",
                "REF-03", "DC", "FAM-ZOO-ESP", "PRD-{000000}", "^PRD-[A-Z0-9-]{3,20}$");
        creerFamilleSiAbsente("FAM-COMPTA", "Plan comptable SYSCOHADA SODEPA",
                "REF-04", "DAF", null, "{000000}", "^[1-9][0-9]{1,9}$");
        creerFamilleSiAbsente("FAM-BUDGET", "Nomencleéature budgétaire par programmes et activités",
                "REF-04", "DAF", null, "PRG-{AAAA}-{000}", "^PRG-[0-9]{4}-[0-9]{3}$");
        creerFamilleSiAbsente("FAM-TIERS-FRN", "Référentiel des Fournisseurs et Prestataires",
                "REF-05", "DAF", null, "FRN-{000000}", "^FRN-[0-9]{4,6}$");
        creerFamilleSiAbsente("FAM-TIERS-CLI", "Référentiel des Clients et Partenaires commerciaux",
                "REF-05", "DC", null, "CLI-{000000}", "^CLI-[0-9]{4,6}$");
        creerFamilleSiAbsente("FAM-IAM-PRF", "Référentiel des Profils fonctionnels et Habilitations",
                "REF-06", "DG", null, "PRF-{0000}", "^PRF-[A-Z0-9_]{3,25}$");
    }

    private void creerFamilleSiAbsente(
            String code, String libelle, String sousDomaine, String proprietaireMetier,
            String codeParent, String masque, String regex) {
        if (!familleRepository.existsByCodeIgnoreCase(code)) {
            FamilleReferentielEntity f = FamilleReferentielEntity.builder()
                    .code(code)
                    .libelle(libelle)
                    .sousDomaine(sousDomaine)
                    .proprietaireMetier(proprietaireMetier)
                    .gestionnaireTechnique("CELLULE_INFORMATIQUE")
                    .codeFamilleParente(codeParent)
                    .masqueCodification(masque)
                    .regexValidationCode(regex)
                    .compteurCourant(0L)
                    .versionNomencleéature(1)
                    .dateEffet(LocalDate.of(2026, 1, 1))
                    .statutCycleVie(StatutCycleVieReferentiel.ACTIF)
                    .build();
            familleRepository.save(f);
        }
    }

    private void seedFichesUnitesOperationnelles() {
        creerFicheUniteSiAbsente("RFD", "Station d'Élevage de Dumbo (RFD)", "chef.rfd@sodepa.cm",
                "DPA (Supervision zootechnique et sanitaire), DAF (Suivi budgétaire et régie d'avances)",
                "CC-RFD-PASTORAL, CC-RFD-FOURRAGE", "MAG-RFD-INTRANTS, MAG-RFD-CARBURANT",
                "CAISSE-REGIE-RFD", "CBC-RFD-001");
        creerFicheUniteSiAbsente("RFJ", "Station d'Élevage de Jakiri (RFJ)", "chef.rfj@sodepa.cm",
                "DPA (Supervision zootechnique), DAF (Suivi financier)",
                "CC-RFJ-PASTORAL", "MAG-RFJ-INTRANTS", "CAISSE-REGIE-RFJ", "CBC-RFJ-001");
        creerFicheUniteSiAbsente("RFF", "Station d'Élevage du Faro (RFF)", "chef.rff@sodepa.cm",
                "DPA (Supervision zootechnique), DAF (Suivi financier)",
                "CC-RFF-PASTORAL", "MAG-RFF-INTRANTS", "CAISSE-REGIE-RFF", "CBC-RFF-001");
        creerFicheUniteSiAbsente("RFN", "Station d'Élevage de Ndokayo (RFN)", "chef.rfn@sodepa.cm",
                "DPA (Supervision zootechnique), DAF (Suivi financier)",
                "CC-RFN-PASTORAL", "MAG-RFN-INTRANTS", "CAISSE-REGIE-RFN", "CBC-RFN-001");
        creerFicheUniteSiAbsente("AIY", "Abattoir Industriel de Yaoundé (AIY)", "directeur.aiy@sodepa.cm",
                "DPA/DAIF (Exploitation industrielle et froid), DC (Commercialisation carcasses), DAF (Recettes d'abattage)",
                "CC-AIY-ABATTAGE, CC-AIY-FROID", "MAG-AIY-FRIGO-01, MAG-AIY-PIECES",
                "CAISSE-RECETTES-AIY, CAISSE-MENUES-DEPENSES-AIY", "AFRILAND-AIY-001");
        creerFicheUniteSiAbsente("AID", "Abattoir Industriel de Douala (AID)", "directeur.aid@sodepa.cm",
                "DPA/DAIF (Exploitation industrielle et froid), DC (Commercialisation), DAF (Recettes d'abattage)",
                "CC-AID-ABATTAGE, CC-AID-FROID", "MAG-AID-FRIGO-01",
                "CAISSE-RECETTES-AID, CAISSE-MENUES-DEPENSES-AID", "BICEC-AID-001");
        creerFicheUniteSiAbsente("AING", "Abattoir Industriel de Ngaoundéré (AING)", "directeur.aing@sodepa.cm",
                "DPA/DAIF (Exploitation industrielle et froid), DAF (Recettes et régie)",
                "CC-AING-ABATTAGE", "MAG-AING-FRIGO-01", "CAISSE-RECETTES-AING", "SCB-AING-001");
    }

    private void creerFicheUniteSiAbsente(
            String codeUnite, String intitule, String responsable, String rattachementsFonctionnels,
            String centresCouts, String magasins, String caisses, String comptesBancaires) {
        if (!evolutionStructureRepository.existsByCodeUniteIgnoreCase(codeUnite)) {
            EvolutionStructureOrganigrammeEntity evo = EvolutionStructureOrganigrammeEntity.builder()
                    .codeUnite(codeUnite)
                    .referenceTexteOrganique("Résolution n° 11/CA/SODEPA/2018")
                    .responsableUsername(responsable)
                    .rattachementsFonctionnels(rattachementsFonctionnels)
                    .centresCoutsRattaches(centresCouts)
                    .magasinsRattaches(magasins)
                    .caissesRattachees(caisses)
                    .comptesBancairesRattaches(comptesBancaires)
                    .typeEvolution("CONSTITUTION_INITIALE")
                    .nouvelIntitule(intitule)
                    .dateEffet(LocalDate.of(2018, 1, 1))
                    .actif(true)
                    .build();
            evolutionStructureRepository.save(evo);
        }
    }

    private void seedModelesDocumentsEtMasques() {
        creerModeleSiAbsent("MOD-BE", "Bon d'Entrée en Stock / Magasin", "BON_ENTREE", "BE-{UO}-{AAAA}-{00000}");
        creerModeleSiAbsent("MOD-BS", "Bon de Sortie de Stock / Magasin", "BON_SORTIE", "BS-{UO}-{AAAA}-{00000}");
        creerModeleSiAbsent("MOD-BC", "Bon de Commande SODEPA", "BON_COMMANDE", "BC-{UO}-{AAAA}-{00000}");
        creerModeleSiAbsent("MOD-FAC", "Facture Commerciale SODEPA", "FACTURE", "FAC-{UO}-{AAAA}-{00000}");
        creerModeleSiAbsent("MOD-CS", "Certificat Sanitaire & Inspection Vétérinaire", "CERTIFICAT_SANITAIRE", "CS-{UO}-{AAAA}-{00000}");
        creerModeleSiAbsent("MOD-OM", "Ordre de Mission Officiel SODEPA", "ORDRE_MISSION", "OM-{UO}-{AAAA}-{00000}");
    }

    private void creerModeleSiAbsent(String codeModele, String intitule, String typeDocument, String masque) {
        if (!modeleDocumentRepository.existsByCodeModeleIgnoreCase(codeModele)) {
            ModeleDocumentNumMasqueEntity m = ModeleDocumentNumMasqueEntity.builder()
                    .codeModele(codeModele)
                    .intitule(intitule)
                    .typeDocument(typeDocument)
                    .masqueNumerotation(masque)
                    .compteurCourant(0L)
                    .archivageGedAutomatique(true)
                    .actif(true)
                    .build();
            modeleDocumentRepository.save(m);
        }
    }
}
