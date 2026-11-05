package com.sodepa.erp.workflow.infrastructure.seeder;

import com.sodepa.erp.workflow.infrastructure.entities.*;
import com.sodepa.erp.workflow.infrastructure.repo.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeder initialisant au démarrage de l'application l'ensemble des 70 workflows
 * officiels de la SODEPA (minimum 10 workflows par structure), s'ils n'existent pas déjà en base.
 *
 * Structures couvertes :
 * 1. Direction Générale & Gouvernance (DG)
 * 2. Direction Administrative et Financière (DAF)
 * 3. Direction de la Production et de la Commercialisation (DPC - Siège)
 * 4. Division Contrôle, Audit Interne & Prospective (DCAI, DPD, QHSE)
 * 5. Ranchs Pastoraux (Faro, Ndokayo, Dumbo)
 * 6. Abattoirs Frigorifiques Industriels (Douala, Yaoundé, Ngaoundéré)
 * 7. Station d'Élevage et Laiterie Spécialisée de Jakiri
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class WorkflowSeeder implements CommandLineRunner {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    public record EtapeSeed(
            String codeEtape,
            String intitule,
            TypeEtapeWorkflow typeEtape,
            PolitiqueValidation politiqueValidation,
            StrategieAssignation strategieAssignation,
            String valeurAssignation,
            Integer delaiSlaHeures,
            ActionDepassementSla actionDepassementSla
    ) {}

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Démarrage de la vérification et initialisation des workflows SODEPA...");

        if (workflowDefinitionRepository.count() > 0) {
            log.info("Des workflows existent déjà en base. Aucune action de création n'est nécessaire.");
            return;
        }
        seedGouvernanceWorkflows();
        seedDafWorkflows();
        seedDpcWorkflows();
        seedAuditProspectiveWorkflows();
        seedRanchsWorkflows();
        seedAbattoirsWorkflows();
        seedJakiriWorkflows();

        log.info("Vérification des workflows SODEPA terminée avec succès.");
    }

    private void seedWorkflowIfNotExists(
            String code,
            String intitule,
            String description,
            WorkflowDomaine domaine,
            List<EtapeSeed> etapes
    ) {
        if (workflowDefinitionRepository.existsByCode(code)) {
            return;
        }

        UUID wfId = UUID.randomUUID();
        WorkflowDefinitionEntity def = WorkflowDefinitionEntity.builder()
                .id(wfId)
                .code(code)
                .intitule(intitule)
                .description(description)
                .domaine(domaine)
                .version(1)
                .actif(true)
                .etapes(new ArrayList<>())
                .dateCreation(Instant.now())
                .build();

        int ordre = 1;
        for (EtapeSeed e : etapes) {
            WorkflowEtapeDefinitionEntity etape = WorkflowEtapeDefinitionEntity.builder()
                    .id(UUID.randomUUID())
                    .workflowDefinition(def)
                    .ordre(ordre++)
                    .codeEtape(e.codeEtape())
                    .intitule(e.intitule())
                    .typeEtape(e.typeEtape())
                    .politiqueValidation(e.politiqueValidation() != null ? e.politiqueValidation() : PolitiqueValidation.UN_SEUL_SUFFIT)
                    .strategieAssignation(e.strategieAssignation())
                    .valeurAssignation(e.valeurAssignation())
                    .delaiSlaHeures(e.delaiSlaHeures())
                    .actionDepassementSla(e.actionDepassementSla() != null ? e.actionDepassementSla() : ActionDepassementSla.RELANCE_EMAIL)
                    .build();
            def.addEtape(etape);
        }

        workflowDefinitionRepository.save(def);
        log.info("Workflow créé : [{}] - {} ({} étapes)", code, intitule, etapes.size());
    }

    // =========================================================================
    // 1. DIRECTION GÉNÉRALE & GOUVERNANCE (10 Workflows)
    // =========================================================================
    private void seedGouvernanceWorkflows() {
        seedWorkflowIfNotExists(
                "WF-DG-01",
                "Arrêté et publication du Plan Stratégique & Budget Annuel",
                "Circuit de gouvernance pour l'approbation institutionnelle du plan stratégique et budget annuel",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_01_ARBITRAGE", "Arbitrage de la Direction Générale", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_01_CA", "Adoption par le Conseil d'Administration", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.MAJORITE, StrategieAssignation.ROLE_KEYCLOAK, "PCA", 72, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-02",
                "Validation des Conventions de Partenariat & Accords-Cadres",
                "Approbation des protocoles d'accord avec les ministères, bailleurs et coopératives d'élevage",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_02_AVIS_TECH", "Avis technique & juridique conjoint DPC/DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.TOUS_DOIVENT_VALIDER, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_02_SIGNATURE", "Signature formelle du Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-03",
                "Délégation de Pouvoirs et de Signature",
                "Formalisation des actes de délégation de signature avec plafonds financiers et limites temporelles",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_03_CONFORMITE", "Contrôle de conformité statutaire par la DCAI", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_03_NOTIFICATION", "Signature de l'acte et notification par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-04",
                "Autorisation Spéciale de Dépassement Budgétaire",
                "Procédure d'exception pour déblocage de crédits d'urgence au-delà des plafonds initiaux",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_04_EVAL_BUDGET", "Évaluation d'impact budgétaire par le Service Budget", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_BUDGET", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_04_ACCORD_DG", "Accord formel et déblocage exceptionnel par le DG", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-05",
                "Mandats Sanitaires et Campagnes d'Urgence Épizootique",
                "Déclenchement du protocole d'urgence en cas de foyer infectieux animal (PPCB, charbon, fièvre aphteuse)",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_05_ALERTE_DPC", "Alerte sanitaire et rapport de cadrage DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("DG_05_ARRETE_DG", "Arrêté d'urgence de la Direction Générale et notification MINEPIA", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-06",
                "Arbitrage des Tarifs Officiels de Cession (Viande & Bétail)",
                "Homologation de la grille tarifaire nationale de vente de viande et d'animaux sur pied",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_06_ETUDE_COUTS", "Étude de rentabilité des coûts de production DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_06_HOMOLOGATION", "Homologation et publication officielle par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-07",
                "Nomination et Affectation des Responsables d'Unités",
                "Affectation des Directeurs d'Abattoirs, Chefs de Ranchs et Chefs de Stations de la SODEPA",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_07_INSTRUCTION_RH", "Instruction du dossier de compétences par la SDRH", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_RH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_07_DECISION_DG", "Décision d'affectation signée par le Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-08",
                "Approbation des Rapports Trimestriels d'Activité pour le Conseil",
                "Consolidation et transmission du rapport trimestriel aux membres du Conseil d'Administration",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_08_REVUE_DIR", "Revue et visa du collège des directeurs centraux", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.TOUS_DOIVENT_VALIDER, StrategieAssignation.ROLE_KEYCLOAK, "DIRECTEUR_CENTRAL", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_08_TRANSMISSION", "Validation finale et transmission par le DG au CA", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-09",
                "Gestion des Contentieux Juridiques et Transactions Amiables",
                "Règlement amiable ou judiciaire des litiges commerciaux, fonciers ou sociaux majeurs",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_09_ANALYSE_RISQUE", "Analyse juridique et chiffrage des provisions par la DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_09_PROTOCOLE_DG", "Autorisation du protocole transactionnel par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-10",
                "Autorisation de Vente ou Réforme d'Actifs Lourds",
                "Mise au rebut, vente aux enchères ou cession de tracteurs, groupes frigorifiques et véhicules",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_10_COMMISSION", "Procès-verbal de la commission technique de réforme", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PATRIMOINE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_10_VISA_DAF", "Visa financier et valorisation comptable DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_10_APPROB_DG", "Approbation définitive de sortie d'actif par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    // =========================================================================
    // 2. DIRECTION ADMINISTRATIVE ET FINANCIÈRE (10 Workflows)
    // =========================================================================
    private void seedDafWorkflows() {
        seedWorkflowIfNotExists(
                "WF-DAF-01",
                "Passation de Commande Publique (Seuils Achat Direct / BC / CIPM)",
                "Contrôle budgétaire, visa financier et passation des marchés et commandes d'acquisitions",
                WorkflowDomaine.ACHATS,
                List.of(
                        new EtapeSeed("DAF_01_DISPO_BUDGET", "Contrôle automatique de disponibilité de crédit", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_BUDGET", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_01_VISA_CF", "Visa de conformité du Contrôle Financier", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CONTROLEUR_FINANCIER", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_01_ENGAGEMENT", "Signature du bon de commande ou lettre-commande par le DAF", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-02",
                "Traitement et Clôture Mensuelle de la Paie",
                "Calcul de la paie indiciaire, contrôle des primes/retenues et virement des salaires du personnel",
                WorkflowDomaine.RH,
                List.of(
                        new EtapeSeed("DAF_02_POINTAGE", "Contrôle des variables et pointage par la SDRH", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_RH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_02_ORDONNANCEMENT", "Ordonnancement bancaire et mandatement par le DAF", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-03",
                "Approvisionnement d'une Régie d'Avances Régionale",
                "Demande d'approvisionnement en fonds de caisse pour les dépenses courantes des abattoirs et ranchs",
                WorkflowDomaine.FINANCE,
                List.of(
                        new EtapeSeed("DAF_03_VERIF_TRESOR", "Vérification de l'épuisement de l'encours par la Trésorerie", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_FINANCIER", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_03_VIREMENT_REGIE", "Ordonnancement du virement bancaire par le DAF", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-04",
                "Régularisation et Contrôle des Pièces de Dépense de Régie",
                "Contrôle des factures et reçus de régie d'avances pour imputation en charges SYSCOHADA",
                WorkflowDomaine.COMPTABILITE,
                List.of(
                        new EtapeSeed("DAF_04_CONTROLE_PIECES", "Pointage comptable des reçus par le Service Comptable", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMPTABLE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_04_QUITUS", "Quitus de reconstitution d'enveloppe par le SDFBM", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_FINANCES", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-05",
                "Liquidation et Ordonnancement de Facture Fournisseur",
                "Traitement de la facture définitive après réception conforme des biens ou travaux",
                WorkflowDomaine.COMPTABILITE,
                List.of(
                        new EtapeSeed("DAF_05_SERVICE_FAIT", "Attestation de service fait et conformité PV de réception", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_MARCHES", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_05_IMPUTATION", "Imputation comptable et visa fiscal retenue à la source", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMPTABLE", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_05_BON_A_PAYER", "Émission du bon à payer par le Directeur DAF", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-06",
                "Réallocation d'Enveloppes Budgétaires entre Lignes",
                "Transfert de crédits d'un poste budgétaire excédentaire vers un poste déficitaire",
                WorkflowDomaine.BUDGET,
                List.of(
                        new EtapeSeed("DAF_06_SOLDE_SOURCE", "Contrôle automatique du solde disponible poste source", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_BUDGET", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_06_VALIDATION", "Validation du virement de crédits par le DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-07",
                "Recrutement et Intégration d'un Salarié (CDI/CDD)",
                "Dossier d'embauche, classification indiciaire, immatriculation CNPS et contrat de travail",
                WorkflowDomaine.RH,
                List.of(
                        new EtapeSeed("DAF_07_INSTRUCTION", "Instruction du dossier et calcul de la rémunération SDRH", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_RH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_07_IMPACT_BUDGET", "Validation de la soutenabilité budgétaire DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_07_CONTRAT_DG", "Signature définitive du contrat par le Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-08",
                "Validation et Règlement des Droits à Congés Payés",
                "Planification, arbitrage des départs en congés et décompte des indemnités associées",
                WorkflowDomaine.RH,
                List.of(
                        new EtapeSeed("DAF_08_AVIS_N1", "Avis d'opportunité du supérieur hiérarchique direct N+1", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.MANAGER_N1, "SUPERVISEUR_DIRECT", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_08_DECOMPTE_RH", "Décompte des jours acquis et accord officiel SDRH", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_RH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-09",
                "Clôture d'Exercice Comptable et Établissement de la DSF",
                "Arrêté des comptes annuels, inventaires et production de la déclaration statistique et fiscale",
                WorkflowDomaine.COMPTABILITE,
                List.of(
                        new EtapeSeed("DAF_09_BALANCE_INV", "Revue de balance après inventaire et provisions", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMPTABLE", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_09_ETATS_DAF", "Validation des états financiers de synthèse par le DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_09_CERTIF_DG", "Certification institutionnelle par le DG et transmission CAC", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-10",
                "Rapprochement Bancaire et Apurement des Suspends",
                "Pointage mensuel des extraits de compte et apurement des écarts de trésorerie",
                WorkflowDomaine.FINANCE,
                List.of(
                        new EtapeSeed("DAF_10_POINTAGE_AUTO", "Rapprochement automatique des flux bancaires", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_FINANCIER", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_10_ECRITURES_AJUST", "Validation des écritures d'ajustement par le Chef Comptable", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMPTABLE", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );
    }

    // =========================================================================
    // 3. DIRECTION PRODUCTION & COMMERCIALISATION (10 Workflows)
    // =========================================================================
    private void seedDpcWorkflows() {
        seedWorkflowIfNotExists(
                "WF-DPC-01",
                "Plan de Campagne Annuel de Déstockage & Vente des Ranchs",
                "Programmation des quotas de déstockage de bovins pour réguler la charge pastorale",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_01_SYNTHESE_RANCHS", "Agrégation des propositions zootechniques par la SDT", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_TECHNIQUE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_01_VALIDATION_DPC", "Adoption du calendrier des ventes par le Directeur DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-02",
                "Approbation du Programme Sanitaire Cheptel (Vaccinations / Déparasitages)",
                "Cadrage annuel des campagnes obligatoires de prophylaxie et commandes de vaccins MINEPIA",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_02_PROTOCOLE_VET", "Revue du protocole de vaccination par le Vétérinaire Central", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_TECHNIQUE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_02_ACCORD_DPC", "Approbation du programme sanitaire et logistique DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-03",
                "Agrément et Attribution de Quotas aux Grossistes / Bouchers",
                "Homologation des clients grossistes avec analyse de solvabilité et encours autorisés",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("DPC_03_SOLVABILITE", "Analyse des capacités de paiement par la SD Commerciale", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_COMMERCIAL", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_03_AGREMENT_DPC", "Attribution des quotas d'enlèvement par le Directeur DPC", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-04",
                "Plan d'Entretien Préventif des Équipements Frigorifiques",
                "Planification de la révision des compresseurs, tunnels et condenseurs des abattoirs",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_04_CHIFFRAGE_MAINT", "Chiffrage des pièces d'usure par la Brigade Maintenance", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_BRIGADE_MAINTENANCE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_04_VALIDATION_DPC", "Validation du plan d'arrêt technique par le Directeur DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-05",
                "Arbitrage et Répartition des Transferts Vifs (Ranchs vers Abattoirs)",
                "Convoiement ferroviaire ou par bétaillères des bœufs gras des ranchs vers Douala/Yaoundé",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("DPC_05_CERTIF_VET", "Vérification des certificats de salubrité de circulation", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_RANCHS", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_05_ORDRE_CONVOI", "Ordre officiel d'embarquement et de transport DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-06",
                "Campagne de Promotion Commerciale et Vente Promotionnelle de Viande",
                "Organisation des ventes de bétail et de viande à prix modérés lors des périodes de fêtes",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("DPC_06_PROPOSITION_MKT", "Montage commercial par le Service Ventes & Marketing", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_COMMERCIAL", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_06_ACCORD_MARGES", "Accord sur les marges résiduelles par la DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_06_LANCEMENT_DPC", "Autorisation de mise en vente promotionnelle DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-07",
                "Gestion des Réclamations Clients sur la Qualité de Viande Livrée",
                "Instruction des non-conformités de carcasses ou de découpe signalées par les distributeurs",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("DPC_07_EXPERTISE", "Expertise technique du lot par le Service Transformation", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_TRANSFORMATION", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_07_AVOIR_DPC", "Arbitrage et émission de l'avoir commercial par le DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-08",
                "Acquisition de Reproducteurs Améliorateurs (Géniteurs Gudali, Brahman)",
                "Achat et introduction de taureaux de race pure pour préserver la vigueur hybride",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_08_EXAMEN_ZOOTECH", "Examen phénotypique et généalogique par la SD Technique", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_TECHNIQUE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_08_ACCORD_INVEST", "Validation de l'investissement zootechnique DPC/DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-09",
                "Plan d'Aménagement Annuel des Parcours et Pare-feux Pastoraux",
                "Stratégie de brûlis dirigés, fauche mécanique et protection contre les feux de brousse tardifs",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_09_ETUDE_PATURAGE", "Étude biomasse par le Service Aménagement & Pâturage", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PATURAGE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_09_VALIDATION_DPC", "Adoption du calendrier des feux contrôlés DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-10",
                "Audit de Rendement des Chaînes d'Abattage et Ratio Cinquième Quartier",
                "Analyse des performances industrielles : rendement carcasse, saisies et valorisation abats",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_10_CALCUL_RATIOS", "Contrôle automatique des ratios de carcasse", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_TRANSFORMATION", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_10_RAPPORT_DPC", "Validation du rapport de performance industrielle DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );
    }

    // =========================================================================
    // 4. DCAI / DPD / QHSE (10 Workflows)
    // =========================================================================
    private void seedAuditProspectiveWorkflows() {
        seedWorkflowIfNotExists(
                "WF-DCAI-01",
                "Mission d'Audit Inopiné sur Site (Ranchs ou Abattoirs)",
                "Déclenchement d'une mission d'inspection comptable, financière ou zootechnique sur site",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_01_CADRAGE", "Cadrage de la mission d'inspection par la DCAI", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DCAI_01_ORDRE_DG", "Ordre de mission d'audit signé par le Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DCAI-02",
                "Traitement d'une Alerte Fraude ou Écart d'Inventaire Cheptel",
                "Instruction contradictoire en cas de suspicion de détournement ou mortalité anormale non justifiée",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_02_INSTRUCTION", "Instruction contradictoire et recueil des preuves DCAI", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DCAI_02_SANCTIONS_DG", "Décision conservatoire ou saisine du conseil de discipline DG", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DCAI-03",
                "Revue et Mise à Jour de la Cartographie des Risques SODEPA",
                "Évaluation périodique des risques opérationnels, climatiques, épidémiologiques et financiers",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_03_CONSOLIDATION", "Consolidation des fiches de risques par l'Auditeur Interne", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DCAI_03_ADOPTION_DG", "Adoption de la cartographie par la Direction Générale", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DCAI-04",
                "Certification Sanitaire et Audit Environnemental QHSE des Sites",
                "Audit de conformité des abattoirs aux normes de traitement des effluents et rejets",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_04_AUDIT_TERRAIN", "Visite d'inspection terrain et prélèvements QHSE", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "ATTACHE_QHSE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DCAI_04_PLAN_ACTION", "Validation du plan de mise en conformité par la DCAI", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DCAI-05",
                "Gestion d'un Incident Grave de Travail ou Pollution Environnementale",
                "Prise en charge d'un accident grave sur une chaîne d'abattage ou pollution accidentelle d'un cours d'eau",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_05_CONSTAT_URG", "Constat immédiat et mesures conservatoires QHSE", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "ATTACHE_QHSE", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("DCAI_05_DECISION_DG", "Déclaration légale et prise en charge institutionnelle DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-06",
                "Programmation des Projets de Développement sur Financements Extérieurs",
                "Montage de dossiers de projets d'appui à la filière bétail-viande (BAD, Banque Mondiale, UE)",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DPD_06_MONTAGE", "Montage du cadre logique et chronogramme par la CSPP", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DPD", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_06_SOUMISSION_DG", "Validation pour transmission institutionnelle par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-07",
                "Recueil et Analyse des Données Statistiques Nationales de l'Élevage",
                "Consolidation des données mensuelles de production de viande, taux de vêlage et cours du bétail",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPD_07_CONTROLE_DONNEES", "Contrôle automatique de cohérence des données chiffrées", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_STATISTIQUE", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_07_DIFFUSION", "Validation du tableau de bord statistique par la DPD", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DPD", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-08",
                "Gestion des Droits d'Accès, Rôles et Sécurité dans l'ERP SODEPA",
                "Attribution ou modification des profils d'habilitation dans le système d'information",
                WorkflowDomaine.GED,
                List.of(
                        new EtapeSeed("DPD_08_ACCORD_HIERARCHIE", "Visa du responsable de structure du collaborateur", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.MANAGER_N1, "SUPERVISEUR_DIRECT", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_08_APPLICATION_SI", "Paramétrage des rôles et permissions par la Cellule Informatique", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_INFORMATIQUE", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-09",
                "Plan de Continuité d'Activité et Sauvegarde du Système d'Information",
                "Vérification de la résilience des serveurs centraux et synchronisation des bases locales hors-ligne",
                WorkflowDomaine.GED,
                List.of(
                        new EtapeSeed("DPD_09_TEST_RESTAUR", "Test automatique de restauration d'un dump de base de données", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_INFORMATIQUE", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_09_VISA_PCA", "Visa du rapport de sécurité informatique par la DPD", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DPD", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-10",
                "Évaluation de Fin de Projet et Clôture Technique",
                "Recette finale des investissements agropastoraux ou informatiques et transfert opérationnel",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DPD_10_RECETTE", "Procès-verbal de recette technique par l'équipe projet", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_PROJETS", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_10_QUITUS_DG", "Quitus de clôture de projet signé par le Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    // =========================================================================
    // 5. RANCHS PASTORAUX (10 Workflows)
    // =========================================================================
    private void seedRanchsWorkflows() {
        seedWorkflowIfNotExists(
                "WF-RANCH-01",
                "Déclaration et Enregistrement des Naissances de Veaux",
                "Pose du numéro de boucle auriculaire, identification généalogique et entrée dans l'actif cheptel",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_01_CONSTAT", "Constat zootechnique de naissance par le Chef de Brigade", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_01_BOUCLAGE", "Validation du bouclage par le Directeur du Ranch", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-02",
                "Constat et Enregistrement de Mort Subite ou Maladie du Cheptel",
                "PV autopsie vétérinaire, mesures de biosécurité et sortie d'actif animal pour mortalité",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_02_AUTOPSIE", "Autopsie vétérinaire et détermination de la cause du décès", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_RANCH", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("RANCH_02_SORTIE_ACTIF", "Validation du PV d'abattage d'urgence ou de perte par le Directeur", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-03",
                "Inventaire Annuel / Fiche de Composition du Troupeau (Herd Composition)",
                "Rapprochement contradictoire physique du cheptel par catégorie zootechnique",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_03_COMPTAGE", "Pointage physique des troupeaux au couloir de contention", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_RANCH", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_03_ARRETE_RANCH", "Validation de la fiche de composition par le Directeur du Ranch", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-04",
                "Pesée Périodique et Suivi du Gain Moyen Quotidien (GMQ)",
                "Évaluation de la prise de poids des animaux en embouche et suivi de la croissance",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_04_SAISIE_PESEE", "Saisie automatique des pesées individuelles bascule", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "ZOOTECHNICIEN_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_04_REVUE_GMQ", "Validation de la fiche d'embouche par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-05",
                "Organisation d'une Foire Pastorale de Vente sur Pied",
                "Sélection des lots de bœufs gras et taureaux de réforme pour mise en adjudication",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("RANCH_05_LOTISSEMENT", "Constitution des lots et fixation des prix de réserve", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMMERCIAL_RANCH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_05_OUVERTURE", "Autorisation d'ouverture de la foire par le Directeur du Ranch", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-06",
                "Cession et Vente d'un Lot d'Animaux sur Pied",
                "Encaissement de la vente, certificat sanitaire de circulation et bon de sortie du troupeau",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("RANCH_06_ENCAISSEMENT", "Contrôle automatique de la quittance d'encaissement régie", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CAISSIER_REGISSEUR_RANCH", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_06_LAISSEZ_PASSER", "Délivrance du certificat sanitaire de circulation vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_RANCH", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("RANCH_06_BON_SORTIE", "Visa du bon de sortie définitive par le Directeur du Ranch", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-07",
                "Traitement Antiparasitaire Massif (Dipping / Pulvérisation au Couloir)",
                "Passage obligatoire au bain acaricide pour éradiquer les tiques et vecteurs hémoparasitaires",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_07_PREPARATION", "Contrôle du dosage et concentration du bain de dipping", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_07_POINTAGE", "Pointage des effectifs traités par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-08",
                "Rotation des Pâturages et Transhumance Intra-Ranch",
                "Mouvement planifié des bêtes pour laisser reposer les parcelles et préserver les sols",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_08_ETUDE_CHARGE", "Évaluation de la charge pastorale par le Service Pâturage", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PATURAGE_RANCH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_08_ORDRE_MOUV", "Ordre officiel de mouvement par le Directeur du Ranch", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-09",
                "Maintenance Lourde des Clôtures, Pistes et Points d'Eau",
                "Réparation des kilomètres de clôture périphérique, abreuvoirs et retenues collinaires",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_09_DEVIS_TRAVAUX", "Constat des brèches et chiffrage matériel par la Maintenance", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "RESPONSABLE_MAINTENANCE_RANCH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_09_ACCORD_TRAVAUX", "Validation et engagement des travaux par le Directeur du Ranch", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-10",
                "Gestion des Conflits Agropastoraux et Dégâts de Champs Voisins",
                "Constat contradictoire et indemnisation amiable des riverains en cas d'intrusion d'animaux",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_10_CONSTAT_COMM", "Constat conjoint avec les autorités traditionnelles et plaignants", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_ADMINISTRATIF_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_10_REGLEMENT", "Accord amiable et décharge signée par le Directeur du Ranch", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    // =========================================================================
    // 6. ABATTOIRS FRIGORIFIQUES INDUSTRIELS (10 Workflows)
    // =========================================================================
    private void seedAbattoirsWorkflows() {
        seedWorkflowIfNotExists(
                "WF-ABAT-01",
                "Réception et Inspection Ante-Mortem du Bétail au Foirail",
                "Examen clinique des bovins vivants avant admission au parc d'attente d'abattage",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_01_EXAMEN_CLINIQUE", "Examen clinique vétérinaire ante-mortem", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_INSPECTEUR_ABATTOIR", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_01_ADMISSION_PARC", "Admission en parc de stabulation par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_ABATTOIR", 12, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-02",
                "Ordonnancement et Séquencement de la Chaîne d'Abattage",
                "Planification quotidienne du flux d'abattage selon les capacités des chaînes et du ressuage",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_02_PLANNING_JOUR", "Ordonnancement automatique selon les lots enregistrés", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_ABATTOIR", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_02_ORDRE_ABATTAGE", "Ordre d'abattage validé par le Directeur de l'Abattoir", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_ABATTOIR", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-03",
                "Inspection Post-Mortem, Estampillage et Classement des Carcasses",
                "Inspection systématique des organes internes, estampille sanitaire de salubrité et calibrage",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_03_INSPECTION_ORG", "Inspection des carcasses et viscères par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_INSPECTEUR_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_03_ESTAMPILLE", "Apposition de l'estampille de salubrité par la cellule HQSE", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_HQSE_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-04",
                "Traitement et Destruction des Saisies Sanitaires Vétérinaires",
                "Retrait de consommation humaine des viandes impropres et destruction sous contrôle vétérinaire",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_04_MOTIF_SAISIE", "Notification du motif légal de saisie par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_INSPECTEUR_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_04_PV_DESTRUCTION", "PV de dénaturation ou incinération par le Directeur de l'Abattoir", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_ABATTOIR", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-05",
                "Pesée Fiscale et Facturation de la Redevance d'Abattage",
                "Pesée électronique sur pont-rail, émission de la quittance et encaissement des droits",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_05_PESEE_PONT", "Enregistrement de la pesée sur rail aérien", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "AGENT_BASCULE_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_05_FACTURATION", "Émission de la facture et encaissement par la régie", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CAISSIER_REGISSEUR_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-06",
                "Gestion de la Chaîne de Froid et Admission en Chambres Frigorifiques",
                "Contrôle de la descente en température et stockage frigorifique régulé",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_06_RELEVE_TEMP", "Contrôle automatique des sondes de température", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "TECHNICIEN_FRIGORISTE_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_06_RANGEMENT", "Affectation de l'emplacement froid par le Magasinier Frigoriste", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "MAGASINIER_FRIGORISTE_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-07",
                "Déstockage et Sortie des Carcasses pour Livraison ou Vente",
                "Contrôle sanitaire de sortie et remise des viandes aux véhicules frigorifiques",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_07_VISITE_SORTIE", "Contrôle de salubrité de sortie par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_INSPECTEUR_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_07_BON_LIVRAISON", "Délivrance du bon d'enlèvement par le Service Commercial", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMMERCIAL_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-08",
                "Conditionnement et Vente du 5ème Quartier (Abats, Cuirs et Peaux)",
                "Traitement en triperie, salage des peaux de bœuf et vente aux grossistes et tanneurs",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_08_PESEE_CUIRS", "Pesée des peaux salées et tri des abats", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "RESPONSABLE_TRIPERIE_ABATTOIR", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_08_VENTE_5EME", "Facturation de cession par le Service Commercial", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMMERCIAL_ABATTOIR", 12, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-09",
                "Nettoyage, Désinfection Quotidienne et Contrôle Microbiologique",
                "Plan de nettoyage nocturne des sols, scies, rails et crochets selon protocole HACCP",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_09_DESINFECTION", "Exécution du nettoyage et désinfection par l'équipe hygiène", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "RESPONSABLE_HYGIENE_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_09_LIBERATION", "Contrôle microbiologique et libération de salle par la cellule HQSE", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_HQSE_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-10",
                "Maintenance d'Urgence des Chaînes Frigorifiques et Groupes Électrogènes",
                "Intervention immédiate en cas de coupure de courant ENEO ou panne de compresseur",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_10_BASCULE_GEN", "Contrôle automatique du démarrage des groupes électrogènes", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_MAINTENANCE_ABATTOIR", 2, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_10_ORDRE_INTERV", "Ordre d'intervention d'urgence validé par le Directeur", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_ABATTOIR", 2, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    // =========================================================================
    // 7. STATION D'ÉLEVAGE ET LAITERIE DE JAKIRI (10 Workflows)
    // =========================================================================
    private void seedJakiriWorkflows() {
        seedWorkflowIfNotExists(
                "WF-JAK-01",
                "Collecte Matinale du Lait et Contrôle Qualité à la Réception",
                "Test d'acidité, densité et alcool à la réception du lait frais pour acceptation ou refus",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_01_TESTS_LABO", "Contrôle des paramètres physico-chimiques du lait", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "TECHNICIEN_LABO_LAITIER", 4, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_01_ACCEPTATION", "Acceptation en cuve isotherme par le Chef de Laiterie", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_LAITERIE_JAKIRI", 4, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-02",
                "Transformation Industrielle Laitière (Pasteurisation, Yaourts, Fromages)",
                "Lancement d'un lot de production, ensemencement des ferments et étiquetage DLC",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_02_LANCEMENT_PROD", "Validation de la fiche de brassage et pasteurisation", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_ATELIER_TRANSFORMATION", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_02_CONFORME_LOT", "Validation de la conformité du lot transformé par le Chef de Station", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_STATION_JAKIRI", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-03",
                "Prélèvement, Conditionnement et Cryoconservation de Semence Bovine",
                "Production de paillettes de semence bovine congelée à -196°C pour l'insémination artificielle",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_03_CONTROLE_SPERME", "Examen microscopique de motilité par le Biotechnologiste", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "BIOTECHNOLOGISTE_REPRODUCTION", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_03_PAILLETTES", "Conditionnement en azote liquide visé par le Vétérinaire", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_JAKIRI", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-04",
                "Demande et Réalisation d'un Protocole d'Insémination Artificielle (IA)",
                "Planification de l'acte d'insémination sur vache en chaleur avec traçabilité du donneur",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_04_DETECTION_CHALEUR", "Constat de chaleur et choix de la paillette de race pure", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "INSEMINATEUR_STATION", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("JAK_04_ENREG_ACTE", "Validation de l'acte d'insémination par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_JAKIRI", 12, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-05",
                "Diagnostic de Gestation par Échographie / Palpation Rectale",
                "Contrôle de réussite de saillie à J+45 pour mise à jour du calendrier de mise-bas",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_05_EXAMEN_ECHO", "Diagnostic échographique par le Médecin Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_JAKIRI", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_05_CALENDRIER", "Mise à jour du calendrier de vêlage par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_JAKIRI", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-06",
                "Commercialisation des Produits Laitiers de Jakiri (Boutiques & Dépôts)",
                "Facturation, gestion des stocks et expédition des yaourts, laits pasteurisés et fromages",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("JAK_06_COMMANDE", "Enregistrement automatique du bon de commande boutique", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "COMMERCIAL_LAITERIE_JAKIRI", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_06_ENCAISSEMENT", "Validation de l'encaissement et bon de livraison par la régie", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CAISSIER_REGISSEUR_JAKIRI", 12, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-07",
                "Approvisionnement et Gestion du Stock d'Azote Liquide et Consommables IA",
                "Réapprovisionnement des bonbonnes cryogéniques pour sécuriser les semences bovines",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_07_NIVEAU_CUVE", "Vérification automatique du niveau critique d'azote liquide", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "RESPONSABLE_LABORATOIRE_JAKIRI", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("JAK_07_COMMANDE_URG", "Ordre d'approvisionnement express signé par le Chef de Station", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_STATION_JAKIRI", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-08",
                "Programme Alimentaire Spécifique du Troupeau Laitier (Compléments & Ensilage)",
                "Ajustement des rations énergétiques et protéiques des vaches laitières selon le pic de lactation",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_08_CALCUL_RATION", "Calcul nutritionnel des rations par le Nutritionniste", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "NUTRITIONNISTE_ANIMAL_JAKIRI", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_08_SORTIE_STOCK", "Autorisation de sortie d'aliments concentrés par le Chef Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_JAKIRI", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-09",
                "Diffusion de Géniteurs Améliorateurs auprès des Éleveurs Encadrés",
                "Mise à disposition de jeunes taureaux laitiers améliorés pour les coopératives de la région",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_09_SELECTION_VET", "Certificat d'aptitude génétique et sanitaire par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_JAKIRI", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_09_ACCORD_CESSION", "Accord de cession et contrat d'encadrement Chef de Station", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_STATION_JAKIRI", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-10",
                "Accueil et Encadrement des Stagiaires d'Écoles Vétérinaires / Agronomiques",
                "Immersion pratique des élèves ingénieurs agronomes et docteurs vétérinaires sur la station",
                WorkflowDomaine.RH,
                List.of(
                        new EtapeSeed("JAK_10_PLAN_STAGE", "Validation du plan de stage par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_JAKIRI", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_10_ATTESTATION", "Signature de l'attestation d'immersion par le Chef de Station", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.UN_SEUL_SUFFIT, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_STATION_JAKIRI", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }
}
