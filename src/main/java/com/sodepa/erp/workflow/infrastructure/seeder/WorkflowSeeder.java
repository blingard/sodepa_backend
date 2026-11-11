package com.sodepa.erp.workflow.infrastructure.seeder;

import com.sodepa.erp.workflow.infrastructure.entities.*;
import com.sodepa.erp.workflow.infrastructure.repo.WorkflowDefinitionRepository;
import com.sodepa.erp.workflow.infrastructure.repo.WorkflowMetierRepository;
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

    /** Référentiel pour la persistance et la vérification d'existence des définitions de workflow. */
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    /** Référentiel des opérations métiers universelles. */
    private final WorkflowMetierRepository workflowMetierRepository;

    /**
     * Enregistrement (Record) immuable modélisant les paramètres de configuration
     * d'une étape de workflow à initialiser dans la base de données.
     *
     * @param codeEtape              Code fonctionnel unique de l'étape au sein du circuit.
     * @param intitule               Libellé descriptif de l'étape affiché à l'écran.
     * @param typeEtape              Type d'étape (validation humaine, signature qualifiée, etc.).
     * @param politiqueValidation    Règle d'approbation (un seul suffit, tous doivent valider, etc.).
     * @param strategieAssignation   Stratégie de résolution de l'approbateur (poste, rôle, manager).
     * @param valeurAssignation      Valeur cible associée à la stratégie (ex: "DIRECTEUR_GENERAL").
     * @param delaiSlaHeures         Délai limite maximal alloué pour traiter cette étape en heures.
     * @param actionDepassementSla   Action automatique à déclencher si le délai SLA expire.
     */
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

    /**
     * Point d'entrée exécuté au démarrage de Spring Boot.
     * Déclenche séquentiellement l'initialisation des circuits pour chaque structure organisationnelle.
     *
     * @param args Arguments de la ligne de commande.
     */
    @Override
    @Transactional
    public void run(String... args) {
        log.info("Démarrage de la vérification et initialisation des workflows SODEPA...");

        seedMetiers();
        seedRhWorkflows();
        seedGouvernanceWorkflows();
        seedDafWorkflows();
        seedDpcWorkflows();
        seedAuditProspectiveWorkflows();
        seedRanchsWorkflows();
        seedAbattoirsWorkflows();
        seedJakiriWorkflows();

        log.info("Vérification des workflows SODEPA terminée avec succès.");
    }

    /**
     * Persiste une définition de workflow et l'ensemble de ses étapes ordonnées si le circuit
     * n'existe pas encore en base (mécanisme idempotent évitant les doublons).
     *
     * @param code        Code fonctionnel unique du circuit (ex: "WF-DG-01").
     * @param intitule    Intitulé lisible du circuit.
     * @param description Description des objectifs et des règles métier.
     * @param domaine     Domaine d'application fonctionnel.
     * @param etapes      Liste ordonnée des étapes à configurer.
     */
    private void seedWorkflowIfNotExists(
            String code,
            String intitule,
            String description,
            WorkflowDomaine domaine,
            List<EtapeSeed> etapes
    ) {
        seedWorkflowIfNotExists(code, intitule, description, domaine, null, null, etapes);
    }

    /**
     * Persiste une définition de workflow avec contrôle d'éligibilité Maker et acteurs d'étapes.
     */
    private void seedWorkflowIfNotExists(
            String code,
            String intitule,
            String description,
            WorkflowDomaine domaine,
            String rolesInitiateurs,
            String postesInitiateurs,
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
                .rolesInitiateurs(rolesInitiateurs)
                .postesInitiateurs(postesInitiateurs)
                .version(1)
                .actif(true)
                .etapes(new ArrayList<>())
                .dateCreation(Instant.now())
                .build();

        // Liaison automatique au métier correspondant dans le référentiel SODEPA
        resolveMetierForWorkflow(code, domaine).ifPresent(def::setMetier);

        int ordre = 1;
        for (EtapeSeed e : etapes) {
            WorkflowEtapeDefinitionEntity etape = WorkflowEtapeDefinitionEntity.builder()
                    .id(UUID.randomUUID())
                    .workflowDefinition(def)
                    .ordre(ordre++)
                    .codeEtape(e.codeEtape())
                    .intitule(e.intitule())
                    .typeEtape(e.typeEtape())
                    .politiqueValidation(e.politiqueValidation() != null ? e.politiqueValidation() : PolitiqueValidation.ONLY_ONE_CAN_VALIDATE)
                    .strategieAssignation(e.strategieAssignation())
                    .delaiSlaHeures(e.delaiSlaHeures())
                    .actionDepassementSla(e.actionDepassementSla() != null ? e.actionDepassementSla() : ActionDepassementSla.RELANCE_EMAIL)
                    .actif(true)
                    .acteurs(new ArrayList<>())
                    .build();

            // Création de l'acteur de validation attendu (REQUIRED = true, ACTIF = true)
            WorkflowEtapeActeurDefinitionEntity acteur = WorkflowEtapeActeurDefinitionEntity.builder()
                    .id(UUID.randomUUID())
                    .etapeDefinition(etape)
                    .intituleAction(e.intitule())
                    .strategieAssignation(e.strategieAssignation() != null ? e.strategieAssignation() : StrategieAssignation.ROLE_KEYCLOAK)
                    .valeurAssignation(e.valeurAssignation())
                    .obligatoire(true)
                    .actif(true)
                    .delaiSlaHeures(e.delaiSlaHeures())
                    .actionDepassementSla(e.actionDepassementSla() != null ? e.actionDepassementSla() : ActionDepassementSla.RELANCE_EMAIL)
                    .build();
            etape.addActeur(acteur);

            def.addEtape(etape);
        }

        workflowDefinitionRepository.save(def);
        log.info("Workflow créé : [{}] - {} ({} étapes)", code, intitule, etapes.size());
    }

    // =========================================================================
    // RÉFÉRENTIEL DES OPÉRATIONS MÉTIERS (Suppression des codes en dur)
    // =========================================================================
    /**
     * Initialise le catalogue officiel des opérations métiers SODEPA (exclusives et transversales).
     */
    private void seedMetiers() {
        // ── Pôle 1 : Ressources Humaines & Personnel ──
        seedMetierIfNotExists("RECRUITMENT", "Recrutement & Intégration Salarié", "Processus d'embauche et contrat de travail", WorkflowDomaine.RH, false);
        seedMetierIfNotExists("DEMANDE_CONGE", "Demande de Congés & Permissions", "Validation hiérarchique des absences et congés annuels", WorkflowDomaine.RH, true);
        seedMetierIfNotExists("FORMATION_PERSONNEL", "Plan de Formation Continue", "Inscription et prise en charge des sessions de formation", WorkflowDomaine.RH, true);
        seedMetierIfNotExists("AVANCEMENT_ECHELON", "Avancement d'Échelon & Promotion", "Changement de classe ou d'échelon indiciaire", WorkflowDomaine.RH, false);
        seedMetierIfNotExists("SANCTION_DISCIPLINAIRE", "Procédure Disciplinaire & Sanctions", "Conseil de discipline, avertissement, blâme ou mise à pied", WorkflowDomaine.RH, false);
        seedMetierIfNotExists("DEMISSION_DEPART", "Départ, Retraite & Démission", "Liquidation des droits et certificat de travail", WorkflowDomaine.RH, false);
        seedMetierIfNotExists("MISSION_ORDER", "Ordres de Mission sur le Terrain", "Déplacements opérationnels dans les ranchs et abattoirs", WorkflowDomaine.RH, true);
        seedMetierIfNotExists("PAIE_VARIABLES", "Saisie des Primes & Heures Supplémentaires", "Validation mensuelle des éléments variables de paie", WorkflowDomaine.RH, false);
        seedMetierIfNotExists("PAIE_CLOTURE", "Clôture & Ordonnancement de la Paie", "Visa DAF et validation de l'état général des salaires", WorkflowDomaine.RH, false);

        // ── Pôle 2 : Finances & Comptabilité ──
        seedMetierIfNotExists("ACCOUNTING_ENTRY", "Écritures & Lettrages Comptables", "Validation des écritures et pièces sensibles au journal", WorkflowDomaine.COMPTABILITE, false);
        seedMetierIfNotExists("CLOTURE_EXERCICE", "Clôture d'Exercice Fiscal & Inventaire", "Arrêté des comptes annuels et états financiers OHADA", WorkflowDomaine.COMPTABILITE, false);
        seedMetierIfNotExists("RAPPROCHEMENT_BANCAIRE", "Rapprochement Bancaire & Relevés", "Pointage mensuel des relevés bancaires et états de rapprochement", WorkflowDomaine.COMPTABILITE, false);
        seedMetierIfNotExists("METIER_BANQUE", "Ouverture & Modification de Compte Bancaire", "Création de compte bancaire et affectation comptable", WorkflowDomaine.COMPTABILITE, false);
        seedMetierIfNotExists("METIER_TIERS", "Création & Agrément Partenaire Tiers", "Homologation clients, fournisseurs et partenaires", WorkflowDomaine.COMPTABILITE, true);
        seedMetierIfNotExists("METIER_COMPTE", "Création & Modification Compte Plan OHADA", "Ajout de sous-compte dans le plan comptable", WorkflowDomaine.COMPTABILITE, false);
        seedMetierIfNotExists("METIER_IMMOBILISATION", "Acquisition & Amortissement d'Immobilisation", "Mise en service et calcul de dotations aux amortissements", WorkflowDomaine.COMPTABILITE, false);

        // ── Pôle 3 : Trésorerie & Caisse ──
        seedMetierIfNotExists("ORDRE_VIREMENT", "Émission d'Ordre de Virement Bancaire", "Double signature DAF/DG pour virements bancaires", WorkflowDomaine.FINANCE, false);
        seedMetierIfNotExists("DECAISSEMENT_CAISSE", "Décaissement & Approvisionnement Caisse", "Retrait de fonds et justification des menues dépenses", WorkflowDomaine.FINANCE, false);
        seedMetierIfNotExists("REGIE_AVANCES", "Gestion des Régies d'Avances Ranches & Abattoirs", "Dotation et apurement des régies décentralisées", WorkflowDomaine.FINANCE, true);

        // ── Pôle 4 : Gestion Budgétaire ──
        seedMetierIfNotExists("BUDGET_INITIALISATION", "Élaboration & Adoption Budget Annuel", "Validation des prévisions budgétaires par unité", WorkflowDomaine.BUDGET, false);
        seedMetierIfNotExists("BUDGET_COMMITMENT", "Engagement Budgétaire de Dépenses", "Réservation de crédits et visa du contrôleur financier", WorkflowDomaine.BUDGET, true);
        seedMetierIfNotExists("BUDGET_VIREMENT_CREDIT", "Virement & Réallocation de Crédits", "Transfert de crédits entre lignes budgétaires", WorkflowDomaine.BUDGET, false);

        // ── Pôle 5 : Achats & Marchés Publics ──
        seedMetierIfNotExists("DEMANDE_ACHAT", "Expression de Besoin & Demande d'Achat", "Demande interne d'approvisionnement", WorkflowDomaine.ACHATS, true);
        seedMetierIfNotExists("PURCHASE_ORDER", "Bons de Commande & Achats Locaux", "Validation d'achats de fournitures et intrants", WorkflowDomaine.ACHATS, true);
        seedMetierIfNotExists("MARCHE_PUBLIC", "Dossier d'Appel d'Offres & Marché Public", "Commission de passation des marchés et attribution", WorkflowDomaine.ACHATS, false);
        seedMetierIfNotExists("RECEPTION_PV", "Procès-Verbal de Réception Livraison", "Contrôle de conformité et visa de réception magasin", WorkflowDomaine.ACHATS, true);
        seedMetierIfNotExists("FACTURE_LIQUIDATION", "Liquidation & Mandatement de Facture", "Certification du service fait et ordre de paiement", WorkflowDomaine.ACHATS, false);
        seedMetierIfNotExists("METIER_FOURNISSEUR", "Homologation & Évaluation Fournisseur", "Inscription au fichier des fournisseurs agréés SODEPA", WorkflowDomaine.ACHATS, false);

        // ── Pôle 6 : Ranches Pastoraux & Élevage Bovin ──
        seedMetierIfNotExists("CHEPTEL_ENTREE", "Déclaration de Naissances & Achats Bétail", "Enregistrement boucle auriculaire et fiche zootechnique", WorkflowDomaine.PASTORAL, false);
        seedMetierIfNotExists("CHEPTEL_SORTIE", "Sortie, Réforme & Vente de Cheptel", "Bon de sortie de troupeau validé par chef de ranch", WorkflowDomaine.PASTORAL, false);
        seedMetierIfNotExists("CHEPTEL_TRANSFERT", "Transfert Bétail Inter-Ranches", "Mouvement d'animaux entre Faro, Ndokayo et Dumbo", WorkflowDomaine.PASTORAL, false);
        seedMetierIfNotExists("CAMPAGNE_VACCINATION", "Campagne Vétérinaire & Traitement Sanitaire", "Vaccination annuelle et prophylaxie contre le charbon/dermatose", WorkflowDomaine.PASTORAL, false);
        seedMetierIfNotExists("INTRANTS_PASTORAUX", "Dotation en Sel, Compléments & Aliments", "Approvisionnement des parcs de nuit et stations de pesée", WorkflowDomaine.PASTORAL, false);
        seedMetierIfNotExists("CATTLE_SALE", "Vente de Bétail sur Pied & Foires Pastorales", "Facturation foraine et adjudication d'animaux", WorkflowDomaine.COMMERCIAL, true);

        // ── Pôle 7 : Abattoirs Industriels & Filière Viande ──
        seedMetierIfNotExists("ABATTOIR_RECEPTION", "Réception & Stabulation Bétail Abattoir", "Contrôle sanitaire d'entrée en bouverie (Yaoundé, Douala)", WorkflowDomaine.ABATTOIR, false);
        seedMetierIfNotExists("CONTROLE_VETERINAIRE", "Inspection Vétérinaire Ante & Post-Mortem", "Saisie sanitaire ou estampillage carcasse 'Salubre'", WorkflowDomaine.ABATTOIR, false);
        seedMetierIfNotExists("ABATTOIR_ABATTAGE", "Ordre d'Abattage & Chaîne de Découpe", "Planification journalière de la tuerie et pesée fiscale", WorkflowDomaine.ABATTOIR, false);
        seedMetierIfNotExists("CHAMBRE_FROIDE_STOCK", "Mise en Stock & Transfert Frigorifique", "Entrée et ressuage en chambres froides", WorkflowDomaine.ABATTOIR, false);
        seedMetierIfNotExists("VENTE_VIANDE_GROS", "Vente et Expédition Viande en Gros", "Livraison grossistes, boucheries centrales et supermarchés", WorkflowDomaine.COMMERCIAL, true);

        // ── Pôle 8 : Gouvernance, Organisation & IT ──
        seedMetierIfNotExists("SERVICE_MEMO", "Rédaction de Note de Service Interne", "Diffusion de directives et notes de service", WorkflowDomaine.GOUVERNANCE, true);
        seedMetierIfNotExists("CIRCULAIRE_DG", "Émission de Circulaire Direction Générale", "Circulaire officielle et instructions d'application", WorkflowDomaine.GOUVERNANCE, false);
        seedMetierIfNotExists("DELEGATION_POUVOIR", "Délégation de Signature & Intérim", "Arrêté portant intérim ou délégation de pouvoir", WorkflowDomaine.GOUVERNANCE, false);
        seedMetierIfNotExists("CONTRAT_CONVENTION", "Validation Juridique Convention & Partenariat", "Visa du contentieux et signature DG", WorkflowDomaine.GED, true);
        seedMetierIfNotExists("CREATION_METIER", "Création et Homologation de Processus Métier", "Création d'une nouvelle opération métier dans l'ERP", WorkflowDomaine.GOUVERNANCE, false);
        seedMetierIfNotExists("DEMANDE_EQUIPEMENT_INFO", "Demande de Matériel & Accès Informatique", "Dotation matériel IT et ouverture de comptes applicatifs", WorkflowDomaine.GOUVERNANCE, true);
    }

    private void seedMetierIfNotExists(String code, String title, String description, WorkflowDomaine domain, boolean isTransversal) {
        if (!workflowMetierRepository.existsByCode(code)) {
            WorkflowMetierEntity m = WorkflowMetierEntity.builder()
                    .id(UUID.randomUUID())
                    .code(code)
                    .title(title)
                    .description(description)
                    .domain(domain)
                    .isTransversal(isTransversal)
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();
            workflowMetierRepository.save(m);
            log.info("Opération Métier initialisée : [{}] - {}", code, title);
        }
    }

    // =========================================================================
    // 0. RESSOURCES HUMAINES & RECRUTEMENT
    // =========================================================================
    /**
     * Initialise les circuits de validation transverses du pôle Ressources Humaines
     * (Recrutement, titularisation, contrat de travail sous visa DRH, DAF et signature DG).
     */
    private void seedRhWorkflows() {
        seedWorkflowIfNotExists(
                "WF-RH-RECRUTEMENT",
                "Recrutement, Intégration et Contrat de Travail d'un Salarié",
                "Circuit officiel d'embauche et d'intégration : création du compte utilisateur, dossier employé, classification indiciaire et approbation finale du Directeur Général par Note de Service ou Circulaire (Maker-Checker)",
                WorkflowDomaine.RH,
                "ROLE_DRH,ROLE_GESTIONNAIRE_RH,ROLE_RH",
                "SOUS_DIRECTEUR_RH,GESTIONNAIRE_RH",
                List.of(
                        new EtapeSeed("RH_01_INSTRUCTION_DRH", "Instruction du dossier et classification indiciaire (DRH / SDRH)", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_RH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RH_02_VISA_BUDGETAIRE_DAF", "Validation de la soutenabilité budgétaire DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RH_03_APPROBATION_NOTE_SERVICE_DG", "Approbation finale et signature de la Note de Service / Contrat (Directeur Général)", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    // =========================================================================
    // 1. DIRECTION GÉNÉRALE & GOUVERNANCE (10 Workflows)
    // =========================================================================
    /**
     * Initialise les circuits de validation de la Direction Générale et de la Gouvernance SODEPA
     * (Plans stratégiques, conventions d'État, partenariats internationaux, délégations de signature).
     */
    private void seedGouvernanceWorkflows() {
        seedWorkflowIfNotExists(
                "WF-DG-01",
                "Arrêté et publication du Plan Stratégique & Budget Annuel",
                "Circuit de gouvernance pour l'approbation institutionnelle du plan stratégique et budget annuel",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_01_ARBITRAGE", "Arbitrage de la Direction Générale", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_01_CA", "Adoption par le Conseil d'Administration", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.MUST_HAVE_MAJORITY, StrategieAssignation.ROLE_KEYCLOAK, "PCA", 72, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-02",
                "Validation des Conventions de Partenariat & Accords-Cadres",
                "Approbation des protocoles d'accord avec les ministères, bailleurs et coopératives d'élevage",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_02_AVIS_TECH", "Avis technique & juridique conjoint DPC/DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.EVERY_BODY_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_02_SIGNATURE", "Signature formelle du Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-03",
                "Délégation de Pouvoirs et de Signature",
                "Formalisation des actes de délégation de signature avec plafonds financiers et limites temporelles",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_03_CONFORMITE", "Contrôle de conformité statutaire par la DCAI", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_03_NOTIFICATION", "Signature de l'acte et notification par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-04",
                "Autorisation Spéciale de Dépassement Budgétaire",
                "Procédure d'exception pour déblocage de crédits d'urgence au-delà des plafonds initiaux",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_04_EVAL_BUDGET", "Évaluation d'impact budgétaire par le Service Budget", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_BUDGET", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_04_ACCORD_DG", "Accord formel et déblocage exceptionnel par le DG", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-05",
                "Mandats Sanitaires et Campagnes d'Urgence Épizootique",
                "Déclenchement du protocole d'urgence en cas de foyer infectieux animal (PPCB, charbon, fièvre aphteuse)",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_05_ALERTE_DPC", "Alerte sanitaire et rapport de cadrage DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("DG_05_ARRETE_DG", "Arrêté d'urgence de la Direction Générale et notification MINEPIA", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-06",
                "Arbitrage des Tarifs Officiels de Cession (Viande & Bétail)",
                "Homologation de la grille tarifaire nationale de vente de viande et d'animaux sur pied",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_06_ETUDE_COUTS", "Étude de rentabilité des coûts de production DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_06_HOMOLOGATION", "Homologation et publication officielle par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-07",
                "Nomination et Affectation des Responsables d'Unités",
                "Affectation des Directeurs d'Abattoirs, Chefs de Ranchs et Chefs de Stations de la SODEPA",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_07_INSTRUCTION_RH", "Instruction du dossier de compétences par la SDRH", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_RH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_07_DECISION_DG", "Décision d'affectation signée par le Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-08",
                "Approbation des Rapports Trimestriels d'Activité pour le Conseil",
                "Consolidation et transmission du rapport trimestriel aux membres du Conseil d'Administration",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_08_REVUE_DIR", "Revue et visa du collège des directeurs centraux", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.EVERY_BODY_CAN_VALIDATE, StrategieAssignation.ROLE_KEYCLOAK, "DIRECTEUR_CENTRAL", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_08_TRANSMISSION", "Validation finale et transmission par le DG au CA", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-09",
                "Gestion des Contentieux Juridiques et Transactions Amiables",
                "Règlement amiable ou judiciaire des litiges commerciaux, fonciers ou sociaux majeurs",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_09_ANALYSE_RISQUE", "Analyse juridique et chiffrage des provisions par la DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_09_PROTOCOLE_DG", "Autorisation du protocole transactionnel par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DG-10",
                "Autorisation de Vente ou Réforme d'Actifs Lourds",
                "Mise au rebut, vente aux enchères ou cession de tracteurs, groupes frigorifiques et véhicules",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DG_10_COMMISSION", "Procès-verbal de la commission technique de réforme", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PATRIMOINE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_10_VISA_DAF", "Visa financier et valorisation comptable DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DG_10_APPROB_DG", "Approbation définitive de sortie d'actif par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    // =========================================================================
    // 2. DIRECTION ADMINISTRATIVE ET FINANCIÈRE (10 Workflows)
    // =========================================================================
    /**
     * Initialise les circuits de validation de la Direction Administrative et Financière
     * (Engagements budgétaires, virements bancaires, inventaires, réallocations de crédits, marchés publics).
     */
    private void seedDafWorkflows() {
        seedWorkflowIfNotExists(
                "WF-DAF-01",
                "Passation de Commande Publique (Seuils Achat Direct / BC / CIPM)",
                "Contrôle budgétaire, visa financier et passation des marchés et commandes d'acquisitions",
                WorkflowDomaine.ACHATS,
                List.of(
                        new EtapeSeed("DAF_01_DISPO_BUDGET", "Contrôle automatique de disponibilité de crédit", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_BUDGET", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_01_VISA_CF", "Visa de conformité du Contrôle Financier", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CONTROLEUR_FINANCIER", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_01_ENGAGEMENT", "Signature du bon de commande ou lettre-commande par le DAF", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-02",
                "Traitement et Clôture Mensuelle de la Paie",
                "Calcul de la paie indiciaire, contrôle des primes/retenues et virement des salaires du personnel",
                WorkflowDomaine.RH,
                List.of(
                        new EtapeSeed("DAF_02_POINTAGE", "Contrôle des variables et pointage par la SDRH", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_RH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_02_ORDONNANCEMENT", "Ordonnancement bancaire et mandatement par le DAF", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-03",
                "Approvisionnement d'une Régie d'Avances Régionale",
                "Demande d'approvisionnement en fonds de caisse pour les dépenses courantes des abattoirs et ranchs",
                WorkflowDomaine.FINANCE,
                List.of(
                        new EtapeSeed("DAF_03_VERIF_TRESOR", "Vérification de l'épuisement de l'encours par la Trésorerie", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_FINANCIER", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_03_VIREMENT_REGIE", "Ordonnancement du virement bancaire par le DAF", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-04",
                "Régularisation et Contrôle des Pièces de Dépense de Régie",
                "Contrôle des factures et reçus de régie d'avances pour imputation en charges SYSCOHADA",
                WorkflowDomaine.COMPTABILITE,
                List.of(
                        new EtapeSeed("DAF_04_CONTROLE_PIECES", "Pointage comptable des reçus par le Service Comptable", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMPTABLE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_04_QUITUS", "Quitus de reconstitution d'enveloppe par le SDFBM", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_FINANCES", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-05",
                "Liquidation et Ordonnancement de Facture Fournisseur",
                "Traitement de la facture définitive après réception conforme des biens ou travaux",
                WorkflowDomaine.COMPTABILITE,
                List.of(
                        new EtapeSeed("DAF_05_SERVICE_FAIT", "Attestation de service fait et conformité PV de réception", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_MARCHES", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_05_IMPUTATION", "Imputation comptable et visa fiscal retenue à la source", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMPTABLE", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_05_BON_A_PAYER", "Émission du bon à payer par le Directeur DAF", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-06",
                "Réallocation d'Enveloppes Budgétaires entre Lignes",
                "Transfert de crédits d'un poste budgétaire excédentaire vers un poste déficitaire",
                WorkflowDomaine.BUDGET,
                List.of(
                        new EtapeSeed("DAF_06_SOLDE_SOURCE", "Contrôle automatique du solde disponible poste source", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_BUDGET", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_06_VALIDATION", "Validation du virement de crédits par le DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        // -----------------------------------------------------------------
        // Workflows Budget supplementaires (reallocations significatives)
        // -----------------------------------------------------------------

        seedWorkflowIfNotExists(
                "WF-BUD-REALLOC-SIGNIFICATIVE",
                "Réallocation Significative de Crédits Budgétaires (> 5 %)",
                "Circuit complet avec comité CSB+SDFBM+DCAI, validation DAF, accord DG et résolution CA",
                WorkflowDomaine.BUDGET,
                List.of(
                        new EtapeSeed("BUD_REALLOC_SIG_COMITE", "Analyse et avis du comité budgétaire (CSB+SDFBM+DCAI)", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.EVERY_BODY_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_BUDGET", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("BUD_REALLOC_SIG_DAF", "Validation de la réallocation par le DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("BUD_REALLOC_SIG_DG", "Accord préalable du Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("BUD_REALLOC_SIG_CA", "Adoption de la résolution par le Conseil d’Administration", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.MUST_HAVE_MAJORITY, StrategieAssignation.ROLE_KEYCLOAK, "PCA", 72, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-BUD-REALLOC-URGENCE",
                "Mesure Conservatoire Budgétaire d’Urgence (Autorisation DG)",
                "Réallocation d’urgence par décision DG, à régulariser en résolution CA lors de la prochaine session",
                WorkflowDomaine.BUDGET,
                List.of(
                        new EtapeSeed("BUD_URGENCE_EVAL_CSB", "Evaluation d’urgence et proposition du Chef Service Budget", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_BUDGET", 4, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("BUD_URGENCE_DG_ACCORD", "Décision d’autorisation spéciale du Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 2, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("BUD_URGENCE_NOTIF_CA", "Notification et inscription à l’ordre du jour de la prochaine session CA", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 1, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-07",
                "Recrutement et Intégration d'un Salarié (CDI/CDD)",
                "Dossier d'embauche, classification indiciaire, immatriculation CNPS et contrat de travail",
                WorkflowDomaine.RH,
                List.of(
                        new EtapeSeed("DAF_07_INSTRUCTION", "Instruction du dossier et calcul de la rémunération SDRH", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_RH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_07_IMPACT_BUDGET", "Validation de la soutenabilité budgétaire DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_07_CONTRAT_DG", "Signature définitive du contrat par le Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-08",
                "Validation et Règlement des Droits à Congés Payés",
                "Planification, arbitrage des départs en congés et décompte des indemnités associées",
                WorkflowDomaine.RH,
                List.of(
                        new EtapeSeed("DAF_08_AVIS_N1", "Avis d'opportunité du supérieur hiérarchique direct N+1", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.MANAGER_N1, "SUPERVISEUR_DIRECT", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_08_DECOMPTE_RH", "Décompte des jours acquis et accord officiel SDRH", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_RH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-09",
                "Clôture d'Exercice Comptable et Établissement de la DSF",
                "Arrêté des comptes annuels, inventaires et production de la déclaration statistique et fiscale",
                WorkflowDomaine.COMPTABILITE,
                List.of(
                        new EtapeSeed("DAF_09_BALANCE_INV", "Revue de balance après inventaire et provisions", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMPTABLE", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_09_ETATS_DAF", "Validation des états financiers de synthèse par le DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_09_CERTIF_DG", "Certification institutionnelle par le DG et transmission CAC", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DAF-10",
                "Rapprochement Bancaire et Apurement des Suspends",
                "Pointage mensuel des extraits de compte et apurement des écarts de trésorerie",
                WorkflowDomaine.FINANCE,
                List.of(
                        new EtapeSeed("DAF_10_POINTAGE_AUTO", "Rapprochement automatique des flux bancaires", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_FINANCIER", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DAF_10_ECRITURES_AJUST", "Validation des écritures d'ajustement par le Chef Comptable", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMPTABLE", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );
    }

    // =========================================================================
    // 3. DIRECTION PRODUCTION & COMMERCIALISATION (10 Workflows)
    // =========================================================================
    /**
     * Initialise les circuits de validation de la Direction de la Production et de la Commercialisation
     * (Programmes d'embouche, campagnes de vaccination, tarifs de vente du bétail, circuits forains).
     */
    private void seedDpcWorkflows() {
        seedWorkflowIfNotExists(
                "WF-DPC-01",
                "Plan de Campagne Annuel de Déstockage & Vente des Ranchs",
                "Programmation des quotas de déstockage de bovins pour réguler la charge pastorale",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_01_SYNTHESE_RANCHS", "Agrégation des propositions zootechniques par la SDT", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_TECHNIQUE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_01_VALIDATION_DPC", "Adoption du calendrier des ventes par le Directeur DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-02",
                "Approbation du Programme Sanitaire Cheptel (Vaccinations / Déparasitages)",
                "Cadrage annuel des campagnes obligatoires de prophylaxie et commandes de vaccins MINEPIA",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_02_PROTOCOLE_VET", "Revue du protocole de vaccination par le Vétérinaire Central", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_TECHNIQUE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_02_ACCORD_DPC", "Approbation du programme sanitaire et logistique DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-03",
                "Agrément et Attribution de Quotas aux Grossistes / Bouchers",
                "Homologation des clients grossistes avec analyse de solvabilité et encours autorisés",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("DPC_03_SOLVABILITE", "Analyse des capacités de paiement par la SD Commerciale", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_COMMERCIAL", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_03_AGREMENT_DPC", "Attribution des quotas d'enlèvement par le Directeur DPC", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-04",
                "Plan d'Entretien Préventif des Équipements Frigorifiques",
                "Planification de la révision des compresseurs, tunnels et condenseurs des abattoirs",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_04_CHIFFRAGE_MAINT", "Chiffrage des pièces d'usure par la Brigade Maintenance", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_BRIGADE_MAINTENANCE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_04_VALIDATION_DPC", "Validation du plan d'arrêt technique par le Directeur DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-05",
                "Arbitrage et Répartition des Transferts Vifs (Ranchs vers Abattoirs)",
                "Convoiement ferroviaire ou par bétaillères des bœufs gras des ranchs vers Douala/Yaoundé",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("DPC_05_CERTIF_VET", "Vérification des certificats de salubrité de circulation", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_RANCHS", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_05_ORDRE_CONVOI", "Ordre officiel d'embarquement et de transport DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-06",
                "Campagne de Promotion Commerciale et Vente Promotionnelle de Viande",
                "Organisation des ventes de bétail et de viande à prix modérés lors des périodes de fêtes",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("DPC_06_PROPOSITION_MKT", "Montage commercial par le Service Ventes & Marketing", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_COMMERCIAL", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_06_ACCORD_MARGES", "Accord sur les marges résiduelles par la DAF", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DAF", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_06_LANCEMENT_DPC", "Autorisation de mise en vente promotionnelle DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-07",
                "Gestion des Réclamations Clients sur la Qualité de Viande Livrée",
                "Instruction des non-conformités de carcasses ou de découpe signalées par les distributeurs",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("DPC_07_EXPERTISE", "Expertise technique du lot par le Service Transformation", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_TRANSFORMATION", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_07_AVOIR_DPC", "Arbitrage et émission de l'avoir commercial par le DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-08",
                "Acquisition de Reproducteurs Améliorateurs (Géniteurs Gudali, Brahman)",
                "Achat et introduction de taureaux de race pure pour préserver la vigueur hybride",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_08_EXAMEN_ZOOTECH", "Examen phénotypique et généalogique par la SD Technique", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "SOUS_DIRECTEUR_TECHNIQUE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_08_ACCORD_INVEST", "Validation de l'investissement zootechnique DPC/DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-09",
                "Plan d'Aménagement Annuel des Parcours et Pare-feux Pastoraux",
                "Stratégie de brûlis dirigés, fauche mécanique et protection contre les feux de brousse tardifs",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_09_ETUDE_PATURAGE", "Étude biomasse par le Service Aménagement & Pâturage", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PATURAGE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_09_VALIDATION_DPC", "Adoption du calendrier des feux contrôlés DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPC-10",
                "Audit de Rendement des Chaînes d'Abattage et Ratio Cinquième Quartier",
                "Analyse des performances industrielles : rendement carcasse, saisies et valorisation abats",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPC_10_CALCUL_RATIOS", "Contrôle automatique des ratios de carcasse", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_TRANSFORMATION", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPC_10_RAPPORT_DPC", "Validation du rapport de performance industrielle DPC", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_DPC", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );
    }

    // =========================================================================
    // 4. DCAI / DPD / QHSE (10 Workflows)
    // =========================================================================
    /**
     * Initialise les circuits de validation du Contrôle, de l'Audit Interne et de la Prospective
     * (Audits inopinés, cartographie des risques, plans d'investissements, non-conformités sanitaires QHSE).
     */
    private void seedAuditProspectiveWorkflows() {
        seedWorkflowIfNotExists(
                "WF-DCAI-01",
                "Mission d'Audit Inopiné sur Site (Ranchs ou Abattoirs)",
                "Déclenchement d'une mission d'inspection comptable, financière ou zootechnique sur site",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_01_CADRAGE", "Cadrage de la mission d'inspection par la DCAI", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DCAI_01_ORDRE_DG", "Ordre de mission d'audit signé par le Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DCAI-02",
                "Traitement d'une Alerte Fraude ou Écart d'Inventaire Cheptel",
                "Instruction contradictoire en cas de suspicion de détournement ou mortalité anormale non justifiée",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_02_INSTRUCTION", "Instruction contradictoire et recueil des preuves DCAI", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DCAI_02_SANCTIONS_DG", "Décision conservatoire ou saisine du conseil de discipline DG", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DCAI-03",
                "Revue et Mise à Jour de la Cartographie des Risques SODEPA",
                "Évaluation périodique des risques opérationnels, climatiques, épidémiologiques et financiers",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_03_CONSOLIDATION", "Consolidation des fiches de risques par l'Auditeur Interne", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DCAI_03_ADOPTION_DG", "Adoption de la cartographie par la Direction Générale", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DCAI-04",
                "Certification Sanitaire et Audit Environnemental QHSE des Sites",
                "Audit de conformité des abattoirs aux normes de traitement des effluents et rejets",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_04_AUDIT_TERRAIN", "Visite d'inspection terrain et prélèvements QHSE", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "ATTACHE_QHSE", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DCAI_04_PLAN_ACTION", "Validation du plan de mise en conformité par la DCAI", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DCAI", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DCAI-05",
                "Gestion d'un Incident Grave de Travail ou Pollution Environnementale",
                "Prise en charge d'un accident grave sur une chaîne d'abattage ou pollution accidentelle d'un cours d'eau",
                WorkflowDomaine.AUDIT_QHSE,
                List.of(
                        new EtapeSeed("DCAI_05_CONSTAT_URG", "Constat immédiat et mesures conservatoires QHSE", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "ATTACHE_QHSE", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("DCAI_05_DECISION_DG", "Déclaration légale et prise en charge institutionnelle DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-06",
                "Programmation des Projets de Développement sur Financements Extérieurs",
                "Montage de dossiers de projets d'appui à la filière bétail-viande (BAD, Banque Mondiale, UE)",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DPD_06_MONTAGE", "Montage du cadre logique et chronogramme par la CSPP", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DPD", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_06_SOUMISSION_DG", "Validation pour transmission institutionnelle par le DG", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-07",
                "Recueil et Analyse des Données Statistiques Nationales de l'Élevage",
                "Consolidation des données mensuelles de production de viande, taux de vêlage et cours du bétail",
                WorkflowDomaine.PRODUCTION,
                List.of(
                        new EtapeSeed("DPD_07_CONTROLE_DONNEES", "Contrôle automatique de cohérence des données chiffrées", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_STATISTIQUE", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_07_DIFFUSION", "Validation du tableau de bord statistique par la DPD", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DPD", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-08",
                "Gestion des Droits d'Accès, Rôles et Sécurité dans l'ERP SODEPA",
                "Attribution ou modification des profils d'habilitation dans le système d'information",
                WorkflowDomaine.GED,
                List.of(
                        new EtapeSeed("DPD_08_ACCORD_HIERARCHIE", "Visa du responsable de structure du collaborateur", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.MANAGER_N1, "SUPERVISEUR_DIRECT", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_08_APPLICATION_SI", "Paramétrage des rôles et permissions par la Cellule Informatique", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_INFORMATIQUE", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-09",
                "Plan de Continuité d'Activité et Sauvegarde du Système d'Information",
                "Vérification de la résilience des serveurs centraux et synchronisation des bases locales hors-ligne",
                WorkflowDomaine.GED,
                List.of(
                        new EtapeSeed("DPD_09_TEST_RESTAUR", "Test automatique de restauration d'un dump de base de données", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_INFORMATIQUE", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_09_VISA_PCA", "Visa du rapport de sécurité informatique par la DPD", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_DIVISION_DPD", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-DPD-10",
                "Évaluation de Fin de Projet et Clôture Technique",
                "Recette finale des investissements agropastoraux ou informatiques et transfert opérationnel",
                WorkflowDomaine.GOUVERNANCE,
                List.of(
                        new EtapeSeed("DPD_10_RECETTE", "Procès-verbal de recette technique par l'équipe projet", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_PROJETS", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("DPD_10_QUITUS_DG", "Quitus de clôture de projet signé par le Directeur Général", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_GENERAL", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    // =========================================================================
    // 5. RANCHS PASTORAUX (10 Workflows)
    // =========================================================================
    /**
     * Initialise les circuits de validation des Ranches Pastoraux (Faro, Dumbo, Ndokayo)
     * (Transhumance, réapprovisionnement en intrants vétérinaires, constats de mortalité, réfection des clôtures).
     */
    private void seedRanchsWorkflows() {
        seedWorkflowIfNotExists(
                "WF-RANCH-01",
                "Déclaration et Enregistrement des Naissances de Veaux",
                "Pose du numéro de boucle auriculaire, identification généalogique et entrée dans l'actif cheptel",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_01_CONSTAT", "Constat zootechnique de naissance par le Chef de Brigade", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_01_BOUCLAGE", "Validation du bouclage par le Directeur du Ranch", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-02",
                "Constat et Enregistrement de Mort Subite ou Maladie du Cheptel",
                "PV autopsie vétérinaire, mesures de biosécurité et sortie d'actif animal pour mortalité",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_02_AUTOPSIE", "Autopsie vétérinaire et détermination de la cause du décès", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_RANCH", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("RANCH_02_SORTIE_ACTIF", "Validation du PV d'abattage d'urgence ou de perte par le Directeur", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-03",
                "Inventaire Annuel / Fiche de Composition du Troupeau (Herd Composition)",
                "Rapprochement contradictoire physique du cheptel par catégorie zootechnique",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_03_COMPTAGE", "Pointage physique des troupeaux au couloir de contention", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_RANCH", 72, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_03_ARRETE_RANCH", "Validation de la fiche de composition par le Directeur du Ranch", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 48, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-04",
                "Pesée Périodique et Suivi du Gain Moyen Quotidien (GMQ)",
                "Évaluation de la prise de poids des animaux en embouche et suivi de la croissance",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_04_SAISIE_PESEE", "Saisie automatique des pesées individuelles bascule", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "ZOOTECHNICIEN_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_04_REVUE_GMQ", "Validation de la fiche d'embouche par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-05",
                "Organisation d'une Foire Pastorale de Vente sur Pied",
                "Sélection des lots de bœufs gras et taureaux de réforme pour mise en adjudication",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("RANCH_05_LOTISSEMENT", "Constitution des lots et fixation des prix de réserve", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMMERCIAL_RANCH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_05_OUVERTURE", "Autorisation d'ouverture de la foire par le Directeur du Ranch", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-06",
                "Cession et Vente d'un Lot d'Animaux sur Pied",
                "Encaissement de la vente, certificat sanitaire de circulation et bon de sortie du troupeau",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("RANCH_06_ENCAISSEMENT", "Contrôle automatique de la quittance d'encaissement régie", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CAISSIER_REGISSEUR_RANCH", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_06_LAISSEZ_PASSER", "Délivrance du certificat sanitaire de circulation vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_RANCH", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("RANCH_06_BON_SORTIE", "Visa du bon de sortie définitive par le Directeur du Ranch", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-07",
                "Traitement Antiparasitaire Massif (Dipping / Pulvérisation au Couloir)",
                "Passage obligatoire au bain acaricide pour éradiquer les tiques et vecteurs hémoparasitaires",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_07_PREPARATION", "Contrôle du dosage et concentration du bain de dipping", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_07_POINTAGE", "Pointage des effectifs traités par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-08",
                "Rotation des Pâturages et Transhumance Intra-Ranch",
                "Mouvement planifié des bêtes pour laisser reposer les parcelles et préserver les sols",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_08_ETUDE_CHARGE", "Évaluation de la charge pastorale par le Service Pâturage", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PATURAGE_RANCH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_08_ORDRE_MOUV", "Ordre officiel de mouvement par le Directeur du Ranch", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-09",
                "Maintenance Lourde des Clôtures, Pistes et Points d'Eau",
                "Réparation des kilomètres de clôture périphérique, abreuvoirs et retenues collinaires",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_09_DEVIS_TRAVAUX", "Constat des brèches et chiffrage matériel par la Maintenance", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "RESPONSABLE_MAINTENANCE_RANCH", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_09_ACCORD_TRAVAUX", "Validation et engagement des travaux par le Directeur du Ranch", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-RANCH-10",
                "Gestion des Conflits Agropastoraux et Dégâts de Champs Voisins",
                "Constat contradictoire et indemnisation amiable des riverains en cas d'intrusion d'animaux",
                WorkflowDomaine.PASTORAL,
                List.of(
                        new EtapeSeed("RANCH_10_CONSTAT_COMM", "Constat conjoint avec les autorités traditionnelles et plaignants", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_ADMINISTRATIF_RANCH", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("RANCH_10_REGLEMENT", "Accord amiable et décharge signée par le Directeur du Ranch", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_RANCH", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    // =========================================================================
    // 6. ABATTOIRS FRIGORIFIQUES INDUSTRIELS (10 Workflows)
    // =========================================================================
    /**
     * Initialise les circuits de validation des Abattoirs Industriels (Douala, Yaoundé, Ngaoundéré)
     * (Abattages d'urgence, saisies vétérinaires sanitaires, maintenance de la chaîne du froid, déconsignations).
     */
    private void seedAbattoirsWorkflows() {
        seedWorkflowIfNotExists(
                "WF-ABAT-01",
                "Réception et Inspection Ante-Mortem du Bétail au Foirail",
                "Examen clinique des bovins vivants avant admission au parc d'attente d'abattage",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_01_EXAMEN_CLINIQUE", "Examen clinique vétérinaire ante-mortem", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_INSPECTEUR_ABATTOIR", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_01_ADMISSION_PARC", "Admission en parc de stabulation par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_ABATTOIR", 12, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-02",
                "Ordonnancement et Séquencement de la Chaîne d'Abattage",
                "Planification quotidienne du flux d'abattage selon les capacités des chaînes et du ressuage",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_02_PLANNING_JOUR", "Ordonnancement automatique selon les lots enregistrés", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_ABATTOIR", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_02_ORDRE_ABATTAGE", "Ordre d'abattage validé par le Directeur de l'Abattoir", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_ABATTOIR", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-03",
                "Inspection Post-Mortem, Estampillage et Classement des Carcasses",
                "Inspection systématique des organes internes, estampille sanitaire de salubrité et calibrage",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_03_INSPECTION_ORG", "Inspection des carcasses et viscères par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_INSPECTEUR_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_03_ESTAMPILLE", "Apposition de l'estampille de salubrité par la cellule HQSE", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_HQSE_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-04",
                "Traitement et Destruction des Saisies Sanitaires Vétérinaires",
                "Retrait de consommation humaine des viandes impropres et destruction sous contrôle vétérinaire",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_04_MOTIF_SAISIE", "Notification du motif légal de saisie par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_INSPECTEUR_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_04_PV_DESTRUCTION", "PV de dénaturation ou incinération par le Directeur de l'Abattoir", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_ABATTOIR", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-05",
                "Pesée Fiscale et Facturation de la Redevance d'Abattage",
                "Pesée électronique sur pont-rail, émission de la quittance et encaissement des droits",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_05_PESEE_PONT", "Enregistrement de la pesée sur rail aérien", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "AGENT_BASCULE_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_05_FACTURATION", "Émission de la facture et encaissement par la régie", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CAISSIER_REGISSEUR_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-06",
                "Gestion de la Chaîne de Froid et Admission en Chambres Frigorifiques",
                "Contrôle de la descente en température et stockage frigorifique régulé",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_06_RELEVE_TEMP", "Contrôle automatique des sondes de température", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "TECHNICIEN_FRIGORISTE_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_06_RANGEMENT", "Affectation de l'emplacement froid par le Magasinier Frigoriste", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "MAGASINIER_FRIGORISTE_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-07",
                "Déstockage et Sortie des Carcasses pour Livraison ou Vente",
                "Contrôle sanitaire de sortie et remise des viandes aux véhicules frigorifiques",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_07_VISITE_SORTIE", "Contrôle de salubrité de sortie par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_INSPECTEUR_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_07_BON_LIVRAISON", "Délivrance du bon d'enlèvement par le Service Commercial", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMMERCIAL_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-08",
                "Conditionnement et Vente du 5ème Quartier (Abats, Cuirs et Peaux)",
                "Traitement en triperie, salage des peaux de bœuf et vente aux grossistes et tanneurs",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_08_PESEE_CUIRS", "Pesée des peaux salées et tri des abats", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "RESPONSABLE_TRIPERIE_ABATTOIR", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_08_VENTE_5EME", "Facturation de cession par le Service Commercial", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_COMMERCIAL_ABATTOIR", 12, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-09",
                "Nettoyage, Désinfection Quotidienne et Contrôle Microbiologique",
                "Plan de nettoyage nocturne des sols, scies, rails et crochets selon protocole HACCP",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_09_DESINFECTION", "Exécution du nettoyage et désinfection par l'équipe hygiène", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "RESPONSABLE_HYGIENE_ABATTOIR", 6, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("ABAT_09_LIBERATION", "Contrôle microbiologique et libération de salle par la cellule HQSE", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_CELLULE_HQSE_ABATTOIR", 6, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-ABAT-10",
                "Maintenance d'Urgence des Chaînes Frigorifiques et Groupes Électrogènes",
                "Intervention immédiate en cas de coupure de courant ENEO ou panne de compresseur",
                WorkflowDomaine.ABATTOIR,
                List.of(
                        new EtapeSeed("ABAT_10_BASCULE_GEN", "Contrôle automatique du démarrage des groupes électrogènes", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_MAINTENANCE_ABATTOIR", 2, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("ABAT_10_ORDRE_INTERV", "Ordre d'intervention d'urgence validé par le Directeur", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "DIRECTEUR_ABATTOIR", 2, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    // =========================================================================
    // 7. STATION D'ÉLEVAGE ET LAITERIE DE JAKIRI (10 Workflows)
    // =========================================================================
    /**
     * Initialise les circuits de validation de la Station d'Élevage et Laiterie Spécialisée de Jakiri
     * (Insémination artificielle, pasteurisation du lait, collecte locale, production de yaourt et fromage).
     */
    private void seedJakiriWorkflows() {
        seedWorkflowIfNotExists(
                "WF-JAK-01",
                "Collecte Matinale du Lait et Contrôle Qualité à la Réception",
                "Test d'acidité, densité et alcool à la réception du lait frais pour acceptation ou refus",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_01_TESTS_LABO", "Contrôle des paramètres physico-chimiques du lait", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "TECHNICIEN_LABO_LAITIER", 4, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_01_ACCEPTATION", "Acceptation en cuve isotherme par le Chef de Laiterie", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_LAITERIE_JAKIRI", 4, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-02",
                "Transformation Industrielle Laitière (Pasteurisation, Yaourts, Fromages)",
                "Lancement d'un lot de production, ensemencement des ferments et étiquetage DLC",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_02_LANCEMENT_PROD", "Validation de la fiche de brassage et pasteurisation", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_ATELIER_TRANSFORMATION", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_02_CONFORME_LOT", "Validation de la conformité du lot transformé par le Chef de Station", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_STATION_JAKIRI", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-03",
                "Prélèvement, Conditionnement et Cryoconservation de Semence Bovine",
                "Production de paillettes de semence bovine congelée à -196°C pour l'insémination artificielle",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_03_CONTROLE_SPERME", "Examen microscopique de motilité par le Biotechnologiste", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "BIOTECHNOLOGISTE_REPRODUCTION", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_03_PAILLETTES", "Conditionnement en azote liquide visé par le Vétérinaire", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_JAKIRI", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-04",
                "Demande et Réalisation d'un Protocole d'Insémination Artificielle (IA)",
                "Planification de l'acte d'insémination sur vache en chaleur avec traçabilité du donneur",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_04_DETECTION_CHALEUR", "Constat de chaleur et choix de la paillette de race pure", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "INSEMINATEUR_STATION", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("JAK_04_ENREG_ACTE", "Validation de l'acte d'insémination par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_JAKIRI", 12, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-05",
                "Diagnostic de Gestation par Échographie / Palpation Rectale",
                "Contrôle de réussite de saillie à J+45 pour mise à jour du calendrier de mise-bas",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_05_EXAMEN_ECHO", "Diagnostic échographique par le Médecin Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_JAKIRI", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_05_CALENDRIER", "Mise à jour du calendrier de vêlage par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_JAKIRI", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-06",
                "Commercialisation des Produits Laitiers de Jakiri (Boutiques & Dépôts)",
                "Facturation, gestion des stocks et expédition des yaourts, laits pasteurisés et fromages",
                WorkflowDomaine.COMMERCIAL,
                List.of(
                        new EtapeSeed("JAK_06_COMMANDE", "Enregistrement automatique du bon de commande boutique", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "COMMERCIAL_LAITERIE_JAKIRI", 12, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_06_ENCAISSEMENT", "Validation de l'encaissement et bon de livraison par la régie", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CAISSIER_REGISSEUR_JAKIRI", 12, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-07",
                "Approvisionnement et Gestion du Stock d'Azote Liquide et Consommables IA",
                "Réapprovisionnement des bonbonnes cryogéniques pour sécuriser les semences bovines",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_07_NIVEAU_CUVE", "Vérification automatique du niveau critique d'azote liquide", TypeEtapeWorkflow.CONTROLE_AUTO, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "RESPONSABLE_LABORATOIRE_JAKIRI", 12, ActionDepassementSla.ESCALADE_MANAGER),
                        new EtapeSeed("JAK_07_COMMANDE_URG", "Ordre d'approvisionnement express signé par le Chef de Station", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_STATION_JAKIRI", 12, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-08",
                "Programme Alimentaire Spécifique du Troupeau Laitier (Compléments & Ensilage)",
                "Ajustement des rations énergétiques et protéiques des vaches laitières selon le pic de lactation",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_08_CALCUL_RATION", "Calcul nutritionnel des rations par le Nutritionniste", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "NUTRITIONNISTE_ANIMAL_JAKIRI", 24, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_08_SORTIE_STOCK", "Autorisation de sortie d'aliments concentrés par le Chef Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_JAKIRI", 24, ActionDepassementSla.RELANCE_EMAIL)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-09",
                "Diffusion de Géniteurs Améliorateurs auprès des Éleveurs Encadrés",
                "Mise à disposition de jeunes taureaux laitiers améliorés pour les coopératives de la région",
                WorkflowDomaine.LAITERIE,
                List.of(
                        new EtapeSeed("JAK_09_SELECTION_VET", "Certificat d'aptitude génétique et sanitaire par le Vétérinaire", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "VETERINAIRE_JAKIRI", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_09_ACCORD_CESSION", "Accord de cession et contrat d'encadrement Chef de Station", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_STATION_JAKIRI", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );

        seedWorkflowIfNotExists(
                "WF-JAK-10",
                "Accueil et Encadrement des Stagiaires d'Écoles Vétérinaires / Agronomiques",
                "Immersion pratique des élèves ingénieurs agronomes et docteurs vétérinaires sur la station",
                WorkflowDomaine.RH,
                List.of(
                        new EtapeSeed("JAK_10_PLAN_STAGE", "Validation du plan de stage par le Chef de Production", TypeEtapeWorkflow.VALIDATION_HUMAINE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_SERVICE_PRODUCTION_JAKIRI", 48, ActionDepassementSla.RELANCE_EMAIL),
                        new EtapeSeed("JAK_10_ATTESTATION", "Signature de l'attestation d'immersion par le Chef de Station", TypeEtapeWorkflow.SIGNATURE_ELECTRONIQUE, PolitiqueValidation.ONLY_ONE_CAN_VALIDATE, StrategieAssignation.POSTE_SPECIFIQUE, "CHEF_STATION_JAKIRI", 24, ActionDepassementSla.ESCALADE_MANAGER)
                )
        );
    }

    /**
     * Résout intelligemment le métier SODEPA rattaché à un code de workflow.
     */
    private java.util.Optional<WorkflowMetierEntity> resolveMetierForWorkflow(String code, WorkflowDomaine domaine) {
        if (code == null) return java.util.Optional.empty();
        String c = code.toUpperCase();
        if (c.contains("RECRUT")) return workflowMetierRepository.findByCode("RECRUITMENT");
        if (c.contains("CONGE")) return workflowMetierRepository.findByCode("DEMANDE_CONGE");
        if (c.contains("FORMATION")) return workflowMetierRepository.findByCode("FORMATION_PERSONNEL");
        if (c.contains("AVANCEMENT") || c.contains("PROMOTION")) return workflowMetierRepository.findByCode("AVANCEMENT_ECHELON");
        if (c.contains("SANCTION") || c.contains("DISCIPLIN")) return workflowMetierRepository.findByCode("SANCTION_DISCIPLINAIRE");
        if (c.contains("DEPART") || c.contains("RETRAITE") || c.contains("DEMISSION")) return workflowMetierRepository.findByCode("DEMISSION_DEPART");
        if (c.contains("MISSION")) return workflowMetierRepository.findByCode("MISSION_ORDER");
        if (c.contains("VARIABLE") || c.contains("PRIME")) return workflowMetierRepository.findByCode("PAIE_VARIABLES");
        if (c.contains("PAIE") || c.contains("SALAIRE")) return workflowMetierRepository.findByCode("PAIE_CLOTURE");

        // Finances / Compta / Trésorerie
        if (c.contains("COMPT") || c.contains("ECRITURE")) return workflowMetierRepository.findByCode("ACCOUNTING_ENTRY");
        if (c.contains("CLOTURE")) return workflowMetierRepository.findByCode("CLOTURE_EXERCICE");
        if (c.contains("RAPPROCH")) return workflowMetierRepository.findByCode("RAPPROCHEMENT_BANCAIRE");
        if (c.contains("VIREMENT") || c.contains("BANQUE")) return workflowMetierRepository.findByCode("ORDRE_VIREMENT");
        if (c.contains("CAISSE") || c.contains("DECAISS")) return workflowMetierRepository.findByCode("DECAISSEMENT_CAISSE");
        if (c.contains("REGIE") || c.contains("AVANCE")) return workflowMetierRepository.findByCode("REGIE_AVANCES");

        // Budget
        if (c.contains("BUDGET-INIT") || c.contains("STRATEG")) return workflowMetierRepository.findByCode("BUDGET_INITIALISATION");
        if (c.contains("ENGAG") || c.contains("DEPENSE")) return workflowMetierRepository.findByCode("BUDGET_COMMITMENT");
        if (c.contains("VIREMENT-CREDIT") || c.contains("REALLOC")) return workflowMetierRepository.findByCode("BUDGET_VIREMENT_CREDIT");

        // Achats & Marchés
        if (c.contains("BESOIN") || c.contains("ACHAT-DEMANDE")) return workflowMetierRepository.findByCode("DEMANDE_ACHAT");
        if (c.contains("COMMANDE") || c.contains("ACHAT")) return workflowMetierRepository.findByCode("PURCHASE_ORDER");
        if (c.contains("MARCHE") || c.contains("APPEL-OFFRE")) return workflowMetierRepository.findByCode("MARCHE_PUBLIC");
        if (c.contains("RECEPTION") || c.contains("PV")) return workflowMetierRepository.findByCode("RECEPTION_PV");
        if (c.contains("FACTURE") || c.contains("LIQUID")) return workflowMetierRepository.findByCode("FACTURE_LIQUIDATION");
        if (c.contains("FOURNISSEUR")) return workflowMetierRepository.findByCode("METIER_FOURNISSEUR");

        // Pastoral & Ranches
        if (c.contains("NAISSANCE") || c.contains("CHEPTEL-IN")) return workflowMetierRepository.findByCode("CHEPTEL_ENTREE");
        if (c.contains("SORTIE") || c.contains("REFORME")) return workflowMetierRepository.findByCode("CHEPTEL_SORTIE");
        if (c.contains("TRANSFERT") || c.contains("TRANSHUM")) return workflowMetierRepository.findByCode("CHEPTEL_TRANSFERT");
        if (c.contains("VACCIN") || c.contains("VET")) return workflowMetierRepository.findByCode("CAMPAGNE_VACCINATION");
        if (c.contains("INTRANT") || c.contains("ALIMENT")) return workflowMetierRepository.findByCode("INTRANTS_PASTORAUX");
        if (c.contains("VENTE-BET") || c.contains("CATTLE")) return workflowMetierRepository.findByCode("CATTLE_SALE");

        // Abattoirs
        if (c.contains("STABUL") || c.contains("ABAT-IN")) return workflowMetierRepository.findByCode("ABATTOIR_RECEPTION");
        if (c.contains("INSPECT") || c.contains("CONTROLE-VET")) return workflowMetierRepository.findByCode("CONTROLE_VETERINAIRE");
        if (c.contains("ABATTAGE") || c.contains("DECOUPE")) return workflowMetierRepository.findByCode("ABATTOIR_ABATTAGE");
        if (c.contains("FROID") || c.contains("RESSUAGE")) return workflowMetierRepository.findByCode("CHAMBRE_FROIDE_STOCK");
        if (c.contains("VIANDE-GROS") || c.contains("EXPEDITION")) return workflowMetierRepository.findByCode("VENTE_VIANDE_GROS");

        // Gouvernance & Transversal
        if (c.contains("NOTE") || c.contains("MEMO")) return workflowMetierRepository.findByCode("SERVICE_MEMO");
        if (c.contains("CIRCULAIRE")) return workflowMetierRepository.findByCode("CIRCULAIRE_DG");
        if (c.contains("DELEGATION")) return workflowMetierRepository.findByCode("DELEGATION_POUVOIR");
        if (c.contains("CONVENTION") || c.contains("ACCORD")) return workflowMetierRepository.findByCode("CONTRAT_CONVENTION");
        if (c.contains("METIER")) return workflowMetierRepository.findByCode("CREATION_METIER");
        if (c.contains("INFO") || c.contains("IT") || c.contains("ACCES")) return workflowMetierRepository.findByCode("DEMANDE_EQUIPEMENT_INFO");

        return java.util.Optional.empty();
    }

}
