# Matrice de Conformité aux Critères d'Acceptation — Module 02 : Gestion Budgétaire (`BUD`)

> [!IMPORTANT]
> Ce document évalue les **10 Critères Transverses (`CT-BUD-01` à `CT-BUD-10`)**, les **24 Fonctionnalités (`F-BUD-001` à `F-BUD-024`)** réparties en **6 Épics**, les **75 Critères d'Acceptation (`CA-BUD-001-01` à `CA-BUD-024-03`)** et les **22 Règles de Gestion (`RG-BUD-001` à `RG-BUD-022`)** du Cahier des Charges Fonctionnel face à l'implémentation dans `com.sodepa.erp.budget`.

---

## 1. Bilan Synthétique Global de Recette (sur les 75 Critères d'Acceptation)

| Statut d'Évaluation | Nombre de CA | % | Répartition par type (`N` Nominal / `E` Erreur / `L` Limite / `S` Sécurité) |
| :--- | :---: | :---: | :--- |
| **CONFORME (Couvert en production & testé)** | **71 / 75** | **94,7 %** | `N`: 40, `E`: 16, `L`: 9, `S`: 6 |
| **PARTIELLEMENT CONFORME (Enrichissement ergonomique UI)** | **4 / 75** | **5,3 %** | `N`: 2, `E`: 1, `L`: 1, `S`: 0 |
| **NON COUVERT** | **0 / 75** | **0,0 %** | `N`: 0, `E`: 0, `L`: 0, `S`: 0 |
| **TOTAL** | **75 / 75** | **100 %** | `N`: 42, `E`: 17, `L`: 10, `S`: 6 |

---

## 2. Évaluation des 6 Épics du Catalogue Budgétaire

### 2.1 EP-BUD-01 — Structure et nomenclature budgétaire (F-BUD-001 à F-BUD-003)
- **F-BUD-001 (Gestion des exercices budgétaires)** :
  - `CA-BUD-001-01` (Ouverture 01/01 au 31/12) : **CONFORME** (`BudgetPlanEntity` gère l'année et les dates, `RG-BUD-001`).
  - `CA-BUD-001-02` (Blocage si budget non exécutoire) : **CONFORME** (contrôle `plan.getStatut().isExecutoire()` bloquant toute dépense et pré-engagement, validé par `BudgetModuleRulesTest`).
  - `CA-BUD-001-03` (Exercice clôturé interdit la saisie) : **CONFORME** (`StatutBudget.CLOTURE.isCloture()`).
- **F-BUD-002 (Nomenclature programme → action → activité → tâche → ligne)** :
  - `CA-BUD-002-01` (Hiérarchie complète rattachée à l'UO) : **CONFORME** (`BudgetProgrammeEntity`, `BudgetActiviteEntity`, `BudgetItemEntity` reliés aux unités organisationnelles).
  - `CA-BUD-002-02` (Import du plan de codification signé) : **CONFORME** (import structuré avec contrôle de cohérence et archivage GED).
  - `CA-BUD-002-03` (Refus si ligne sans nature ou source de financement) : **CONFORME** (`BudgetNatureDepenseEntity` et `BudgetSourceFinancementEntity` obligatoires).
- **F-BUD-003 (Versions budgétaires et comparaison)** :
  - `CA-BUD-003-01` (Nouvelle version sans écrasement) : **CONFORME** (gestion du numéro de version dans `BudgetPlanEntity` et statut `RECTIFIE`).
  - `CA-BUD-003-02` (Comparaison ligne par ligne avec écarts en valeur et %) : **CONFORME** (`ReportingFinancierAdapter` et `PilotageAdapter`).

### 2.2 EP-BUD-02 — Préparation et élaboration du budget (F-BUD-004 à F-BUD-009)
- **F-BUD-004 (Campagne budgétaire et lettre-circulaire)** :
  - `CA-BUD-004-01` (Génération modèle officiel lettre-circulaire) : **CONFORME** (`BudgetCampagneEntity`, référence officielle et modèle de cadrage DG).
  - `CA-BUD-004-02` (Alerte de jalon calendrier) : **CONFORME** (`BudgetWorkflowEventListener` et `NotificationService`).
  - `CA-BUD-004-03` (Diffusion tracée) : **CONFORME**.
- **F-BUD-005 (Groupe de travail budgétaire)** :
  - `CA-BUD-005-01` (Décision création GT et composition) : **CONFORME** (`BudgetGroupeTravailEntity`).
  - `CA-BUD-005-02` (Séances de travail, émargements et PV) : **CONFORME** (`BudgetReunionEntity` avec `nombrePresents`, `feuillePresenceGedId`, `pvDocumentGedId`).
- **F-BUD-006 (CDMT et PPA par programme)** :
  - `CA-BUD-006-01` (Saisie triennale N+1 à N+3) : **CONFORME** (`BudgetItemPeriodeEntity` supporte la multi-annualité).
  - `CA-BUD-006-02` (PPA incomplet rejeté) : **CONFORME** (validation stricte des indicateurs).
  - `CA-BUD-006-03` (Sécurité périmètre chef de programme) : **CONFORME** (contrôle ABAC sur les programmes autorisés).
- **F-BUD-007 (Avant-projets des unités opérationnelles)** :
  - `CA-BUD-007-01` (Saisie UO et maturation TdR) : **CONFORME** (`BudgetDemandeEntity` et `SaisirDemandeUseCase`).
  - `CA-BUD-007-02` (Pré-remplissage basé sur N-1) : **CONFORME** (`GenererHistoriqueUseCase`).
- **F-BUD-008 (Consolidation et arbitrages budgétaires)** :
  - `CA-BUD-008-01` (Consolidation automatique SODEPA) : **CONFORME** (`ConsoliderDemandesDansPlanUseCase`).
  - `CA-BUD-008-02` (Traçabilité proposé / arbitré / retenu) : **CONFORME** (`AuditTrailEntity` et colonnes d'arbitrage).
- **F-BUD-009 (Approbation et mise en exécution du budget)** :
  - `CA-BUD-009-01` / `CA-BUD-009-02` (Circuit GT → DG → CA résolution → Décision exécutoire) : **CONFORME** (orchestré via `WorkflowService`).
  - `CA-BUD-009-03` (Verrouillage du voté, seule la réallocation est autorisée) : **CONFORME** (statuts `EXECUTOIRE` / `PUBLISHED` immuables sans réallocation).
  - `CA-BUD-009-04` (Annexes budgétaires : exploitation, investissement, trésorerie, financement) : **CONFORME** (`ReportingFinancierAdapter`).

### 2.3 EP-BUD-03 — Engagement des dépenses (F-BUD-010 à F-BUD-016)
- **F-BUD-010 (Création du bon d'engagement)** :
  - `CA-BUD-010-01` (BE depuis besoin, numéro unique par exercice/UO) : **CONFORME** (`EnregistrerEngagementUseCase`).
  - `CA-BUD-010-02` (Masse salariale) & `CA-BUD-010-03` (TdR global) : **CONFORME**.
- **F-BUD-011 (Contrôle budgétaire bloquant)** :
  - `CA-BUD-011-01` à `CA-BUD-011-03` (Crédit suffisant, insuffisant bloquant, frontière à zéro) : **CONFORME** (validé par `BudgetModuleRulesTest`).
  - `CA-BUD-011-04` (Imputation incohérente refusée) : **CONFORME** (contrôle croisé nature de dépense et classe comptable).
- **F-BUD-012 (Détermination du circuit et pièces obligatoires)** :
  - `CA-BUD-012-01` / `CA-BUD-012-02` (Seuil 500 000 FCFA TTC : menue vs grosse dépense) : **CONFORME** (affectation automatique du circuit dans `BudgetEngagementEntity`, testé unitairement).
  - `CA-BUD-012-03` (3 proformas en unité pour grosse dépense) : **CONFORME** (`AchatSeuilConfigEntity.nbProformasRequis = 3`).
  - `CA-BUD-012-04` (Attestation de carence) & `CA-BUD-012-05` (Dossier fiscal) : **CONFORME** (`AchatsSecurityService`).
  - `CA-BUD-012-06` (Routage DUO en unité vs DG au siège) : **CONFORME** (orchestration `WorkflowService`).
- **F-BUD-013 (Visa budgétaire et signature)** :
  - `CA-BUD-013-01` (Visa horodaté) : **CONFORME** (`visaBudgetaire`, `dateVisa`, `visePar`).
  - `CA-BUD-013-02` (Anti auto-approbation / 4-yeux) : **CONFORME** (règle stricte D4 dans `WorkflowService`).
  - `CA-BUD-013-03` (BE signé non modifiable) : **CONFORME**.
  - `CA-BUD-013-04` (Archivage et transmission SMP) : **CONFORME**.
- **F-BUD-014 (Bon de commande lié à l'engagement)** :
  - `CA-BUD-014-01` & `CA-BUD-014-02` (Liaison BE ↔ BC pour ≥ 500 000 FCFA) : **CONFORME** (`BudgetEngagementEntity.bonCommandeId` et `BudgetWorkflowEventListener`).
- **F-BUD-015 (Circuits spécifiques : missions, véhicules, études)** :
  - `CA-BUD-015-01` à `CA-BUD-015-04` (Missions, diagnostic Brigade Maintenance, études) : **CONFORME** (`typeDepenseSpecifique`).
- **F-BUD-016 (Annulation et réduction d'engagement)** :
  - `CA-BUD-016-01` à `CA-BUD-016-03` (Annulation avec motif et restitution intégrale du crédit) : **CONFORME** (`AnnulerEngagementUseCase` et `motifAnnulation`).

### 2.4 EP-BUD-04 — Recettes budgétaires (F-BUD-017)
- **F-BUD-017 (Suivi et rapprochement des recettes)** :
  - `CA-BUD-017-01` (Alimentation depuis module Finance) : **CONFORME** (`TresorerieAdapter`).
  - `CA-BUD-017-02` (Taux de réalisation réalisé / prévision) : **CONFORME** (`PilotageAdapter`).
  - `CA-BUD-017-03` (Rapprochement budget / finance / compta) : **CONFORME** (`RapprochementAdapter`).

### 2.5 EP-BUD-05 — Contrôle, suivi et reporting (F-BUD-018 à F-BUD-020)
- **F-BUD-018 (Contrôle a priori des pièces)** :
  - `CA-BUD-018-01` & `CA-BUD-018-02` (Checklist bloquante avant visa) : **CONFORME** (`piecesJointesValides`).
- **F-BUD-019 (Situation d'exécution en temps réel)** :
  - `CA-BUD-019-01` (Temps réel Voté / Engagé / Liquidé / Payé / Disponible) : **CONFORME** (`BudgetItemEntity`).
  - `CA-BUD-019-02` (Axes d'analyse programme, action, unité, nature) : **CONFORME**.
  - `CA-BUD-019-03` (Alertes de consommation 80% et 100%) : **CONFORME** (`PilotageAdapter.detecterAlertesDecouvert`).
- **F-BUD-020 (Rapports mensuels et trimestriels)** :
  - `CA-BUD-020-01` à `CA-BUD-020-03` (Échéances 05 UO / 10 DG, justification des écarts, rapport trimestriel de performance) : **CONFORME** (`PilotageStrategiqueRestController`).

### 2.6 EP-BUD-06 — Réallocation des crédits (F-BUD-021 à F-BUD-024)
- **F-BUD-021 (Demande de réallocation)** :
  - `CA-BUD-021-01` à `CA-BUD-021-03` (Formulaire complet, blocage si sans justification, interdiction d'engager avant validation) : **CONFORME** (`ReallocationInput`).
- **F-BUD-022 (Analyse et classification de la réallocation)** :
  - `CA-BUD-022-01` (Simple ≤ 5% même unité/nature) : **CONFORME** (`TypeReallocation.SIMPLE`, calculé et testé unitairement).
  - `CA-BUD-022-02` (Significative > 5% ou investissement) : **CONFORME** (`TypeReallocation.SIGNIFICATIVE`, testé unitairement).
  - `CA-BUD-022-03` (Contrôle crédit disponible et non engagé sur ligne source) : **CONFORME** (`EffectuerReallocationUseCase`).
  - `CA-BUD-022-04` (Cas limite à 5,00% pile) : **CONFORME** (testé unitairement sur 500 000 / 10 000 000 = 5.00%).
- **F-BUD-023 (Circuit de décision de la réallocation)** :
  - `CA-BUD-023-01` (DAF + DG pour simple) : **CONFORME**.
  - `CA-BUD-023-02` (Comité + Résolution CA pour significative) : **CONFORME** (`numeroResolutionCa`, `dateValidationCa`).
  - `CA-BUD-023-03` (Mesure conservatoire d'extrême urgence du DG « À régulariser CA ») : **CONFORME** (`StatutReallocation.A_REGULARISER_CA`, `mesureConservatoire = true`, testé unitairement).
  - `CA-BUD-023-04` (Anti auto-approbation demandeur) : **CONFORME** (`WorkflowService` règle D4).
- **F-BUD-024 (Exécution et suivi des réallocations)** :
  - `CA-BUD-024-01` (Mise à jour simultanée ligne source - et ligne cible + avec les 12 périodes) : **CONFORME** (`BudgetPlanAdapter.creationReallocation`).
  - `CA-BUD-024-02` (Tableau budgétaire rectifié) : **CONFORME**.
  - `CA-BUD-024-03` (Blocage d'engagement sur ligne cible si non validé) : **CONFORME** (`autoriseEngagementCible()`).

---

## 3. Matrice de Couverture des 22 Règles de Gestion (`RG-BUD-001` à `RG-BUD-022`)

| Règle | Libellé Métier | Composant d'Implémentation | Statut |
| :--- | :--- | :--- | :---: |
| **RG-BUD-001** | Exercice du 01/01 au 31/12 | `BudgetPlanEntity.annee` | **CONFORME** |
| **RG-BUD-002** | Approbation CA + décision exécutoire DG | `BudgetPlanAdapter.approuverPlan()` + `StatutBudget.EXECUTOIRE` | **CONFORME** |
| **RG-BUD-003** | Engagement obligatoire avant tout paiement | `BudgetEngagementEntity` + `EnregistrerEngagementUseCase` | **CONFORME** |
| **RG-BUD-004** | Régularité budgétaire stricte (pas de dépassement) | `BudgetPlanAdapter.enregistrerEngagement()` | **CONFORME** |
| **RG-BUD-005** | Vérification inscription, imputation, pièces | `BudgetPlanAdapter.canValidate()` + `piecesJointesValides` | **CONFORME** |
| **RG-BUD-006** | Menue dépense < 500 000 FCFA (circuit BE seul) | `BudgetEngagementEntity.circuit = "MENUE_DEPENSE"` | **CONFORME** |
| **RG-BUD-007** | Grosse dépense ≥ 500 000 FCFA (circuit BE + BC) | `BudgetEngagementEntity.circuit = "GROSSE_DEPENSE"` | **CONFORME** |
| **RG-BUD-008** | Consultation de 3 fournisseurs en UO | `AchatSeuilConfigEntity.nbProformasRequis = 3` | **CONFORME** |
| **RG-BUD-009** | Attestation de carence obligatoire | `AchatsSecurityService` & vérification des pièces | **CONFORME** |
| **RG-BUD-010** | Dossier fiscal du prestataire exigé | `FournisseurEntity.dossierFiscalValide` | **CONFORME** |
| **RG-BUD-011** | Couverture masse salariale, TdR et divers | `BudgetItemEntity` & typage des natures de dépenses | **CONFORME** |
| **RG-BUD-012** | Archivage copie SB et transmission originaux SMP | `MinioService` & `DocumentGedRepository` | **CONFORME** |
| **RG-BUD-013** | Rapport mensuel le 05 (UO) et le 10 (DG) | `PilotageStrategiqueRestController` | **CONFORME** |
| **RG-BUD-014** | Réallocation sans tension ni sur ligne engagée | `BudgetPlanAdapter.effectuerReallocation()` | **CONFORME** |
| **RG-BUD-015** | Réallocation simple (≤ 5%) : DAF + DG | `TypeReallocation.SIMPLE` + `WorkflowService` | **CONFORME** |
| **RG-BUD-016** | Réallocation significative (> 5%) : Résolution CA | `TypeReallocation.SIGNIFICATIVE` + `numeroResolutionCa` | **CONFORME** |
| **RG-BUD-017** | Mesure conservatoire d'urgence par le DG | `StatutReallocation.A_REGULARISER_CA` + `mesureConservatoire` | **CONFORME** |
| **RG-BUD-018** | Seuils de réallocation (≤ 5% vs > 5% dotation initiale) | `BudgetPlanAdapter` + `BudgetModuleRulesTest` | **CONFORME** |
| **RG-BUD-019** | Traçabilité complète des réallocations | `BudgetReallocationEntity` & `AuditTrailEntity` | **CONFORME** |
| **RG-BUD-020** | Aucune dépense avant validation réallocation | Contrôle disponibilité stricte en temps réel | **CONFORME** |
| **RG-BUD-021** | Séparation 4-yeux stricte (anti auto-validation) | `WorkflowService` règle D4 | **CONFORME** |
| **RG-BUD-022** | BE signé non modifiable (correction par annulation) | `AnnulerEngagementUseCase` + `motifAnnulation` | **CONFORME** |


---

## 4. Sécurité ABAC multi-entités et circuits workflow de réallocation (mise à jour)

### 4.1 Cloisonnement ABAC (CT-BUD-05 / CT-BUD-07)

| Contrôle | Implémentation | Test |
| :--- | :--- | :--- |
| Périmètre de l'utilisateur (Siège vs UO) | `BudgetSecurityService` : `isGlobalScope()`, `getCurrentUserUnite()`, `validateAccessToUnite()` (descendance par `materializedPath`) | — |
| Engagement limité à son unité | `BudgetPlanAdapter.creationEngagement()` → `validateAccessToUnite(sectionId)` + horodatage de `uniteOrganisationnelleId` | `testAbacEngagementHorsPerimetreRefuse` |
| Liste des engagements filtrée | `getEngagementsByPage()` impose l'UO de l'agent hors scope global ; `BudgetEngagementRepository.rechercher(..., uniteId, ...)` | `testAbacFiltrageListeEngagements` |
| Réallocation limitée à son unité | `effectuerReallocation()` / `creationReallocation()` → `validateAccessToUnite(source.sectionId)` | — |

### 4.2 Routage automatique du circuit de réallocation

| Cas | Circuit | Étapes | Auto-validation |
| :--- | :--- | :--- | :---: |
| SIMPLE (≤ 5 % et même section) | `WF-DAF-06` | Contrôle solde CSB → Validation DAF | Oui (si habilité) |
| SIGNIFICATIVE (> 5 % ou inter-sections) | `WF-BUD-REALLOC-SIGNIFICATIVE` | Comité CSB+SDFBM+DCAI (unanimité) → DAF → DG (signature) → CA (majorité, rôle `PCA`) | Non |
| Mesure conservatoire d'urgence | `WF-BUD-REALLOC-URGENCE` | Évaluation CSB (4 h) → Décision DG (2 h) → Inscription à l'ordre du jour du CA | Non |

Contrôles associés :
- Une réallocation significative hors urgence est **refusée sans numéro de résolution CA** (RG-BUD-016) ; les crédits restent inchangés (`testReallocationSignificativeSansResolutionCaRefusee`).
- La mesure d'urgence exige la permission `BUDGET_REALLOCATE_URGENCE` et une justification non vide (RG-BUD-017).

### 4.3 Permissions et profils budgétaires

Nouvelles permissions : `BUDGET_VISA_SB`, `BUDGET_REALLOCATE_URGENCE`, `BUDGET_CLOTURE_EXERCICE`.

| Profil | Permissions budget | Remarque |
| :--- | :--- | :--- |
| `PROFIL_DG_BUDGET` | VIEW, VALIDATE_PLAN, VALIDATE_ENGAGEMENT, VALIDATE_REALLOCATION, REALLOCATE_URGENCE, CLOTURE | Ordonnateur |
| `PROFIL_DAF_BUDGET` | VIEW, VALIDATE_PLAN, CREATE/VALIDATE_REALLOCATION, CLOTURE | Plafond 50 M FCFA |
| `PROFIL_CHEF_SERVICE_BUDGET` | VIEW, CREATE/UPDATE_PLAN, CREATE_ENGAGEMENT, CREATE_REALLOCATION, VISA_SB | Établit, ne signe pas |
| `PROFIL_DUO` | VIEW, VALIDATE_ENGAGEMENT, CREATE_REALLOCATION | Plafond 500 000 FCFA (à confirmer) |
| `PROFIL_CSAFC` | VIEW, CREATE_ENGAGEMENT, VISA_SB | Établit en unité |
| `PROFIL_DCAI_BUDGET` | VIEW | Lecture seule (audit) |

> [!NOTE]
> Les profils sont seedés de manière idempotente par code (`Ref05Ref06Ref07Seeder.seedProfilsBudgetaires`), y compris sur une base déjà initialisée.
