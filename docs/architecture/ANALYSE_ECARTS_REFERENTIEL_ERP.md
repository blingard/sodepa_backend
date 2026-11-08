# Analyse des Écarts (Fit-Gap) — Module 01 : Référentiel de l'ERP (`REF`)

> [!IMPORTANT]
> Ce document confronte point par point le **Cahier des Charges Fonctionnel — Module 01 (Référentiel de l'ERP, v1.0)** avec le code source actuellement implémenté dans `c:\Users\GLC\Documents\sodepa\erp`.
> Il distingue ce qui est **Conforme (Implémenté)**, **Partiel (En écart de modèle ou de règle)** et **Manquant (Non implémenté)**.

---

## 1. Synthèse Globale de Couverture par Sous-Domaine

| Sous-domaine | Périmètre Cahier des Charges | État dans le Code | Niveau de couverture | Principaux écarts constatés |
| :--- | :--- | :---: | :---: | :--- |
| **REF-01** | Référentiels maîtres (MDM générique, familles, déduplication, import/export, versioning) | **Manquant / Éclaté** | ~20 % | Pas de moteur MDM générique ni d'attributs de gouvernance unifiés (`propriétaire métier`, `version`, `dateEffet`/`dateFin`, fusion de doublons, import/export). |
| **REF-02** | Organisation, structures, postes, affectations, délégations, versions | **Avancé** | ~75 % | Liens fonctionnels multiples absents (`parent_id` unique), pas d'analyse d'impact avant réorganisation, référentiel géographique/sites découplé, cycle de vie `StatutUnite` incomplet. |
| **REF-03** | Référentiels métiers & techniques (élevage, sanitaire, abattage, produits, unités de mesure) | **Très partiel** | ~20 % | Seuls les bovins/catégories cheptel (`pastoral`) existent. Absence des catalogues produits/sous-produits, sanitaire/vétérinaire, abattage/transformation et unités de mesure/conversions. |
| **REF-04** | Référentiels financiers, comptables (SYSCOHADA), budgétaires, banques/caisses, fiscal, seuils | **Avancé mais fragmenté** | ~70 % | Module `fiscalite` vide (0 classe Java), seuils non unifiés (`achat_seuils_config` vs plafonds en dur), absence d'opérateurs Mobile Money structurés. |
| **REF-05** | Tiers et partenaires (Fournisseurs, Clients, Partenaires, conformité, déduplication) | **Partiel & Dupliqué** | ~45 % | **Double table contradictoire** (`tiers` en comptabilité vs `achat_fournisseurs` en achats), pas de fiche Client commerciale complète, pas de suivi des pièces obligatoires avec dates d'expiration, pas d'auto-création du compte auxiliaire à la validation. |
| **REF-06** | Utilisateurs, rôles, habilitations multidimensionnelles, délégations, SoD | **Partiel** | ~55 % | Permissions à plat (`enum Permissions` sur `UtilisateurEntity`), pas d'entité `ProfilFonctionnel` paramétrable, pas de matrice SoD dynamique, pas de rattachement direct `matricule`/`structure`/`responsable` dans `UtilisateurEntity`, pas de désactivation auto sur événement RH de sortie. |
| **REF-07** | Paramètres système, Moteur de Workflows, GED & Signature électronique | **Workflow avancé / GED inachevée** | ~60 % | Workflow très complet (`workflow`), mais **GED incomplète** (seuls `DocumentGedEntity` et `DocumentGedRepository` existent, **aucun service ni contrôleur REST**, pas d'OCR, versioning ou signature électronique) ; paramètres système sans `dateEffet` ni historisation. |
| **§8 & §13** | Synchronisation inter-unités, mode dégradé, Tableau de bord de gouvernance MDM | **Manquant** | ~10 % | Module `dashboard` vide (aucune classe Java), pas d'indicateurs de complétude/doublons/SLA MDM, pas de synchronisation mode dégradé multi-sites. |

---

## 2. Points de Discordance Détaillés par Sous-Domaine

### 2.1. REF-01 — Référentiels Maîtres (MDM Générique) — `BF-REF-001` à `BF-REF-010`

> [!WARNING]
> Le Cahier des Charges définit `REF-01` comme un socle MDM transversal doté d'attributs de gouvernance communs à toutes les données de référence (§6.1 `BF-REF-010` et §8.1). Dans le code actuel, chaque module gère ses propres tables sans socle MDM commun.

1. **Absence d'entité ou de contrat de gouvernance MDM (`BF-REF-001`, `BF-REF-010`, `RG-REF-014`)** :
   - **Attendu** : Chaque donnée de référence doit porter : `code`, `libelle`, `description`, `categorie`, `parent`, `proprietaireMetier` (ex. SDFBM, DPC, DG/DAF), `gestionnaire`, `statut` (`BROUILLON`, `SOUMIS`, `ACTIF`, `SUSPENDU`, `ARCHIVE`), `dateCreation`, `dateEffet`, `dateFin`, `version`, `historique`.
   - **Implémenté** : Les entités de référence (`BanqueEntity`, `CompteEntity`, `JournalEntity`, `TiersEntity`, `FournisseurEntity`, `PastoralCategorieCheptelEntity`) n'ont qu'un simple booléen `actif = true/false` (voire aucun statut sur `PastoralCategorieCheptelEntity`), sans `proprietaireMetier`, sans `version`, ni `dateEffet`/`dateFin`.
2. **Détection et fusion de doublons (`BF-REF-006`, `RG-REF-002`)** :
   - **Attendu** : Détection proactive des doublons avant création (similarité sur raison sociale, NIU, RCCM, RIB, libellé) et outil de **fusion contrôlée des doublons**.
   - **Implémenté** : Seule l'unicité stricte SQL (`unique = true` sur `code` ou `username`/`email`) existe. Aucune détection phonétique/floue ni fonctionnalité de fusion de doublons n'est implémentée.
3. **Règles de codification paramétrables (`BF-REF-005`)** :
   - **Attendu** : Codification automatique ou manuelle selon des masques/règles définis par famille dans `REF-07`.
   - **Implémenté** : Les codes sont saisis manuellement par l'utilisateur dans les requêtes (`CreateTiersRequest.code()`, `CreerFournisseurRequest.code()`, `CreerUniteRequest.code()`) sans générateur de séquences/masques paramétrable.
4. **Import massif et export de réversibilité (`BF-REF-007`, `BF-REF-059`, `RG-REF-015`)** :
   - **Attendu** : Import en masse (CSV/Excel) avec contrôle de cohérence et rapport d'erreurs ; export intégral des référentiels et de leurs historiques.
   - **Implémenté** : Aucun endpoint d'import/export de référentiels n'existe.
5. **Contrôle d'utilisation avant désactivation/suppression (`Processus P3`, `BF-REF-002`, `RG-REF-009`)** :
   - **Attendu** : Rapport d'utilisation vérifiant les transactions en cours avant désactivation ; suppression physique autorisée uniquement si la donnée n'a jamais été utilisée.
   - **Implémenté** : Pas de service de vérification des dépendances inter-modules ni de rapport d'impact avant désactivation.

---

### 2.2. REF-02 — Organisation (`organigramme`) — `BF-REF-011` à `BF-REF-018`

Le package `organigramme` est riche (arborescence par `materializedPath`, postes budgétés, affectations, délégations, versions d'organigramme, objectifs d'unités), mais présente **6 discordances précises** avec le cahier des charges :

1. **Rattachements hiérarchiques vs fonctionnels multiples (`BF-REF-012`)** :
   - **Attendu** : Gérer à la fois le lien hiérarchique et le lien fonctionnel, y compris les rattachements multiples justifiés.
   - **Implémenté** : `UniteOrganisationnelleEntity` ne possède qu'un seul `parent` (`parent_id`). Il n'y a ni `parentFonctionnel`, ni table de rattachements secondaires.
2. **Cycle de vie et statuts d'une structure (`§9 Cycles de vie`, `BF-REF-016`)** :
   - **Attendu** : `Projetée → Active → Fermée temporairement → Rouverte → Supprimée (historisée)`, ainsi que les opérations de **fusion** et **scission** créant de nouvelles versions.
   - **Implémenté** : `StatutUnite` ne contient que `ACTIF`, `EN_RESTRUCTURATION`, `INACTIF`. Les opérations métier de fusion/scission d'unités et de fermeture temporaire/réouverture ne sont pas implémentées.
3. **Attributs de rattachement opérationnel d'une unité (`BF-REF-015`)** :
   - **Attendu** : Enregistrer pour chaque unité : activités, centres de coûts et de profits, magasins, caisses, comptes bancaires, équipements, budget, habilitations.
   - **Implémenté** : `UniteOrganisationnelleEntity` ne contient que `code`, `intitule`, `typeUnite`, `typeStructure`, `parent`, `localisation` (simple `String`), `statut`, `dateCreation`, `dateFermeture`. Par ailleurs, les ranchs sont dupliqués dans une table séparée `PastoralRanchEntity` (`pastoral_ranches`) sans clé étrangère vers `unites_organisationnelles`, et les régies (`TresorerieRegieEntity`) utilisent un `codeUnite` libre au lieu d'une relation vers `UniteOrganisationnelleEntity`.
4. **Référentiel géographique et des sites (`BF-REF-018`)** :
   - **Attendu** : Découpage administratif structuré (Régions → Départements → Arrondissements → Localités) et référentiel typé des sites (ranchs, secteurs, campements, abattoirs, entrepôts, boucheries, périmètres, magasins, parcs, points de vente/encaissement) avec coordonnées géographiques.
   - **Implémenté** : `localisation` est un simple champ texte (`VARCHAR(100)`) sur `UniteOrganisationnelleEntity` et `region` un `VARCHAR(100)` sur `PastoralRanchEntity`. Aucun référentiel géographique ni référentiel des sites/points de vente n'existe.
5. **Rapport d'impact lors d'une réorganisation (`Processus P4 étape 3`)** :
   - **Attendu** : Contrôle automatique des impacts sur les utilisateurs, centres de coûts, budgets, caisses et workflows avant validation d'une réorganisation.
   - **Implémenté** : `OrganigrammeVersionEntity` est une simple étiquette de version (`numeroVersion`, `libelle`, `dateEffet`, `statut`) qui n'est **pas reliée** aux unités (`unites_organisationnelles` n'a pas de `version_id`) et ne génère aucun rapport d'impact.
6. **Types d'unités de gouvernance (`BF-REF-011`, `RG-REF-017`)** :
   - **Attendu** : Modéliser les organes de gouvernance de la Résolution n° 11/CA/SODEPA/2018 (AG, CA, DG, DGA, Comité d'Audit, CIPM, Cellule de lutte contre la corruption).
   - **Implémenté** : `TypeUniteOrganisationnelle` ne prévoit pas `ASSEMBLEE_GENERALE`, `CONSEIL_ADMINISTRATION`, `COMITE_AUDIT`, `COMMISSION_MARCHES` (`CIPM`), ni `BOUCHERIE` / `POINT_VENTE`.

---

### 2.3. REF-03 — Référentiels Métiers et Techniques — `BF-REF-019` à `BF-REF-024`

> [!CAUTION]
> C'est le sous-domaine présentant le **plus grand déficit fonctionnel** (~80 % manquant).

1. **Zootechnie & Élevage (`BF-REF-019`, `BF-REF-021`)** :
   - **Implémenté** : `PastoralCategorieCheptelEntity` et l'énumération figée `RaceBovine` (`GOUDALI`, `MBORORO`, `WHITE_FULANI`, `BRAHMAN`, `HOLSTEIN`, `CHAROLAISE`, `CROISE`).
   - **Manquant** : Multi-espèces paramétrables (ovins, caprins, porcins, volailles...), lignées, aptitudes, caractéristiques génétiques, classification des reproducteurs, critères de sélection, nomenclatures de croisement, et référentiel des activités d'élevage (embouche, IA, transfert d'embryons, fourrage, pâturages, hydraulique pastorale).
2. **Catalogue des produits et sous-produits animaux (`BF-REF-020`)** :
   - **Manquant** : Aucune table ni entité de catalogue produits/articles/sous-produits animaux (code, catégorie, unité, conservation, statut commercial, tarifs).
3. **Référentiel sanitaire et vétérinaire (`BF-REF-022`)** :
   - **Manquant** : Aucune table pour les maladies, pathologies, protocoles, actes vétérinaires, vaccins, médicaments, quarantaines, motifs de mortalité, calendriers de prophylaxie (seul un enum `EtatSanteBovin` à 4 valeurs existe).
4. **Référentiel d'abattage et de transformation (`BF-REF-023`)** :
   - **Manquant** : Aucune entité pour les types d'abattage, chaînes, postes, opérations, découpe, conditionnement, normes de rendement, pertes, classification des carcasses.
5. **Unités de mesure et règles de conversion (`BF-REF-024`, `§10`)** :
   - **Manquant** : Aucune table de référence des unités de mesure (`kg, t, g, l, unité, tête, sac, carton, caisse, ha, m², m³, h, jour, kWh`) ni table des facteurs de conversion contrôlés.

---

### 2.4. REF-04 — Référentiels Financiers, Comptables et Budgétaires — `BF-REF-025` à `BF-REF-029`

1. **Référentiel Fiscal (`BF-REF-028`) — TOTALEMENT ABSENT** :
   - **Attendu** : Impôts, taxes, taux (TVA, AIR/retenues à la source, centimes additionnels, taxes communales/vétérinaires), régimes fiscaux, exonérations, codes de taxation, périodes déclaratives, règles de calcul et dates d'effet.
   - **Implémenté** : Le package `com.sodepa.erp.fiscalite` contient uniquement des dossiers vides (**0 fichier Java**). Quelques taux isolés sont stockés en clé-valeur dans `app_parametres_systeme`.
2. **Plan comptable SYSCOHADA & Schémas d'écritures (`BF-REF-025`)** :
   - **Implémenté** : `CompteEntity`, `JournalEntity`, `AxeAnalytiqueEntity`, `SectionAnalytiqueEntity`, `CleRepartitionEntity`.
   - **Manquant** : Référentiel des **schémas d'écritures**, **modèles d'écritures automatiques** et **règles d'imputation** paramétrables, ainsi qu'une entité dédiée aux **Exercices et Périodes comptables** (ouverts/clôturés).
3. **Référentiel Budgétaire (`BF-REF-026`)** :
   - **Attendu** : Nomenclature budgétaire structurée (programmes, activités, projets, chapitres, articles, sources de financement, natures de dépenses, catégories de recettes).
   - **Implémenté** : `BudgetItemEntity` lie directement une enveloppe à un compte comptable et un centre de coût, sans entités de référentiel pour les **Programmes**, **Projets** (propriété DPD/CSPP selon §4), **Sources de financement** ou **Chapitres/Articles budgétaires**. (De plus, il existe deux modèles de budget parallèles : `budget/BudgetPlanEntity` et `comptabilite/analytique/BudgetEntity`).
4. **Banques, Caisses et Mobile Money (`BF-REF-027`)** :
   - **Implémenté** : `BanqueEntity` et `TresorerieRegieEntity`.
   - **Manquant** : Distinction entre **Banque**, **Agence** et **Compte bancaire** (avec devise, titulaire, RIB complet), référentiel des **moyens de paiement**, des **opérateurs Mobile Money**, des plafonds d'encaissement/décaissement par caisse et des règles de rapprochement paramétrables.
5. **Table unique des seuils de gestion (`BF-REF-029`, `PO-REF-02`, `§10`)** :
   - **Attendu** : Une table unique des seuils de gestion (dépenses, achats, caisse, réallocation) avec **date d'effet et historisation** (`RG-REF-007`).
   - **Implémenté** : `AchatSeuilConfigEntity` ne couvre que les procédures d'achat (`BON_COMMANDE`, `LETTRE_COMMANDE`, `MARCHE_CIPM`), sans `dateEffet` ni historisation. Les plafonds de régie sont en dur dans `PastoralRanchEntity` (`10_000_000`) ou dispersés dans `app_parametres_systeme`.

---

### 2.5. REF-05 — Tiers et Partenaires — `BF-REF-030` à `BF-REF-035` & `Processus P5`

> [!WARNING]
> **Rupture d'unicité du référentiel Tiers (`RG-REF-001`, `RG-REF-016`)** : Le code comporte actuellement **deux tables de tiers concurrentes et non synchronisées**.

1. **Duplication architecturale entre `tiers` et `achat_fournisseurs`** :
   - D'un côté, `TiersEntity` (`table = "tiers"`) dans `comptabilite.generale` ne stocke que `code`, `raisonSociale`, `adresse`, `telephone`, `email`, `typeTiers`, `actif`, `compteCollectifCode` (sans NIU, ni RCCM, ni RIB).
   - De l'autre, `FournisseurEntity` (`table = "achat_fournisseurs"`) dans `achats` stocke `code`, `raisonSociale`, `numeroContribuable`, `rccm`, `regimeFiscal`, `specialite`, `banqueNom`, `compteBancaireRib`, `agreeSodepa`, `dateAgrement`.
   - **Conséquence** : Contrairement au **Processus P5 (étape 4)** et à **`RG-REF-016`**, créer et valider un fournisseur dans `achats` ne crée pas de `TiersEntity` ni de compte auxiliaire dans le module Comptabilité (`05`), et inversement.
2. **Incomplétude du référentiel Clients (`BF-REF-031`) et Autres Partenaires (`BF-REF-032`)** :
   - Aucune gestion des attributs clients : segment (boucheries, grossistes, détaillants, collectivités, institutionnels), conditions commerciales et de paiement, **plafond de crédit**, justificatifs.
   - `TypeTiers` se limite à `CLIENT, FOURNISSEUR, PERSONNEL, ORGANISME` sans couvrir la typologie détaillée de `BF-REF-032` (consultants, bailleurs/organismes de financement, éleveurs, bouchers, détaillants).
3. **Gestion documentaire et conformité des tiers (`BF-REF-033`, `BF-REF-034`, `BF-REF-035`)** :
   - **Manquant** : Liste des pièces administratives obligatoires par type de tiers (attestation de non-redevance, CNPS, RCCM, NIU, RIB) avec **dates de validité et alertes d'expiration**.
   - **Manquant** : Évaluations fournisseurs, incidents, sanctions, relations maison-mère / filiales (groupe), et cycle de vie complet (`BROUILLON → SOUMIS → VALIDE → SUSPENDU → ARCHIVE`, §9).

---

### 2.6. REF-06 — Utilisateurs, Rôles et Habilitations (IAM) — `BF-REF-036` à `BF-REF-045` & `Processus P6`

1. **Modèle Utilisateur incomplet (`BF-REF-036`, `BF-REF-038`)** :
   - **Attendu** : Chaque compte (`UtilisateurEntity`) doit être rattaché à un **matricule**, une **structure**, une **unité**, un **responsable hiérarchique**, et supporter une **date d'expiration** (comptes temporaires / prestataires externes, `BF-REF-038`) ainsi qu'un statut riche (`CREE → ACTIF → SUSPENDU → VERROUILLE → DESACTIVE`, §9).
   - **Implémenté** : `UtilisateurEntity` ne contient que `id`, `username`, `nom`, `prenom`, `email`, `iam`, `photoProfile`, `actif` (`boolean`), `telephones`, `permissions`. Il n'a ni `matricule`, ni `uniteId`, ni `dateExpiration`, ni statut `SUSPENDU`/`VERROUILLE`.
2. **Absence de Profils Fonctionnels paramétrables (`BF-REF-039`, `RG-REF-018`, `§10`)** :
   - **Attendu** : Définir des profils fonctionnels paramétrables en base (les 13 profils de base du §10 : Administrateur technique, Administrateur fonctionnel, Propriétaire métier, Gestionnaire de référentiel, Comptable, Auditeur, etc.), distincts des postes organiques, regroupant des permissions.
   - **Implémenté** : Les permissions sont attribuées **individuellement et directement** à chaque utilisateur via la table `utilisateur_permissions` et l'enum codé en dur `Permissions`. Le package `com.sodepa.erp.authorisation` prévu à cet effet est vide.
3. **Permissions multidimensionnelles (`BF-REF-040`, `BF-REF-041`)** :
   - **Attendu** : Permissions paramétrées par **module, fonctionnalité, données, structure, unité, exercice, opération, montant/seuil, document et fonction de validation**, incluant le droit `EXPORTER`.
   - **Implémenté** : L'enum `Permissions` est binaire (possédé ou non à l'échelle globale de l'ERP), sans périmètre d'unité organisationnelle, d'exercice comptable ou de seuil monétaire, et sans droits d'export.
4. **Matrice de Séparation des Tâches — SoD (`BF-REF-043`, `RG-REF-019` à `023`, `PO-REF-04`)** :
   - **Attendu** : Matrice SoD paramétrable (couples de fonctions incompatibles, niveau de blocage, contrôle compensatoire) contrôlée **à l'attribution des droits** (Processus P6 étape 4 : rapport SoD) ET **à l'exécution**.
   - **Implémenté** : Contrôle uniquement à l'exécution sur le cas Maker != Checker (`findAllToValideByOther` et `WorkflowService`). **Aucune table ni contrôle de matrice SoD** n'empêche d'attribuer simultanément à un même utilisateur `CREATE_PROVIDER` et `VALIDATE_PROVIDER`, ou `INIT_CREATE_TIERS` et `VALIDATE_OR_REJECT_TIERS` dans `UpdateUserPermissionsUseCase`.
5. **Désactivation automatique à la sortie du personnel (`BF-REF-037`, `RG-REF-024`, `Processus P6`)** :
   - **Attendu** : Lorsqu'un agent quitte la SODEPA (fin/résiliation de contrat RH, retraite, licenciement), son compte utilisateur IAM est désactivé automatiquement.
   - **Implémenté** : Aucun listener ou événement entre le module `rh` (`EmployeEntity` / `ContratTravailEntity`) et `user`/`KeycloakProvisioningPort` ne désactive le compte à la sortie de l'agent.
6. **Délégations (`BF-REF-042`, `§8.1`, `§9`)** :
   - **Attendu** : Délégation avec `périmètre` (unité/module), `statut` incluant `PROGRAMMEE → ACTIVE → EXPIREE / REVOQUEE`, et traçabilité explicite dans les journaux d'audit qu'une opération a été exécutée « par délégation de X ».
   - **Implémenté** : `DelegationPouvoirEntity` n'a pas de champ `perimetre` (unité/domaine) et `StatutDelegation` n'a pas l'état `PROGRAMMEE`.
7. **Détection des tentatives d'accès anormales & appareils autorisés (`BF-REF-044`, `BF-REF-045`)** :
   - **Implémenté** : Sessions Keycloak (`ListSessionsUseCase`), Rate Limiting Redis et journalisation des requêtes dans DuckDB/ClickHouse.
   - **Manquant** : Gestion des appareils autorisés (device trust) et détection/alerte sur les tentatives d'accès anormales (échecs répétés, connexions suspectes).

---

### 2.7. REF-07 — Paramètres, Workflows et GED — `BF-REF-046` à `BF-REF-052` & `Processus P7`

1. **Paramètres Généraux (`BF-REF-046`, `BF-REF-047`, `RG-REF-007`)** :
   - **Attendu** : Paramétrage avec **date d'effet et historisation** (`RG-REF-007`), informations institutionnelles (logos, formats de numérotation, règles de codification, devises), et **gestionnaire de modèles de documents** (décisions, lettres, bons, PV).
   - **Implémenté** : `ParametreSystemeEntity` écrase la valeur en place (`UPDATE`) sans historiser les anciennes valeurs ni gérer de `dateEffet`. Aucun gestionnaire de modèles de documents (templates) n'existe.
2. **Moteur de Workflow (`BF-REF-048`, `BF-REF-049`, `Processus P7`) — POINT FORT DU CODE, quelques écarts résiduels** :
   - **Implémenté** : Le module `workflow` couvre les étapes séquentielles/parallèles/conditionnelles, politiques `UNANIMITE`/`MAJORITE`/`PREMIER_REPONDANT`, SLA, décisions `APPROUVER`/`REJETER`/`DEMANDER_MODIFICATION`, audit trail et intégration Maker-Checker.
   - **Écarts résiduels** :
     - Pas de **versioning des définitions de workflow** (`version` absente de `WorkflowDefinitionEntity`, alors que `§8.1` l'exige).
     - La modification d'un circuit de workflow existant (`PUT /api/workflows/definitions/{id}`) s'applique directement sans passer elle-même par la validation du propriétaire métier (**Processus P7 étape 2**).
     - Pas de job planifié (`@Scheduled`) exécutant les **relances automatiques et escalades SLA** lorsque `dateEcheanceSla` est dépassée sur une `WorkflowTacheEntity`.
3. **Gestion Électronique des Documents — GED (`BF-REF-050` à `BF-REF-052`) — INACHEVÉE** :
   - **Attendu** : Numérisation, **OCR**, indexation, classement, **recherche plein texte et multicritère**, **gestion des versions de documents**, droits d'accès documentaires, **durée de conservation**, **signature et cachet électroniques** (`BF-REF-051`), consultation directe depuis chaque transaction (`BF-REF-052`).
   - **Implémenté** : Seules l'entité `DocumentGedEntity` et l'interface `DocumentGedRepository` existent. Les répertoires `ged/application` et `ged/presentation/rest` sont **vides** (aucun endpoint REST d'upload/download/recherche GED, pas de versioning documentaire, pas de `dureeConservation`, pas de signature électronique, pas d'OCR).

---

### 2.8. Interfaces, Synchronisation et Tableau de Bord de Gouvernance — `BF-REF-053` à `BF-REF-059` & `§13`

1. **Tableau de bord de gouvernance des référentiels (`BF-REF-058`, `§13`) — ABSENT** :
   - **Attendu** : 10 indicateurs exigés au §13 :
     1. Nombre de référentiels actifs
     2. Taux de complétude des fiches
     3. Taux de doublons
     4. Taux de validation
     5. Référentiels en attente de validation
     6. Taux de conformité
     7. Taux de synchronisation inter-unités
     8. Délai moyen de validation (SLA)
     9. Nombre de modifications par période
     10. Référentiels obsolètes à réviser/désactiver
   - **Implémenté** : Le package `com.sodepa.erp.dashboard` est **vide** (0 fichier Java).
2. **Synchronisation multi-sites & Mode dégradé (`BF-REF-054`, `BF-REF-055`, `BF-REF-057`) — ABSENT** :
   - **Attendu** : Mécanisme de synchronisation des référentiels vers les unités opérationnelles distantes (ranchs, abattoirs) avec gestion des conflits, cache local / consultation en mode dégradé (coupure réseau) et notification d'événements de mise à jour aux modules consommateurs.
   - **Implémenté** : Aucun mécanisme de synchronisation hors-ligne ou de flux de réplication référentielle inter-sites n'est présent.

---

## 3. Feuille de Route Recommandée pour Résorber les Écarts (Par Priorité)

### Lot Prioritaire 1 — Cohérence & Unicité des Données (Bloquant Métier)
1. **Unifier `TiersEntity` et `FournisseurEntity` (`REF-05` / `P5`)** :
   - Fusionner ou synchroniser `achat_fournisseurs` avec `tiers` (NIU, RCCM, RIB, régime fiscal, agrément, plafond crédit client, segment, statut `BROUILLON/SOUMIS/ACTIF/SUSPENDU/ARCHIVE`, pièces obligatoires avec dates d'expiration) et automatiser la création du compte auxiliaire comptable dès la validation Maker-Checker.
2. **Compléter le module `GED` (`REF-07` / `BF-REF-050` à `052`)** :
   - Implémenter `GedService` (stockage MinIO + SHA-256 + versioning + métadonnées de conservation + signature) et `GedRestController` (`POST /api/ged/documents`, `GET /api/ged/documents/{id}`, recherche multicritère par `typeEntite`/`entiteId`), et relier les pièces justificatives des Tiers et transactions.
3. **Unifier la table des seuils de gestion (`REF-04` / `BF-REF-029` / `PO-REF-02`)** :
   - Étendre `achat_seuils_config` en un référentiel transversal des seuils (Budget, Achats, Caisse/Régies, Réallocations) avec `dateEffet`, `dateFin` et historisation.

### Lot Prioritaire 2 — IAM, Profils Fonctionnels & Matrice SoD (`REF-06` / `P6`)
4. **Implémenter le package `authorisation` (`BF-REF-039`, `BF-REF-040`, `BF-REF-043`)** :
   - Créer les entités `ProfilFonctionnelEntity` (les 13 profils SODEPA), `PermissionDimensionEntity` (périmètre unité, exercice, seuil) et `MatriceSodEntity` (couples de permissions/rôles incompatibles).
   - Enrichir `UtilisateurEntity` avec `matricule`, `uniteOrganisationnelleId`, `responsableId`, `dateExpiration` et un enum `StatutCompteUtilisateur`.
   - Connecter l'événement RH de fin de contrat/sortie à la désactivation automatique du compte (`RG-REF-024`).

### Lot Prioritaire 3 — Référentiel Fiscal & Référentiels Techniques (`REF-03` & `REF-04`)
5. **Implémenter le package `fiscalite` (`BF-REF-028`)** :
   - Créer les entités `TaxeImpotsEntity`, `RegimeFiscalEntity`, `RetenueSourceEntity` avec taux, règles de calcul et `dateEffet`.
6. **Créer les référentiels métiers `REF-03` (`BF-REF-019` à `024`)** :
   - Unités de mesure et table de conversion (`BF-REF-024`).
   - Catalogue des produits et sous-produits animaux (`BF-REF-020`).
   - Référentiel sanitaire & vétérinaire (`BF-REF-022`) et référentiel d'abattage & transformation (`BF-REF-023`).

### Lot Prioritaire 4 — Gouvernance MDM, Organigramme avancé & Dashboard (`REF-01`, `REF-02`, `§13`)
7. **Socle MDM & Dashboard (`REF-01` / `BF-REF-058` / `§13`)** :
   - Ajouter les attributs de gouvernance (`proprietaireMetier`, `version`, `dateEffet`, `statut`), la détection de doublons, le contrôle d'impact avant désactivation et le contrôleur REST du tableau de bord des 10 KPIs MDM dans `dashboard`.
