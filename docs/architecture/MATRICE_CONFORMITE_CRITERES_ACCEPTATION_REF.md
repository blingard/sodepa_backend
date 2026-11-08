# Matrice de Conformité aux Critères d'Acceptation — Module 01 (`REF`)

> [!IMPORTANT]
> Ce document évalue les **10 Critères Transverses (`CT-REF-01` à `CT-REF-10`)**, les **34 Fonctionnalités (`F-REF-001` à `F-REF-034`)**, les **101 Critères d'Acceptation (`CA-REF-001-01` à `CA-REF-034-02`)** et les **28 Règles de Gestion (`RG-REF-001` à `RG-REF-028`)** face à l'implémentation actuelle dans `c:\Users\GLC\Documents\sodepa\erp`.

---

## 1. Bilan Quantitatif Global de Recette (sur les 101 Critères d'Acceptation)

| Statut de Recette | Nombre de CA | % | Répartition par type (`N` Nominal / `E` Erreur / `L` Limite / `S` Sécurité) |
| :--- | :---: | :---: | :--- |
| **CONFORME (Pass)** | **16 / 101** | **15,8 %** | `N`: 8, `E`: 2, `L`: 0, `S`: 6 |
| **PARTIEL (Écart de règle ou d'attribut)** | **26 / 101** | **25,7 %** | `N`: 16, `E`: 4, `L`: 2, `S`: 4 |
| **NON CONFORME / ABSENT (Fail)** | **59 / 101** | **58,4 %** | `N`: 32, `E`: 11, `L`: 8, `S`: 8 |
| **TOTAL** | **101 / 101** | **100 %** | `N`: 56, `E`: 17, `L`: 10, `S`: 18 |

---

## 2. Évaluation des 10 Critères Transverses (`CT-REF-01` à `CT-REF-10`)

| ID | Critère transverse | Statut | Diagnostic dans le code source |
| :--- | :--- | :---: | :--- |
| **CT-REF-01** | Refus explicite sans droit requis | **CONFORME** | `UtilsService.hasPermission()` lève `AccesRefuseException` (HTTP 403) en nommant l'habilitation manquante. |
| **CT-REF-02** | Journalisation auteur, date/heure, action et **valeurs avant/après** | **PARTIEL** | `AuditTrailEntity` possède `ancienneValeur` et `nouvelleValeur`, mais n'est utilisé que dans le module Budget. `DuckDbAuditManager` et `MakerCheckerEngineAdapter` ne stockent que le `payload` soumis (après), **sans snapshot de la valeur avant modification**. |
| **CT-REF-03** | Champs obligatoires signalés avec message clair | **CONFORME** | Annotations Jakarta Validation (`@NotBlank`, `@NotNull`) sur les DTO `Request` et gestionnaire global d'exceptions. |
| **CT-REF-04** | Filtrage strict par périmètre (structure, unité, exercice) | **PARTIEL** | Implémenté uniquement sur les Achats via `AchatsSecurityService.getScopeMaterializedPath()` et les définitions de Workflow. **Absent** sur les Tiers, Comptes, Banques, Journaux, Organigramme et Budget. |
| **CT-REF-05** | Notification étape suivante + **rejet exige un motif** + retour à l'émetteur | **NON CONFORME** | Dans `WorkflowService.traiterTache()` et `RejectOrValidateSubmitRequest`, un rejet (`DecisionWorkflow.REJETE` ou `MakerCheckerStatus.REJECTED`) est **accepté même si `commentaire` / `notes` est `null` ou vide**. |
| **CT-REF-06** | Modèles officiels SODEPA générés et archivés automatiquement dans la GED | **NON CONFORME** | Aucun moteur de génération de documents à partir de modèles, ni archivage automatique dans `ged_documents` lors des validations. |
| **CT-REF-07** | Montants en FCFA sans décimale ; dates `JJ/MM/AAAA` | **PARTIEL** | Les colonnes monétaires JPA sont définies en `scale = 2` (ex: `precision = 18, scale = 2` dans `DelegationPouvoirEntity`) et les dates JSON sont sérialisées en ISO `AAAA-MM-JJ`. |
| **CT-REF-08** | Export des listes et états en Excel et PDF respectant filtres et périmètre | **NON CONFORME** | Aucun endpoint d'export Excel/PDF sur les référentiels (uniquement sur le reporting comptable/analytique). |
| **CT-REF-09** | Affichage < 3s et recherche < 5s | **CONFORME** | Pagination `PageRecord` (plafonnée à `MAX_PAGE_SIZE = 100`), index B-Tree sur `code` et `materialized_path`, cache `@Cacheable("organigramme-tree")`. |
| **CT-REF-10** | Seuils, taux et délais issus du paramétrage **avec date d'effet** | **NON CONFORME** | `ParametreSystemeEntity` et `AchatSeuilConfigEntity` n'ont **aucune colonne `date_effet`** ; certains plafonds sont codés en dur (ex: `10_000_000` dans `PastoralRanchEntity`). |

---

## 3. Analyse Détaillée des 34 Fonctionnalités et 101 Critères d'Acceptation

### 3.1. EP-REF-01 — Gestion des données de référence (MDM) (`F-REF-001` à `F-REF-007`)

| Fonctionnalité | ID Critère | Type | Statut | Discordance constatée dans le code |
| :--- | :--- | :---: | :---: | :--- |
| **F-REF-001**<br>Familles de référentiels | `CA-REF-001-01` | N | **ABSENT** | Aucune table/entité `FamilleReferentielEntity` ni attributs de gouvernance (`proprietaireMetier`, `gestionnaire`, `version`, `dateEffet`). |
| | `CA-REF-001-02` | E | **ABSENT** | Pas de notion de famille ni de contrôle « Propriétaire métier obligatoire ». |
| | `CA-REF-001-03` | N | **ABSENT** | Pas de paramétrage dynamique de dépendance parent-enfant entre familles de référentiels (hors `TypeStructureEntity` limité à l'organigramme). |
| | `CA-REF-001-04` | S | **PARTIEL** | Un admin technique qui possède toutes les permissions (`UserSeeder`) peut valider un fournisseur ; absence de séparation stricte entre `Cellule Informatique` et `Propriétaire métier` (`RG-REF-027`). |
| | `CA-REF-001-05` | N | **NON CONFORME** | L'enum standard `BROUILLON, ACTIF, SUSPENDU, ARCHIVE` (`RG-REF-025`) n'existe pas : les référentiels utilisent un simple `boolean actif`. |
| **F-REF-002**<br>Cycle de vie d'une donnée | `CA-REF-002-01` | N | **PARTIEL** | Le passage par `MakerCheckerRequestEntity` (`PENDING` → `ACCEPTED`) existe, mais la donnée n'existe pas à l'état `BROUILLON` ou `SOUMISE` dans sa table métier avant acceptation. |
| | `CA-REF-002-02` | N | **NON CONFORME** | Les modifications (`UpdateTiersUseCase`, `UpdateCompteUseCase`, `UpdateBankUseCase`) écrasent l'enregistrement existant après validation sans créer de `version 2` ni conserver la `version 1` consultable. |
| | `CA-REF-002-03` | E | **ABSENT** | Aucune vérification des transactions non soldées avant la désactivation d'un référentiel (`actif = false`). |
| | `CA-REF-002-04` | E | **PARTIEL** | Certaines entités n'exposent pas de `DELETE`, mais `OrganigrammeRestController.supprimerTypeStructure()` ou `deleteBank` ne retournent pas de liste d'utilisations. |
| | `CA-REF-002-05` | N | **ABSENT** | Pas de statut `BROUILLON` persisté permettant la suppression physique contrôlée d'un brouillon jamais utilisé. |
| | `CA-REF-002-06` | L | **PARTIEL** | `TiersRestController` (`/list`) et `AchatsRestController` (`agreeSeulement=true`) filtrent les actifs, mais par défaut `GET /api/achats/fournisseurs` renvoie aussi les inactifs (`agreeSeulement=false`). |
| | `CA-REF-002-07` | S | **PARTIEL** | Vérifié dans `AchatsService` (`checkerOutput.maker().id().equals(checkerId)`), **mais** dans `WorkflowService.creerTachesPourEtape()` si aucun valideur n'est résolu, la tâche est assignée à l'`initiateur` lui-même ! De plus, dans `TiersAdapter` et `CompteAdapter`, `validateOrReject` délègue au workflow où `isSuperviseurOuAdmin` autorise un superviseur à valider sa propre demande. |
| | `CA-REF-002-08` | E | **PARTIEL** | L'unicité du `code` bloque la création, mais ne retourne pas la fiche existante proposée à la réutilisation. |
| **F-REF-003**<br>Codification auto/manuelle | `CA-REF-003-01` | N | **PARTIEL** | Seul `AchatsService.creerFournisseur()` génère un code auto, mais avec un format codé en dur `"FRN-%04d"` (au lieu d'un masque paramétrable `FRN-000000`). Tous les autres référentiels (`Tiers`, `Compte`, `Banque`, `Unite`) exigent une saisie manuelle. |
| | `CA-REF-003-02` | E | **CONFORME** | Vérifié dans `TiersAdapter.initCreateTiers()`, `CompteAdapter.initCreateCompte()` et `OrganigrammeAdapter.creerUnite()`. |
| | `CA-REF-003-03` | L | **ABSENT** | Aucun contrôle de masque/expression régulière paramétrable par famille de référentiel. |
| **F-REF-004**<br>Détection & fusion doublons | `CA-REF-004-01` | N | **ABSENT** | Aucune détection insensible à la casse/accents ou floue sur `raisonSociale` / `intitule` avant création. |
| | `CA-REF-004-02` | N | **ABSENT** | Aucun service ni endpoint de fusion de deux fiches doublons avec réaffectation des transactions et archivage de la fiche secondaire. |
| | `CA-REF-004-03` | S | **ABSENT** | Ni la fonctionnalité de fusion ni la permission dédiée n'existent. |
| **F-REF-005**<br>Recherche & consultation | `CA-REF-005-01` | N | **PARTIEL** | Seuls `UserRestController` (`/search`) et `BudgetPlanRestController` ont des filtres de recherche ; `TiersRestController`, `BanqueRestController`, `CompteRestController`, `OrganigrammeRestController` n'ont pas de recherche multicritère (famille, statut, libellé). |
| | `CA-REF-005-02` | N | **PARTIEL** | La fiche par ID existe (`GET /{id}`), mais n'affiche ni les données liées ni l'historique des versions de la fiche. |
| | `CA-REF-005-03` | S | **ABSENT** | Il n'existe pas de référentiel des magasins filtré par unité de l'utilisateur connecté. |
| **F-REF-006**<br>Historique, versions & dates d'effet | `CA-REF-006-01` | N | **PARTIEL** | `AuditTrailRestController` (`GET /api/audit/logs`) affiche `ancienneValeur` et `nouvelleValeur` uniquement pour le module Budget ; pas pour les référentiels (Tiers, Comptes, Unités, Banques). |
| | `CA-REF-006-02` | N | **ABSENT** | Aucune gestion de `dateEffet` future sur les taux, paramètres ou référentiels permettant d'appliquer automatiquement la nouvelle valeur à l'échéance. |
| | `CA-REF-006-03` | L | **ABSENT** | Aucune requête de type *time-travel* (`asOfDate`) permettant de consulter l'état d'une donnée de référence à une date passée. |
| **F-REF-007**<br>Import et export massifs | `CA-REF-007-01` | N | **ABSENT** | Aucun endpoint d'import Excel/CSV de référentiels en brouillon avec rapport d'import. |
| | `CA-REF-007-02` | E | **ABSENT** | Aucun moteur de validation ligne par ligne d'import Excel/CSV. |
| | `CA-REF-007-03` | N | **ABSENT** | Aucun endpoint d'export Excel filtré des fournisseurs ou autres référentiels. |

---

### 3.2. EP-REF-02 — Organisation et structures (`F-REF-008` à `F-REF-012`)

| Fonctionnalité | ID Critère | Type | Statut | Discordance constatée dans le code |
| :--- | :--- | :---: | :---: | :--- |
| **F-REF-008**<br>Organigramme & rattachements | `CA-REF-008-01` | N | **CONFORME** | `OrganigrammeAdapter.creerUnite()` soumet la création au Maker-Checker et rattache l'unité à son parent avec `materializedPath`. |
| | `CA-REF-008-02` | N | **NON CONFORME** | `UniteOrganisationnelleEntity` ne supporte qu'un seul `parent_id` hiérarchique ; **aucun rattachement fonctionnel multiple** avec justification n'est modélisé. |
| | `CA-REF-008-03` | E | **PARTIEL** | La création d'unité passe bien par validation Maker-Checker, mais `CreerUniteRequest` ne comporte pas de champ `referenceTexteOrganique` (ex: Résolution n° 11/CA/SODEPA/2018). |
| **F-REF-009**<br>Responsables, intérims & délégations | `CA-REF-009-01` | N | **PARTIEL** | L'affectation d'un collaborateur sur un poste avec `dateDebut` existe (`AffectationPosteEntity`), mais `UniteOrganisationnelleEntity` n'a pas de champ `responsable` explicite ni de bascule automatique des tâches de workflow à la `dateDebut`. |
| | `CA-REF-009-02` | N | **NON CONFORME** | Bien que `DelegationPouvoirEntity` et `TypeAffectation.INTERIM` existent, `WorkflowService.creerTachesPourEtape()` **n'interroge jamais `DelegationPouvoirRepository`** pour router les tâches vers l'intérimaire/délégataire pendant la période d'absence ! |
| | `CA-REF-009-03` | S | **NON CONFORME** | `WorkflowHistoriqueAuditEntity` et `MakerCheckerRequestEntity` n'enregistrent pas le délégant ni la mention « par délégation de ». |
| **F-REF-010**<br>Fiche unité opérationnelle | `CA-REF-010-01` | N | **NON CONFORME** | `UniteOrganisationnelleOutput` n'expose ni responsable, ni centres de coûts, ni magasins, ni caisses, ni comptes bancaires rattachés. |
| | `CA-REF-010-02` | E | **NON CONFORME** | `CreerUniteRequest` permet de créer une unité sans désigner de responsable (aucun champ `responsableId` obligatoire). |
| **F-REF-011**<br>Évolutions de structures | `CA-REF-011-01` | N | **ABSENT** | Pas d'endpoint de renommage d'unité avec `dateEffet` conservant l'ancien nom sur les transactions antérieures. |
| | `CA-REF-011-02` | N | **ABSENT** | Aucun service de préparation de fusion/scission d'unités ni de génération de rapport d'impact (utilisateurs, budgets, caisses, circuits). |
| | `CA-REF-011-03` | L | **ABSENT** | Pas de gestion de période de fermeture temporaire (`dateDebutFermeture` / `dateReouverture`) bloquant les opérations saisies dans l'intervalle. |
| **F-REF-012**<br>Référentiel géographique & sites | `CA-REF-012-01` | N | **ABSENT** | Aucune table de découpage administratif (Région, Département, Arrondissement, Localité) ni d'entité `SiteExploitationEntity` rattachée à une localité et une unité. |
| | `CA-REF-012-02` | N | **ABSENT** | Pas de référentiel des points de vente / sites locaux proposés par une unité et soumis à validation centrale. |

---

### 3.3. EP-REF-03 — Référentiels métiers et techniques (`F-REF-013` à `F-REF-016`)

| Fonctionnalité | ID Critère | Type | Statut | Discordance constatée dans le code |
| :--- | :--- | :---: | :---: | :--- |
| **F-REF-013**<br>Référentiel zootechnique | `CA-REF-013-01` | N | **NON CONFORME** | Les races sont figées dans un enum Java `RaceBovine` ; il n'existe ni table `EspeceEntity`, ni table `RaceEntity` paramétrable avec ses aptitudes. |
| | `CA-REF-013-02` | E | **NON CONFORME** | Aucun formulaire/endpoint de création de race rattachée à une espèce. |
| **F-REF-014**<br>Catalogue produits & sous-produits | `CA-REF-014-01` | N | **ABSENT** | Aucune entité ni table de catalogue des produits et sous-produits animaux (code, catégorie, unité, conservation, statut commercial). |
| | `CA-REF-014-02` | E | **ABSENT** | Pas de catalogue produits ni de détection de doublon insensible à la casse (« Carcasse bovine » vs « carcasse bovine »). |
| **F-REF-015**<br>Activités, sanitaire & abattage | `CA-REF-015-01` | N | **ABSENT** | Aucune table/API pour le référentiel sanitaire (maladies, protocoles de traitement, calendriers de vaccination). |
| | `CA-REF-015-02` | N | **ABSENT** | Aucune table/API pour le référentiel d'abattage et les normes de rendement carcasse. |
| **F-REF-016**<br>Unités de mesure & conversions | `CA-REF-016-01` | N | **ABSENT** | Aucune table `UniteMesureEntity` ni `RegleConversionUniteEntity` (ex: 1 carton = 20 kg). |
| | `CA-REF-016-02` | E | **ABSENT** | Aucun contrôle de cohérence dimensionnelle (poids vs volume vs effectif) entre unités de mesure. |

---

### 3.4. EP-REF-04 — Référentiels financiers, comptables et budgétaires (`F-REF-017` à `F-REF-020`)

| Fonctionnalité | ID Critère | Type | Statut | Discordance constatée dans le code |
| :--- | :--- | :---: | :---: | :--- |
| **F-REF-017**<br>Référentiel comptable SYSCOHADA | `CA-REF-017-01` | N | **CONFORME** | `CompteAdapter.initCreateCompte()` crée une subdivision avec `parentCode` et `niveau` soumise à validation Maker-Checker. |
| | `CA-REF-017-02` | E | **NON CONFORME** | `CompteAdapter.initCreateCompte()` ne vérifie **pas** que le code du compte respecte les classes SYSCOHADA (1 à 8, ou 9 uniquement si structure analytique validée) ni que le `parentCode` existe ! |
| | `CA-REF-017-03` | N | **PARTIEL** | Les journaux (`JournalEntity`) existent, mais il n'y a **aucune entité ni génération des 12 périodes mensuelles d'un exercice comptable** (ouvertes/clôturées). |
| **F-REF-018**<br>Référentiel budgétaire | `CA-REF-018-01` | N | **PARTIEL** | Les plans et lignes budgétaires (`BudgetPlanEntity`, `BudgetItemEntity`) existent, mais sans référentiel structuré des **Programmes**, **Activités** et **Sources de financement**. |
| | `CA-REF-018-02` | E | **PARTIEL** | `AjouterItemRequest` exige `compteId` et `centreCoutId`, mais n'a pas de champ explicite `natureDepense` validé contre un référentiel des natures de dépenses. |
| **F-REF-019**<br>Banques, caisses & moyens de paiement | `CA-REF-019-01` | N | **PARTIEL** | `TresorerieRegieEntity` gère les caisses/régies d'unités avec `plafondEncaisse`, mais il manque un endpoint REST de création/paramétrage de nouvelles caisses d'unité par le CSFR dans `RegiesTresorerieRestController`. |
| | `CA-REF-019-02` | N | **ABSENT** | Aucune table/API de référentiel des moyens de paiement et opérateurs Mobile Money (MTN MoMo, Orange Money). |
| **F-REF-020**<br>Référentiel fiscal & table des seuils | `CA-REF-020-01` | N | **ABSENT** | Le package `com.sodepa.erp.fiscalite` est vide ; aucune gestion de taux de TVA/impôts avec `dateEffet`. |
| | `CA-REF-020-02` | N | **PARTIEL** | `AchatSeuilConfigEntity` est partagée entre Budget et Achats, mais n'inclut pas les seuils de Finance/Caisse et n'a pas de `dateEffet`. |
| | `CA-REF-020-03` | L | **ABSENT** | Sans `dateEffet` ni historisation sur `achat_seuils_config`, la modification d'un seuil s'applique rétroactivement à toutes les dates au lieu de distinguer le `31/12/2026` du `01/01/2027`. |

---

### 3.5. EP-REF-05 — Tiers et partenaires (`F-REF-021` à `F-REF-023`)

| Fonctionnalité | ID Critère | Type | Statut | Discordance constatée dans le code |
| :--- | :--- | :---: | :---: | :--- |
| **F-REF-021**<br>Fiche fournisseur | `CA-REF-021-01` | N | **PARTIEL** | `AchatsService.creerFournisseur()` soumet bien NIU (`numeroContribuable`), RCCM et RIB à validation Maker-Checker, mais : (1) ne gère pas les pièces jointes obligatoires à la création, et (2) est isolé de la table `tiers` comptable (`RG-REF-016`). |
| | `CA-REF-021-02` | S | **CONFORME** | `AchatsService.validateOrRejectFournisseur()` bloque explicitement si `checkerOutput.maker().id().equals(checkerId)`. |
| **F-REF-022**<br>Fiche client & partenaires | `CA-REF-022-01` | N | **NON CONFORME** | `TiersEntity` n'a ni `segment` (grossiste, boucherie, détaillant...), ni `conditionsPaiement`, ni `plafondCredit`, ni `niu`. |
| | `CA-REF-022-02` | E | **NON CONFORME** | `TiersEntity` n'a pas de colonne `niu` ; dans `FournisseurEntity`, `numero_contribuable` n'a même pas de contrainte d'unicité ni de vérification de doublon dans `AchatsService.creerFournisseur()`. |
| **F-REF-023**<br>Contrôle qualité & conformité tiers | `CA-REF-023-01` | N | **ABSENT** | Aucune gestion des pièces obligatoires d'un tiers avec dates d'expiration et alertes à J-15. |
| | `CA-REF-023-02` | E | **ABSENT** | Aucun algorithme de validation de la clé RIB (modulo 97 CEMAC/Cameroun) lors de la saisie de `compteBancaireRib`. |
| | `CA-REF-023-03` | N | **PARTIEL** | `FournisseurEntity` possède `agreeSodepa` et `actif`, mais il n'existe **aucun endpoint REST** dans `AchatsRestController` pour suspendre un fournisseur non conforme ! |

---

### 3.6. EP-REF-06 — Identités, habilitations et sécurité (IAM) (`F-REF-024` à `F-REF-028`)

| Fonctionnalité | ID Critère | Type | Statut | Discordance constatée dans le code |
| :--- | :--- | :---: | :---: | :--- |
| **F-REF-024**<br>Comptes utilisateurs | `CA-REF-024-01` | N | **PARTIEL** | `EmployeEntity` a un lien optionnel `utilisateur_id`, mais `CreateUserRequest` et `UtilisateurEntity` ne contiennent ni `matricule`, ni `uniteId`, ni `responsableId`. |
| | `CA-REF-024-02` | N | **NON CONFORME** | Aucune tâche planifiée ni listener RH ne désactive automatiquement `UtilisateurEntity` (et Keycloak) lorsque la date de sortie / fin de contrat d'un agent est atteinte (`RG-REF-024`). |
| | `CA-REF-024-03` | L | **NON CONFORME** | `UtilisateurEntity` ne possède pas de champ `dateExpiration` pour les comptes temporaires / prestataires externes. |
| | `CA-REF-024-04` | S | **CONFORME** | La modification de `actif` via `UserRestController.initUpdate()` passe obligatoirement par Maker-Checker (`validate_or_reject`) et est journalisée. |
| **F-REF-025**<br>Profils, rôles & permissions | `CA-REF-025-01` | N | **NON CONFORME** | Le module Stocks n'existe pas et les permissions (`Permissions`) ne portent pas de dimension de périmètre par unité/magasin. |
| | `CA-REF-025-02` | L | **PARTIEL** | Les transitions conditionnelles par montant existent dans le workflow (`WorkflowConditionTransitionEntity`), mais les profils/permissions utilisateurs n'ont pas de plafond monétaire intrinsèque. |
| | `CA-REF-025-03` | N | **NON CONFORME** | Il n'existe **aucune entité `ProfilFonctionnel`** en base (`com.sodepa.erp.authorisation` est vide) : les permissions sont attachées directement à chaque utilisateur (`utilisateur_permissions`). On ne peut donc pas modifier un profil pour impacter automatiquement tous les agents qui le portent (`RG-REF-018`). |
| **F-REF-026**<br>Délégations temporaires | `CA-REF-026-01` | N | **PARTIEL** | `DelegationPouvoirEntity` enregistre `dateDebut` et `dateFin`, mais le moteur de validation (`WorkflowService.traiterTache()`) ne vérifie pas les délégations actives pour autoriser le délégataire à valider à la place du délégant. |
| | `CA-REF-026-02` | N | **NON CONFORME** | Aucun endpoint REST `PUT/POST /api/organigramme/delegations/{id}/revoquer` n'existe dans `OrganigrammeRestController` pour révoquer de manière anticipée une délégation active. |
| | `CA-REF-026-03` | S | **NON CONFORME** | Aucun champ `delegationId` ou `delegantId` dans les tables d'audit permettant de filtrer les opérations réalisées par délégation. |
| **F-REF-027**<br>Séparation des tâches (SoD) | `CA-REF-027-01` | S | **NON CONFORME** | `UpdateUserPermissionsUseCase` et `UserAdapter` n'effectuent **aucun contrôle de matrice SoD** lors de l'attribution de permissions incompatibles (`RG-REF-021`). |
| | `CA-REF-027-02` | S | **CONFORME** | Dans `AchatsService.validateOrRejectExpressionBesoin()`, le créateur (`maker`) est bloqué s'il tente d'approuver sa propre demande d'achat (`RG-REF-019`). |
| | `CA-REF-027-03` | S | **ABSENT** | Aucun module de gestion des stocks / inventaires physiques n'est implémenté (`RG-REF-022`). |
| | `CA-REF-027-04` | S | **CONFORME** | `EcritureEntity` verrouille les écritures validées/clôturées et n'autorise aucune modification directe (`RG-REF-023`). |
| | `CA-REF-027-05` | S | **CONFORME** | `AchatsService.validateOrRejectFournisseur()` interdit à l'agent créateur de valider le fournisseur (`RG-REF-020`). |
| **F-REF-028**<br>Authentification & sécurité | `CA-REF-028-01` | S | **PARTIEL** | Délégué à Keycloak, mais aucun contrôle applicatif dans le filtre JWT (`JwtAuthenticationFilter`) ne vérifie le claim `acr`/`amr` (MFA) pour les opérations sensibles de paiement. |
| | `CA-REF-028-02` | L | **PARTIEL** | Le rate-limiting Redis (`EndpointThrottleService`) bloque temporairement l'IP/utilisateur, mais ne verrouille pas le statut du compte applicatif après 5 échecs de login et n'alerte pas l'administrateur. |
| | `CA-REF-028-03` | S | **ABSENT** | Aucune gestion d'appareils autorisés (empreinte d'appareil / device trust) ni blocage/alerte sur appareil inconnu. |

---

### 3.7. EP-REF-07 — Paramètres, workflows et GED (`F-REF-029` à `F-REF-031`)

| Fonctionnalité | ID Critère | Type | Statut | Discordance constatée dans le code |
| :--- | :--- | :---: | :---: | :--- |
| **F-REF-029**<br>Paramètres & modèles de documents | `CA-REF-029-01` | N | **ABSENT** | Aucune entité/table de modèles de documents (`ModeleDocumentEntity`) avec logo et variables dynamiques. |
| | `CA-REF-029-02` | N | **ABSENT** | Aucun moteur de numérotation paramétrable par masque (`BE-{UO}-{AAAA}-{00000}`) avec compteur par unité et par exercice. |
| **F-REF-030**<br>Moteur de workflows | `CA-REF-030-01` | N | **CONFORME** | Supporté par `WorkflowConditionTransitionEntity` et les seuils de routage. |
| | `CA-REF-030-02` | E | **NON CONFORME** | `WorkflowService.traiterTache()` n'exige **pas** que `request.getCommentaire()` soit renseigné lorsque `request.getDecision() == DecisionWorkflow.REJETE`. |
| | `CA-REF-030-03` | L | **PARTIEL** | `dateEcheance` est calculée sur `WorkflowTacheEntity`, mais **aucun scheduler (`@Scheduled`)** n'envoie de relance automatique ni ne marque la tâche en dépassement de SLA au bout du délai. |
| | `CA-REF-030-04` | S | **PARTIEL** | `WorkflowService.modifierDefinition()` modifie les étapes de la définition existante en place (`WorkflowDefinitionEntity` n'est pas versionnée), ce qui affecte les instances en cours rattachées à cette `definition_id` au lieu d'isoler l'ancienne version ! |
| **F-REF-031**<br>GED & Signature électronique | `CA-REF-031-01` | N | **ABSENT** | Pas d'intégration OCR ni de recherche plein texte dans les documents GED. |
| | `CA-REF-031-02` | N | **PARTIEL** | `DocumentGedEntity` possède `typeEntite` et `entiteId`, mais **aucun `GedRestController`** n'existe pour consulter ou télécharger les pièces liées à un BE ou une transaction. |
| | `CA-REF-031-03` | S | **PARTIEL** | Le hash `hashSha256` est stocké dans `DocumentGedEntity`, mais il n'y a ni signature électronique, ni endpoint de vérification d'intégrité détectant une altération du fichier dans MinIO. |
| | `CA-REF-031-04` | L | **ABSENT** | `DocumentGedEntity` n'a pas de champ `dureeConservationAns` / `dateFinConservation` bloquant toute suppression avant échéance (`RG-REF-012`). |

---

### 3.8. EP-REF-08 — Interopérabilité et gouvernance (`F-REF-032` à `F-REF-034`)

| Fonctionnalité | ID Critère | Type | Statut | Discordance constatée dans le code |
| :--- | :--- | :---: | :---: | :--- |
| **F-REF-032**<br>API & synchronisation | `CA-REF-032-01` | N | **NON CONFORME** | La modification d'un fournisseur dans `achats` (`FournisseurEntity`) ne notifie ni ne met à jour `TiersEntity` dans `comptabilite`. |
| | `CA-REF-032-02` | E | **ABSENT** | Aucun mécanisme de détection/résolution de conflits de synchronisation inter-unités (pas même de `@Version` JPA d'optimistic locking sur les entités de référentiel). |
| | `CA-REF-032-03` | S | **CONFORME** | `JwtAuthenticationFilter` et `TrackingUserActionsFilter` rejettent (HTTP 401) et journalisent les appels non authentifiés. |
| **F-REF-033**<br>Mode dégradé & réversibilité | `CA-REF-033-01` | N | **ABSENT** | Pas de cache/synchronisation locale pour consultation en mode dégradé (hors ligne) dans les unités. |
| | `CA-REF-033-02` | N | **ABSENT** | Aucun endpoint d'export intégral des référentiels et de leurs historiques (`RG-REF-015`). |
| **F-REF-034**<br>Tableau de bord gouvernance | `CA-REF-034-01` | N | **ABSENT** | Le package `com.sodepa.erp.dashboard` est vide : aucun calcul ni endpoint pour les indicateurs (complétude, doublons, validations en attente, conformité, synchronisation, délai moyen de validation). |
| | `CA-REF-034-02` | N | **ABSENT** | Aucun tableau de bord filtrable par structure propriétaire métier. |

---

## 4. Couverture des 28 Règles de Gestion (`RG-REF-001` à `RG-REF-028`)

| Règle | Libellé résumé | État dans le code | Détail technique |
| :--- | :--- | :---: | :--- |
| **RG-REF-001** | Identifiant unique par donnée | **PARTIEL** | UUID + `code` unique par table, mais tables dupliquées (`tiers` vs `achat_fournisseurs`, `unites_organisationnelles` vs `pastoral_ranches`). |
| **RG-REF-002** | Détection des doublons avant création | **NON CONFORME** | Pas de détection de doublons sur libellé/raison sociale/NIU ni validation de clé RIB. |
| **RG-REF-003** | Circuit de validation sur création/modification | **CONFORME** | Couvert par `MakerCheckerEngineAdapter` et `WorkflowService`. |
| **RG-REF-004** | Historisation de toute modification | **PARTIEL** | Historisé dans ClickHouse/MakerChecker, mais sans snapshot systématique `valeurAvant` / `valeurApres` consultable par fiche. |
| **RG-REF-005** | Traçabilité auteur, date, nature et délégation | **PARTIEL** | Auteur, date et nature tracés ; mention « par délégation de » absente. |
| **RG-REF-006** | Gestion des versions de nomenclature | **NON CONFORME** | Pas de versioning des fiches de référentiel ni des définitions de workflow. |
| **RG-REF-007** | Dates d'effet futures et historisées | **NON CONFORME** | Absentes sur les paramètres, seuils, taux fiscaux et renommages d'unités. |
| **RG-REF-008** | Seules les données actives proposées en saisie | **PARTIEL** | Appliqué sur certains endpoints (`/active_by_id`, `/list`), mais pas généralisé sur toutes les listes. |
| **RG-REF-009** | Interdiction de suppression physique si utilisée | **PARTIEL** | Pas de contrôle d'intégrité métier explicite retournant les transactions liées avant suppression/désactivation. |
| **RG-REF-010** | Cohérence intermodules et rapport d'impact | **NON CONFORME** | Pas de rapport d'impact lors des fusions/désactivations ni de synchronisation `Fournisseur` ↔ `Tiers`. |
| **RG-REF-011** | Contrôle des accès (périmètre, expiration, MFA) | **PARTIEL** | Permissions vérifiées, mais absence de `dateExpiration` sur `UtilisateurEntity` et de filtrage généralisé par unité. |
| **RG-REF-012** | Archivage et durée de conservation GED | **NON CONFORME** | Pas de statut `ARCHIVE` unifié ni de blocage de suppression avant fin de durée de conservation dans `DocumentGedEntity`. |
| **RG-REF-013** | Synchronisation inter-unités et mode dégradé | **NON CONFORME** | Non implémenté. |
| **RG-REF-014** | Propriétaire métier identifié sur chaque référentiel | **NON CONFORME** | Aucun champ `proprietaireMetier` sur les référentiels ni responsable obligatoire à la création d'unité. |
| **RG-REF-015** | Réversibilité (export intégral) | **NON CONFORME** | Non implémenté. |
| **RG-REF-016** | Donnée créée une seule fois et réutilisée partout | **NON CONFORME** | Violée par la séparation `tiers` / `achat_fournisseurs` et `unites_organisationnelles` / `pastoral_ranches`. |
| **RG-REF-017** | Conformité à la Résolution n° 11/CA/SODEPA/2018 | **PARTIEL** | Seeder conforme, mais absence de référence au texte organique lors de la création dynamique d'une structure. |
| **RG-REF-018** | Profils fonctionnels distincts des postes organiques | **NON CONFORME** | Aucune entité `ProfilFonctionnel` paramétrable en base (`authorisation` est vide). |
| **RG-REF-019** | Un demandeur ne peut pas approuver sa propre demande | **PARTIEL** | Bloqué dans `MakerCheckerEngineAdapter`, mais contournable dans `WorkflowService` via le fallback `assigneUser = instance.getInitiateur()` ou le bypass `isSuperviseurOuAdmin`. |
| **RG-REF-020** | Fournisseur créé et validé par deux agents distincts | **CONFORME** | Bloqué dans `AchatsService.validateOrRejectFournisseur()`. |
| **RG-REF-021** | Agent de paiement ne peut valider seul (SoD profils) | **NON CONFORME** | Aucune matrice SoD bloquant l'attribution cumulée des droits de préparation et de validation de paiement (`CA-REF-027-01`). |
| **RG-REF-022** | Gestionnaire de stock ne valide pas seul les écarts | **NON CONFORME** | Module Stocks / Inventaires non implémenté. |
| **RG-REF-023** | Interdiction de modifier une écriture clôturée | **CONFORME** | Respecté dans `comptabilite.generale`. |
| **RG-REF-024** | Désactivation automatique du compte à la sortie RH | **NON CONFORME** | Aucun automatisme entre la date de sortie RH et `UtilisateurEntity.actif`. |
| **RG-REF-025** | Statuts : Brouillon, Actif, Suspendu, Archivé | **NON CONFORME** | Simple booléen `actif` sur la majorité des tables. |
| **RG-REF-026** | Conformité SYSCOHADA des comptes créés | **PARTIEL** | Plan comptable SYSCOHADA initialisé, mais `initCreateCompte` ne bloque pas un numéro de compte hors classe valide (`CA-REF-017-02`). |
| **RG-REF-027** | Séparation Cellule Informatique / Propriétaire métier | **NON CONFORME** | Pas de distinction entre permissions d'administration technique et responsabilité de validation métier par famille. |
| **RG-REF-028** | Proposition locale d'unité soumise à validation centrale | **PARTIEL** | Le workflow le permet pour les opérations métier, mais pas pour les référentiels locaux (sites, points de vente). |

---

## 5. Corrections Rapides ("Quick Wins") vs Chantiers Structurants

### A. Quick Wins (Corrections immédiates sur l'existant — 12 CA gagnés rapidement)
1. **`CT-REF-05` & `CA-REF-030-02` (Motif de rejet obligatoire)** :
   - Dans `WorkflowService.traiterTache()` et `MakerCheckerEngineAdapter.validateOrReject()`, refuser tout rejet (`REJETE` / `REJECTED`) si le commentaire/motif est vide ou blanc.
2. **`CA-REF-002-07` & `RG-REF-019` (Auto-approbation interdite dans WorkflowService)** :
   - Dans `WorkflowService.traiterTache()` et `creerTachesPourEtape()`, interdire formellement que `acteurId.equals(instance.getInitiateur().getId())` lors d'une approbation et supprimer le fallback qui assigne la tâche à l'initiateur.
3. **`CA-REF-017-02` & `RG-REF-026` (Validation SYSCOHADA des comptes)** :
   - Dans `CompteAdapter.initCreateCompte()`, vérifier que le code commence par une classe SYSCOHADA valide (`1` à `8`, ou `9` uniquement si structure analytique autorisée) et que `parentCode` existe.
4. **`CA-REF-023-02` & `CA-REF-023-03` (Validation clé RIB + Endpoint de suspension fournisseur)** :
   - Ajouter la vérification de format/clé RIB et un endpoint `PATCH /api/achats/fournisseurs/{id}/suspendre`.
5. **`CA-REF-009-02`, `CA-REF-009-03`, `CA-REF-026-01` à `03` (Prise en compte effective des délégations et révocation)** :
   - Ajouter l'endpoint de révocation dans `OrganigrammeRestController`, vérifier `DelegationPouvoirRepository` dans `WorkflowService.traiterTache()` et tracer `"par délégation de <délégant>"` dans l'historique d'audit.

### B. Chantiers Structurants (Nouveaux sous-modules à développer)
1. **Unification `TiersEntity` + `FournisseurEntity` + Fiche Client & Pièces obligatoires (`F-REF-021` à `023`)**
2. **Module `authorisation` : Profils fonctionnels, Permissions multidimensionnelles, Matrice SoD & Désactivation RH (`F-REF-024` à `027`)**
3. **Module `ged` complet : `GedService`, `GedRestController`, durée de conservation, vérification SHA-256/signature (`F-REF-031`)**
4. **Module `fiscalite` + Table unique des seuils avec `dateEffet` (`F-REF-020`)**
5. **Référentiels métiers & techniques `REF-03` (`F-REF-013` à `016`) + Sites & Géographie (`F-REF-012`)**
6. **Socle MDM (`F-REF-001` à `007`) + Tableau de bord de gouvernance (`F-REF-034`)**
