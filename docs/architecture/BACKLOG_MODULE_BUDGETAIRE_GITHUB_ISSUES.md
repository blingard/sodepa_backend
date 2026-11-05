# Référentiel du Backlog & Tickets GitHub — Module de Gestion Budgétaire (ERP SODEPA)

> **Dépôt cible :** `blingard/sodepa_backend`  
> **Auteur :** Antigravity Pair Programming (Architecte Logiciel Senior)  
> **Date de génération :** 2 novembre 2026  
> **Format :** User Stories complètes (En tant que / Je veux / Afin de) + Spécifications techniques + Critères d'acceptation Gherkin (Given/When/Then) + Références normatives RF/RNF.

---

## Vue d'ensemble des Milestones & Épics

| Milestone | Épic | Priorité | Description sommaire |
| :--- | :--- | :---: | :--- |
| **Phase 1 — Socle & Contrôle** | **EPIC 1 — Socle & Référentiels** | **P0** | Exercices, axes, versions, enveloppes hiérarchiques, grilles de saisie et rattachement organigramme |
| **Phase 1 — Socle & Contrôle** | **EPIC 2 — Contrôle des Dépenses & Réservations** | **P1** | Réservations atomiques, cycle Commande-Réception-Facture, Maker-Checker, dérogations & virements |
| **Phase 2 — Rapprochement & Pilotage** | **EPIC 3 — Rapprochement & Intégrations** | **P1** | Intégration comptabilité générale/analytique, idempotence stricte, gestion DLQ et clôtures |
| **Phase 2 — Rapprochement & Pilotage** | **EPIC 4 — Suivi, Écarts & Piste d'Audit** | **P1** | Comparatifs temps réel, décomposition volume/prix, drill-down comptable et audit trail infalsifiable |
| **Phase 3 — Budgets Métiers & Prévisions** | **EPIC 5 — Budgets Métiers Spécifiques** | **P2** | Masse salariale RH confidentielle, investissements CAPEX et trésorerie prévisionnelle |
| **Phase 3 — Budgets Métiers & Prévisions** | **EPIC 6 — Prévisions & Consolidation** | **P2** | Reforecast périodique, rolling forecast 12-18 mois, scénarios what-if et consolidation multi-devises |
| **Phase 3 — Budgets Métiers & Prévisions** | **EPIC 7 — Administration & Gouvernance** | **P2/P3** | Moteur de règles sans code, supervision technique des flux et qualité des référentiels |

---

## Nomenclature des Labels GitHub

- **Priorités :** `priority:P0` (Bloquant/Socle), `priority:P1` (Indispensable), `priority:P2` (Avancé), `priority:P3` (Évolution)
- **Épics :** `epic:socle`, `epic:controle-depenses`, `epic:rapprochement`, `epic:pilotage-audit`, `epic:budgets-metiers`, `epic:previsions-consolidation`, `epic:administration`
- **Types :** `type:feature`, `type:refactor`, `type:integration`, `type:security`

---

# EPIC 1 (P0) — Socle & Référentiels Budgétaires

---

### Issue #1 : [EPIC-P0] [SOCLE] Modélisation des axes analytiques et intégration de l'organigramme hiérarchique

- **Milestone :** `Phase 1 - Socle & Contrôle (P0/P1)`
- **Labels :** `epic:socle`, `priority:P0`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Contrôleur de gestion ou DAF,  
**Je veux** définir et gérer une structure multi-axes (entité juridique, direction, département, service, centre de coûts, projet, zone géographique/ranch) rattachée à l'organigramme de l'entreprise,  
**Afin de** décliner les budgets selon la structure organisationnelle réelle et filtrer automatiquement les droits de saisie et de validation.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Création de la table `budget_axe_analytique` (`id`, `code`, `libelle`, `type_axe`, `actif`, `date_debut`, `date_fin`).
  - Création de la table `budget_unite_organisationnelle` (`id`, `code`, `libelle`, `parent_id`, `responsable_id`, `entite_juridique_id`, `version`).
  - Table de liaison `budget_axe_unite_mapping`.
- **Existant impacté :**
  - Classe `com.sodepa.erp.budget.infrastructure.entities.BudgetItemEntity` : actuellement `sectionId` (UUID) est isolé. Ajouter les relations `@ManyToOne` vers l'unité organisationnelle et les axes obligatoires.
- **API REST :**
  - `GET /api/v1/budget/axes` : Liste paginée des axes actifs avec filtres par type.
  - `POST /api/v1/budget/axes` : Création/mise à jour d'un axe avec validation de non-chevauchement de dates.
  - `GET /api/v1/budget/organigramme/arbre` : Restitution hiérarchique récursive de l'arbre des unités pour les filtres de droits.
- **Règles métier :** `RF-02`, `RF-80`, `RF-81`, `RF-84`, `RF-88`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Création réussie d'une unité organisationnelle avec responsable
  Etant donné que l'administrateur budgétaire est authentifié avec le rôle "ROLE_ADMIN_BUDGET"
  Quand il soumet une unité "Direction Financière" rattachée à l'entité "SODEPA Siège" avec un responsable valide "USER-102"
  Alors l'unité est persistée avec un identifiant UUID
  Et elle est immédiatement disponible dans l'arbre hiérarchique des centres de coûts

Scénario: Détection d'un centre de coûts sans responsable assigné
  Etant donné un centre de coûts "Ranch Dumbo - Élevage" dont le responsable a quitté l'entreprise
  Quand le contrôleur de gestion consulte le rapport de conformité du référentiel
  Alors une alerte bloquante RF-88 est levée interdisant l'ouverture d'une campagne budgétaire sur ce centre
```

---

### Issue #2 : [EPIC-P0] [SOCLE] Refactorisation des plans budgétaires : versions, statuts et hiérarchie d'enveloppes

- **Milestone :** `Phase 1 - Socle & Contrôle (P0/P1)`
- **Labels :** `epic:socle`, `priority:P0`, `type:refactor`

#### 1. Description fonctionnelle
**En tant que** Directeur Financier (DAF),  
**Je veux** gérer une hiérarchie d'enveloppes budgétaires (enveloppe globale $\rightarrow$ direction $\rightarrow$ sous-enveloppes $\rightarrow$ lignes) et versionner chaque budget (Initial, Révisé, Reforecast),  
**Afin de** garantir qu'aucune sous-enveloppe ne dépasse l'enveloppe globale et qu'une version validée devienne strictement immuable.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Refactoring de `BudgetPlanEntity` : ajout des champs `type_budget` (OPEX, CAPEX, TRESORERIE), `version_officielle` (booléen), `date_verrouillage`, `verrouille_par`.
  - Création de la table `budget_enveloppe` (`id`, `budget_plan_id`, `parent_enveloppe_id`, `unite_id`, `montant_plafond`, `devise`, `statut`).
- **Existant impacté :**
  - `com.sodepa.erp.budget.infrastructure.entities.BudgetPlanEntity` : enrichir l'énumération `StatutBudget` (DRAFT, SUBMITTED, IN_REVIEW, VALIDATED, LOCKED, ARCHIVED - `RF-21`).
  - `com.sodepa.erp.budget.application.usecase.CreerBudgetPlanUseCase` : adapter la logique pour initialiser l'enveloppe racine.
- **API REST :**
  - `POST /api/v1/budget/plans/{id}/enveloppes` : Création d'une sous-enveloppe avec validation de cohérence de somme par rapport au parent.
  - `POST /api/v1/budget/plans/{id}/lock` : Verrouillage formel du plan validé (`RF-25`).
- **Règles métier :** `RF-01`, `RF-04`, `RF-07`, `RF-21`, `RF-25`, Règle 4 et 5 (Section 15).

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Contrôle de dépassement lors de la création d'une sous-enveloppe
  Etant donné une enveloppe de Direction "Exploitation Ranches" plafonnée à 500 000 000 XAF
  Quand un utilisateur tente d'allouer une sous-enveloppe "Ranch Faro" de 350 000 000 XAF alors que 200 000 000 XAF sont déjà alloués
  Alors le système rejette la création avec un code HTTP 422 Unprocessable Entity
  Et affiche le message explicite "La somme des sous-enveloppes (550 000 000 XAF) dépasse l'enveloppe parente (500 000 000 XAF)"

Scénario: Immuabilité absolue d'un budget verrouillé
  Etant donné un plan budgétaire 2026 au statut "LOCKED"
  Quand un utilisateur tente d'envoyer un PUT sur une ligne de ce plan
  Alors la requête est rejetée avec une erreur HTTP 403 Forbidden
  Et un événement d'alerte de tentative de modification non autorisée est consigné dans l'audit trail
```

---

### Issue #3 : [EPIC-P0] [SOCLE] Moteur de saisie budgétaire en grille, ventilation par période et initialisation

- **Milestone :** `Phase 1 - Socle & Contrôle (P0/P1)`
- **Labels :** `epic:socle`, `priority:P0`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Responsable de centre de coûts,  
**Je veux** saisir mon budget annuel par grille avec ventilation automatique par période (mensuelle, trimestrielle) ou initialisation depuis l'historique N-1 (avec coefficient),  
**Afin de** préparer rapidement mes propositions budgétaires avec justification sur chaque ligne.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_item` : colonnes `compte_code`, `section_id`, `montant_annuel`, `formule_calcul`, `justificatif_texte`.
  - Table `budget_item_periode` : répartition par période (mois 1 à 12) liée à `BudgetItemEntity`.
  - Table `budget_justificatif` : métadonnées des pièces jointes stockées sur MinIO.
- **Existant impacté :**
  - `BudgetItemEntity` & `BudgetItemPeriodeEntity`.
  - `GenererBudgetDepuisHistoriqueUseCase` : adapter pour supporter la formule $Budget_N = Realise_{N-1} \times (1 + \tau)$ ou base zéro (`RF-12`).
  - `AjouterItemPlanUseCase`.
- **API REST :**
  - `POST /api/v1/budget/plans/{id}/items/batch` : Saisie par lot de lignes avec ventilation (linéaire, saisonnière, personnalisée - `RF-13`).
  - `POST /api/v1/budget/plans/{id}/initialiser-historique` : Génération automatique basée sur N-1.
- **Règles métier :** `RF-11`, `RF-12`, `RF-13`, `RF-14`, `RF-15`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Ventilation mensuelle avec saisonnalité agricole/pastorale
  Etant donné une ligne budgétaire annuelle de 12 000 000 XAF sur le compte "6022 - Aliments bétail"
  Quand l'utilisateur applique le profil de ventilation "Saison sèche (70% Nov-Fév)"
  Alors les 12 périodes mensuelles dans "budget_item_periode" sont créées avec les montants exacts pondérés
  Et la somme des 12 périodes est rigoureusement égale à 12 000 000.0000 XAF
```

---

### Issue #4 : [EPIC-P0] [SOCLE] Assistant d'import/export Excel contrôlé avec validation ligne par ligne et simulation

- **Milestone :** `Phase 1 - Socle & Contrôle (P0/P1)`
- **Labels :** `epic:socle`, `priority:P0`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Contrôleur de gestion ou contributeur,  
**Je veux** importer un modèle de saisie budgétaire Excel/CSV avec prévisualisation, rapport d'erreurs téléchargeable et mode simulation,  
**Afin d'**intégrer en toute sécurité des volumes importants sans risquer de corrompre les données par des erreurs de syntaxe ou des comptes invalides.

#### 2. Spécifications techniques & Données
- **Composants backend :**
  - Service `BudgetImportService` s'appuyant sur Apache POI avec streaming (SXSSF) pour limiter la consommation mémoire.
  - Protection contre les injections de formules CSV/Excel dangereuses (OWASP - neutralisation des caractères `=,+,-,@`).
  - Table `budget_import_lot` (`id`, `fichier_nom`, `sha256_hash`, `auteur_id`, `statut`, `rapport_erreurs_json`).
- **API REST :**
  - `POST /api/v1/budget/plans/{id}/import/preview` : Analyse sans persistance, renvoyant le comptage des lignes valides et les anomalies détectées (`RF-130`).
  - `POST /api/v1/budget/plans/{id}/import/execute` : Exécution transactionnelle complète (mode tout-ou-rien par défaut - `RF-131`).
  - `GET /api/v1/budget/plans/{id}/export/template` : Génération du template Excel officiel avec menus déroulants des comptes et axes valides.
- **Règles métier :** `RF-126`, `RF-130`, `RF-131`, `RF-132`, Critère d'acceptation 12.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Rejet d'un fichier d'import contenant un compte comptable inexistant
  Etant donné un fichier Excel contenant 50 lignes budgétaires dont la ligne 12 comporte le compte "699999" absent du plan SYSCOHADA
  Quand l'utilisateur téléverse le fichier en mode simulation
  Alors le statut de l'import est "REJECTED_ERRORS"
  Et le rapport téléchargeable indique précisément "Ligne 12 : Compte 699999 invalide ou inactif"
  Et aucune ligne n'est insérée dans la table "budget_item"
```

---

# EPIC 2 (P1) — Contrôle des Dépenses, Réservations Atomiques & Dérogations

---

### Issue #5 : [EPIC-P1] [CONTROLE] Moteur de réservation budgétaire atomique et prévention des dépassements concurrents

- **Milestone :** `Phase 1 - Socle & Contrôle (P0/P1)`
- **Labels :** `epic:controle-depenses`, `priority:P1`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Système d'engagement (Achats, Contrats, Missions),  
**Je veux** vérifier et réserver de manière atomique un crédit budgétaire sur une enveloppe avant validation d'une dépense,  
**Afin d'**empêcher que deux dépenses concurrentes ne consomment le même solde disponible et n'engendrent un dépassement non autorisé.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_reservation` (`id`, `reference_externe`, `budget_item_id`, `montant`, `devise`, `statut`, `date_expiration`, `cle_idempotence`).
  - Statuts de réservation : `PENDING_APPROVAL`, `CONFIRMED`, `RELEASED`, `EXPIRED`.
- **Mécanisme de verrouillage & Concurrence :**
  - Verrouillage pessimiste (`PESSIMISTIC_WRITE`) sur la ligne budgétaire `BudgetItemEntity` lors du contrôle du disponible, ou lock distribué Redis via Redisson sur la clé `budget:item:{id}`.
  - Formule stricte du disponible :
    $$\text{Disponible} = \text{Budget\_Validé} - \text{Réservations\_En\_Cours} - \text{Engagements\_Actifs} - \text{Réalisé\_Non\_Couvert}$$
- **API REST :**
  - `POST /api/v1/budget-controls/check` : Consultation synchrone en lecture seule (< 500ms).
  - `POST /api/v1/budget-reservations` : Réservation atomique bloquante avec clé d'idempotence (`RF-92`, `RF-95`, `RNF-01`).
  - `POST /api/v1/budget-reservations/{id}/release` : Libération idempotente en cas de rejet d'achat.
- **Règles métier :** `RF-06`, `RF-30`, `RF-31`, `RF-90`, `RF-92`, `RF-95`, `RNF-01`, Critère 8.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Réservation atomique concurrente sur solde limité
  Etant donné une ligne budgétaire disposant de 1 000 000 XAF de crédit disponible
  Quand deux demandes d'achat simultanées de 800 000 XAF chacune sont soumises à la même milliseconde
  Alors une seule des deux requêtes obtient un statut "RESERVED" avec HTTP 201
  Et la seconde requête reçoit un refus HTTP 409 Conflict ou un statut "BLOCKED_OVERBUDGET"
  Et le disponible restant est exactement de 200 000 XAF sans aucun dépassement
```

---

### Issue #6 : [EPIC-P1] [CONTROLE] Cycle de vie complet de l'engagement : Commande, Réception, Facture et prévention du double comptage

- **Milestone :** `Phase 1 - Socle & Contrôle (P0/P1)`
- **Labels :** `epic:controle-depenses`, `priority:P1`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Comptable et Contrôleur budgétaire,  
**Je veux** suivre la transition d'un engagement lors de la commande, de la réception et de la facturation fournisseur,  
**Afin de** solder automatiquement l'engagement au fur et à mesure de la comptabilisation des factures sans jamais décompter deux fois le montant du disponible.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Refactoring de `budget_engagement` (`com.sodepa.erp.budget.infrastructure.entities.BudgetEngagementEntity`) :
    - Ajout des colonnes : `montant_commande`, `montant_receptionne`, `montant_facture`, `montant_solde`, `id_commande_externe`, `id_facture_externe`.
    - Nouveaux statuts conformes à `RF-91` : `REQUESTED`, `COMMITTED`, `PARTIALLY_INVOICED`, `FULLY_INVOICED`, `CANCELLED`.
- **Existant impacté :**
  - `EnregistrerEngagementUseCase` et `LiquiderEngagementUseCase`.
- **Logique transactionnelle :**
  - Réception d'une facture liée à une commande :
    1. Diminution de l'engagement ouvert : $\Delta Eng = - Montant\_Facture$.
    2. Augmentation du réalisé comptable : $\Delta Real = + Montant\_Facture$.
    3. Variation nette du disponible = $0$ (pas de double comptage).
- **API REST & Événements :**
  - `POST /api/v1/budget/engagements/{id}/transition-facture` : Endpoint appelé lors de la comptabilisation d'une facture fournisseur.
  - Listener RabbitMQ `BudgetInvoiceEventListener` sur l'exchange `compta.facture.validee`.
- **Règles métier :** `RF-30`, `RF-34`, `RF-91`, Section 15 Règle 1, Critère d'acceptation 9.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Facturation partielle sans double comptage du disponible
  Etant donné une commande engagée de 10 000 000 XAF sur un budget de 20 000 000 XAF (disponible = 10 000 000 XAF)
  Quand une première facture de 4 000 000 XAF est comptabilisée sur cette commande
  Alors l'engagement ouvert passe à 6 000 000 XAF
  Et le réalisé comptabilisé passe à 4 000 000 XAF
  Et le disponible reste strictement égal à 10 000 000 XAF
```

---

### Issue #7 : [EPIC-P1] [CONTROLE] Workflow de dérogation budgétaire avec principe Maker-Checker et délégation

- **Milestone :** `Phase 1 - Socle & Contrôle (P0/P1)`
- **Labels :** `epic:controle-depenses`, `priority:P1`, `type:security`

#### 1. Description fonctionnelle
**En tant que** Responsable opérationnel dépassant son enveloppe ou DAF,  
**Je veux** soumettre une demande de dérogation exceptionnelle avec motif et justificatif, qui ne pourra en aucun cas être approuvée par le demandeur lui-même,  
**Afin d'**autoriser des dépenses urgentes tout en garantissant une séparation stricte des pouvoirs et une piste d'audit opposable.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_derogation` (`id`, `reference`, `budget_item_id`, `demandeur_id`, `valideur_id`, `montant_demande`, `montant_accorde`, `motif_detaille`, `date_expiration`, `statut`).
  - Table `budget_delegation` (`id`, `delegant_id`, `delegataire_id`, `date_debut`, `date_fin`, `plafond_montant`, `actif`).
- **Contrôles de sécurité (Maker-Checker) :**
  - Règle stricte : `demandeur_id != valideur_id`. Rejet automatique HTTP 403 en cas d'auto-approbation (`RF-100`).
  - Traçabilité des délégations actives au moment précis du clic d'approbation (`RF-105`, `RF-106`).
- **API REST :**
  - `POST /api/v1/budget-derogations` : Demande de dérogation avec pièces jointes.
  - `POST /api/v1/budget-derogations/{id}/arbitrer` : Approbation/Rejet par le valideur habilité avec commentaire obligatoire.
- **Règles métier :** `RF-94`, `RF-100`, `RF-101`, `RF-103`, `RF-104`, `RF-105`, Critère 10.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Interdiction formelle d'auto-approbation d'une dérogation
  Etant donné que le Directeur Financier "USER-DF-01" est également demandeur d'un achat hors budget de 5 000 000 XAF
  Quand "USER-DF-01" tente de valider sa propre demande de dérogation
  Alors le système bloque l'action avec une exception "MakerCheckerSecurityException"
  Et renvoie un code HTTP 403 Forbidden "L'initiateur d'une demande ne peut pas valider sa propre dérogation"

Scénario: Approbation par délégation temporaire tracée
  Etant donné que le DAF a délégué ses validations à l'adjoint "USER-ADJ-02" pour la période du 01 au 15 du mois
  Quand "USER-ADJ-02" approuve une dérogation conforme au plafond délégué
  Alors la décision est enregistrée au statut "APPROVED"
  Et l'audit trail consigne "Approuvé par USER-ADJ-02 par délégation de USER-DF-01"
```

---

### Issue #8 : [EPIC-P1] [CONTROLE] Gestion des virements budgétaires (réallocations) entre lignes et enveloppes

- **Milestone :** `Phase 1 - Socle & Contrôle (P0/P1)`
- **Labels :** `epic:controle-depenses`, `priority:P1`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Responsable de département ou Contrôleur de gestion,  
**Je veux** effectuer un virement budgétaire entre deux lignes ou sous-enveloppes avec contrôle de disponibilité de la source et circuit de validation,  
**Afin d'**ajuster les crédits en cours d'année sans altérer l'enveloppe globale de l'entreprise.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Enrichissement de `budget_reallocation` (`com.sodepa.erp.budget.infrastructure.entities.BudgetReallocationEntity`) :
    - Colonnes : `numero_virement`, `source_item_id`, `destination_item_id`, `montant`, `motif`, `statut` (BROUILLON, SOUMIS, APPROUVE, REJETE), `date_effet`.
- **Existant impacté :**
  - `EffectuerReallocationUseCase` et `ReallocationInput` / `ReallocationRequest`.
- **Contrôles métier :**
  - Vérifier que `montant <= disponible(source_item)`.
  - Vérifier que la source et la destination appartiennent à des périmètres compatibles selon les règles d'étanchéité (ex: interdiction virement CAPEX $\rightarrow$ OPEX sans accord DG).
- **API REST :**
  - `POST /api/v1/budget-transfers` : Initialisation du virement.
  - `POST /api/v1/budget-transfers/{id}/valider` : Validation et exécution atomique des écritures de réallocation.
- **Règles métier :** `RF-32`, Section 17 API `POST /api/v1/budget-transfers`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Virement budgétaire valide avec équilibre des masses
  Etant donné une ligne source "Carburant" disposant de 5 000 000 XAF disponible
  Et une ligne cible "Maintenance véhicules" nécessitant 2 000 000 XAF
  Quand le virement de 2 000 000 XAF est validé par le DAF
  Alors le montant planned de la source est décrémenté de 2 000 000 XAF
  Et le montant planned de la cible est incrémenté de 2 000 000 XAF
  Et le total cumulé du plan budgétaire demeure rigoureusement invariant
```

---

# EPIC 3 (P1) — Rapprochement Comptable, Intégrations & Idempotence

---

### Issue #9 : [EPIC-P1] [RAPPROCHEMENT] Rapprochement automatisé budget / comptabilité générale et analytique

- **Milestone :** `Phase 2 - Rapprochement & Pilotage (P1)`
- **Labels :** `epic:rapprochement`, `priority:P1`, `type:integration`

#### 1. Description fonctionnelle
**En tant que** Comptable ou Contrôleur financier,  
**Je veux** exécuter un rapprochement automatisé entre les lignes budgétaires et les écritures réelles comptabilisées dans le plan comptable SYSCOHADA,  
**Afin d'**identifier immédiatement tout écart, écriture non imputée ou facture sans engagement préalable.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_rapprochement` (`id`, `exercice_annee`, `periode_mois`, `date_execution`, `total_budgete`, `total_realise_compta`, `ecart_global`, `statut`).
  - Table `budget_rapprochement_ligne` (`id`, `rapprochement_id`, `compte_code`, `section_id`, `montant_budget`, `montant_ecritures`, `ecart`, `justification`).
- **Existant impacté :**
  - `com.sodepa.erp.budget.application.usecase.ExecuterRapprochementBudgetaireUseCase`.
  - Intégration avec `com.sodepa.erp.comptabilite.infrastructure.entities.EcritureComptableEntity`.
- **API REST :**
  - `POST /api/v1/budget/rapprochements/executer` : Lancement du batch de comparaison.
  - `GET /api/v1/budget/rapprochements/ecarts` : Liste des anomalies détectées avec filtres par gravité.
- **Règles métier :** `RF-70`, `RF-153`, `RF-154`, Critères 2 et 13.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Détection d'une facture comptabilisée sans commande d'engagement
  Etant donné une écriture comptable de 750 000 XAF débitée sur le compte "6061" sans référence d'engagement
  Quand le batch de rapprochement mensuel est exécuté
  Alors la ligne est marquée avec l'anomalie "FACTURE_SANS_ENGAGEMENT"
  Et le statut du rapprochement de la période ne peut pas être validé sans justification obligatoire
```

---

### Issue #10 : [EPIC-P1] [RAPPROCHEMENT] Idempotence stricte et résilience des intégrations événementielles

- **Milestone :** `Phase 2 - Rapprochement & Pilotage (P1)`
- **Labels :** `epic:rapprochement`, `priority:P1`, `type:technical`

#### 1. Description fonctionnelle
**En tant qu'**Architecte technique,  
**Je veux** garantir l'idempotence stricte de toutes les API d'écriture et de tous les consommateurs d'événements RabbitMQ via des clés d'idempotence et une file d'erreur (DLQ),  
**Afin d'**assurer qu'un message rejoué ou une panne réseau ne crée jamais de doublon d'engagement ou de réalisé.

#### 2. Spécifications techniques & Données
- **Composants d'infrastructure :**
  - Table / Cache `idempotency_record` (`key_hash`, `ressource_type`, `response_payload`, `created_at`, `expires_at`).
  - Implémentation via base in-process DuckDB (`sodepa_audit.db`) et compteurs Redis (`FAIL_CLOSED`).
  - Configuration RabbitMQ : Queue `budget.events.dlq` pour les messages en échec après 3 retries avec backoff exponentiel.
- **Intercepteur HTTP :**
  - Vérification obligatoire de l'en-tête `Idempotency-Key` sur tous les `POST /api/v1/budget-*` d'écriture.
- **Règles métier :** `RF-161`, `RF-162`, `RF-163`, `RNF-02`, Critère 11.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Message d'événement renvoyé en doublon par RabbitMQ
  Etant donné un événement d'engagement "EVT-ENG-987" déjà traité et consigné dans la base
  Quand le même événement est réémis par le broker suite à un timeout réseau
  Alors le consommateur détecte la clé d'idempotence déjà enregistrée
  Et acquitte le message (ACK) sans exécuter de nouvelle déduction budgétaire
  Et consigne un log informatif "Message duplicate ignoré avec succès"
```

---

### Issue #11 : [EPIC-P1] [RAPPROCHEMENT] Procédures de clôture budgétaire périodique et annuelle avec report des engagements ouverts

- **Milestone :** `Phase 2 - Rapprochement & Pilotage (P1)`
- **Labels :** `epic:rapprochement`, `priority:P1`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** DAF et Chef comptable,  
**Je veux** verrouiller les périodes budgétaires mensuelles et exécuter la clôture annuelle avec report automatique des engagements ouverts non facturés sur l'exercice N+1,  
**Afin de** garantir la sincérité des comptes de fin d'année et initialiser fidèlement l'exercice budgétaire suivant.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_cloture_exercice` (`id`, `annee`, `date_cloture`, `responsable_id`, `total_engagements_reportes`, `statut`).
  - Flag `cloture` sur chaque période mensuelle dans `budget_item_periode`.
- **Règles de clôture annuelle :**
  - Identification de tous les `budget_engagement` avec `statut = 'COMMITTED'` et solde > 0.
  - Création des engagements d'ouverture sur le budget $N+1$ rattachés au compte et au projet d'origine.
- **API REST :**
  - `POST /api/v1/budget/clotures/mensuelle/{periode}` : Verrouillage de la période après contrôle des interfaces.
  - `POST /api/v1/budget/clotures/annuelle` : Exécution du basculement d'exercice et génération des soldes de report.
- **Règles métier :** `RF-70`, `RF-71`, `RF-72`, Section 15 Règle 6.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Report automatique d'un engagement pluriannuel à la clôture de l'exercice
  Etant donné un contrat de maintenance avec un engagement ouvert restant de 3 500 000 XAF au 31 décembre 2026
  Quand le DAF déclenche la clôture annuelle de l'exercice 2026
  Alors l'engagement 2026 est marqué comme "CLOSED_ROLLED_OVER"
  Et une ligne de réservation d'ouverture équivalente de 3 500 000 XAF est créée sur le budget 2027
```

---

# EPIC 4 (P1) — Suivi, Analyse des Écarts & Piste d'Audit

---

### Issue #12 : [EPIC-P1] [PILOTAGE] Tableau comparatif temps réel Budget / Engagé / Réalisé / Disponible avec drill-down

- **Milestone :** `Phase 2 - Rapprochement & Pilotage (P1)`
- **Labels :** `epic:pilotage-audit`, `priority:P1`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Responsable de centre de coûts ou Contrôleur de gestion,  
**Je veux** consulter en temps réel le tableau comparatif entre budget validé, engagements ouverts, réalisé comptable et disponible, avec capacité de descendre jusqu'à la pièce d'origine (drill-down),  
**Afin de** piloter ma consommation au quotidien et réagir dès les premiers signaux de tension financière.

#### 2. Spécifications techniques & Données
- **Requêtes & Performance :**
  - Vue matérialisée PostgreSQL ou requêtes d'agrégation optimisées avec indexes composites `(budget_plan_id, compte_code, section_id)`.
  - Temps de réponse d'affichage cible : $< 1.5$ seconde (RNF Performance 7.1).
- **API REST :**
  - `GET /api/v1/budget-variance/tableau-bord` : Agrégats consolidés filtrables par axe, période, direction et compte.
  - `GET /api/v1/budget-variance/drill-down/{itemId}` : Détail des pièces composant le montant (commandes, contrats, factures, écritures - `RF-64`).
- **Règles métier :** `RF-30`, `RF-40`, `RF-60`, `RF-64`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Drill-down depuis une ligne budgétaire vers les pièces justificatives
  Etant donné une ligne "Fournitures administratives" affichant 2 400 000 XAF d'engagé et 1 100 000 XAF de réalisé
  Quand l'utilisateur clique sur le montant engagé
  Alors l'API renvoie la liste exhaustive des 3 bons de commande actifs composant ce total avec leurs numéros et dates
```

---

### Issue #13 : [EPIC-P1] [PILOTAGE] Moteur d'analyse des écarts (effets Volume / Prix / Périmètre) et alertes configurables

- **Milestone :** `Phase 2 - Rapprochement & Pilotage (P1)`
- **Labels :** `epic:pilotage-audit`, `priority:P1`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Contrôleur de gestion,  
**Je veux** analyser les écarts entre prévisions et réalisés décomposés en effet volume, effet prix et effet périmètre, et déclencher des alertes automatiques sur seuils de consommation (80%, 100%),  
**Afin d'**expliquer les causes des dérives financières et prévenir les directions avant blocage.

#### 2. Spécifications techniques & Données
- **Algorithmes financiers :**
  - Décomposition standard :
    $$\Delta Total = \Delta Volume \times P_{prev} + \Delta Prix \times V_{real} + \text{Effet Périmètre}$$
  - Taxonomie d'explication d'écart (`RF-145`) : Prix, Volume, Calendrier, Périmètre, Change, Aléas climatiques/sanitaires.
- **API & Notifications :**
  - `GET /api/v1/budget-variance/analyse-ecarts` : Calcul des composantes d'écarts.
  - Système d'alertes asynchrone (RabbitMQ $\rightarrow$ Webhook & Email) lorsque le ratio de consommation $\ge 80\%$.
- **Règles métier :** `RF-36`, `RF-41`, `RF-141`, `RF-145`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Notification automatique lors du franchissement du seuil de 80%
  Etant donné une ligne budgétaire dotée de 10 000 000 XAF
  Quand un nouvel engagement porte le cumul engagé + réalisé à 8 100 000 XAF (81%)
  Alors une alerte "WARNING_THRESHOLD_80" est enregistrée
  Et un email et une notification in-app sont immédiatement transmis au responsable du centre et au contrôleur
```

---

### Issue #14 : [EPIC-P1] [PILOTAGE] Piste d'audit infalsifiable avec valeurs avant/après et export vers ClickHouse

- **Milestone :** `Phase 2 - Rapprochement & Pilotage (P1)`
- **Labels :** `epic:pilotage-audit`, `priority:P1`, `type:security`

#### 1. Description fonctionnelle
**En tant qu'**Auditeur interne ou Commissaire aux comptes,  
**Je veux** disposer d'un journal d'audit immuable consignant toute modification budgétaire (qui, quand, quoi, pourquoi, valeurs avant/après, identifiant de corrélation),  
**Afin de** garantir la conformité réglementaire et l'auditabilité intégrale des décisions financières.

#### 2. Spécifications techniques & Données
- **Pipeline d'audit :**
  - Publication via `AuditEventPublisher` dans RabbitMQ (`audit.exchange`).
  - Ingestion batch dans ClickHouse (`sodepa-clickhouse:26.7`) via `AuditRabbitListener`.
  - Format d'événement standardisé : `eventId`, `timestamp`, `userId`, `actionType`, `entityName`, `entityId`, `correlationId`, `oldValueJson`, `newValueJson`, `justification`.
- **Existant impacté :**
  - `com.sodepa.erp.budget.application.usecase.ConsulterAuditTrailBudgetUseCase` et `AuditTrailRestController`.
- **Règles de sécurité :**
  - Tables d'audit en lecture seule stricte côté application (aucune possibilité de `UPDATE` ou `DELETE`).
- **Règles métier :** `RF-120`, `RF-121`, `RF-122`, Critères 4 et 14.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Traçabilité inaltérable d'une modification de plafond budgétaire
  Etant donné un plan budgétaire dont le plafond est réévalué de 50M à 60M XAF par le DAF avec justification
  Quand l'opération est validée
  Alors un enregistrement immuable est écrit dans ClickHouse
  Et la consultation de la piste d'audit restitue fidèlement la valeur "avant=50000000", "apres=60000000" et le motif saisi
```

---

# EPIC 5 (P2) — Budgets Métiers Spécifiques (RH, CAPEX, Trésorerie)

---

### Issue #15 : [EPIC-P2] [BUDGETS-METIERS] Élaboration et contrôle confidentiel du budget de masse salariale (RH / Paie)

- **Milestone :** `Phase 3 - Budgets Métiers & Prévisions (P2)`
- **Labels :** `epic:budgets-metiers`, `priority:P2`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Directeur des Ressources Humaines et DAF,  
**Je veux** bâtir les prévisions de masse salariale à partir des effectifs, grilles, charges patronales et mouvements prévisionnels, avec masquage strict des salaires individuels,  
**Afin d'**allouer les enveloppes de rémunération par service tout en respectant la confidentialité des données personnelles.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_masse_salariale` (`id`, `annee`, `unite_id`, `effectif_actuel`, `recrutements_prevus`, `departs_prevus`, `total_brut`, `total_charges_patronales`).
- **Sécurité & Droits :**
  - Contrôle d'habilitation renforcé `@PreAuthorize("hasAuthority('BUDGET_RH_ACCESS')")`.
  - Masquage des détails nominatifs dans les agrégats renvoyés aux chefs de département (`RF-124`).
- **API REST :**
  - `POST /api/v1/budget/masse-salariale/calculer` : Simulation intégrant le taux de charges sociales.
  - `GET /api/v1/budget/masse-salariale/enveloppes` : Synthèse par département sans données individuelles.
- **Règles métier :** `RF-16`, `RF-124`, Section 5.2 RH et Paie.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Masquage des salaires individuels pour les responsables opérationnels
  Etant donné un responsable de ranch consultant le budget de masse salariale de son unité
  Quand il charge la vue budgétaire de son équipe
  Alors il visualise l'enveloppe globale et le nombre d'ETP autorisés
  Et les salaires de base nominatifs de chaque employé sont rigoureusement masqués
```

---

### Issue #16 : [EPIC-P2] [BUDGETS-METIERS] Gestion des budgets d'investissement (CAPEX) et amortissements prévisionnels

- **Milestone :** `Phase 3 - Budgets Métiers & Prévisions (P2)`
- **Labels :** `epic:budgets-metiers`, `priority:P2`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Responsable des investissements ou DAF,  
**Je veux** gérer les fiches projets d'investissement (infrastructures, cheptel reproducteur, matériel agricole) avec échéancier de décaissement et calcul des amortissements prévisionnels,  
**Afin de** maîtriser les enveloppes CAPEX séparément des charges d'exploitation et mesurer l'impact sur le compte de résultat futur.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_capex_projet` (`id`, `code_projet`, `intitule`, `cout_total_estime`, `duree_amortissement_mois`, `type_amortissement`, `date_mise_en_service`).
  - Table `budget_capex_echeancier` (`id`, `projet_id`, `annee`, `mois`, `montant_decaissement_prevu`).
- **Existant impacté :**
  - `SimulerAmortissementUseCase`.
- **Règles métier :** `RF-17`, Section 5.2 Immobilisations & investissements.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Calcul automatique des dotations aux amortissements prévisionnels
  Etant donné un investissement de 60 000 000 XAF amortissable en linéaire sur 5 ans mis en service en janvier 2026
  Quand le plan prévisionnel d'amortissement est généré
  Alors une dotation mensuelle de 1 000 000 XAF est calculée pour le compte de résultat budgétaire de chaque mois de l'exercice
```

---

### Issue #17 : [EPIC-P2] [BUDGETS-METIERS] Génération de la trésorerie prévisionnelle issue des budgets validés

- **Milestone :** `Phase 3 - Budgets Métiers & Prévisions (P2)`
- **Labels :** `epic:budgets-metiers`, `priority:P2`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Trésorier d'entreprise,  
**Je veux** déduire automatiquement l'échéancier prévisionnel des encaissements et décaissements à partir des budgets d'exploitation et d'investissement validés (en intégrant les délais de paiement),  
**Afin d'**anticiper les besoins de financement et prévenir les risques de cessation de paiement.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_tresorerie_previsionnelle` (`id`, `exercice`, `mois`, `solde_initial`, `encaissements_prevus`, `decaissements_prevus`, `solde_net_prevu`).
- **Existant impacté :**
  - `com.sodepa.erp.budget.application.usecase.GenererCashFlowPrevisionnelUseCase` et `GenererTftOhadaUseCase`.
  - Entité `PrevisionTresorerieEntity`.
- **API REST :**
  - `GET /api/v1/budget/tresorerie/tableau-flux` : Restitution conforme au Tableau des Flux de Trésorerie (TFT) SYSCOHADA.
- **Règles métier :** `RF-19`, `RF-150`, `RF-151`, `RF-152`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Décalage d'encaissement selon les conditions de règlement
  Etant donné un budget de ventes de 100 000 000 XAF prévu en mars avec un délai de règlement client moyen de 60 jours
  Quand la trésorerie prévisionnelle est calculée
  Alors l'encaissement correspondant est automatiquement imputé sur le mois de mai
```

---

# EPIC 6 (P2) — Prévisions Glissantes, Scénarios & Consolidation

---

### Issue #18 : [EPIC-P2] [PREVISIONS] Moteur de Reforecast périodique et Rolling Forecast (12–18 mois)

- **Milestone :** `Phase 3 - Budgets Métiers & Prévisions (P2)`
- **Labels :** `epic:previsions-consolidation`, `priority:P2`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Contrôleur de gestion ou DAF,  
**Je veux** actualiser mes prévisions en combinant le réalisé comptable à date et le reste à faire réestimé (Reforecast), et projeter une trajectoire budgétaire glissante (Rolling Forecast sur 12 à 18 mois),  
**Afin d'**ajuster les atterrissages annuels face aux aléas de conjoncture économique et de production animale.

#### 2. Spécifications techniques & Données
- **Modèle de calcul :**
  $$\text{Atterrissage (EAC)} = \text{Réalisé cumulé à date} + \text{Engagements ouverts résiduels} + \text{Nouvelle estimation reste à faire}$$
- **API REST :**
  - `POST /api/v1/budget/reforecast/initialiser` : Duplication de la version active avec figeage du réalisé.
  - `GET /api/v1/budget/reforecast/comparatif-atterrissage` : Tableau comparatif Budget Initial vs Reforecast vs Réel.
- **Règles métier :** `RF-42`, `RF-44`, `RF-140`, `RF-142`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Initialisation d'un Reforecast à mi-année (M6)
  Etant donné un exercice 2026 dont les 6 premiers mois sont clôturés en comptabilité
  Quand le contrôleur de gestion lance le "Reforecast M6"
  Alors les mois M1 à M6 sont automatiquement figés sur les montants réalisés comptables
  Et les mois M7 à M12 restent ouverts à la réestimation des équipes métiers
```

---

### Issue #19 : [EPIC-P2] [PREVISIONS] Simulation de scénarios budgétaires What-If et analyses de sensibilité

- **Milestone :** `Phase 3 - Budgets Métiers & Prévisions (P2)`
- **Labels :** `epic:previsions-consolidation`, `priority:P2`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Direction Générale et DAF,  
**Je veux** créer et comparer des scénarios de simulation (optimiste, pessimiste, crise épizootique, hausse des cours de l'aliment) sans jamais altérer le budget officiel,  
**Afin d'**éclairer les décisions stratégiques et tester la résilience financière de la SODEPA.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Colonne `is_scenario` (booléen) et `scenario_parent_id` dans `BudgetPlanEntity`.
  - Table `budget_scenario_hypothese` (`id`, `scenario_id`, `nom_variable`, `variation_pourcent`, `impact_calcule`).
- **Existant impacté :**
  - `com.sodepa.erp.budget.application.usecase.SimulerHypothesesWhatIfUseCase`.
- **Isolation :**
  - Les scénarios ont interdiction absolue d'émettre des réservations ou de bloquer des commandes réelles (`RF-184`).
- **Règles métier :** `RF-04`, `RF-43`, `RF-143`, `RF-144`, `RF-184`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Simulation d'un choc sur le cours des aliments pour bétail
  Etant donné le budget officiel 2026
  Quand le DAF applique une hypothèse "+25% sur les comptes d'intrants 6022" dans un scénario baptisé "Crise Sécheresse"
  Alors le résultat net prévisionnel du scénario est recalculé instantanément
  Et le budget officiel demeure strictement inchangé et disponible pour les opérations courantes
```

---

### Issue #20 : [EPIC-P2] [PREVISIONS] Consolidation budgétaire multi-sociétés, multi-devises et élimination des flux internes

- **Milestone :** `Phase 3 - Budgets Métiers & Prévisions (P2)`
- **Labels :** `epic:previsions-consolidation`, `priority:P2`, `type:feature`

#### 1. Description fonctionnelle
**En tant que** Directeur Financier du Groupe,  
**Je veux** consolider les budgets de plusieurs entités juridiques avec conversion de devises (taux budgétaires fixes) et élimination des flux réciproques intra-groupe,  
**Afin d'**obtenir une vision financière globale et sincère du groupe SODEPA.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_taux_change` (`devise_source`, `devise_cible`, `exercice`, `taux_budgetaire_fixe`).
  - Table `budget_flux_reciproque` (`entite_emettrice_id`, `entite_receptrice_id`, `compte_code`, `montant_elimination`).
- **API REST :**
  - `POST /api/v1/budget/consolidation/executer` : Lancement du calcul de consolidation groupe.
- **Règles métier :** `RF-50`, `RF-51`, `RF-52`, `RF-53`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Élimination d'une refacturation interne entre deux entités du groupe
  Etant donné que l'entité A budgète 50 000 000 XAF de prestations d'assistance au profit de l'entité B
  Quand la consolidation budgétaire groupe est exécutée
  Alors les écritures réciproques de produit chez A et de charge chez B sont automatiquement annulées
  Et le budget consolidé groupe ne comporte aucun gonflement artificiel
```

---

# EPIC 7 (P2/P3) — Administration No-Code, Supervision & Qualité

---

### Issue #21 : [EPIC-P2] [ADMINISTRATION] Moteur de règles de contrôle budgétaire paramétrable sans développement

- **Milestone :** `Phase 3 - Budgets Métiers & Prévisions (P2)`
- **Labels :** `epic:administration`, `priority:P2`, `type:feature`

#### 1. Description fonctionnelle
**En tant qu'**Administrateur fonctionnel du module budgétaire,  
**Je veux** configurer, prioriser et simuler des règles de contrôle budgétaire (seuils, tolérances, blocage strict, dérogations) par type de dépense ou centre de coûts sans modifier le code source,  
**Afin d'**adapter rapidement la politique financière sans dépendre des cycles de déploiement technique.

#### 2. Spécifications techniques & Données
- **Modèle de données & Liquibase :**
  - Table `budget_regle_controle` (`id`, `code`, `priorite`, `type_action`, `perimetre_json`, `seuil_alerte`, `seuil_blocage`, `tolerance_pourcent`, `date_effet_debut`, `date_effet_fin`, `actif`).
- **Moteur d'évaluation :**
  - Moteur d'expressions Spring SpEL ou JSON-Rule sécurisé.
  - Détection automatique des règles contradictoires avant activation (`RF-180`).
  - Mode bac à sable de simulation (`RF-181`, Critère 17).
- **Règles métier :** `RF-180`, `RF-181`, `RF-182`, `RF-183`.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Simulation d'une nouvelle règle de tolérance de 5%
  Etant donné une règle accordant 5% de tolérance sur les achats urgents de carburant
  Quand l'administrateur teste la règle en mode simulation sur l'historique des dépenses du mois précédent
  Alors le simulateur affiche le nombre exact de demandes qui auraient été débloquées sans produire d'effet réel
```

---

### Issue #22 : [EPIC-P2] [ADMINISTRATION] Tableau de bord de santé opérationnelle et supervision des interfaces d'intégration

- **Milestone :** `Phase 3 - Budgets Métiers & Prévisions (P2)`
- **Labels :** `epic:administration`, `priority:P2`, `type:technical`

#### 1. Description fonctionnelle
**En tant qu'**Équipe Support et DSI,  
**Je veux** surveiller la santé des échanges de données (latence des API, volume des événements reçus, messages en Dead Letter Queue, campagnes en retard),  
**Afin d'**intervenir proactivement avant qu'une panne d'interface n'impacte les opérations d'engagement sur le terrain.

#### 2. Spécifications techniques & Données
- **Indicateurs supervisés :**
  - Taux d'échec des réservations budgétaires.
  - Nombre de messages dans `budget.events.dlq`.
  - Décalage de synchronisation avec la balance comptable.
- **Endpoints & Alerting :**
  - `GET /api/v1/integrations/health` : Métriques exposées pour le tableau de bord et sondes Prometheus (`RF-170`, `RF-175`).
  - Alertes automatiques vers les administrateurs en cas de blocage d'une interface critique (`RF-171`).
- **Règles métier :** `RF-113`, `RF-170`, `RF-171`, `RF-175`, Critère 18.

#### 3. Critères d'acceptation (Gherkin)
```gherkin
Scénario: Alerte automatique lors de l'accumulation de messages en file d'échec
  Etant donné que la file "budget.events.dlq" dépasse le seuil critique de 5 messages non traités
  Quand la sonde de santé interroge l'état des intégrations
  Alors le statut de l'indicateur bascule en "CRITICAL"
  Et une alerte d'exploitation est immédiatement émise vers l'équipe support
```
