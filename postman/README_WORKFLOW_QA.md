# Guide QA Postman — Module Workflow SODEPA ERP

Ce dossier contient la suite de tests QA **Postman v2.1** complète pour le module **Workflow** (`Workflow Management` et `Instances Métier`), couvrant les **13 endpoints** de [WorkflowRestController.java](file:///c:/Users/GLC/Documents/sodepa/erp/src/main/java/com/sodepa/erp/workflow/presentation/rest/WorkflowRestController.java) ainsi que le déclenchement réel d'instances depuis les modules métiers (**Users / Maker-Checker**, **RH**, **Achats**, **Budget**) et la piste d'**Audit**.

---

## 1. Livrables

| Fichier | Rôle |
| --- | --- |
| [SODEPA_ERP_Workflow_QA.postman_collection.json](file:///c:/Users/GLC/Documents/sodepa/erp/postman/SODEPA_ERP_Workflow_QA.postman_collection.json) | Collection Postman v2.1 (**95 requêtes**, **7 dossiers**, scripts `Pre-request` et `Tests` chaînables). |
| [SODEPA_ERP_Workflow_QA.postman_environment.json](file:///c:/Users/GLC/Documents/sodepa/erp/postman/SODEPA_ERP_Workflow_QA.postman_environment.json) | Environnement local (`http://localhost:8087`) préconfiguré avec les 6 comptes QA et **69 variables**. |
| [README_WORKFLOW_QA.md](file:///c:/Users/GLC/Documents/sodepa/erp/postman/README_WORKFLOW_QA.md) | Documentation d'exécution QA, matrice de couverture et anomalies connues à surveiller. |

---

## 2. Prérequis & Import

1. Démarrer l'infrastructure locale SODEPA : PostgreSQL, Redis, Keycloak (`http://localhost:8070`) et le backend ERP (`http://localhost:8087`, voir [application.yml](file:///c:/Users/GLC/Documents/sodepa/erp/src/main/resources/application.yml)).
2. Dans **Postman** :
   - Cliquer sur **Import** et sélectionner les deux fichiers `.json` ci-dessus.
   - Activer l'environnement **`SODEPA ERP — Local QA Workflow (8087)`** en haut à droite.
   - *(Automatique)* Le script `Pre-request` au niveau de la collection injecte automatiquement un UUID dans l'en-tête **`X-Correlation-Id`** exigé par `JwtAuthenticationFilter` et `TrackingUserActionsFilter` sur chaque requête.

### Exécution automatisée (Collection Runner / Newman)

En raison du rate-limiting Redis `FAIL_CLOSED` configuré dans `application.yml` :
- **`sodepa-auth-public`** (`/api/auth/login`) : **10 requêtes / 60 secondes**.
- **`sodepa-default-catchall`** (endpoints authentifiés) : **30 requêtes / 60 secondes** par path et utilisateur.

> **Recommandation :** Dans Postman Collection Runner, régler **Delay = `1000 ms`** (ou `1500 ms` sur la batterie complète) afin d'éviter un `429 Too Many Requests` sur `/api/workflows/task/decision` ou `/api/workflows/instances/{id}`.

Avec **Newman** :
```bash
newman run postman/SODEPA_ERP_Workflow_QA.postman_collection.json \
  -e postman/SODEPA_ERP_Workflow_QA.postman_environment.json \
  --delay-request 1000 \
  --reporters cli,htmlextra
```

---

## 3. Acteurs QA Seedés

Tous les comptes utilisent le mot de passe par défaut configuré (`DefaultPassword123!`).

| Persona | Username | Origine | Permissions clés | Rôle dans la suite QA |
| --- | --- | --- | --- | --- |
| **Admin Maker** | `admin_maker` | [UserSeeder](file:///c:/Users/GLC/Documents/sodepa/erp/src/main/java/com/sodepa/erp/user/infrastructure/adapter/UserSeeder.java) | Toutes permissions (`WORKFLOW_CONFIG`, `WORKFLOW_DECISION`) | Administre les circuits (`01`, `02`), initie des instances (`USER`, `EMPLOYE`), teste le blocage **4-Yeux (`403`)** et la levée **`ASK_REVISION` (`200`)**. |
| **Admin Checker** | `admin_checker` | [UserSeeder](file:///c:/Users/GLC/Documents/sodepa/erp/src/main/java/com/sodepa/erp/user/infrastructure/adapter/UserSeeder.java) | Toutes permissions (`WORKFLOW_CONFIG`, `WORKFLOW_DECISION`) | Validateur principal (`APPROUVED`, `REJETE`, `ASK_REVISION`, `SET_ATTRIBUTE`). |
| **Directeur Général** | `denis.koulagna` | [SodepaScrapedDataSeeder](file:///c:/Users/GLC/Documents/sodepa/erp/src/main/java/com/sodepa/erp/organigramme/infrastructure/adapter/SodepaScrapedDataSeeder.java) | `POSTE_DG` — Toutes permissions | Valide la **signature électronique** finale sur `WF-RH-RECRUTEMENT`. |
| **Directeur DAF** | `desire.wetjeng` | [SodepaScrapedDataSeeder](file:///c:/Users/GLC/Documents/sodepa/erp/src/main/java/com/sodepa/erp/organigramme/infrastructure/adapter/SodepaScrapedDataSeeder.java) | `POSTE_DIRECTEUR_DAF` | Vérifie la visibilité dans `GET /taches/my-tasks` des tâches assignées par `CIRCUIT_VALIDATION_STANDARD`. |
| **Chef DPD Budget** | `godwill.fuh` | [SodepaScrapedDataSeeder](file:///c:/Users/GLC/Documents/sodepa/erp/src/main/java/com/sodepa/erp/organigramme/infrastructure/adapter/SodepaScrapedDataSeeder.java) | `CREATE_BUDGET` (sans `VALIDATE_BUDGET`) | Soumet un `BUDGET_PLAN` sans auto-validation pour vérifier l'instanciation workflow `BUDGET_PLAN`. |
| **Agent Courrier** | `flora.fuben` | [SodepaScrapedDataSeeder](file:///c:/Users/GLC/Documents/sodepa/erp/src/main/java/com/sodepa/erp/organigramme/infrastructure/adapter/SodepaScrapedDataSeeder.java) | `CREATE_EXPRESSION`, `VIEW_EXPRESSION` (sans droits WF) | Soumet des `EXPRESSION_BESOIN` sans auto-validation et porte les tests négatifs **RBAC / Need-to-Know (`403`)**. |

---

## 4. Structure de la Collection & Couverture

| Dossier | Objectif |
| --- | --- |
| **00 - Initialisation QA & Authentification Multi-Acteurs** | Connexion des 6 personas + récupération dynamique des UUIDs (profils utilisateurs, unités `DG`/`SDRH`/`SERV_COURRIER`/`DPD`, postes budgétés). |
| **01 - Workflow Management — Catalogue des Métiers** | `GET /metiers`, alias `/api/v1/workflows/metiers`, `GET /metiers/active` (pagination/tri), `PATCH /metiers/{id}/statut?active=false\|true`. |
| **02 - Workflow Management — Définitions, Étapes & Acteurs** | `GET /definitions?active=true\|false`, `GET /verifier-maker`, `POST /definitions` (3 étapes multi-types), `PUT /definitions/{id}` (vérif `version: 1 → 2`), `PATCH .../etapes/{etapeId}/statut`, `PATCH .../acteurs/{acteurId}/statut`. |
| **03 - Instances Métier — Déclenchement Multi-Modules & Consultation** | Initiation réelle sans endpoint `/demarrer` public via `POST /api/v1/users/init_create` (`USER`), `POST /api/v1/rh/employes` (`WF-RH-RECRUTEMENT`), `POST /api/v1/achats/expressions-besoin` (`EXPRESSION_BESOIN`), `POST /api/budget/plans` (`BUDGET_PLAN`) ; contrôle `GET /instances/{id}`, `GET /instances/entite/{entityName}/{entityId}` et `204 No Content` sur entité absente. |
| **04 - Exécution des Tâches & Cycles de Décision E2E** | **4A** : `ASK_REVISION` → `SUSPENDU` → re-soumission initiateur (`APPROUVED`) → approbation finale → vérification matérialisation `GET /api/v1/users/{id}`.<br>**4B** : Boîte DAF, approbation Achats → `APPROUVEE_DG`, circuit complet 3 étapes `WF-RH-RECRUTEMENT` avec `champsModifiesJson` (`SET_ATTRIBUTE`) et signature DG.<br>**4C** : Rejet définitif (`REJETE`) → propagation `ExpressionBesoin.statut === "REJETEE"` + consultation historique `REJETED` / `APPROUVED` / `EXPIRED`. |
| **05 - Tests Négatifs, Sécurité & Cas Limites** | `401` sans JWT ; `403 ACCES_REFUSE` (RBAC `WORKFLOW_CONFIG` / `WORKFLOW_DECISION`, Need-to-Know, Règle 4-Yeux même pour `admin_maker`) ; `400 REQUETE_INVALIDE` (`decision=PENDING`) ; `400 VALIDATION` (`@Valid`) ; `404 RESSOURCE_INTROUVABLE` ; `409 CONFLIT_METIER` (tâche déjà traitée, étape désactivée avec restauration immédiate) ; documentation `ADD_FILE` vs `@RequestBody`. |
| **06 - Audit, Cycle Refresh/Logout & Nettoyage QA** | Audit `GET /api/auth/audit/activities` & ClickHouse, archivage (`actif=false`) de la définition QA créée, test `POST /api/auth/refresh` & `POST /api/auth/logout` sur session éphémère. |

---

## 5. Points Techniques & Anomalies Connues à Surveiller en QA

1. **Mode `ADD_FILE` incompatible JSON (`05.17`)** : `TraitTaskWorkflowRequest` contient `private MultipartFile file;`, mais `POST /api/workflows/task/decision` est annoté `@RequestBody`. Une étape exigeant un fichier (`ADD_FILE` ou `FILE_AND_ATTRIBUTE`) est documentée comme bloquée en `application/json`.
2. **Observation double instance sur `EMPLOYE` (`03.5`)** : `RhService.creerEmploye()` appelle à la fois `submitChange("EMPLOYE", ...)` et `demarrerWorkflow("WF-RH-RECRUTEMENT", "EMPLOYE", ...)`. Or `WorkflowInstanceRepository.findByEntityNameAndEntityId(...)` retourne un `Optional<WorkflowInstanceEntity>`. La requête `03.5` relève si une non-unicité Hibernate (`500`) survient.
3. **Régénération des UUIDs d'étapes lors d'un `PUT /definitions/{id}` (`02.8`)** : `modifierDefinition` remplace la collection `etapes` en base ; les scripts Postman re-capturent automatiquement les nouveaux IDs après chaque `PUT`.
4. **Casse et orthographe exactes des Enums Java** :
   - Décision (`WorkflowDecision`) : **`APPROUVED`** (double `P`), **`REJETE`** (sans `D`), **`ASK_REVISION`**, **`PENDING`** (interdit en décision).
   - Statut Tâche (`StatutWorkflowTache`) : **`PENDING`**, **`APPROUVED`**, **`REJETED`** (avec `D`), **`DELEGATED`**, **`EXPIRED`**.
   - Statut Instance (`StatutWorkflowInstance`) : **`EN_COURS`**, **`APPROUVE`** (un `P`, sans `D`), **`REJETE`**, **`SUSPENDU`**, **`ANNULE`**.
   - Query params : `PATCH /metiers/{id}/statut?active=...` utilise **`active`**, tandis que `PATCH .../etapes/{id}/statut?actif=...` et `PATCH .../acteurs/{id}/statut?actif=...` utilisent **`actif`**.
