package com.sodepa.erp.authorisation.infrastructure.seeder;

import com.sodepa.erp.authorisation.infrastructure.entities.ProfilFonctionnelEntity;
import com.sodepa.erp.authorisation.infrastructure.entities.RegleSodEntity;
import com.sodepa.erp.authorisation.infrastructure.repo.ProfilFonctionnelRepository;
import com.sodepa.erp.authorisation.infrastructure.repo.RegleSodRepository;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Seeder initialisant les 13 Profils Fonctionnels SODEPA (F-REF-025 / RG-REF-018)
 * et la Matrice de Séparation des Tâches (SoD — F-REF-027 / RG-REF-020 à 027).
 */
@Component
@Order(6)
@RequiredArgsConstructor
@Slf4j
public class Ref05Ref06Ref07Seeder implements CommandLineRunner {

    private final ProfilFonctionnelRepository profilRepository;
    private final RegleSodRepository regleSodRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedMatriceSod();
        seedProfilsFonctionnelsSodepa();
    }

    private void seedMatriceSod() {
        if (regleSodRepository.count() == 0) {
            regleSodRepository.saveAll(List.of(
                    RegleSodEntity.builder().codeRegle("SOD-REF-001")
                            .libelle("Incompatibilité Création vs Validation d'un Tiers (Comptabilité)")
                            .typeIncompatibilite("PERMISSION_VS_PERMISSION")
                            .elementA(Permissions.INIT_CREATE_TIERS.name())
                            .elementB(Permissions.VALIDATE_OR_REJECT_TIERS.name())
                            .regleGestionRef("RG-REF-020")
                            .risqueMetier("Création et validation d'un tiers fictif par le même agent")
                            .bloquant(true).actif(true).build(),
                    RegleSodEntity.builder().codeRegle("SOD-REF-002")
                            .libelle("Incompatibilité Création vs Validation d'un Fournisseur (Achats)")
                            .typeIncompatibilite("PERMISSION_VS_PERMISSION")
                            .elementA(Permissions.CREATE_PROVIDER.name())
                            .elementB(Permissions.VALIDATE_PROVIDER.name())
                            .regleGestionRef("RG-REF-020")
                            .risqueMetier("Agrément frauduleux d'un fournisseur sans contrôle contradictoire")
                            .bloquant(true).actif(true).build(),
                    RegleSodEntity.builder().codeRegle("SOD-REF-003")
                            .libelle("Incompatibilité Préparation Liquidation vs Validation Liquidation / Paiement")
                            .typeIncompatibilite("PERMISSION_VS_PERMISSION")
                            .elementA(Permissions.LIQUIDATE_INVOICE.name())
                            .elementB(Permissions.VALIDATE_LIQUIDATION.name())
                            .regleGestionRef("RG-REF-021")
                            .risqueMetier("Un agent préparant une liquidation ou un paiement ne peut le valider seul")
                            .bloquant(true).actif(true).build(),
                    RegleSodEntity.builder().codeRegle("SOD-REF-004")
                            .libelle("Incompatibilité Saisie vs Validation d'Écriture Comptable")
                            .typeIncompatibilite("PERMISSION_VS_PERMISSION")
                            .elementA(Permissions.INIT_CREATE_ECRITURE.name())
                            .elementB(Permissions.VALIDATE_OR_REJECT_ECRITURE.name())
                            .regleGestionRef("RG-REF-019")
                            .risqueMetier("Imputation comptable sans revue du Chef de Service Comptable ou DAF")
                            .bloquant(true).actif(true).build(),
                    RegleSodEntity.builder().codeRegle("SOD-REF-005")
                            .libelle("Séparation Administration Technique (Cellule Informatique) vs Validation Métier")
                            .typeIncompatibilite("PROFIL_VS_PROFIL")
                            .elementA("PROFIL_ADMIN_TECHNIQUE_CI")
                            .elementB("PROFIL_DAF_VALIDATION")
                            .regleGestionRef("RG-REF-027")
                            .risqueMetier("La Cellule Informatique administre la plateforme mais ne validée jamais le contenu métier")
                            .bloquant(true).actif(true).build(),
                    RegleSodEntity.builder().codeRegle("SOD-REF-006")
                            .libelle("Incompatibilité Gestionnaire de Stock vs Validateur d'Écarts d'Inventaire")
                            .typeIncompatibilite("PROFIL_VS_PROFIL")
                            .elementA("PROFIL_GESTIONNAIRE_STOCK")
                            .elementB("PROFIL_CONTROLEUR_INVENTAIRE")
                            .regleGestionRef("RG-REF-022")
                            .risqueMetier("Un gestionnaire de stock ne peut valider seul ses écarts d'inventaire")
                            .bloquant(true).actif(true).build()
            ));
            log.info("Matrice de Séparation des Tâches (SoD) initialisée (CA-REF-027-01 & 02).");
        }
    }

    private void seedProfilsFonctionnelsSodepa() {
        if (profilRepository.count() == 0) {
            profilRepository.saveAll(List.of(
                    ProfilFonctionnelEntity.builder().code("PROFIL_DG").libelle("Directeur Général (Ordonnateur Principal)")
                            .description("Validation stratégique, approbation finale des marchés, budgets et décisions")
                            .plafondEngagementFcfa(null).mfaRequis(true)
                            .permissions(Set.of(Permissions.VALIDATE_BUDGET, Permissions.VALIDATE_FOLDER, Permissions.VALIDATE_OR_REJECT_ORGANIGRAMME))
                            .actif(true).build(),
                    ProfilFonctionnelEntity.builder().code("PROFIL_DAF_VALIDATION").libelle("Directeur Administratif et Financier (DAF)")
                            .description("Supervision comptable, financière, fiscale et validation des référentiels financiers")
                            .plafondEngagementFcfa(new BigDecimal("50000000")).mfaRequis(true)
                            .permissions(Set.of(Permissions.VALIDATE_OR_REJECT_COMPTE, Permissions.VALIDATE_OR_REJECT_TIERS,
                                    Permissions.VALIDATE_OR_REJECT_ECRITURE, Permissions.VALIDATE_LIQUIDATION))
                            .actif(true).build(),
                    ProfilFonctionnelEntity.builder().code("PROFIL_COMPTABLE_SAISIE").libelle("Comptable (Saisie & Préparation)")
                            .description("Préparation des comptes, tiers et écritures comptables (soumis à validation)")
                            .plafondEngagementFcfa(new BigDecimal("5000000")).mfaRequis(false)
                            .permissions(Set.of(Permissions.INIT_CREATE_COMPTE, Permissions.INIT_CREATE_TIERS,
                                    Permissions.INIT_CREATE_ECRITURE, Permissions.VIEW_ECRITURE))
                            .actif(true).build(),
                    ProfilFonctionnelEntity.builder().code("PROFIL_CSFR").libelle("Chef Service Financier / Régisseur de Caisse")
                            .description("Gestion des caisses d'unité, menues dépenses et demandées d'approvisionnement")
                            .plafondEngagementFcfa(new BigDecimal("10000000")).mfaRequis(true)
                            .permissions(Set.of(Permissions.GET_FULL_BANK_INFO, Permissions.LIST_ALL_BANKS))
                            .actif(true).build(),
                    ProfilFonctionnelEntity.builder().code("PROFIL_GESTIONNAIRE_ACHATS").libelle("Gestionnaire des Achats & Marchés (Saisie)")
                            .description("Saisie des fournisseurs, expressions de besoins et dossiers de commande")
                            .plafondEngagementFcfa(new BigDecimal("15000000")).mfaRequis(false)
                            .permissions(Set.of(Permissions.CREATE_PROVIDER, Permissions.LIST_PROVIDERS,
                                    Permissions.CREATE_EXPRESSION, Permissions.CREATE_FOLDER))
                            .actif(true).build(),
                    ProfilFonctionnelEntity.builder().code("PROFIL_VALIDATEUR_ACHATS").libelle("Validateur des Achats & Fournisseurs")
                            .description("Validation contradictoire des fournisseurs, réceptions et dossiers d'achat")
                            .plafondEngagementFcfa(new BigDecimal("50000000")).mfaRequis(true)
                            .permissions(Set.of(Permissions.VALIDATE_PROVIDER, Permissions.VALIDATE_EXPRESSION,
                                    Permissions.VALIDATE_FOLDER, Permissions.VALIDATE_RECEPTION))
                            .actif(true).build(),
                    ProfilFonctionnelEntity.builder().code("PROFIL_GESTIONNAIRE_STOCK").libelle("Gestionnaire de Stock / Magasinier d'Unité")
                            .description("Entrées, sorties et suivi des stocks d'intrants et produits dans son magasin")
                            .plafondEngagementFcfa(new BigDecimal("1000000")).mfaRequis(false)
                            .permissions(Set.of(Permissions.LIST_RECEPTIONS, Permissions.CREATE_RECEPTION))
                            .actif(true).build(),
                    ProfilFonctionnelEntity.builder().code("PROFIL_CONTROLEUR_INVENTAIRE").libelle("Contrôleur de Gestion / Validateur d'Inventaire")
                            .description("Contrôle budgétaire, analytique et validation indépendante des écarts d'inventaire")
                            .plafondEngagementFcfa(new BigDecimal("25000000")).mfaRequis(true)
                            .permissions(Set.of(Permissions.VIEW_BUDGET, Permissions.VALIDATE_RECEPTION))
                            .actif(true).build(),
                    ProfilFonctionnelEntity.builder().code("PROFIL_ADMIN_TECHNIQUE_CI").libelle("Administrateur Technique — Cellule Informatique")
                            .description("Administration technique des comptes et paramètres sans droit de validation métier (RG-REF-027)")
                            .plafondEngagementFcfa(BigDecimal.ZERO).mfaRequis(true)
                            .permissions(Set.of(Permissions.INIT_CREATE_USER, Permissions.INIT_UPDATE_USER, Permissions.GET_FULL_USER_INFO))
                            .actif(true).build()
            ));
            log.info("Profils fonctionnels SODEPA initialisés (CA-REF-025-03 / RG-REF-018).");
        }
    }
}
