package com.sodepa.erp.organigramme.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.authentication.application.ports.KeycloakProvisioningPort;
import com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService;
import com.sodepa.erp.configuration.parametres.infrastructure.entities.ParametreSystemeEntity;
import com.sodepa.erp.configuration.parametres.infrastructure.repo.ParametreSystemeRepository;
import com.sodepa.erp.organigramme.infrastructure.entities.*;
import com.sodepa.erp.organigramme.infrastructure.repo.AffectationPosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.TypeStructureRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.rh.infrastructure.entities.*;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.rh.infrastructure.repo.GrilleSalarialeRepository;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.Permissions;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeder officiel de démarrage SODEPA.
 * <p>
 * Ce composant implémente les directives de configuration et de peuplement issues des données
 * réelles extraites de l'organigramme (Résolution Conseil d'Administration 2018, Top Management,
 * Direction Générale, et Paramètres de Configuration Système).
 * </p>
 * Règles d'intégrité strictes :
 * 1. Purge propre des données en base et dans Keycloak si configuré.
 * 2. Création initiale des utilisateurs système racines 'admin_maker' et 'admin_checker' (qui ne sont PAS des employés).
 * 3. Initialisation de tous les paramètres de configuration système (Paie, Achats, Cheptel, Trésorerie, Formation, Sécurité).
 * 4. Création des 11 types de structures selon la nomenclature du CA 2018.
 * 5. Création des unités organisationnelles avec chemins matérialisés (/CA/DG/...).
 * 6. Création des postes budgétés avec leurs habilitations (permissions portées par les postes).
 * 7. Création des comptes utilisateurs, dossiers employés et affectations pour tout le personnel réel SODEPA.
 * 8. Désactivation irréversible des comptes racines 'admin_maker' et 'admin_checker' une fois le Directeur Général installé.
 * 9. Traçabilité intégrale de chaque opération dans ClickHouse.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SodepaScrapedDataSeeder implements CommandLineRunner {

    private final EntityManager entityManager;
    private final TypeStructureRepository typeStructureRepository;
    private final UniteOrganisationnelleRepository uniteRepository;
    private final PosteRepository posteRepository;
    private final AffectationPosteRepository affectationRepository;
    private final EmployeRepository employeRepository;
    private final GrilleSalarialeRepository grilleSalarialeRepository;
    private final UserRepository userRepository;
    private final ParametreSystemeRepository parametreSystemeRepository;
    private final ParametreConfigurationService parametreConfigurationService;
    private final KeycloakProvisioningPort keycloakProvisioningPort;
    private final ClickHouseManager clickHouseManager;
    private final ObjectMapper objectMapper;

    @Value("${sodepa.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${sodepa.seed.purge-on-startup:true}")
    private boolean purgeOnStartup;

    private static final UUID ORIGIN_SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID ADMIN_MAKER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_CHECKER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            log.info("SODEPA Data Seeder désactivé par configuration (sodepa.seed.enabled=false).");
            return;
        }

        log.info("═══════════════════════════════════════════════════════════════════════");
        log.info("     DÉMARRAGE DU PEUPLEMENT DES DONNÉES OFFICIELLES SODEPA            ");
        log.info("═══════════════════════════════════════════════════════════════════════");

        if (purgeOnStartup) {
            purgeDatabaseAndKeycloak();
        }

        // Étape 0.1 : Initialiser les super-administrateurs système bootstrap
        initBootstrapRoots();

        // Étape 1 : Initialiser tous les paramètres de configuration système (Paie, Achats, Cheptel, Trésorerie, Formation, Sécurité)
        seedParametresSysteme();

        // Étape 2 : Créer les 11 types de structures selon la résolution CA 2018
        Map<String, TypeStructureEntity> typesMap = seedTypesStructures();

        // Étape 3 : Créer les unités organisationnelles hiérarchiques avec materialized_path
        Map<String, UniteOrganisationnelleEntity> unitesMap = seedUnitesOrganisationnelles(typesMap);

        // Étape 4 : Créer les grilles salariales de référence SODEPA
        Map<Integer, GrilleSalarialeEntity> grillesMap = seedGrillesSalariales();

        // Étape 5 : Créer les postes budgétés avec leurs permissions associées
        Map<String, PosteEntity> postesMap = seedPostes(unitesMap);

        // Étape 6 : Créer l'ensemble des utilisateurs, dossiers employés et affectations du personnel réel
        seedCollaborateurs(unitesMap, postesMap, grillesMap);

        // Étape 0.2 : Désactivation stricte des comptes bootstrap 'admin_maker' et 'admin_checker'
        deactivateBootstrapRoots();

        log.info("═══════════════════════════════════════════════════════════════════════");
        log.info("     INITIALISATION ET PEUPLEMENT SODEPA ACHEVÉS AVEC SUCCÈS          ");
        log.info("═══════════════════════════════════════════════════════════════════════");
    }

    /**
     * Purge intégrale de la base de données locale (tables organigramme, RH, paramètres et utilisateurs)
     * et suppression de tous les comptes dans Keycloak.
     */
    private void purgeDatabaseAndKeycloak() {
        log.warn("Lancement de la purge complète en base de données et dans Keycloak...");

        // 1. Purge Keycloak
        try {
            keycloakProvisioningPort.purgeAllKeycloakUsers();
            log.info("Comptes Keycloak purgés avec succès.");
        } catch (Exception e) {
            log.warn("Avertissement lors de la purge Keycloak : {}", e.getMessage());
        }

        // 2. Purge PostgreSQL (ordre avec CASCADE)
        try {
            entityManager.createNativeQuery("""
                TRUNCATE TABLE
                    derogations_permissions,
                    affectations_postes,
                    rh_employes,
                    poste_permissions,
                    postes,
                    objectifs_unites,
                    unites_organisationnelles,
                    types_structures,
                    utilisateur_permissions,
                    utilisateur_telephones,
                    utilisateurs,
                    app_parametres_systeme
                CASCADE
            """).executeUpdate();
            entityManager.flush();
            log.info("Tables PostgreSQL purgées avec succès.");
        } catch (Exception e) {
            log.error("Erreur lors de la purge PostgreSQL : {}", e.getMessage(), e);
        }

        auditEvent("PURGE", "SYSTEM", "Purge intégrale préalable au peuplement SODEPA", ORIGIN_SYSTEM_ID.toString());
    }

    /**
     * Crée les utilisateurs système racines indispensables au démarrage.
     */
    private void initBootstrapRoots() {
        Set<Permissions> allPermissions = new HashSet<>(Arrays.asList(Permissions.values()));

        if (userRepository.findById(ADMIN_MAKER_ID).isEmpty()) {
            UtilisateurEntity adminMaker = UtilisateurEntity.builder()
                    .id(ADMIN_MAKER_ID)
                    .username("admin_maker")
                    .nom("Maker")
                    .prenom("Admin")
                    .email("admin.maker@sodepa.com")
                    .photoProfile("")
                    .actif(true)
                    .telephones(Set.of("+237600000001"))
                    .permissions(new HashSet<>(allPermissions))
                    .build();

            UUID iamId = provisionKeycloakSafe(ADMIN_MAKER_ID, "admin_maker", "Admin", "Maker", "admin.maker@sodepa.com");
            adminMaker.setIam(iamId != null ? iamId : ADMIN_MAKER_ID);
            userRepository.save(adminMaker);
            auditEvent("USER_BOOTSTRAP", ADMIN_MAKER_ID.toString(), "Création root admin_maker", ORIGIN_SYSTEM_ID.toString());
            log.info("Compte bootstrap 'admin_maker' initialisé.");
        }

        if (userRepository.findById(ADMIN_CHECKER_ID).isEmpty()) {
            UtilisateurEntity adminChecker = UtilisateurEntity.builder()
                    .id(ADMIN_CHECKER_ID)
                    .username("admin_checker")
                    .nom("Checker")
                    .prenom("Admin")
                    .email("admin.checker@sodepa.com")
                    .photoProfile("")
                    .actif(true)
                    .telephones(Set.of("+237600000002"))
                    .permissions(new HashSet<>(allPermissions))
                    .build();

            UUID iamId = provisionKeycloakSafe(ADMIN_CHECKER_ID, "admin_checker", "Admin", "Checker", "admin.checker@sodepa.com");
            adminChecker.setIam(iamId != null ? iamId : ADMIN_CHECKER_ID);
            userRepository.save(adminChecker);
            auditEvent("USER_BOOTSTRAP", ADMIN_CHECKER_ID.toString(), "Création root admin_checker", ORIGIN_SYSTEM_ID.toString());
            log.info("Compte bootstrap 'admin_checker' initialisé.");
        }
    }

    /**
     * Initialise l'ensemble des paramètres de configuration système SODEPA
     * (Paie & Fiscalité, Achats & Marchés, Cheptel Pastoral, Régies de Trésorerie, Formation et Sécurité).
     */
    private void seedParametresSysteme() {
        log.info("Initialisation des Paramètres de Configuration Système SODEPA...");

        List<ParametreSystemeEntity> parametres = List.of(
                // --- PAIE & FISCALITÉ CAMEROUNAISE ---
                createParam("paie.plafond_cnps", "750000", "DECIMAL", "PAIE",
                        "Plafond mensuel d'assiette cotisable CNPS (FCFA)", "DAF,DRH"),
                createParam("paie.taux_cnps_pvid_salarial", "0.042", "DECIMAL", "PAIE",
                        "Taux CNPS PVID salarial (4.2%)", "DAF,DRH"),
                createParam("paie.taux_cnps_pvid_patronal", "0.042", "DECIMAL", "PAIE",
                        "Taux CNPS PVID patronal (4.2%)", "DAF,DRH"),
                createParam("paie.taux_cnps_pf_patronal", "0.070", "DECIMAL", "PAIE",
                        "Taux CNPS Prestations Familiales patronal (7.0%)", "DAF,DRH"),
                createParam("paie.taux_cnps_at_patronal", "0.025", "DECIMAL", "PAIE",
                        "Taux CNPS Accidents du Travail patronal (2.5%)", "DAF,DRH"),
                createParam("paie.taux_cfc_salarial", "0.010", "DECIMAL", "PAIE",
                        "Taux Crédit Foncier du Cameroun salarial (1.0%)", "DAF,DRH"),
                createParam("paie.taux_cfc_patronal", "0.015", "DECIMAL", "PAIE",
                        "Taux Crédit Foncier du Cameroun patronal (1.5%)", "DAF,DRH"),
                createParam("paie.taux_fne_patronal", "0.010", "DECIMAL", "PAIE",
                        "Taux Fonds National de l'Emploi patronal (1.0%)", "DAF,DRH"),
                createParam("paie.taux_cac_sur_irpp", "0.100", "DECIMAL", "PAIE",
                        "Taux Centimes Additionnels Communaux sur IRPP (10%)", "DAF,DRH"),
                createParam("paie.taux_abattement_frais_pro", "0.30", "DECIMAL", "PAIE",
                        "Taux d'abattement forfaitaire pour frais professionnels IRPP (30%)", "DAF,DRH"),
                createParam("paie.indemnite_transport_defaut", "20000", "DECIMAL", "PAIE",
                        "Indemnité forfaitaire de transport par défaut (FCFA)", "DRH,DAF"),
                createParam("paie.indemnite_logement_cadre", "35000", "DECIMAL", "PAIE",
                        "Indemnité forfaitaire de logement cadres (FCFA)", "DRH,DAF"),
                createParam("paie.categorie_min_logement", "7", "INTEGER", "PAIE",
                        "Catégorie socioprofessionnelle minimale donnant droit à indemnité de logement", "DRH"),

                // --- ACHATS & MARCHÉS PUBLICS ---
                createParam("achats.taux_tva", "19.25", "DECIMAL", "ACHATS",
                        "Taux légal de la TVA Cameroun (19.25%)", "DAF"),
                createParam("achats.seuil_achat_direct", "500000", "DECIMAL", "ACHATS",
                        "Seuil maximum Achat Direct / Caisse sans proforma formel (FCFA)", "DAF,DG"),
                createParam("achats.seuil_bon_commande", "15000000", "DECIMAL", "ACHATS",
                        "Seuil maximum Bon de Commande sur 3 factures proforma (FCFA)", "DAF,DG"),
                createParam("achats.seuil_lettre_commande", "50000000", "DECIMAL", "ACHATS",
                        "Seuil maximum Lettre-Commande (FCFA), au-delà Marché Public CIPM", "DAF,DG"),

                // --- CHEPTEL & RANCHES PASTORAUX ---
                createParam("pastoral.matricule_masque", "SDP-%s-%d-%04d", "STRING", "CHEPTEL",
                        "Masque format du numéro de boucle auriculaire bovin", "ADMIN"),
                createParam("pastoral.poids_naissance_defaut", "28.0", "DECIMAL", "CHEPTEL",
                        "Poids standard de naissance d'un veau (kg)", "DIRECTEUR_RANCH"),
                createParam("pastoral.compte_debit_naissance", "2191", "STRING", "CHEPTEL",
                        "Compte SYSCOHADA débit naissance veau", "DAF"),
                createParam("pastoral.compte_credit_naissance", "7217", "STRING", "CHEPTEL",
                        "Compte SYSCOHADA crédit naissance veau", "DAF"),
                createParam("pastoral.compte_debit_mortalite", "8310", "STRING", "CHEPTEL",
                        "Compte SYSCOHADA débit perte/mortalité cheptel", "DAF"),

                // --- RÉGIES D'AVANCES & TRÉSORERIE ---
                createParam("tresorerie.regie_plafond_menue_depense", "100000", "DECIMAL", "TRESORERIE",
                        "Plafond maximum par bon de caisse régie d'avance (FCFA)", "DAF,DG"),

                // --- FORMATION & STAGES ---
                createParam("formation.delai_forclusion_stage_jours", "14", "INTEGER", "FORMATION",
                        "Délai minimum (en jours) avant la fin pour déposer une demande de prolongation de stage", "DRH"),
                createParam("formation.anciennete_min_reclassement_annees", "5", "INTEGER", "FORMATION",
                        "Ancienneté minimale requise (années) pour reclassement indiciaire post-formation", "DRH,DG"),
                createParam("formation.duree_min_formation_reclassement_mois", "18", "INTEGER", "FORMATION",
                        "Durée minimale (mois) d'une formation diplômante ouvrant droit à reclassement", "DRH,DG"),

                // --- SÉCURITÉ & SESSIONS ---
                createParam("securite.session_inactivite_minutes", "30", "INTEGER", "SECURITE",
                        "Délai d'inactivité avant expiration de session (minutes)", "ADMIN"),
                createParam("securite.tentatives_login_max", "5", "INTEGER", "SECURITE",
                        "Nombre maximum de tentatives d'authentification avant verrouillage temporaire", "ADMIN")
        );

        for (ParametreSystemeEntity p : parametres) {
            parametreSystemeRepository.findById(p.getCle()).ifPresentOrElse(
                    existing -> {
                        existing.setValeur(p.getValeur());
                        existing.setDescription(p.getDescription());
                        existing.setTypeValeur(p.getTypeValeur());
                        existing.setCategorie(p.getCategorie());
                        existing.setModifiablePar(p.getModifiablePar());
                        existing.setDerniereModification(LocalDateTime.now());
                        existing.setModifiePar("SYSTEM_SEEDER");
                        parametreSystemeRepository.save(existing);
                        auditEvent("PARAMETRE_SYSTEME", existing.getCle(), "Paramètre mis à jour " + existing.getCle() + " = " + existing.getValeur(), ADMIN_MAKER_ID.toString());
                    },
                    () -> {
                        parametreSystemeRepository.save(p);
                        auditEvent("PARAMETRE_SYSTEME", p.getCle(), "Paramètre initialisé " + p.getCle() + " = " + p.getValeur(), ADMIN_MAKER_ID.toString());
                    }
            );
        }

        // Rechargement immédiat du cache mémoire
        parametreConfigurationService.rechargerParametres();
        log.info("{} paramètres de configuration système initialisés et mis en cache avec succès.", parametres.size());
    }

    private ParametreSystemeEntity createParam(String cle, String valeur, String typeValeur, String categorie,
                                              String description, String modifiablePar) {
        return ParametreSystemeEntity.builder()
                .cle(cle)
                .valeur(valeur)
                .typeValeur(typeValeur)
                .categorie(categorie)
                .description(description)
                .modifiablePar(modifiablePar)
                .derniereModification(LocalDateTime.now())
                .modifiePar("SYSTEM_SEEDER")
                .build();
    }

    /**
     * Peuple les 11 types de structures selon la nomenclature officielle du CA 2018.
     */
    private Map<String, TypeStructureEntity> seedTypesStructures() {
        log.info("Création des 11 Types de Structures SODEPA...");
        Map<String, TypeStructureEntity> map = new HashMap<>();

        // 1. Conseil d'Administration (Niveau 1, pas de parent)
        TypeStructureEntity ca = createTypeStructure("CA", "Conseil d'Administration", 1, null,
                "solar:crown-line-duotone", "#b91c1c",
                "Définition des orientations stratégiques, contrôle des activités et approbation budgétaire.");
        map.put("CA", ca);

        // 2. Direction Générale (Niveau 2, parent CA)
        TypeStructureEntity dg = createTypeStructure("DG", "Direction Générale", 2, ca,
                "solar:buildings-2-line-duotone", "#1e3a8a",
                "Conduite générale, garant de la vision, gestion opérationnelle et représentation légale.");
        map.put("DG", dg);

        // 3. Division (Niveau 3, parent DG)
        TypeStructureEntity div = createTypeStructure("DIV", "Division", 3, dg,
                "solar:branching-paths-down-line-duotone", "#0891b2",
                "Contrôle interne, audit, prospective, planification et appui transversal.");
        map.put("DIV", div);

        // 4. Direction Centrale (Niveau 3, parent DG)
        TypeStructureEntity dir = createTypeStructure("DIR", "Direction Centrale", 3, dg,
                "solar:buildings-2-line-duotone", "#2563eb",
                "Pilotage de domaines majeurs : Production, Commercialisation, Finances et RH.");
        map.put("DIR", dir);

        // 5. Sous-Direction (Niveau 4, parent DIR)
        TypeStructureEntity sdir = createTypeStructure("SDIR", "Sous-Direction", 4, dir,
                "solar:branching-paths-down-line-duotone", "#0284c7",
                "Coordination opérationnelle de sous-secteurs techniques, financiers et administratifs.");
        map.put("SDIR", sdir);

        // 6. Cellule Rattachée (Niveau 4, parent DG)
        TypeStructureEntity cell = createTypeStructure("CELL", "Cellule Rattachée", 4, dg,
                "solar:shield-check-line-duotone", "#4f46e5",
                "Études spécialisées, systèmes d'information, partenariats, statistiques et QHSE.");
        map.put("CELL", cell);

        // 7. Service (Niveau 5, parent SDIR)
        TypeStructureEntity serv = createTypeStructure("SERV", "Service", 5, sdir,
                "solar:document-text-line-duotone", "#059669",
                "Exécution des opérations quotidiennes administratives, techniques ou commerciales.");
        map.put("SERV", serv);

        // 8. Ranch Pastoral (Niveau 3, parent DG)
        TypeStructureEntity ranch = createTypeStructure("RANCH", "Ranch Pastoral", 3, dg,
                "solar:leaf-line-duotone", "#15803d",
                "Production pastorale extensive, amélioration génétique, gestion du cheptel et des pâturages.");
        map.put("RANCH", ranch);

        // 9. Abattoir Industriel Frigorifique (Niveau 3, parent DG)
        TypeStructureEntity abat = createTypeStructure("ABAT", "Abattoir Industriel Frigorifique", 3, dg,
                "solar:shop-2-line-duotone", "#ea580c",
                "Abattage hygiénique, découpe, chaîne du froid, stockage frigorifique et commercialisation de carcasses.");
        map.put("ABAT", abat);

        // 10. Station d'Élevage et de Recherche (Niveau 3, parent DG)
        TypeStructureEntity stat = createTypeStructure("STAT", "Station d'Élevage et de Recherche", 3, dg,
                "solar:test-tube-line-duotone", "#7c3aed",
                "Laiterie, recherche et biotechnologie de reproduction (insémination, amélioration de races).");
        map.put("STAT", stat);

        // 11. Entrepôt & Magasin Frigorifique (Niveau 4, parent ABAT)
        TypeStructureEntity entr = createTypeStructure("ENTR", "Entrepôt & Magasin Frigorifique", 4, abat,
                "solar:box-minimalistic-line-duotone", "#d97706",
                "Réception, conservation en chaîne du froid, gestion des stocks et distribution de viande.");
        map.put("ENTR", entr);

        return map;
    }

    private TypeStructureEntity createTypeStructure(String code, String libelle, int niveau,
                                                   TypeStructureEntity parent, String icone, String couleur, String missions) {
        TypeStructureEntity entity = typeStructureRepository.findByCode(code).orElseGet(() -> {
            TypeStructureEntity t = TypeStructureEntity.builder()
                    .id(UUID.randomUUID())
                    .code(code)
                    .libelle(libelle)
                    .niveauHierarchique(niveau)
                    .typeParent(parent)
                    .icone(icone)
                    .couleur(couleur)
                    .missionsTypes(missions)
                    .actif(true)
                    .build();
            return typeStructureRepository.save(t);
        });
        auditEvent("TYPE_STRUCTURE", entity.getId().toString(), "Type Structure " + code, ADMIN_MAKER_ID.toString());
        return entity;
    }

    /**
     * Peuple l'ensemble des unités organisationnelles avec hiérarchie et materialized_path.
     */
    private Map<String, UniteOrganisationnelleEntity> seedUnitesOrganisationnelles(Map<String, TypeStructureEntity> types) {
        log.info("Création des Unités Organisationnelles SODEPA...");
        Map<String, UniteOrganisationnelleEntity> map = new HashMap<>();

        LocalDate dateCreationSodepa = LocalDate.of(1974, 3, 8);

        // 1. Conseil d'Administration
        UniteOrganisationnelleEntity ca = createUnite("CA", "Conseil d'Administration",
                TypeUniteOrganisationnelle.DIRECTION_GENERALE, types.get("CA"), null, "/CA/", "Yaoundé", dateCreationSodepa);
        map.put("CA", ca);

        // 2. Direction Générale
        UniteOrganisationnelleEntity dg = createUnite("DG", "Direction Générale",
                TypeUniteOrganisationnelle.DIRECTION_GENERALE, types.get("DG"), ca, "/CA/DG/", "Siège Yaoundé", dateCreationSodepa);
        map.put("DG", dg);

        // Rattachés DG
        UniteOrganisationnelleEntity dcai = createUnite("DCAI", "Division du Contrôle et Audit Interne",
                TypeUniteOrganisationnelle.DEPARTEMENT, types.get("DIV"), dg, "/CA/DG/DCAI/", "Siège Yaoundé", dateCreationSodepa);
        map.put("DCAI", dcai);

        UniteOrganisationnelleEntity dpd = createUnite("DPD", "Division Prospective et Développement",
                TypeUniteOrganisationnelle.DEPARTEMENT, types.get("DIV"), dg, "/CA/DG/DPD/", "Siège Yaoundé", dateCreationSodepa);
        map.put("DPD", dpd);

        UniteOrganisationnelleEntity cellQhse = createUnite("CELL_QHSE", "Cellule Qualité, Hygiène, Sécurité, Environnement",
                TypeUniteOrganisationnelle.CELLULE, types.get("CELL"), dg, "/CA/DG/CELL_QHSE/", "Siège Yaoundé", dateCreationSodepa);
        map.put("CELL_QHSE", cellQhse);

        UniteOrganisationnelleEntity cellCom = createUnite("CELL_COM", "Cellule Communication et Traduction",
                TypeUniteOrganisationnelle.CELLULE, types.get("CELL"), dg, "/CA/DG/CELL_COM/", "Siège Yaoundé", dateCreationSodepa);
        map.put("CELL_COM", cellCom);

        UniteOrganisationnelleEntity servCourrier = createUnite("SERV_COURRIER", "Service du Courrier & Liaison",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), dg, "/CA/DG/SERV_COURRIER/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_COURRIER", servCourrier);

        // Sous DPD
        UniteOrganisationnelleEntity cspp = createUnite("CSPP", "Cellule Statistiques, Programmation et Projets",
                TypeUniteOrganisationnelle.CELLULE, types.get("CELL"), dpd, "/CA/DG/DPD/CSPP/", "Siège Yaoundé", dateCreationSodepa);
        map.put("CSPP", cspp);

        UniteOrganisationnelleEntity cpe = createUnite("CPE", "Cellule Partenariat et Encadrement",
                TypeUniteOrganisationnelle.CELLULE, types.get("CELL"), dpd, "/CA/DG/DPD/CPE/", "Siège Yaoundé", dateCreationSodepa);
        map.put("CPE", cpe);

        UniteOrganisationnelleEntity ci = createUnite("CI", "Cellule Informatique",
                TypeUniteOrganisationnelle.CELLULE, types.get("CELL"), dpd, "/CA/DG/DPD/CI/", "Siège Yaoundé", dateCreationSodepa);
        map.put("CI", ci);

        // Directions Métier
        UniteOrganisationnelleEntity dpc = createUnite("DPC", "Direction de la Production et de la Commercialisation",
                TypeUniteOrganisationnelle.DIRECTION, types.get("DIR"), dg, "/CA/DG/DPC/", "Siège Yaoundé", dateCreationSodepa);
        map.put("DPC", dpc);

        UniteOrganisationnelleEntity daf = createUnite("DAF", "Direction Administrative et Financière",
                TypeUniteOrganisationnelle.DIRECTION, types.get("DIR"), dg, "/CA/DG/DAF/", "Siège Yaoundé", dateCreationSodepa);
        map.put("DAF", daf);

        // Sous DPC
        UniteOrganisationnelleEntity sdTech = createUnite("SD_TECH", "Sous-Direction Technique",
                TypeUniteOrganisationnelle.DEPARTEMENT, types.get("SDIR"), dpc, "/CA/DG/DPC/SD_TECH/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SD_TECH", sdTech);

        UniteOrganisationnelleEntity sdComm = createUnite("SD_COMM", "Sous-Direction Commerciale",
                TypeUniteOrganisationnelle.DEPARTEMENT, types.get("SDIR"), dpc, "/CA/DG/DPC/SD_COMM/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SD_COMM", sdComm);

        UniteOrganisationnelleEntity servRanchs = createUnite("SERV_RANCHS", "Service des Ranchs",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdTech, "/CA/DG/DPC/SD_TECH/SERV_RANCHS/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_RANCHS", servRanchs);

        UniteOrganisationnelleEntity servPaturage = createUnite("SERV_PATURAGE", "Service Aménagement & Pâturage",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdTech, "/CA/DG/DPC/SD_TECH/SERV_PATURAGE/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_PATURAGE", servPaturage);

        UniteOrganisationnelleEntity servTransfo = createUnite("SERV_TRANSFO", "Service Industries de Transformation",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdTech, "/CA/DG/DPC/SD_TECH/SERV_TRANSFO/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_TRANSFO", servTransfo);

        UniteOrganisationnelleEntity brigMaint = createUnite("BRIG_MAINT", "Brigade de Maintenance",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdTech, "/CA/DG/DPC/SD_TECH/BRIG_MAINT/", "Siège Yaoundé", dateCreationSodepa);
        map.put("BRIG_MAINT", brigMaint);

        UniteOrganisationnelleEntity servVentes = createUnite("SERV_VENTES", "Service des Ventes",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdComm, "/CA/DG/DPC/SD_COMM/SERV_VENTES/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_VENTES", servVentes);

        UniteOrganisationnelleEntity servMkt = createUnite("SERV_MKT", "Service Marketing",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdComm, "/CA/DG/DPC/SD_COMM/SERV_MKT/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_MKT", servMkt);

        // Sous DAF
        UniteOrganisationnelleEntity sdrh = createUnite("SDRH", "Sous-Direction des Ressources Humaines",
                TypeUniteOrganisationnelle.DEPARTEMENT, types.get("SDIR"), daf, "/CA/DG/DAF/SDRH/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SDRH", sdrh);

        UniteOrganisationnelleEntity sdfbm = createUnite("SDFBM", "Sous-Direction Finances, Budget & Matériel",
                TypeUniteOrganisationnelle.DEPARTEMENT, types.get("SDIR"), daf, "/CA/DG/DAF/SDFBM/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SDFBM", sdfbm);

        UniteOrganisationnelleEntity servPers = createUnite("SERV_PERS", "Service du Personnel",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdrh, "/CA/DG/DAF/SDRH/SERV_PERS/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_PERS", servPers);

        UniteOrganisationnelleEntity servSolde = createUnite("SERV_SOLDE", "Service de la Solde et des Pensions",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdrh, "/CA/DG/DAF/SDRH/SERV_SOLDE/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_SOLDE", servSolde);

        UniteOrganisationnelleEntity servForm = createUnite("SERV_FORM", "Service Formation",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdrh, "/CA/DG/DAF/SDRH/SERV_FORM/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_FORM", servForm);

        UniteOrganisationnelleEntity servCompta = createUnite("SERV_COMPTA", "Service Comptable",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdfbm, "/CA/DG/DAF/SDFBM/SERV_COMPTA/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_COMPTA", servCompta);

        UniteOrganisationnelleEntity servFin = createUnite("SERV_FIN", "Service Financier et des Recettes",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdfbm, "/CA/DG/DAF/SDFBM/SERV_FIN/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_FIN", servFin);

        UniteOrganisationnelleEntity servBudget = createUnite("SERV_BUDGET", "Service du Budget",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdfbm, "/CA/DG/DAF/SDFBM/SERV_BUDGET/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_BUDGET", servBudget);

        UniteOrganisationnelleEntity servMarches = createUnite("SERV_MARCHES", "Service Marchés et Patrimoine",
                TypeUniteOrganisationnelle.SERVICE, types.get("SERV"), sdfbm, "/CA/DG/DAF/SDFBM/SERV_MARCHES/", "Siège Yaoundé", dateCreationSodepa);
        map.put("SERV_MARCHES", servMarches);

        // Pôles Régionaux
        UniteOrganisationnelleEntity ranchFaro = createUnite("RANCH_FARO", "Ranch Pastoral de Faro",
                TypeUniteOrganisationnelle.RANCH, types.get("RANCH"), dg, "/CA/DG/RANCH_FARO/", "Poli (Nord)", dateCreationSodepa);
        map.put("RANCH_FARO", ranchFaro);

        UniteOrganisationnelleEntity ranchNdokayo = createUnite("RANCH_NDOKAYO", "Ranch Pastoral de Ndokayo",
                TypeUniteOrganisationnelle.RANCH, types.get("RANCH"), dg, "/CA/DG/RANCH_NDOKAYO/", "Bétaré-Oya (Est)", dateCreationSodepa);
        map.put("RANCH_NDOKAYO", ranchNdokayo);

        UniteOrganisationnelleEntity ranchDumbo = createUnite("RANCH_DUMBO", "Ranch Pastoral de Dumbo",
                TypeUniteOrganisationnelle.RANCH, types.get("RANCH"), dg, "/CA/DG/RANCH_DUMBO/", "Misaje (Nord-Ouest)", dateCreationSodepa);
        map.put("RANCH_DUMBO", ranchDumbo);

        UniteOrganisationnelleEntity abatDla = createUnite("ABAT_DLA", "Abattoir Frigorifique de Douala",
                TypeUniteOrganisationnelle.ABATTOIR, types.get("ABAT"), dg, "/CA/DG/ABAT_DLA/", "Douala (Littoral)", dateCreationSodepa);
        map.put("ABAT_DLA", abatDla);

        UniteOrganisationnelleEntity abatYde = createUnite("ABAT_YDE", "Abattoir Frigorifique de Yaoundé",
                TypeUniteOrganisationnelle.ABATTOIR, types.get("ABAT"), dg, "/CA/DG/ABAT_YDE/", "Yaoundé (Centre)", dateCreationSodepa);
        map.put("ABAT_YDE", abatYde);

        UniteOrganisationnelleEntity abatNgao = createUnite("ABAT_NGAO", "Abattoir Frigorifique de Ngaoundéré",
                TypeUniteOrganisationnelle.ABATTOIR, types.get("ABAT"), dg, "/CA/DG/ABAT_NGAO/", "Ngaoundéré (Adamaoua)", dateCreationSodepa);
        map.put("ABAT_NGAO", abatNgao);

        UniteOrganisationnelleEntity statJakiri = createUnite("STAT_JAKIRI", "Station d'Élevage et Laiterie de Jakiri",
                TypeUniteOrganisationnelle.POSTE_AVANCE, types.get("STAT"), dg, "/CA/DG/STAT_JAKIRI/", "Jakiri (Nord-Ouest)", dateCreationSodepa);
        map.put("STAT_JAKIRI", statJakiri);

        return map;
    }

    private UniteOrganisationnelleEntity createUnite(String code, String intitule, TypeUniteOrganisationnelle typeUnite,
                                                     TypeStructureEntity typeStructure, UniteOrganisationnelleEntity parent,
                                                     String matPath, String localite, LocalDate dateCreation) {
        UniteOrganisationnelleEntity entity = uniteRepository.findByCode(code).orElseGet(() -> {
            UniteOrganisationnelleEntity u = UniteOrganisationnelleEntity.builder()
                    .id(UUID.randomUUID())
                    .code(code)
                    .intitule(intitule)
                    .typeUnite(typeUnite)
                    .typeStructure(typeStructure)
                    .parent(parent)
                    .materializedPath(matPath)
                    .localisation(localite)
                    .statut(StatutUnite.ACTIF)
                    .dateCreation(dateCreation)
                    .build();
            return uniteRepository.save(u);
        });
        auditEvent("UNITE_ORGANISATIONNELLE", entity.getId().toString(), "Unité " + code + " (" + matPath + ")", ADMIN_MAKER_ID.toString());
        return entity;
    }

    /**
     * Crée les grilles indiciaires salariales SODEPA (Catégories 1 à 12, Échelon A).
     */
    private Map<Integer, GrilleSalarialeEntity> seedGrillesSalariales() {
        log.info("Initialisation de la grille salariale conventionnelle SODEPA...");
        Map<Integer, GrilleSalarialeEntity> map = new HashMap<>();

        double[] bases = {
                60000.0, 75000.0, 95000.0, 120000.0, 150000.0, 190000.0,
                240000.0, 310000.0, 400000.0, 520000.0, 680000.0, 900000.0
        };

        for (int cat = 1; cat <= 12; cat++) {
            final int c = cat;
            BigDecimal base = BigDecimal.valueOf(bases[cat - 1]);
            GrilleSalarialeEntity grille = grilleSalarialeRepository.findByCategorieAndEchelon(cat, "A").orElseGet(() -> {
                GrilleSalarialeEntity g = GrilleSalarialeEntity.builder()
                        .categorie(c)
                        .echelon("A")
                        .groupe(1)
                        .salaireBase(base)
                        .tauxHoraire(base.divide(BigDecimal.valueOf(173.33), 2, java.math.RoundingMode.HALF_UP))
                        .actif(true)
                        .build();
                return grilleSalarialeRepository.save(g);
            });
            map.put(cat, grille);
        }
        return map;
    }

    /**
     * Peuple l'ensemble des postes avec leurs permissions portées.
     */
    private Map<String, PosteEntity> seedPostes(Map<String, UniteOrganisationnelleEntity> unites) {
        log.info("Création des Postes budgétés SODEPA avec habilitations...");
        Map<String, PosteEntity> map = new HashMap<>();

        Set<Permissions> allPermissions = new HashSet<>(Arrays.asList(Permissions.values()));

        // 1. Postes Gouvernance & Direction Générale
        PosteEntity posteDg = createPoste("POSTE_DG", "Directeur Général", unites.get("DG"), null,
                CategorieConvention.CADRE_DIRIGEANT, allPermissions);
        map.put("POSTE_DG", posteDg);

        PosteEntity postePca = createPoste("POSTE_PCA", "Président du Conseil d'Administration", unites.get("CA"), null,
                CategorieConvention.CADRE_DIRIGEANT, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.GET_FULL_USER_INFO,
                        Permissions.VIEW_BUDGET, Permissions.VIEW_FOLDER,
                        Permissions.VALIDATE_OR_REJECT_ORGANIGRAMME, Permissions.VALIDATE_BUDGET
                ));
        map.put("POSTE_PCA", postePca);

        for (int i = 1; i <= 9; i++) {
            String code = "POSTE_ADMIN_CA_" + i;
            PosteEntity adminCa = createPoste(code, "Membre du Conseil d'Administration n°" + i, unites.get("CA"), postePca,
                    CategorieConvention.CADRE_DIRIGEANT, Set.of(
                            Permissions.GET_ORGANIGRAMME_INFO, Permissions.VIEW_BUDGET, Permissions.VIEW_FOLDER
                    ));
            map.put(code, adminCa);
        }

        PosteEntity posteAttache = createPoste("POSTE_ATTACHE_DIR", "Attaché de Direction", unites.get("DG"), posteDg,
                CategorieConvention.CADRE_SUPERIEUR, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.CREATE_EXPRESSION, Permissions.VIEW_EXPRESSION
                ));
        map.put("POSTE_ATTACHE_DIR", posteAttache);

        PosteEntity posteCscg = createPoste("POSTE_CSCG", "Chargé du Suivi et Contrôle de Gestion", unites.get("DG"), posteDg,
                CategorieConvention.CADRE_SUPERIEUR, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.VIEW_BUDGET, Permissions.VIEW_FOLDER,
                        Permissions.VIEW_ECRITURE, Permissions.VIEW_IMMOBILISATION, Permissions.LIST_ALL_COMPTE
                ));
        map.put("POSTE_CSCG", posteCscg);

        PosteEntity posteCom = createPoste("POSTE_COM_TRAD", "Chargée de la Communication et Traduction", unites.get("CELL_COM"), posteDg,
                CategorieConvention.CADRE_SUPERIEUR, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.CREATE_EXPRESSION, Permissions.VIEW_EXPRESSION
                ));
        map.put("POSTE_COM_TRAD", posteCom);

        PosteEntity posteQhse = createPoste("POSTE_QHSE", "Attachée Qualité, Hygiène, Sécurité, Environnement", unites.get("CELL_QHSE"), posteDg,
                CategorieConvention.CADRE_SUPERIEUR, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.CREATE_EXPRESSION, Permissions.VIEW_EXPRESSION
                ));
        map.put("POSTE_QHSE", posteQhse);

        PosteEntity posteCourrier = createPoste("POSTE_CHEF_COURRIER", "Chef Service du Courrier", unites.get("SERV_COURRIER"), posteDg,
                CategorieConvention.AGENT_DE_MAITRISE, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.CREATE_EXPRESSION, Permissions.VIEW_EXPRESSION
                ));
        map.put("POSTE_CHEF_COURRIER", posteCourrier);

        // 2. Division Contrôle et Audit Interne (DCAI)
        Set<Permissions> dcaiPermissions = Set.of(
                Permissions.GET_ORGANIGRAMME_INFO, Permissions.GET_FULL_USER_INFO,
                Permissions.VIEW_BUDGET, Permissions.VIEW_FOLDER, Permissions.VIEW_ECRITURE,
                Permissions.VIEW_IMMOBILISATION, Permissions.GET_FULL_BANK_INFO, Permissions.GET_FULL_COMPTE_INFO,
                Permissions.GET_FULL_JOURNAL_INFO, Permissions.GET_FULL_TIERS_INFO, Permissions.LIST_ALL_BANKS,
                Permissions.LIST_ALL_COMPTE, Permissions.LIST_ALL_JOURNAL, Permissions.LIST_RELEVE_BANCAIRE,
                Permissions.LIST_EXPRESSIONS, Permissions.LIST_FOLDERS, Permissions.LIST_RECEPTIONS,
                Permissions.VALIDATE_OR_REJECT_ECRITURE, Permissions.VALIDATE_OR_REJECT_JOURNAL,
                Permissions.VALIDATE_OR_REJECT_BANK_INFO, Permissions.VALIDATE_OR_REJECT_COMPTE,
                Permissions.VALIDATE_OR_REJECT_TIERS, Permissions.VALIDATE_OR_REJECT_IMMOBILISATION,
                Permissions.VALIDATE_OR_REJECT_RELEVE_BANCAIRE, Permissions.VALIDATE_FOLDER,
                Permissions.VALIDATE_RECEPTION, Permissions.VALIDATE_LIQUIDATION
        );

        PosteEntity posteChefDcai = createPoste("POSTE_CHEF_DCAI", "Chef de Division DCAI", unites.get("DCAI"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, dcaiPermissions);
        map.put("POSTE_CHEF_DCAI", posteChefDcai);

        PosteEntity posteAuditeur1 = createPoste("POSTE_AUDITEUR_1", "Auditrice Interne", unites.get("DCAI"), posteChefDcai,
                CategorieConvention.CADRE_SUPERIEUR, dcaiPermissions);
        map.put("POSTE_AUDITEUR_1", posteAuditeur1);

        PosteEntity posteAuditeur2 = createPoste("POSTE_AUDITEUR_2", "Auditeur Assistant n°1", unites.get("DCAI"), posteChefDcai,
                CategorieConvention.CADRE_SUPERIEUR, dcaiPermissions);
        map.put("POSTE_AUDITEUR_2", posteAuditeur2);

        PosteEntity posteAuditeur3 = createPoste("POSTE_AUDITEUR_3", "Auditeur Assistant n°2", unites.get("DCAI"), posteChefDcai,
                CategorieConvention.CADRE_SUPERIEUR, dcaiPermissions);
        map.put("POSTE_AUDITEUR_3", posteAuditeur3);

        // 3. Division Prospective et Développement (DPD)
        Set<Permissions> dpdPermissions = Set.of(
                Permissions.GET_ORGANIGRAMME_INFO, Permissions.VIEW_BUDGET, Permissions.CREATE_BUDGET,
                Permissions.UPDATE_BUDGET, Permissions.VIEW_EXPRESSION, Permissions.VIEW_FOLDER
        );

        PosteEntity posteChefDpd = createPoste("POSTE_CHEF_DPD", "Chef de Division DPD", unites.get("DPD"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, dpdPermissions);
        map.put("POSTE_CHEF_DPD", posteChefDpd);

        PosteEntity posteDpdCspp1 = createPoste("POSTE_DPD_CSPP_1", "Chargé d'Études Assistant n°1 (CSPP)", unites.get("CSPP"), posteChefDpd,
                CategorieConvention.CADRE_SUPERIEUR, dpdPermissions);
        map.put("POSTE_DPD_CSPP_1", posteDpdCspp1);

        PosteEntity posteDpdCspp2 = createPoste("POSTE_DPD_CSPP_2", "Chargé d'Études Assistant n°2 (CSPP)", unites.get("CSPP"), posteChefDpd,
                CategorieConvention.CADRE_SUPERIEUR, dpdPermissions);
        map.put("POSTE_DPD_CSPP_2", posteDpdCspp2);

        PosteEntity posteChefCpe = createPoste("POSTE_CHEF_CPE", "Chef de Cellule Partenariat & Encadrement", unites.get("CPE"), posteChefDpd,
                CategorieConvention.CADRE_SUPERIEUR, dpdPermissions);
        map.put("POSTE_CHEF_CPE", posteChefCpe);

        PosteEntity posteCpe2 = createPoste("POSTE_CPE_2", "Chargé d'Études Assistant n°2 (CPE)", unites.get("CPE"), posteChefCpe,
                CategorieConvention.CADRE_SUPERIEUR, dpdPermissions);
        map.put("POSTE_CPE_2", posteCpe2);

        PosteEntity posteChefCi = createPoste("POSTE_CHEF_CI", "Chef de Cellule Informatique", unites.get("CI"), posteChefDpd,
                CategorieConvention.CADRE_SUPERIEUR, dpdPermissions);
        map.put("POSTE_CHEF_CI", posteChefCi);

        PosteEntity posteCi1 = createPoste("POSTE_CI_1", "Chargé d'Études Assistant n°1 (Informatique)", unites.get("CI"), posteChefCi,
                CategorieConvention.CADRE_SUPERIEUR, dpdPermissions);
        map.put("POSTE_CI_1", posteCi1);

        PosteEntity posteCi2 = createPoste("POSTE_CI_2", "Chargée d'Études Assistant n°2 (Informatique)", unites.get("CI"), posteChefCi,
                CategorieConvention.CADRE_SUPERIEUR, dpdPermissions);
        map.put("POSTE_CI_2", posteCi2);

        // 4. Direction de la Production et de la Commercialisation (DPC)
        Set<Permissions> dpcDirecteurPerms = Set.of(
                Permissions.GET_ORGANIGRAMME_INFO, Permissions.LIST_EXPRESSIONS, Permissions.CREATE_EXPRESSION,
                Permissions.VALIDATE_EXPRESSION, Permissions.VIEW_EXPRESSION, Permissions.UPDATE_EXPRESSION,
                Permissions.LIST_FOLDERS, Permissions.CREATE_FOLDER, Permissions.VIEW_FOLDER, Permissions.VIEW_ORDER,
                Permissions.VALIDATE_FOLDER, Permissions.UPDATE_FOLDER, Permissions.LIST_RECEPTIONS,
                Permissions.CREATE_RECEPTION, Permissions.VALIDATE_RECEPTION, Permissions.VIEW_BUDGET,
                Permissions.ENGAGE_BUDGET, Permissions.VIEW_IMMOBILISATION, Permissions.LIST_PROVIDERS
        );

        PosteEntity posteDirecteurDpc = createPoste("POSTE_DIRECTEUR_DPC", "Directeur DPC", unites.get("DPC"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, dpcDirecteurPerms);
        map.put("POSTE_DIRECTEUR_DPC", posteDirecteurDpc);

        PosteEntity posteSdTech = createPoste("POSTE_SD_TECH", "Sous-Directeur Technique", unites.get("SD_TECH"), posteDirecteurDpc,
                CategorieConvention.CADRE_SUPERIEUR, dpcDirecteurPerms);
        map.put("POSTE_SD_TECH", posteSdTech);

        PosteEntity posteBrigMaint = createPoste("POSTE_BRIG_MAINT", "Chef de Brigade de Maintenance", unites.get("BRIG_MAINT"), posteSdTech,
                CategorieConvention.AGENT_DE_MAITRISE, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.CREATE_EXPRESSION, Permissions.VIEW_EXPRESSION,
                        Permissions.CREATE_RECEPTION, Permissions.VIEW_IMMOBILISATION
                ));
        map.put("POSTE_BRIG_MAINT", posteBrigMaint);

        PosteEntity posteChefRanchs = createPoste("POSTE_CHEF_RANCHS", "Chef Service des Ranchs", unites.get("SERV_RANCHS"), posteSdTech,
                CategorieConvention.CADRE_SUPERIEUR, dpcDirecteurPerms);
        map.put("POSTE_CHEF_RANCHS", posteChefRanchs);

        PosteEntity posteChefPaturage = createPoste("POSTE_CHEF_PATURAGE", "Chef Service de l'Aménagement et du Pâturage", unites.get("SERV_PATURAGE"), posteSdTech,
                CategorieConvention.CADRE_SUPERIEUR, dpcDirecteurPerms);
        map.put("POSTE_CHEF_PATURAGE", posteChefPaturage);

        PosteEntity posteChefTransfo = createPoste("POSTE_CHEF_TRANSFO", "Chef Service des Industries de Transformation", unites.get("SERV_TRANSFO"), posteSdTech,
                CategorieConvention.CADRE_SUPERIEUR, dpcDirecteurPerms);
        map.put("POSTE_CHEF_TRANSFO", posteChefTransfo);

        PosteEntity posteSdComm = createPoste("POSTE_SD_COMM", "Sous-Directeur Commercial", unites.get("SD_COMM"), posteDirecteurDpc,
                CategorieConvention.CADRE_SUPERIEUR, dpcDirecteurPerms);
        map.put("POSTE_SD_COMM", posteSdComm);

        PosteEntity posteChefVentes = createPoste("POSTE_CHEF_VENTES", "Chef Service des Ventes", unites.get("SERV_VENTES"), posteSdComm,
                CategorieConvention.CADRE_SUPERIEUR, dpcDirecteurPerms);
        map.put("POSTE_CHEF_VENTES", posteChefVentes);

        PosteEntity posteChefMkt = createPoste("POSTE_CHEF_MKT", "Chef Service Marketing", unites.get("SERV_MKT"), posteSdComm,
                CategorieConvention.CADRE_SUPERIEUR, dpcDirecteurPerms);
        map.put("POSTE_CHEF_MKT", posteChefMkt);

        // 5. Direction Administrative et Financière (DAF)
        Set<Permissions> dafDirecteurPerms = Set.of(
                Permissions.GET_ORGANIGRAMME_INFO, Permissions.GET_FULL_USER_INFO, Permissions.INIT_CREATE_USER,
                Permissions.INIT_UPDATE_USER, Permissions.VALIDATE_OR_REJECT_USER,
                Permissions.CREATE_BUDGET, Permissions.VALIDATE_BUDGET, Permissions.UPDATE_BUDGET,
                Permissions.VIEW_BUDGET, Permissions.ENGAGE_BUDGET,
                Permissions.INIT_CREATE_COMPTE, Permissions.INIT_UPDATE_COMPTE, Permissions.GET_FULL_COMPTE_INFO,
                Permissions.VALIDATE_OR_REJECT_COMPTE, Permissions.LIST_ALL_COMPTE,
                Permissions.INIT_CREATE_JOURNAL, Permissions.INIT_UPDATE_JOURNAL, Permissions.GET_FULL_JOURNAL_INFO,
                Permissions.VALIDATE_OR_REJECT_JOURNAL, Permissions.LIST_ALL_JOURNAL,
                Permissions.INIT_CREATE_ECRITURE, Permissions.INIT_UPDATE_ECRITURE, Permissions.GET_FULL_ECRITURE_INFO,
                Permissions.VALIDATE_OR_REJECT_ECRITURE, Permissions.VIEW_ECRITURE,
                Permissions.INIT_CREATE_BANK, Permissions.INIT_UPDATE_BANK_INFO, Permissions.GET_FULL_BANK_INFO,
                Permissions.VALIDATE_OR_REJECT_BANK_INFO, Permissions.LIST_ALL_BANKS, Permissions.CREATE_BANK,
                Permissions.VALIDATE_BANK, Permissions.UPDATE_BANK, Permissions.DELETE_BANK,
                Permissions.LIST_RELEVE_BANCAIRE, Permissions.INIT_CREATE_RELEVE_BANCAIRE,
                Permissions.VALIDATE_OR_REJECT_RELEVE_BANCAIRE, Permissions.INIT_UPDATE_RELEVE_BANCAIRE,
                Permissions.INIT_CREATE_TIERS, Permissions.INIT_UPDATE_TIERS, Permissions.GET_FULL_TIERS_INFO,
                Permissions.VALIDATE_OR_REJECT_TIERS, Permissions.VALIDATE_OR_REJECT_CREATE_TIERS,
                Permissions.VALIDATE_OR_REJECT_UPDATE_TIERS,
                Permissions.INIT_CREATE_IMMOBILISATION, Permissions.INIT_UPDATE_IMMOBILISATION,
                Permissions.GET_FULL_IMMOBILISATION_INFO, Permissions.VALIDATE_OR_REJECT_IMMOBILISATION,
                Permissions.VIEW_IMMOBILISATION,
                Permissions.LIST_PROVIDERS, Permissions.CREATE_PROVIDER, Permissions.VALIDATE_PROVIDER,
                Permissions.LIST_EXPRESSIONS, Permissions.CREATE_EXPRESSION, Permissions.VALIDATE_EXPRESSION,
                Permissions.VIEW_EXPRESSION, Permissions.UPDATE_EXPRESSION,
                Permissions.LIST_FOLDERS, Permissions.CREATE_FOLDER, Permissions.VIEW_FOLDER,
                Permissions.VIEW_ORDER, Permissions.VALIDATE_FOLDER, Permissions.UPDATE_FOLDER,
                Permissions.NOTIFY_PROVIDER, Permissions.NOTIFY_SUPPLIER, Permissions.VALIDATE_NOTIFICATION,
                Permissions.LIST_RECEPTIONS, Permissions.CREATE_RECEPTION, Permissions.VALIDATE_RECEPTION,
                Permissions.LIQUIDATE_INVOICE, Permissions.VALIDATE_LIQUIDATION,
                Permissions.INIT_CREATE_FISCAL_YEAR_CLOSING
        );

        PosteEntity posteDirecteurDaf = createPoste("POSTE_DIRECTEUR_DAF", "Directeur DAF", unites.get("DAF"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, dafDirecteurPerms);
        map.put("POSTE_DIRECTEUR_DAF", posteDirecteurDaf);

        // Sous-Direction RH & Services
        Set<Permissions> rhPerms = Set.of(
                Permissions.GET_ORGANIGRAMME_INFO, Permissions.GET_FULL_USER_INFO,
                Permissions.INIT_CREATE_USER, Permissions.INIT_UPDATE_USER, Permissions.VALIDATE_OR_REJECT_USER,
                Permissions.CREATE_EXPRESSION, Permissions.VIEW_EXPRESSION
        );

        PosteEntity posteSdRh = createPoste("POSTE_SD_RH", "Sous-Directeur des Ressources Humaines", unites.get("SDRH"), posteDirecteurDaf,
                CategorieConvention.CADRE_SUPERIEUR, rhPerms);
        map.put("POSTE_SD_RH", posteSdRh);

        PosteEntity posteChefPers = createPoste("POSTE_CHEF_PERS", "Chef Service du Personnel", unites.get("SERV_PERS"), posteSdRh,
                CategorieConvention.CADRE_SUPERIEUR, rhPerms);
        map.put("POSTE_CHEF_PERS", posteChefPers);

        PosteEntity posteChefSolde = createPoste("POSTE_CHEF_SOLDE", "Chef Service de la Solde et des Pensions", unites.get("SERV_SOLDE"), posteSdRh,
                CategorieConvention.AGENT_DE_MAITRISE, rhPerms);
        map.put("POSTE_CHEF_SOLDE", posteChefSolde);

        PosteEntity posteChefForm = createPoste("POSTE_CHEF_FORM", "Chef Service Formation", unites.get("SERV_FORM"), posteSdRh,
                CategorieConvention.AGENT_DE_MAITRISE, rhPerms);
        map.put("POSTE_CHEF_FORM", posteChefForm);

        // Sous-Direction Finances, Budget & Matériel & Services
        PosteEntity posteSdFbm = createPoste("POSTE_SD_FBM", "Sous-Directeur Finances, Budget & Matériel", unites.get("SDFBM"), posteDirecteurDaf,
                CategorieConvention.CADRE_SUPERIEUR, dafDirecteurPerms);
        map.put("POSTE_SD_FBM", posteSdFbm);

        PosteEntity posteChefFin = createPoste("POSTE_CHEF_FIN", "Chef Service Financier et des Recettes", unites.get("SERV_FIN"), posteSdFbm,
                CategorieConvention.CADRE_SUPERIEUR, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.INIT_CREATE_BANK, Permissions.INIT_UPDATE_BANK_INFO,
                        Permissions.GET_FULL_BANK_INFO, Permissions.LIST_ALL_BANKS, Permissions.CREATE_BANK,
                        Permissions.UPDATE_BANK, Permissions.LIST_RELEVE_BANCAIRE, Permissions.INIT_CREATE_RELEVE_BANCAIRE,
                        Permissions.INIT_UPDATE_RELEVE_BANCAIRE, Permissions.LIQUIDATE_INVOICE, Permissions.VIEW_BUDGET
                ));
        map.put("POSTE_CHEF_FIN", posteChefFin);

        PosteEntity posteChefCompta = createPoste("POSTE_CHEF_COMPTA", "Chef Service Comptable", unites.get("SERV_COMPTA"), posteSdFbm,
                CategorieConvention.CADRE_SUPERIEUR, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.INIT_CREATE_COMPTE, Permissions.INIT_UPDATE_COMPTE,
                        Permissions.GET_FULL_COMPTE_INFO, Permissions.LIST_ALL_COMPTE, Permissions.INIT_CREATE_JOURNAL,
                        Permissions.INIT_UPDATE_JOURNAL, Permissions.GET_FULL_JOURNAL_INFO, Permissions.LIST_ALL_JOURNAL,
                        Permissions.INIT_CREATE_ECRITURE, Permissions.INIT_UPDATE_ECRITURE, Permissions.GET_FULL_ECRITURE_INFO,
                        Permissions.VIEW_ECRITURE, Permissions.INIT_CREATE_IMMOBILISATION, Permissions.INIT_UPDATE_IMMOBILISATION,
                        Permissions.GET_FULL_IMMOBILISATION_INFO, Permissions.VIEW_IMMOBILISATION,
                        Permissions.INIT_CREATE_FISCAL_YEAR_CLOSING
                ));
        map.put("POSTE_CHEF_COMPTA", posteChefCompta);

        PosteEntity posteChefBudget = createPoste("POSTE_CHEF_BUDGET", "Chef Service du Budget", unites.get("SERV_BUDGET"), posteSdFbm,
                CategorieConvention.AGENT_DE_MAITRISE, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.CREATE_BUDGET, Permissions.UPDATE_BUDGET,
                        Permissions.VIEW_BUDGET, Permissions.ENGAGE_BUDGET, Permissions.LIST_EXPRESSIONS, Permissions.VIEW_EXPRESSION
                ));
        map.put("POSTE_CHEF_BUDGET", posteChefBudget);

        PosteEntity posteChefMarches = createPoste("POSTE_CHEF_MARCHES", "Chef Service des Marchés et du Patrimoine", unites.get("SERV_MARCHES"), posteSdFbm,
                CategorieConvention.CADRE_SUPERIEUR, Set.of(
                        Permissions.GET_ORGANIGRAMME_INFO, Permissions.LIST_PROVIDERS, Permissions.CREATE_PROVIDER,
                        Permissions.LIST_FOLDERS, Permissions.CREATE_FOLDER, Permissions.VIEW_FOLDER,
                        Permissions.VIEW_ORDER, Permissions.UPDATE_FOLDER, Permissions.NOTIFY_PROVIDER,
                        Permissions.NOTIFY_SUPPLIER, Permissions.LIST_RECEPTIONS, Permissions.CREATE_RECEPTION
                ));
        map.put("POSTE_CHEF_MARCHES", posteChefMarches);

        // 6. Postes Unités Opérationnelles Régionales
        Set<Permissions> regionalDirecteurPerms = Set.of(
                Permissions.GET_ORGANIGRAMME_INFO, Permissions.LIST_EXPRESSIONS, Permissions.CREATE_EXPRESSION,
                Permissions.VIEW_EXPRESSION, Permissions.LIST_FOLDERS, Permissions.VIEW_FOLDER,
                Permissions.LIST_RECEPTIONS, Permissions.CREATE_RECEPTION, Permissions.VIEW_BUDGET,
                Permissions.ENGAGE_BUDGET
        );

        PosteEntity dirAbatDla = createPoste("POSTE_DIR_ABAT_DLA", "Directeur Abattoir Frigorifique de Douala", unites.get("ABAT_DLA"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, regionalDirecteurPerms);
        map.put("POSTE_DIR_ABAT_DLA", dirAbatDla);

        PosteEntity dirAbatYde = createPoste("POSTE_DIR_ABAT_YDE", "Directeur Abattoir Frigorifique de Yaoundé", unites.get("ABAT_YDE"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, regionalDirecteurPerms);
        map.put("POSTE_DIR_ABAT_YDE", dirAbatYde);

        PosteEntity dirAbatNgao = createPoste("POSTE_DIR_ABAT_NGAO", "Directeur Abattoir Frigorifique de Ngaoundéré", unites.get("ABAT_NGAO"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, regionalDirecteurPerms);
        map.put("POSTE_DIR_ABAT_NGAO", dirAbatNgao);

        PosteEntity dirRanchFaro = createPoste("POSTE_DIR_RANCH_FARO", "Directeur Ranch Pastoral de Faro", unites.get("RANCH_FARO"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, regionalDirecteurPerms);
        map.put("POSTE_DIR_RANCH_FARO", dirRanchFaro);

        PosteEntity dirRanchNdokayo = createPoste("POSTE_DIR_RANCH_NDOKAYO", "Directeur Ranch Pastoral de Ndokayo", unites.get("RANCH_NDOKAYO"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, regionalDirecteurPerms);
        map.put("POSTE_DIR_RANCH_NDOKAYO", dirRanchNdokayo);

        PosteEntity chefStatJakiri = createPoste("POSTE_CHEF_STAT_JAKIRI", "Chef Station d'Élevage et Laiterie de Jakiri", unites.get("STAT_JAKIRI"), posteDg,
                CategorieConvention.CADRE_SUPERIEUR, regionalDirecteurPerms);
        map.put("POSTE_CHEF_STAT_JAKIRI", chefStatJakiri);

        PosteEntity dirRanchDumbo = createPoste("POSTE_DIR_RANCH_DUMBO", "Directeur Ranch Pastoral de Dumbo", unites.get("RANCH_DUMBO"), posteDg,
                CategorieConvention.CADRE_DIRIGEANT, regionalDirecteurPerms);
        map.put("POSTE_DIR_RANCH_DUMBO", dirRanchDumbo);

        return map;
    }

    private PosteEntity createPoste(String code, String intitule, UniteOrganisationnelleEntity unite,
                                    PosteEntity superieur, CategorieConvention cat, Set<Permissions> permissions) {
        PosteEntity poste = posteRepository.findByCodePoste(code).orElseGet(() -> {
            PosteEntity p = PosteEntity.builder()
                    .id(UUID.randomUUID())
                    .codePoste(code)
                    .intitule(intitule)
                    .uniteOrganisationnelle(unite)
                    .posteSuperieur(superieur)
                    .categorieConvention(cat)
                    .effectifAutorise(1)
                    .effectifActuel(0)
                    .statut(StatutPoste.OUVERT)
                    .descriptionMission("Mission officielle pour " + intitule + " au sein de l'unité " + unite.getIntitule())
                    .permissions(new HashSet<>(permissions))
                    .build();
            return posteRepository.save(p);
        });
        auditEvent("POSTE", poste.getId().toString(), "Poste " + code + " avec " + permissions.size() + " permissions", ADMIN_MAKER_ID.toString());
        return poste;
    }

    /**
     * Crée l'ensemble des 54 collaborateurs réels issus du scraping SODEPA.
     * Pour chacun :
     * 1. UtilisateurEntity (sans permissions directes, conformément à la règle d'architecture).
     * 2. Provisionnement dans Keycloak.
     * 3. Dossier EmployeEntity lié au compte utilisateur (utilisateur_id).
     * 4. AffectationPosteEntity titulaire ou intérimaire actif.
     */
    private void seedCollaborateurs(Map<String, UniteOrganisationnelleEntity> unites,
                                    Map<String, PosteEntity> postes,
                                    Map<Integer, GrilleSalarialeEntity> grilles) {
        log.info("Création des Utilisateurs, Dossiers Employés et Affectations SODEPA...");

        // Dossiers du Top Management et Direction Générale
        // 1. DG
        createAgent("denis.koulagna", "KOULAGNA KOUTOU", "Denis", "denis.koulagna@sodepa.cm", Sexe.M,
                LocalDate.of(1968, 5, 14), SituationMatrimoniale.MARIE, 3, "+237699000001",
                postes.get("POSTE_DG"), unites.get("DG"), grilles.get(12), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 1, 15), "SDP-2018-0001", "DÉCISION PRESIDENTIELLE D'INSTALLATION DG N°001/PR/2018");

        // 2. PCA
        createAgent("zoutene.doufene", "ZOUTENE", "Doufene", "zoutene.doufene@sodepa.cm", Sexe.M,
                LocalDate.of(1962, 8, 20), SituationMatrimoniale.MARIE, 4, "+237699000002",
                postes.get("POSTE_PCA"), unites.get("CA"), grilles.get(12), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 1, 10), "SDP-2018-0002", "DECRET DE NOMINATION PCA N°2018/002");

        // Attaché & Rattachés DG
        createAgent("ayissi.mba", "AYISSI MBA", "Paul", "ayissi.mba@sodepa.cm", Sexe.M,
                LocalDate.of(1975, 4, 12), SituationMatrimoniale.MARIE, 2, "+237699000003",
                postes.get("POSTE_ATTACHE_DIR"), unites.get("DG"), grilles.get(10), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 2, 1), "SDP-2018-0003", "DECISION D'AFFECTATION DG/003/2018");

        createAgent("marcel.kamegni", "KAMEGNI", "Marcel Laurier", "marcel.kamegni@sodepa.cm", Sexe.M,
                LocalDate.of(1980, 11, 3), SituationMatrimoniale.MARIE, 2, "+237699000004",
                postes.get("POSTE_CSCG"), unites.get("DG"), grilles.get(10), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 3, 1), "SDP-2019-0004", "DECISION D'AFFECTATION DG/004/2019");

        createAgent("shwiri.eshwa", "SHWIRI", "ESHWA Chumbow", "shwiri.eshwa@sodepa.cm", Sexe.F,
                LocalDate.of(1985, 7, 19), SituationMatrimoniale.CELIBATAIRE, 1, "+237699000005",
                postes.get("POSTE_COM_TRAD"), unites.get("CELL_COM"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 6, 1), "SDP-2019-0005", "DECISION D'AFFECTATION DG/005/2019");

        createAgent("aude.guentang", "GUENTANG BOFIA", "Aude Larissa", "aude.guentang@sodepa.cm", Sexe.F,
                LocalDate.of(1987, 2, 28), SituationMatrimoniale.CELIBATAIRE, 0, "+237699000006",
                postes.get("POSTE_QHSE"), unites.get("CELL_QHSE"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2020, 1, 15), "SDP-2020-0006", "DECISION D'AFFECTATION DG/006/2020");

        createAgent("flora.fuben", "FUBEN DJATCHE", "Flora", "flora.fuben@sodepa.cm", Sexe.F,
                LocalDate.of(1989, 9, 14), SituationMatrimoniale.CELIBATAIRE, 1, "+237699000007",
                postes.get("POSTE_CHEF_COURRIER"), unites.get("SERV_COURRIER"), grilles.get(7), TypeAffectation.INTERIMAIRE,
                LocalDate.of(2021, 5, 2), "SDP-2021-0007", "DECISION D'INTERIM DG/007/2021");

        // DCAI
        createAgent("theodor.aladoum", "ALADOUM", "Theodor", "theodor.aladoum@sodepa.cm", Sexe.M,
                LocalDate.of(1972, 6, 18), SituationMatrimoniale.MARIE, 4, "+237699000008",
                postes.get("POSTE_CHEF_DCAI"), unites.get("DCAI"), grilles.get(11), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 2, 15), "SDP-2018-0008", "DECISION DE NOMINATION DG/008/2018");

        createAgent("hortense.male", "MALE SAHA", "Hortense", "hortense.male@sodepa.cm", Sexe.F,
                LocalDate.of(1982, 3, 22), SituationMatrimoniale.MARIE, 2, "+237699000009",
                postes.get("POSTE_AUDITEUR_1"), unites.get("DCAI"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 4, 1), "SDP-2019-0009", "DECISION D'AFFECTATION DG/009/2019");

        createAgent("abbassi.oumarou", "OUMAROU", "Abbassi", "abbassi.oumarou@sodepa.cm", Sexe.M,
                LocalDate.of(1984, 12, 10), SituationMatrimoniale.MARIE, 3, "+237699000010",
                postes.get("POSTE_AUDITEUR_2"), unites.get("DCAI"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2020, 2, 1), "SDP-2020-0010", "DECISION D'AFFECTATION DG/010/2020");

        createAgent("jane.debey", "DEBEY", "Jane Louisa Alexandra", "jane.debey@sodepa.cm", Sexe.F,
                LocalDate.of(1988, 10, 5), SituationMatrimoniale.CELIBATAIRE, 1, "+237699000011",
                postes.get("POSTE_AUDITEUR_3"), unites.get("DCAI"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2021, 3, 1), "SDP-2021-0011", "DECISION D'AFFECTATION DG/011/2021");

        // DPD
        createAgent("godwill.fuh", "FUH", "GODWILL NGWA", "godwill.fuh@sodepa.cm", Sexe.M,
                LocalDate.of(1973, 8, 15), SituationMatrimoniale.MARIE, 3, "+237699000012",
                postes.get("POSTE_CHEF_DPD"), unites.get("DPD"), grilles.get(11), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 3, 1), "SDP-2018-0012", "DECISION DE NOMINATION DG/012/2018");

        createAgent("christian.zogo", "ZOGO GWET", "Christian", "christian.zogo@sodepa.cm", Sexe.M,
                LocalDate.of(1981, 1, 14), SituationMatrimoniale.MARIE, 2, "+237699000013",
                postes.get("POSTE_DPD_CSPP_1"), unites.get("CSPP"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 5, 1), "SDP-2019-0013", "DECISION D'AFFECTATION DG/013/2019");

        createAgent("celestin.bodol", "BODOL SOMBOG", "Célestin", "celestin.bodol@sodepa.cm", Sexe.M,
                LocalDate.of(1983, 11, 25), SituationMatrimoniale.MARIE, 2, "+237699000014",
                postes.get("POSTE_DPD_CSPP_2"), unites.get("CSPP"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2020, 7, 1), "SDP-2020-0014", "DECISION D'AFFECTATION DG/014/2020");

        createAgent("freddy.ngah", "NGAH OSOE BOULI", "Freddy", "freddy.ngah@sodepa.cm", Sexe.M,
                LocalDate.of(1980, 4, 30), SituationMatrimoniale.MARIE, 3, "+237699000015",
                postes.get("POSTE_CHEF_CPE"), unites.get("CPE"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 8, 1), "SDP-2019-0015", "DECISION D'AFFECTATION DG/015/2019");

        createAgent("bouba", "BOUBA", "Bouba", "bouba@sodepa.cm", Sexe.M,
                LocalDate.of(1985, 9, 9), SituationMatrimoniale.MARIE, 2, "+237699000016",
                postes.get("POSTE_CPE_2"), unites.get("CPE"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2020, 9, 1), "SDP-2020-0016", "DECISION D'AFFECTATION DG/016/2020");

        createAgent("philippe.asseng", "ASSENG NGUELE", "Philippe", "philippe.asseng@sodepa.cm", Sexe.M,
                LocalDate.of(1982, 12, 19), SituationMatrimoniale.MARIE, 2, "+237699000017",
                postes.get("POSTE_CHEF_CI"), unites.get("CI"), grilles.get(10), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 11, 1), "SDP-2018-0017", "DECISION D'AFFECTATION DG/017/2018");

        createAgent("george.ambe", "AMBE", "George Che", "george.ambe@sodepa.cm", Sexe.M,
                LocalDate.of(1986, 3, 11), SituationMatrimoniale.CELIBATAIRE, 0, "+237699000018",
                postes.get("POSTE_CI_1"), unites.get("CI"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2020, 2, 15), "SDP-2020-0018", "DECISION D'AFFECTATION DG/018/2020");

        createAgent("christelle.mendouga", "MENDOUGA", "Christelle Rose", "christelle.mendouga@sodepa.cm", Sexe.F,
                LocalDate.of(1989, 5, 23), SituationMatrimoniale.CELIBATAIRE, 1, "+237699000019",
                postes.get("POSTE_CI_2"), unites.get("CI"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2021, 1, 15), "SDP-2021-0019", "DECISION D'AFFECTATION DG/019/2021");

        // DPC
        createAgent("walters.andu", "ANDU NDI", "Walters", "walters.andu@sodepa.cm", Sexe.M,
                LocalDate.of(1969, 7, 7), SituationMatrimoniale.MARIE, 4, "+237699000020",
                postes.get("POSTE_DIRECTEUR_DPC"), unites.get("DPC"), grilles.get(11), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 2, 1), "SDP-2018-0020", "DECISION DE NOMINATION DG/020/2018");

        createAgent("louis.bikoro", "BIKORO MVOMO", "Louis Benoît", "louis.bikoro@sodepa.cm", Sexe.M,
                LocalDate.of(1976, 10, 16), SituationMatrimoniale.MARIE, 3, "+237699000021",
                postes.get("POSTE_SD_TECH"), unites.get("SD_TECH"), grilles.get(10), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 6, 1), "SDP-2018-0021", "DECISION D'AFFECTATION DG/021/2018");

        createAgent("leonard.djongwe", "DJONGWE", "Leonard", "leonard.djongwe@sodepa.cm", Sexe.M,
                LocalDate.of(1982, 1, 29), SituationMatrimoniale.MARIE, 2, "+237699000022",
                postes.get("POSTE_BRIG_MAINT"), unites.get("BRIG_MAINT"), grilles.get(7), TypeAffectation.INTERIMAIRE,
                LocalDate.of(2020, 4, 1), "SDP-2020-0022", "DECISION D'INTERIM DG/022/2020");

        createAgent("aime.hounge", "HOUNGE MGBATOU", "Aimé Serge", "aime.hounge@sodepa.cm", Sexe.M,
                LocalDate.of(1977, 8, 24), SituationMatrimoniale.MARIE, 3, "+237699000023",
                postes.get("POSTE_CHEF_RANCHS"), unites.get("SERV_RANCHS"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 1, 15), "SDP-2019-0023", "DECISION D'AFFECTATION DG/023/2019");

        createAgent("saa.tekounegning", "TEKOUNEGNING", "Saa", "saa.tekounegning@sodepa.cm", Sexe.M,
                LocalDate.of(1980, 2, 11), SituationMatrimoniale.MARIE, 2, "+237699000024",
                postes.get("POSTE_CHEF_PATURAGE"), unites.get("SERV_PATURAGE"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 7, 1), "SDP-2019-0024", "DECISION D'AFFECTATION DG/024/2019");

        createAgent("aloa.souleymanou", "SOULEYMANOU", "Aloa", "aloa.souleymanou@sodepa.cm", Sexe.M,
                LocalDate.of(1979, 12, 3), SituationMatrimoniale.MARIE, 3, "+237699000025",
                postes.get("POSTE_CHEF_TRANSFO"), unites.get("SERV_TRANSFO"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 9, 1), "SDP-2019-0025", "DECISION D'AFFECTATION DG/025/2019");

        createAgent("armand.hamoua", "HAMOUA BAKA", "Armand", "armand.hamoua@sodepa.cm", Sexe.M,
                LocalDate.of(1978, 6, 17), SituationMatrimoniale.MARIE, 3, "+237699000026",
                postes.get("POSTE_SD_COMM"), unites.get("SD_COMM"), grilles.get(10), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 9, 15), "SDP-2018-0026", "DECISION D'AFFECTATION DG/026/2018");

        createAgent("christelle.assougouma", "ASSOUGOUMA NDEME", "Christelle", "christelle.assougouma@sodepa.cm", Sexe.F,
                LocalDate.of(1984, 5, 9), SituationMatrimoniale.MARIE, 1, "+237699000027",
                postes.get("POSTE_CHEF_VENTES"), unites.get("SERV_VENTES"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 10, 1), "SDP-2019-0027", "DECISION D'AFFECTATION DG/027/2019");

        createAgent("boanerges.doua", "DOUA NDOMBE KOUTOU", "Boanerges", "boanerges.doua@sodepa.cm", Sexe.M,
                LocalDate.of(1985, 3, 27), SituationMatrimoniale.MARIE, 2, "+237699000028",
                postes.get("POSTE_CHEF_MKT"), unites.get("SERV_MKT"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2020, 1, 10), "SDP-2020-0028", "DECISION D'AFFECTATION DG/028/2020");

        // DAF
        createAgent("desire.wetjeng", "WETJENG", "Désiré Magloire", "desire.wetjeng@sodepa.cm", Sexe.M,
                LocalDate.of(1971, 9, 14), SituationMatrimoniale.MARIE, 4, "+237699000029",
                postes.get("POSTE_DIRECTEUR_DAF"), unites.get("DAF"), grilles.get(11), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 2, 1), "SDP-2018-0029", "DECISION DE NOMINATION DG/029/2018");

        createAgent("gustave.dooh", "DO'OH", "Gustave", "gustave.dooh@sodepa.cm", Sexe.M,
                LocalDate.of(1974, 4, 18), SituationMatrimoniale.MARIE, 3, "+237699000030",
                postes.get("POSTE_SD_RH"), unites.get("SDRH"), grilles.get(10), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 7, 1), "SDP-2018-0030", "DECISION D'AFFECTATION DG/030/2018");

        createAgent("elisabeth.simo", "SIMO", "Elisabeth Sidonie", "elisabeth.simo@sodepa.cm", Sexe.F,
                LocalDate.of(1981, 11, 23), SituationMatrimoniale.MARIE, 2, "+237699000031",
                postes.get("POSTE_CHEF_PERS"), unites.get("SERV_PERS"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 2, 1), "SDP-2019-0031", "DECISION D'AFFECTATION DG/031/2019");

        createAgent("marcel.eyinga", "EYINGA EBOUTOU", "Marcel", "marcel.eyinga@sodepa.cm", Sexe.M,
                LocalDate.of(1983, 8, 12), SituationMatrimoniale.MARIE, 2, "+237699000032",
                postes.get("POSTE_CHEF_SOLDE"), unites.get("SERV_SOLDE"), grilles.get(7), TypeAffectation.INTERIMAIRE,
                LocalDate.of(2020, 5, 1), "SDP-2020-0032", "DECISION D'INTERIM DG/032/2020");

        createAgent("lise.nlango", "NLANGO", "Lise Sophie", "lise.nlango@sodepa.cm", Sexe.F,
                LocalDate.of(1986, 12, 4), SituationMatrimoniale.CELIBATAIRE, 1, "+237699000033",
                postes.get("POSTE_CHEF_FORM"), unites.get("SERV_FORM"), grilles.get(7), TypeAffectation.INTERIMAIRE,
                LocalDate.of(2021, 2, 1), "SDP-2021-0033", "DECISION D'INTERIM DG/033/2021");

        createAgent("pierre.nkou", "NKOU ABINA", "Pierre", "pierre.nkou@sodepa.cm", Sexe.M,
                LocalDate.of(1975, 1, 30), SituationMatrimoniale.MARIE, 3, "+237699000034",
                postes.get("POSTE_SD_FBM"), unites.get("SDFBM"), grilles.get(10), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 8, 1), "SDP-2018-0034", "DECISION D'AFFECTATION DG/034/2018");

        createAgent("biloa.nke", "NKE", "Biloa", "biloa.nke@sodepa.cm", Sexe.M,
                LocalDate.of(1979, 7, 21), SituationMatrimoniale.MARIE, 2, "+237699000035",
                postes.get("POSTE_CHEF_FIN"), unites.get("SERV_FIN"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 4, 15), "SDP-2019-0035", "DECISION D'AFFECTATION DG/035/2019");

        createAgent("ruth.saa", "SAA", "Ruth", "ruth.saa@sodepa.cm", Sexe.F,
                LocalDate.of(1982, 6, 16), SituationMatrimoniale.MARIE, 2, "+237699000036",
                postes.get("POSTE_CHEF_COMPTA"), unites.get("SERV_COMPTA"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 5, 1), "SDP-2019-0036", "DECISION D'AFFECTATION DG/036/2019");

        createAgent("aissatou.mama", "MAMA", "Aïssatou", "aissatou.mama@sodepa.cm", Sexe.F,
                LocalDate.of(1987, 10, 8), SituationMatrimoniale.CELIBATAIRE, 1, "+237699000037",
                postes.get("POSTE_CHEF_BUDGET"), unites.get("SERV_BUDGET"), grilles.get(7), TypeAffectation.INTERIMAIRE,
                LocalDate.of(2021, 6, 1), "SDP-2021-0037", "DECISION D'INTERIM DG/037/2021");

        createAgent("grace.atam", "ATAM", "Grace", "grace.atam@sodepa.cm", Sexe.F,
                LocalDate.of(1983, 3, 19), SituationMatrimoniale.MARIE, 1, "+237699000038",
                postes.get("POSTE_CHEF_MARCHES"), unites.get("SERV_MARCHES"), grilles.get(9), TypeAffectation.TITULAIRE,
                LocalDate.of(2019, 11, 1), "SDP-2019-0038", "DECISION D'AFFECTATION DG/038/2019");

        // Pôles Régionaux
        createAgent("ferdinand.woudamyata", "WOUDAMYATA ABBA", "Ferdinand", "ferdinand.woudamyata@sodepa.cm", Sexe.M,
                LocalDate.of(1973, 5, 26), SituationMatrimoniale.MARIE, 4, "+237699000039",
                postes.get("POSTE_DIR_ABAT_DLA"), unites.get("ABAT_DLA"), grilles.get(11), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 3, 15), "SDP-2018-0039", "DECISION DE NOMINATION REGIONALE DG/039/2018");

        createAgent("christian.secke", "SECKE", "Christian", "christian.secke@sodepa.cm", Sexe.M,
                LocalDate.of(1975, 9, 2), SituationMatrimoniale.MARIE, 3, "+237699000040",
                postes.get("POSTE_DIR_ABAT_YDE"), unites.get("ABAT_YDE"), grilles.get(11), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 4, 1), "SDP-2018-0040", "DECISION DE NOMINATION REGIONALE DG/040/2018");

        createAgent("hayatou.aliyou", "ALIYOU", "Hayatou", "hayatou.aliyou@sodepa.cm", Sexe.M,
                LocalDate.of(1976, 12, 11), SituationMatrimoniale.MARIE, 3, "+237699000041",
                postes.get("POSTE_DIR_ABAT_NGAO"), unites.get("ABAT_NGAO"), grilles.get(11), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 5, 1), "SDP-2018-0041", "DECISION DE NOMINATION REGIONALE DG/041/2018");

        createAgent("hamidou.hayatou", "HAYATOU", "Hamidou", "hamidou.hayatou@sodepa.cm", Sexe.M,
                LocalDate.of(1970, 4, 15), SituationMatrimoniale.MARIE, 5, "+237699000042",
                postes.get("POSTE_DIR_RANCH_FARO"), unites.get("RANCH_FARO"), grilles.get(11), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 2, 20), "SDP-2018-0042", "DECISION DE NOMINATION REGIONALE DG/042/2018");

        createAgent("alioum.yaya", "YAYA", "Alioum", "alioum.yaya@sodepa.cm", Sexe.M,
                LocalDate.of(1972, 8, 28), SituationMatrimoniale.MARIE, 4, "+237699000043",
                postes.get("POSTE_DIR_RANCH_NDOKAYO"), unites.get("RANCH_NDOKAYO"), grilles.get(11), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 3, 10), "SDP-2018-0043", "DECISION DE NOMINATION REGIONALE DG/043/2018");

        // Dr KWENYE Joseph : Titulaire sur Jakiri ET intérimaire sur Dumbo
        UtilisateurEntity userKwenye = createAgent("joseph.kwenye", "KWENYE", "Joseph", "joseph.kwenye@sodepa.cm", Sexe.M,
                LocalDate.of(1974, 11, 17), SituationMatrimoniale.MARIE, 3, "+237699000044",
                postes.get("POSTE_CHEF_STAT_JAKIRI"), unites.get("STAT_JAKIRI"), grilles.get(10), TypeAffectation.TITULAIRE,
                LocalDate.of(2018, 6, 1), "SDP-2018-0044", "DECISION DE NOMINATION REGIONALE DG/044/2018");

        // Affectation intérimaire de cumul sur Dumbo pour Dr KWENYE
        createSecondAffectation(userKwenye, postes.get("POSTE_DIR_RANCH_DUMBO"), TypeAffectation.INTERIMAIRE,
                LocalDate.of(2020, 1, 1), "DECISION D'INTERIM RANCH DUMBO DG/045/2020");

        // Administrateurs Membres du Conseil d'Administration
        String[][] adminsCa = {
                {"jeanfrancois.ngok", "NGOK EVINA", "Jean François", "jf.ngok@sodepa.cm", "+237699000046", "Représentant PRC"},
                {"jeanavit.kongape", "KONGAPE", "Jean Avit", "ja.kongape@sodepa.cm", "+237699000047", "Représentant SPM"},
                {"gana.zanyine", "GANA", "Zanyine", "gana.zanyine@sodepa.cm", "+237699000048", "Représentant MINFI"},
                {"haman.atkam", "ATKAM", "Haman", "haman.atkam@sodepa.cm", "+237699000049", "Représentant MINEPIA"},
                {"remon.fohopa", "FOHOPA KUE", "Remon", "remon.fohopa@sodepa.cm", "+237699000050", "Représentant MINEPAT"},
                {"magelan.omballa", "OMBALLA", "Magelan", "magelan.omballa@sodepa.cm", "+237699000051", "Représentant MINCOMMERCE"},
                {"constance.kamgang", "KAMGANG", "Constance Clarisse", "cc.kamgang@sodepa.cm", "+237699000052", "Représentante MINDCAF"},
                {"aliou.moussa", "ALIOU", "Moussa", "aliou.moussa@sodepa.cm", "+237699000053", "Représentant MINDDEVEL"},
                {"mohamadou.bello", "BELLO", "Mohamadou", "mohamadou.bello@sodepa.cm", "+237699000054", "Représentant SNI"}
        };

        for (int i = 0; i < adminsCa.length; i++) {
            String[] data = adminsCa[i];
            int postNum = i + 1;
            createAgent(data[0], data[1], data[2], data[3], Sexe.M,
                    LocalDate.of(1965 + (i % 10), 1 + (i % 12), 10 + i), SituationMatrimoniale.MARIE, 3, data[4],
                    postes.get("POSTE_ADMIN_CA_" + postNum), unites.get("CA"), grilles.get(12), TypeAffectation.TITULAIRE,
                    LocalDate.of(2018, 1, 15), "SDP-2018-00" + (46 + i), "DECRET DESIGNATION ADMINISTRATEUR CA/" + postNum + "/2018");
        }
    }

    private UtilisateurEntity createAgent(String username, String nom, String prenom, String email,
                                          Sexe sexe, LocalDate dob, SituationMatrimoniale sitMat, int enfants,
                                          String tel, PosteEntity poste, UniteOrganisationnelleEntity unite,
                                          GrilleSalarialeEntity grille, TypeAffectation typeAff,
                                          LocalDate datePriseFonction, String matricule, String refDecision) {

        // 1. Utilisateur (Règle d'or : Tout employé est d'abord un utilisateur, sans permissions directes)
        UtilisateurEntity user = userRepository.findByUsername(username).orElseGet(() -> {
            UtilisateurEntity u = UtilisateurEntity.builder()
                    .id(UUID.randomUUID())
                    .username(username)
                    .nom(nom)
                    .prenom(prenom)
                    .email(email)
                    .photoProfile("")
                    .actif(true)
                    .telephones(Set.of(tel))
                    .permissions(new HashSet<>()) // Permissions portées par le PosteEntity
                    .build();

            UUID iamId = provisionKeycloakSafe(u.getId(), username, prenom, nom, email);
            u.setIam(iamId != null ? iamId : u.getId());
            return userRepository.save(u);
        });

        auditEvent("UTILISATEUR", user.getId().toString(), "Utilisateur " + username, ADMIN_MAKER_ID.toString());

        // 2. Dossier Employé (Règle d'or : Unicité stricte utilisateur_id, admin_maker/checker exclus)
        if (!employeRepository.existsByUtilisateurId(user.getId())) {
            EmployeEntity employe = EmployeEntity.builder()
                    .id(UUID.randomUUID())
                    .matricule(matricule)
                    .nom(nom)
                    .prenom(prenom)
                    .sexe(sexe)
                    .dateNaissance(dob)
                    .lieuNaissance("Cameroun")
                    .situationMatrimoniale(sitMat)
                    .nombreEnfantsACharge(enfants)
                    .email(email)
                    .telephone(tel)
                    .adresse("SODEPA, Cameroun")
                    .numeroCnps("CNPS-" + matricule.replace("-", ""))
                    .compteBancaireRib("CM21 10005 00001 " + matricule.substring(4).replace("-", "") + " 45")
                    .banqueNom("Afriland First Bank")
                    .typeContrat(TypeContrat.CDI)
                    .dateEmbauche(datePriseFonction)
                    .statutActivite(StatutActivite.EN_FONCTION)
                    .uniteOrganisationnelle(unite)
                    .poste(poste)
                    .grilleSalariale(grille)
                    .utilisateur(user)
                    .build();
            employeRepository.save(employe);
            auditEvent("EMPLOYE", employe.getId().toString(), "Employé " + matricule + " (" + username + ")", ADMIN_MAKER_ID.toString());
        }

        // 3. Affectation au Poste
        List<AffectationPosteEntity> existingAffs = affectationRepository.findByUtilisateurIdAndStatut(user.getId(), StatutAffectation.ACTIF);
        boolean alreadyAssignedToThisPoste = existingAffs.stream().anyMatch(a -> a.getPoste().getId().equals(poste.getId()));

        if (!alreadyAssignedToThisPoste) {
            AffectationPosteEntity aff = AffectationPosteEntity.builder()
                    .id(UUID.randomUUID())
                    .utilisateur(user)
                    .poste(poste)
                    .typeAffectation(typeAff)
                    .dateDebut(datePriseFonction)
                    .statut(StatutAffectation.ACTIF)
                    .referenceDecision(refDecision)
                    .build();
            affectationRepository.save(aff);

            poste.setEffectifActuel(poste.getEffectifActuel() + 1);
            posteRepository.save(poste);

            auditEvent("AFFECTATION_POSTE", aff.getId().toString(), "Affectation " + username + " -> " + poste.getCodePoste(), ADMIN_MAKER_ID.toString());
        }

        return user;
    }

    private void createSecondAffectation(UtilisateurEntity user, PosteEntity poste, TypeAffectation typeAff,
                                        LocalDate dateDebut, String refDecision) {
        List<AffectationPosteEntity> existingAffs = affectationRepository.findByUtilisateurIdAndStatut(user.getId(), StatutAffectation.ACTIF);
        boolean alreadyAssigned = existingAffs.stream().anyMatch(a -> a.getPoste().getId().equals(poste.getId()));
        if (!alreadyAssigned) {
            AffectationPosteEntity aff = AffectationPosteEntity.builder()
                    .id(UUID.randomUUID())
                    .utilisateur(user)
                    .poste(poste)
                    .typeAffectation(typeAff)
                    .dateDebut(dateDebut)
                    .statut(StatutAffectation.ACTIF)
                    .referenceDecision(refDecision)
                    .build();
            affectationRepository.save(aff);

            poste.setEffectifActuel(poste.getEffectifActuel() + 1);
            posteRepository.save(poste);

            auditEvent("AFFECTATION_POSTE_CUMUL", aff.getId().toString(), "Cumul Affectation " + user.getUsername() + " -> " + poste.getCodePoste(), ADMIN_MAKER_ID.toString());
        }
    }

    /**
     * Désactive irréversiblement les deux utilisateurs bootstrap 'admin_maker' et 'admin_checker'
     * en base locale et dans Keycloak, une fois le premier utilisateur (Directeur Général) opérationnel.
     */
    private void deactivateBootstrapRoots() {
        log.info("Désactivation définitive des comptes bootstrap 'admin_maker' et 'admin_checker'...");

        deactivateSystemUser("admin_maker");
        deactivateSystemUser("admin_checker");
    }

    private void deactivateSystemUser(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.isActif()) {
                user.setActif(false);
                userRepository.save(user);
                log.info("Compte bootstrap '{}' désactivé en base de données.", username);
            }
        });

        try {
            keycloakProvisioningPort.setUserActive(username, false);
            log.info("Compte bootstrap '{}' désactivé dans Keycloak.", username);
        } catch (Exception e) {
            log.warn("Impossible de désactiver le compte Keycloak '{}' : {}", username, e.getMessage());
        }

        auditEvent("DEACTIVATION_ROOT", username, "Désactivation root " + username + " post-bootstrap", ORIGIN_SYSTEM_ID.toString());
    }

    private UUID provisionKeycloakSafe(UUID id, String username, String firstName, String lastName, String email) {
        try {
            return keycloakProvisioningPort.createKeycloakUser(id, username, email, firstName, lastName, true);
        } catch (Exception e) {
            log.warn("Keycloak non joignable ou erreur lors du provisionnement de '{}' (ID généré en repli) : {}", username, e.getMessage());
            return id;
        }
    }

    private void auditEvent(String entityName, String entityPk, String description, String actorId) {
        try {
            Map<String, Object> payload = Map.of(
                    "action", "SEED_SODEPA",
                    "entity", entityName,
                    "pk", entityPk,
                    "description", description,
                    "timestamp", LocalDateTime.now().toString()
            );

            clickHouseManager.saveMakerChecker(
                    MakerCheckerMessageInput.builder()
                            .id(UUID.randomUUID())
                            .entityName(entityName)
                            .entityPk(entityPk)
                            .payload(objectMapper.writeValueAsString(payload))
                            .maker_id(actorId)
                            .checker_id(actorId)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        } catch (Exception e) {
            log.debug("Tracé ClickHouse ignoré (ClickHouse non connecté) : {}", e.getMessage());
        }
    }
}
