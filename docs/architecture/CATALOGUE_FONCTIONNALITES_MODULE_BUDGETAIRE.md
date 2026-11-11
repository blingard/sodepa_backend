**CATALOGUE DES FONCTIONNALITÉS**

**Module 02 — Gestion budgétaire**

| **Élément**                         | **Valeur**                                           |
| ----------------------------------- | ---------------------------------------------------- |
| Code du module                      | BUD                                                  |
| Cahier des charges de référence     | CdC Module 02 — Gestion budgétaire (v1.0)            |
| Document source SODEPA              | Procédure BUDGETAIRE OK.pdf (20 pages)               |
| Nombre d'epics / de fonctionnalités | 6 / 24                                               |
| Couverture des besoins fonctionnels | 47 / 47 BF couverts (100 %)                          |
| Version / date                      | 1.0 — Projet — 06/11/2026                            |
| Statut                              | À valider par le propriétaire métier                 |
| Propriétaire métier pressenti       | SDFBM — Service du Budget (en lien avec la DPD/CSPP) |
| Étape suivante                      | Critères d'acceptation par fonctionnalité (étape 3)  |

**Sommaire**

1\. Objet et méthode

2\. Vue d'ensemble

3\. Fiches des fonctionnalités

4\. Matrice de traçabilité besoins → fonctionnalités

5\. Validation du document

# 1. Objet et méthode

Ce document constitue l'étape 2 de la démarche qualité du projet ERP SODEPA. Il transforme les 47 besoins fonctionnels du cahier des charges du module 02 en 24 fonctionnalités, regroupées en 6 epics (grands ensembles fonctionnels).

Chaque fonctionnalité est décrite par une fiche comprenant : les acteurs concernés, une user story (« En tant que…, je veux…, afin de… »), les comportements attendus, les besoins fonctionnels (BF) couverts et les règles de gestion (RG) à respecter. Les comportements attendus et les règles de gestion serviront de base à la rédaction des critères d'acceptation (étape 3), puis des cas de test (étape 4).

- **EP-BUD-nn** : identifiant d'un epic.
- **F-BUD-nnn** : identifiant d'une fonctionnalité, unique dans le projet.
- **BF-BUD-nnn / RG-BUD-nnn** : besoins et règles du cahier des charges, repris à l'identique.
- **Priorité** : la plus haute des priorités des besoins couverts (Haute, Moyenne, Basse), sauf arbitrage contraire.

La matrice de traçabilité (section 4) garantit que chaque besoin du cahier des charges est couvert par au moins une fonctionnalité.

# 2. Vue d'ensemble

## 2.1 Epics du module

| **ID**    | **Epic**                             | **Nb** | **Fonctionnalités**                                                         |
| --------- | ------------------------------------ | ------ | --------------------------------------------------------------------------- |
| EP-BUD-01 | Structure et nomenclature budgétaire | 3      | F-BUD-001, F-BUD-002, F-BUD-003                                             |
| EP-BUD-02 | Préparation et élaboration du budget | 6      | F-BUD-004, F-BUD-005, F-BUD-006, F-BUD-007, F-BUD-008, F-BUD-009            |
| EP-BUD-03 | Engagement des dépenses              | 7      | F-BUD-010, F-BUD-011, F-BUD-012, F-BUD-013, F-BUD-014, F-BUD-015, F-BUD-016 |
| EP-BUD-04 | Recettes budgétaires                 | 1      | F-BUD-017                                                                   |
| EP-BUD-05 | Contrôle, suivi et reporting         | 3      | F-BUD-018, F-BUD-019, F-BUD-020                                             |
| EP-BUD-06 | Réallocation des crédits             | 4      | F-BUD-021, F-BUD-022, F-BUD-023, F-BUD-024                                  |

## 2.2 Liste des fonctionnalités

| **ID**    | **Fonctionnalité**                                 | **Epic**  | **Priorité** | **BF couverts**    |
| --------- | -------------------------------------------------- | --------- | ------------ | ------------------ |
| F-BUD-001 | Gestion des exercices budgétaires                  | EP-BUD-01 | Haute        | 001                |
| F-BUD-002 | Nomenclature et plan de codification budgétaire    | EP-BUD-01 | Haute        | 002, 003, 005, 006 |
| F-BUD-003 | Versions budgétaires et comparaison                | EP-BUD-01 | Haute        | 004                |
| F-BUD-004 | Campagne budgétaire et lettre-circulaire           | EP-BUD-02 | Haute        | 007                |
| F-BUD-005 | Groupe de travail budgétaire                       | EP-BUD-02 | Moyenne      | 008                |
| F-BUD-006 | CDMT et Projet de Performance Annuel par programme | EP-BUD-02 | Haute        | 009, 010           |
| F-BUD-007 | Avant-projets des unités opérationnelles           | EP-BUD-02 | Haute        | 011, 013           |
| F-BUD-008 | Consolidation et arbitrages budgétaires            | EP-BUD-02 | Haute        | 012, 014           |
| F-BUD-009 | Approbation et mise en exécution du budget         | EP-BUD-02 | Haute        | 015, 016, 017      |
| F-BUD-010 | Création du bon d'engagement                       | EP-BUD-03 | Haute        | 018, 026, 029      |
| F-BUD-011 | Contrôle budgétaire bloquant                       | EP-BUD-03 | Haute        | 019, 020           |
| F-BUD-012 | Détermination du circuit et pièces obligatoires    | EP-BUD-03 | Haute        | 021, 022, 024      |
| F-BUD-013 | Visa budgétaire et signature                       | EP-BUD-03 | Haute        | 023                |
| F-BUD-014 | Bon de commande lié à l'engagement                 | EP-BUD-03 | Haute        | 025                |
| F-BUD-015 | Circuits spécifiques : missions, véhicules, études | EP-BUD-03 | Haute        | 027                |
| F-BUD-016 | Annulation et réduction d'un engagement            | EP-BUD-03 | Haute        | 028                |
| F-BUD-017 | Suivi et rapprochement des recettes                | EP-BUD-04 | Haute        | 030, 031, 032      |
| F-BUD-018 | Contrôle a priori des pièces                       | EP-BUD-05 | Haute        | 033                |
| F-BUD-019 | Situation d'exécution en temps réel                | EP-BUD-05 | Haute        | 034, 035, 039      |
| F-BUD-020 | Rapports mensuels et trimestriels                  | EP-BUD-05 | Haute        | 036, 037, 038      |
| F-BUD-021 | Demande de réallocation                            | EP-BUD-06 | Haute        | 040                |
| F-BUD-022 | Analyse et classification de la réallocation       | EP-BUD-06 | Haute        | 041, 042           |
| F-BUD-023 | Circuit de décision de la réallocation             | EP-BUD-06 | Haute        | 043, 044, 045      |
| F-BUD-024 | Exécution et suivi des réallocations               | EP-BUD-06 | Haute        | 046, 047           |

_Les numéros de la colonne « BF couverts » sont abrégés (ex. 012 = BF-BUD-012)._

# 3. Fiches des fonctionnalités

## 3.1 EP-BUD-01 — Structure et nomenclature budgétaire

### F-BUD-001 — Gestion des exercices budgétaires

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                     |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | Chef Service du Budget, administrateur fonctionnel                                                                                                                                                                                              |
| Priorité          | Haute                                                                                                                                                                                                                                           |
| Besoins couverts  | BF-BUD-001 — Gérer les exercices budgétaires (ouverture, préparation, exécution, clôture) du 1er janvier au 31 décembre.                                                                                                                        |
| Règles de gestion | RG-BUD-001 — Le budget couvre la période du 1er janvier au 31 décembre de l'année N.<br><br>RG-BUD-002 — Le Conseil d'Administration est l'unique instance d'approbation du budget ; le budget n'est exécutoire qu'après décision signée du DG. |

**User story :** En tant que chef du budget, je veux ouvrir, faire évoluer et clôturer les exercices budgétaires, afin que chaque opération soit rattachée au bon exercice (1er janvier – 31 décembre).

**Comportements attendus :**

- Créer un exercice N+1 au statut « En préparation ».
- Passer l'exercice à « Voté » puis « Exécutoire » à la décision du DG ; seul un exercice exécutoire accepte des engagements.
- Clôturer l'exercice et interdire toute nouvelle saisie.

### F-BUD-002 — Nomenclature et plan de codification budgétaire

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | CSB, SDFBM, DPD, DAF, DG                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| Besoins couverts  | BF-BUD-002 — Gérer la nomenclature programme → action → activité → tâche → ligne budgétaire, avec rattachement aux unités et centres de responsabilité.<br><br>BF-BUD-003 — Importer et versionner le plan de codification budgétaire signé par le DG.<br><br>BF-BUD-005 — Distinguer les natures : charges de fonctionnement, produits, activités, investissements.<br><br>BF-BUD-006 — Rattacher chaque ligne à une source de financement (ressources propres, subvention de l'État, partenaire). |
| Règles de gestion | —                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |

**User story :** En tant que chef du budget, je veux gérer la nomenclature programme → action → activité → tâche → ligne et importer le plan de codification signé, afin que toutes les imputations utilisent les mêmes codes.

**Comportements attendus :**

- Créer et rattacher programmes, actions, activités, tâches et lignes aux unités et centres de responsabilité.
- Qualifier chaque ligne par nature (fonctionnement, produits, activités, investissements) et source de financement.
- Importer le plan de codification signé et en conserver les versions.

### F-BUD-003 — Versions budgétaires et comparaison

| **Rubrique**      | **Contenu**                                                                                                                     |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | CSB, DAF                                                                                                                        |
| Priorité          | Haute                                                                                                                           |
| Besoins couverts  | BF-BUD-004 — Gérer plusieurs versions budgétaires par exercice (avant-projet, projet, voté, rectifié) et comparer les versions. |
| Règles de gestion | —                                                                                                                               |

**User story :** En tant que chef du budget, je veux gérer plusieurs versions (avant-projet, projet, voté, rectifié) et les comparer, afin de suivre l'évolution du budget.

**Comportements attendus :**

- Créer une version à partir d'une version existante.
- Comparer deux versions ligne par ligne avec écarts en valeur et en pourcentage.
- Conserver chaque version sans écrasement.

## 3.2 EP-BUD-02 — Préparation et élaboration du budget

### F-BUD-004 — Campagne budgétaire et lettre-circulaire

| **Rubrique**      | **Contenu**                                                                                                                  |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | DPD/CSPP, DG, Secrétariat DG                                                                                                 |
| Priorité          | Haute                                                                                                                        |
| Besoins couverts  | BF-BUD-007 — Créer la campagne budgétaire et générer la lettre-circulaire à partir d'un modèle, avec chronogramme et jalons. |
| Règles de gestion | —                                                                                                                            |

**User story :** En tant que chef de la DPD, je veux lancer la campagne budgétaire avec la lettre-circulaire et le chronogramme, afin que tous les services connaissent les orientations et les jalons.

**Comportements attendus :**

- Générer la lettre-circulaire à partir d'un modèle et la soumettre à la signature du DG.
- Définir le chronogramme et ses jalons ; alerter les responsables avant chaque échéance.
- Diffuser la lettre signée aux services.

### F-BUD-005 — Groupe de travail budgétaire

| **Rubrique**      | **Contenu**                                                                                                                             |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | DPD, DG, membres du groupe de travail                                                                                                   |
| Priorité          | Moyenne                                                                                                                                 |
| Besoins couverts  | BF-BUD-008 — Enregistrer la décision de création du groupe de travail et ses membres ; gérer convocations, présences et comptes rendus. |
| Règles de gestion | —                                                                                                                                       |

**User story :** En tant que chef de la DPD, je veux enregistrer la décision de création du groupe de travail, ses membres et ses réunions, afin de tracer la concertation.

**Comportements attendus :**

- Enregistrer la décision et la liste des membres.
- Gérer convocations, feuilles de présence et comptes rendus.

### F-BUD-006 — CDMT et Projet de Performance Annuel par programme

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                       |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | Chefs de programme, CSPP/CEA, groupe de travail                                                                                                                                                                                                                   |
| Priorité          | Haute                                                                                                                                                                                                                                                             |
| Besoins couverts  | BF-BUD-009 — Permettre la saisie et la révision du CDMT par programme sur trois ans avec indicateurs, cibles et coûts.<br><br>BF-BUD-010 — Permettre à chaque chef de programme de saisir son PPA (stratégie, objectifs, activités, indicateurs, cibles, moyens). |
| Règles de gestion | —                                                                                                                                                                                                                                                                 |

**User story :** En tant que chef de programme, je veux saisir et réviser le CDMT sur trois ans et le PPA de mon programme, afin de décliner la stratégie en activités, indicateurs et cibles.

**Comportements attendus :**

- Saisir le CDMT (années N+1 à N+3) avec activités, indicateurs, cibles et coûts.
- Saisir le PPA : stratégie, objectifs, activités, indicateurs, cibles, moyens.
- Soumettre au groupe de travail pour validation.

### F-BUD-007 — Avant-projets des unités opérationnelles

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                      |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | Chefs d'unités, CSAFC                                                                                                                                                                                                                                                                                                            |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                            |
| Besoins couverts  | BF-BUD-011 — Permettre à chaque unité opérationnelle de saisir ses avant-projets (fonctionnement, produits, activités, investissements) et d'y joindre les éléments de maturation (TdR).<br><br>BF-BUD-013 — Pré-remplir les propositions à partir de l'historique des ventes, de l'exécution N-1 et des activités à reconduire. |
| Règles de gestion | —                                                                                                                                                                                                                                                                                                                                |

**User story :** En tant que chef d'unité, je veux saisir les avant-projets de mon unité et y joindre les éléments de maturation, afin qu'ils soient consolidés au siège.

**Comportements attendus :**

- Saisir les budgets de fonctionnement, produits, activités et investissements.
- Joindre les TdR et éléments de maturation.
- Pré-remplir à partir de l'historique des ventes, de l'exécution N-1 et des activités reconduites.

### F-BUD-008 — Consolidation et arbitrages budgétaires

| **Rubrique**      | **Contenu**                                                                                                                                                                                                    |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | Service du Budget, groupe de travail, chefs de programme                                                                                                                                                       |
| Priorité          | Haute                                                                                                                                                                                                          |
| Besoins couverts  | BF-BUD-012 — Consolider automatiquement CDMT, PPA et avant-projets au niveau SODEPA.<br><br>BF-BUD-014 — Gérer les arbitrages (débats budgétaires) avec historique des montants proposés, arbitrés et retenus. |
| Règles de gestion | —                                                                                                                                                                                                              |

**User story :** En tant que membre du groupe de travail, je veux consolider automatiquement CDMT, PPA et avant-projets et tracer les arbitrages, afin de produire un projet de budget global cohérent.

**Comportements attendus :**

- Consolider automatiquement au niveau SODEPA par programme, unité et nature.
- Enregistrer pour chaque ligne les montants proposés, arbitrés et retenus avec commentaire.

### F-BUD-009 — Approbation et mise en exécution du budget

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                                                                                              |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Acteurs           | Groupe de travail, DG, CA, Service du Budget                                                                                                                                                                                                                                                                                                                                                                             |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                                                                                    |
| Besoins couverts  | BF-BUD-015 — Faire suivre au projet de budget le circuit : GT → DG (validation interne) → CA (approbation) → décision exécutoire.<br><br>BF-BUD-016 — Verrouiller le budget voté et le rendre seul ouvert à l'engagement après la décision exécutoire.<br><br>BF-BUD-017 — Produire les annexes : budget d'exploitation, budget d'investissement, plan d'opérations en capital, plan de trésorerie, plan de financement. |
| Règles de gestion | RG-BUD-002 — Le Conseil d'Administration est l'unique instance d'approbation du budget ; le budget n'est exécutoire qu'après décision signée du DG.                                                                                                                                                                                                                                                                      |

**User story :** En tant que DG, je veux faire valider le projet de budget en interne, le soumettre au CA puis le rendre exécutoire, afin que seul le budget approuvé soit ouvert à l'engagement.

**Comportements attendus :**

- Faire suivre le circuit GT → DG → CA → décision exécutoire, avec pièces (résolution, décision).
- Verrouiller la version votée ; aucune modification hors réallocation.
- Ouvrir les crédits à l'engagement dès la décision exécutoire.
- Produire les annexes : budget d'exploitation, d'investissement, plan d'opérations en capital, de trésorerie et de financement.

## 3.3 EP-BUD-03 — Engagement des dépenses

### F-BUD-010 — Création du bon d'engagement

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB (siège), CSAFC (unité)                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Besoins couverts  | BF-BUD-018 — Créer un bon d'engagement à partir d'une expression de besoin, d'un TdR, d'un ordre de mission, d'une fiche d'entretien, d'un état de paie ou d'un contrat.<br><br>BF-BUD-026 — Gérer les engagements non liés aux achats : dépenses de personnel, TdR d'activités, contrats, baux, conventions, indemnités des administrateurs.<br><br>BF-BUD-029 — Numéroter automatiquement et de façon unique les bons d'engagement par exercice et par unité. |
| Règles de gestion | RG-BUD-003 — Tout acte de gestion ayant une incidence financière sur le budget doit être engagé avant paiement.<br><br>RG-BUD-011 — L'engagement couvre aussi les dépenses de personnel, les TdR (montant global de l'activité) et les dépenses diverses (contrats, baux, conventions, indemnités de session).                                                                                                                                                  |

**User story :** En tant qu'agent du Service du Budget, je veux créer un bon d'engagement à partir de la pièce d'origine (besoin, TdR, OM, fiche d'entretien, état de paie, contrat), afin d'engager la dépense avant tout paiement.

**Comportements attendus :**

- Créer le BE en reprenant les données de la pièce d'origine.
- Attribuer un numéro unique par exercice et par unité.
- Gérer aussi les engagements non liés aux achats : personnel, TdR d'activités, contrats, baux, conventions, indemnités.

### F-BUD-011 — Contrôle budgétaire bloquant

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                               |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB, CSAFC                                                                                                                                                                                                                                                 |
| Priorité          | Haute                                                                                                                                                                                                                                                     |
| Besoins couverts  | BF-BUD-019 — Contrôler automatiquement à la saisie l'inscription de la dépense au budget voté, l'exactitude de l'imputation et la disponibilité du crédit.<br><br>BF-BUD-020 — Bloquer tout engagement dont le montant dépasse le disponible de la ligne. |
| Règles de gestion | RG-BUD-004 — Aucune dépense ne peut être engagée sans crédit disponible (régularité budgétaire).<br><br>RG-BUD-005 — Avant engagement, le SB vérifie : inscription au budget approuvé, exactitude de l'imputation, conformité et régularité des pièces.   |

**User story :** En tant que chef du budget, je veux que l'ERP contrôle automatiquement l'inscription au budget, l'imputation et la disponibilité des crédits, afin qu'aucune dépense ne soit engagée sans crédit.

**Comportements attendus :**

- Vérifier que la ligne existe dans le budget exécutoire et que l'imputation est cohérente avec la nature de la dépense.
- Bloquer l'engagement dont le montant dépasse le disponible de la ligne.
- Afficher le disponible avant et après engagement.

### F-BUD-012 — Détermination du circuit et pièces obligatoires

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB, CSAFC, DG, DUO                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Besoins couverts  | BF-BUD-021 — Déterminer automatiquement le circuit selon le montant TTC : menue dépense (< 500 000 FCFA) ou grosse dépense (≥ 500 000 FCFA, bon de commande obligatoire).<br><br>BF-BUD-022 — Différencier les circuits Direction Générale et Unités opérationnelles (signataires DG ou DUO).<br><br>BF-BUD-024 — Exiger les pièces obligatoires selon le type de dépense (expression de besoin, attestation de carence, pro forma, dossier fiscal, TdR).                                                                                                                                                                          |
| Règles de gestion | RG-BUD-006 — Une dépense de 0 à 499 999 FCFA TTC est une menue dépense : bon d'engagement seul.<br><br>RG-BUD-007 — Une dépense ≥ 500 000 FCFA TTC est une grosse dépense : bon d'engagement et bon de commande.<br><br>RG-BUD-008 — En unité, pour une grosse dépense, trois fournisseurs sont consultés et la commande est attribuée à la meilleure offre financière.<br><br>RG-BUD-009 — Une attestation de carence du stock (magasinier/CSAFC ou SMP) accompagne toute demande d'acquisition.<br><br>RG-BUD-010 — Le dossier fiscal du prestataire (patente, carte de contribuable, RCCM) est exigé pour les grosses dépenses. |

**User story :** En tant qu'agent du budget, je veux que le circuit (menue ou grosse dépense, DG ou unité) et les pièces obligatoires soient déterminés automatiquement, afin d'appliquer la procédure sans erreur.

**Comportements attendus :**

- Classer en menue dépense (< 500 000 FCFA TTC) ou grosse dépense (≥ 500 000 FCFA TTC).
- Router vers les signataires du siège (SB → SDFBM → DAF → DG) ou de l'unité (CSAFC → DUO).
- Exiger les pièces : expression de besoin, attestation de carence, pro forma, dossier fiscal, TdR ; 3 pro forma en unité pour une grosse dépense.

### F-BUD-013 — Visa budgétaire et signature

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                                              |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Acteurs           | SB, SDFBM, DAF, DG, DUO                                                                                                                                                                                                                                                                                                                                                  |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                                    |
| Besoins couverts  | BF-BUD-023 — Appliquer un visa budgétaire électronique et tracer le signataire, la date et le montant visé.                                                                                                                                                                                                                                                              |
| Règles de gestion | RG-BUD-012 — Le SB conserve une copie de chaque bon d'engagement signé et transmet les originaux au SMP.<br><br>RG-BUD-021 — Un demandeur ne peut pas valider son propre engagement ni sa propre demande de réallocation.<br><br>RG-BUD-022 — Un bon d'engagement signé ne peut être modifié ; toute correction passe par une annulation tracée et un nouvel engagement. |

**User story :** En tant que signataire, je veux viser et signer électroniquement le bon d'engagement, afin que l'engagement soit valide et tracé.

**Comportements attendus :**

- Apposer le visa budgétaire en traçant signataire, date et montant.
- Interdire au demandeur de valider son propre engagement.
- Rendre le BE non modifiable après signature.
- Archiver une copie et transmettre l'original au SMP.

### F-BUD-014 — Bon de commande lié à l'engagement

| **Rubrique**      | **Contenu**                                                                                                                    |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Acteurs           | SB, CSAFC, module Marchés                                                                                                      |
| Priorité          | Haute                                                                                                                          |
| Besoins couverts  | BF-BUD-025 — Générer le bon de commande lié au bon d'engagement pour les grosses dépenses et le transmettre au module Marchés. |
| Règles de gestion | RG-BUD-007 — Une dépense ≥ 500 000 FCFA TTC est une grosse dépense : bon d'engagement et bon de commande.                      |

**User story :** En tant qu'agent du budget, je veux générer le bon de commande lié au bon d'engagement pour les grosses dépenses, afin de le transmettre au prestataire et au module Marchés.

**Comportements attendus :**

- Générer le BC à partir du BE pour toute dépense ≥ 500 000 FCFA TTC.
- Transmettre BE et BC au module Marchés et le BC au prestataire.

### F-BUD-015 — Circuits spécifiques : missions, véhicules, études

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                      |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB, logisticien, comptable-matières, CBM, service initiateur, DG                                                                                                                                                                 |
| Priorité          | Haute                                                                                                                                                                                                                            |
| Besoins couverts  | BF-BUD-027 — Gérer les cas types missions, entretien de véhicules (commerciaux et de service) et études/consultants avec leurs circuits propres.                                                                                 |
| Règles de gestion | RG-BUD-003 — Tout acte de gestion ayant une incidence financière sur le budget doit être engagé avant paiement.<br><br>RG-BUD-007 — Une dépense ≥ 500 000 FCFA TTC est une grosse dépense : bon d'engagement et bon de commande. |

**User story :** En tant qu'agent du budget, je veux appliquer les circuits propres aux missions, à l'entretien des véhicules et aux études, afin de respecter les cas pratiques de la procédure.

**Comportements attendus :**

- Missions : TdR + note de service + OM → visa → BE → transmission au SFR.
- Véhicules : fiche d'entretien → diagnostic Brigade de Maintenance → visa → autorisation DG → BE (+ BC si > 499 999 FCFA).
- Études : TdR → vérification de ligne et visa → autorisation DG → BE (+ BC) → transmission au CSFR.

### F-BUD-016 — Annulation et réduction d'un engagement

| **Rubrique**      | **Contenu**                                                                                                                             |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB, DG                                                                                                                                  |
| Priorité          | Haute                                                                                                                                   |
| Besoins couverts  | BF-BUD-028 — Annuler ou réduire un engagement avec motif, et restituer le crédit correspondant.                                         |
| Règles de gestion | RG-BUD-022 — Un bon d'engagement signé ne peut être modifié ; toute correction passe par une annulation tracée et un nouvel engagement. |

**User story :** En tant que chef du budget, je veux annuler ou réduire un engagement avec motif, afin de restituer le crédit non utilisé.

**Comportements attendus :**

- Annuler ou réduire un BE avec motif obligatoire et validation.
- Restituer automatiquement le crédit à la ligne.
- Conserver l'engagement initial et la trace de l'annulation.

## 3.4 EP-BUD-04 — Recettes budgétaires

### F-BUD-017 — Suivi et rapprochement des recettes

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                               |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB, CSAFC                                                                                                                                                                                                                                                                                                                                                 |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                     |
| Besoins couverts  | BF-BUD-030 — Suivre les recettes par nature (ventes de produits, ventes de services, subventions) et par unité.<br><br>BF-BUD-031 — Rapprocher les recettes budgétaires, les encaissements (module Finance) et la comptabilité.<br><br>BF-BUD-032 — Produire la fiche de suivi mensuel des recettes et le taux de réalisation par rapport aux prévisions. |
| Règles de gestion | —                                                                                                                                                                                                                                                                                                                                                         |

**User story :** En tant que chef du budget, je veux suivre les recettes par nature et par unité et les rapprocher des encaissements et de la comptabilité, afin de mesurer le taux de réalisation des prévisions.

**Comportements attendus :**

- Alimenter automatiquement les recettes réalisées depuis le module Finance.
- Rapprocher budget, encaissements et comptabilité et lister les écarts.
- Produire la fiche de suivi mensuel et le taux de réalisation.

## 3.5 EP-BUD-05 — Contrôle, suivi et reporting

### F-BUD-018 — Contrôle a priori des pièces

| **Rubrique**      | **Contenu**                                                                                                                                     |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB                                                                                                                                              |
| Priorité          | Haute                                                                                                                                           |
| Besoins couverts  | BF-BUD-033 — Fournir une liste de contrôle a priori paramétrable par type de dépense, à valider avant signature.                                |
| Règles de gestion | RG-BUD-005 — Avant engagement, le SB vérifie : inscription au budget approuvé, exactitude de l'imputation, conformité et régularité des pièces. |

**User story :** En tant qu'agent du budget, je veux une liste de contrôle paramétrable par type de dépense à valider avant signature, afin de vérifier la régularité des pièces.

**Comportements attendus :**

- Afficher la liste des contrôles applicables (expression de besoin/TdR, contrats, BE, carence, BC, pro forma, facture, BL, attestation ARMP, dossier fiscal).
- Bloquer la soumission tant que les contrôles obligatoires ne sont pas cochés.

### F-BUD-019 — Situation d'exécution en temps réel

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                                                 |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB, DAF, DG, chefs de programme, DUO                                                                                                                                                                                                                                                                                                                                        |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                                       |
| Besoins couverts  | BF-BUD-034 — Mettre à jour en temps réel la situation d'exécution : crédit voté, engagé, liquidé, payé, disponible.<br><br>BF-BUD-035 — Consulter l'exécution par programme, action, activité, ligne, unité, nature et source de financement.<br><br>BF-BUD-039 — Alerter le gestionnaire lorsqu'une ligne atteint un seuil de consommation paramétrable (ex. 80 %, 100 %). |
| Règles de gestion | —                                                                                                                                                                                                                                                                                                                                                                           |

**User story :** En tant que responsable, je veux consulter à tout moment le voté, l'engagé, le liquidé, le payé et le disponible selon tous les axes, afin de piloter l'exécution.

**Comportements attendus :**

- Mettre à jour la situation à chaque événement (engagement, liquidation, paiement, réallocation).
- Consulter par programme, action, activité, ligne, unité, nature et source de financement.
- Alerter lorsqu'une ligne atteint 80 % ou 100 % de consommation.

### F-BUD-020 — Rapports mensuels et trimestriels

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                                      |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB, DG, DUO                                                                                                                                                                                                                                                                                                                                                      |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                            |
| Besoins couverts  | BF-BUD-036 — Générer automatiquement le rapport mensuel d'exécution (dépassements, économies, niveau d'engagement en valeur et en nombre).<br><br>BF-BUD-037 — Permettre la saisie des explications et justificatifs des dépassements et des économies.<br><br>BF-BUD-038 — Générer le rapport trimestriel de performance (exécution financière et indicateurs). |
| Règles de gestion | RG-BUD-013 — Le rapport mensuel d'exécution est produit au plus tard le 05 du mois pour les unités et le 10 du mois pour la Direction Générale.                                                                                                                                                                                                                  |

**User story :** En tant que chef du budget, je veux générer automatiquement les rapports mensuels et trimestriels et y saisir les justifications des écarts, afin de respecter les échéances du 05 et du 10 du mois.

**Comportements attendus :**

- Générer le rapport mensuel par unité (le 05) et pour la DG (le 10) : dépassements, économies, niveau d'engagement en valeur et en nombre.
- Saisir les explications et justificatifs des dépassements et des économies.
- Générer le rapport trimestriel de performance (financier et indicateurs).

## 3.6 EP-BUD-06 — Réallocation des crédits

### F-BUD-021 — Demande de réallocation

| **Rubrique**      | **Contenu**                                                                                                                                       |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | Direction, unité ou service demandeur                                                                                                             |
| Priorité          | Haute                                                                                                                                             |
| Besoins couverts  | BF-BUD-040 — Saisir une fiche de demande de réallocation avec toutes les rubriques exigées et les pièces justificatives.                          |
| Règles de gestion | RG-BUD-019 — Toute réallocation laisse une trace : demande, analyse, décision, mise à jour budgétaire, imputation comptable, rapport d'exécution. |

**User story :** En tant que demandeur, je veux saisir une fiche de demande de réallocation complète, afin de solliciter le transfert de crédits vers une ligne prioritaire.

**Comportements attendus :**

- Saisir objet, ligne à renforcer, ligne à diminuer, montant, justification, urgence, impact, risques, pièces.
- Bloquer la soumission si une rubrique obligatoire manque.

### F-BUD-022 — Analyse et classification de la réallocation

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | DAF/SDFBM/SB                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Besoins couverts  | BF-BUD-041 — Classer automatiquement la demande (simple, significative, exceptionnelle) selon les lignes, unités, natures et montants concernés.<br><br>BF-BUD-042 — Contrôler la disponibilité du crédit à prélever et l'absence d'engagement sur la ligne source.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Règles de gestion | RG-BUD-014 — Une réallocation ne doit ni créer de tension de trésorerie, ni prélever sur une ligne déjà engagée, ni masquer une insuffisance structurelle.<br><br>RG-BUD-015 — Réallocation simple (même direction, même nature, sans modification du global ni du plan de passation) : proposée par la DAF, autorisée par le DG.<br><br>RG-BUD-016 — Réallocation entre fonctionnement et investissement, entre unités, entre projets stratégiques, ou affectant subventions, emprunts, trésorerie ou budget voté : note motivée au DG et résolution du CA si l'incidence est significative.<br><br>RG-BUD-018 — Seuils recommandés : ≤ 5 % de la ligne → DAF + DG ; > 5 % ou plusieurs directions → DAF + comité + DG ; investissement, endettement, subventions, PPA ou équilibre général → CA. |

**User story :** En tant qu'analyste de la DAF, je veux que la demande soit classée automatiquement et que la disponibilité de la ligne source soit contrôlée, afin de déterminer le circuit applicable.

**Comportements attendus :**

- Classer en simple, significative ou exceptionnelle selon lignes, unités, natures et montant (≤ 5 %, > 5 %, investissement/CA).
- Contrôler le disponible de la ligne source et l'absence d'engagement sur cette ligne.
- Produire la note technique et financière (impact trésorerie, plan de passation, performance).

### F-BUD-023 — Circuit de décision de la réallocation

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | Comité de réallocation, DG, CA, tutelle                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| Priorité          | Haute                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| Besoins couverts  | BF-BUD-043 — Router la demande vers l'autorité compétente (DAF+DG, comité, CA, tutelle) selon le niveau.<br><br>BF-BUD-044 — Enregistrer l'avis du comité de réallocation et la décision du DG ; générer la décision interne de réallocation à partir du modèle.<br><br>BF-BUD-045 — Gérer la mesure conservatoire du DG en cas d'urgence, avec obligation de régularisation devant le CA.                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Règles de gestion | RG-BUD-016 — Réallocation entre fonctionnement et investissement, entre unités, entre projets stratégiques, ou affectant subventions, emprunts, trésorerie ou budget voté : note motivée au DG et résolution du CA si l'incidence est significative.<br><br>RG-BUD-017 — En urgence (panne grave, urgence sanitaire/vétérinaire, continuité de service, obligation réglementaire), le DG peut prendre une mesure conservatoire, régularisée devant le CA si elle dépasse son seuil de compétence.<br><br>RG-BUD-018 — Seuils recommandés : ≤ 5 % de la ligne → DAF + DG ; > 5 % ou plusieurs directions → DAF + comité + DG ; investissement, endettement, subventions, PPA ou équilibre général → CA.<br><br>RG-BUD-021 — Un demandeur ne peut pas valider son propre engagement ni sa propre demande de réallocation. |

**User story :** En tant que DG, je veux que la demande arrive à l'autorité compétente avec l'avis du comité, afin de décider en connaissance de cause.

**Comportements attendus :**

- Router vers DAF + DG, comité + DG ou CA (et tutelle) selon le niveau.
- Enregistrer l'avis du comité et la décision du DG (approuver, rejeter, demander des compléments, saisir le CA).
- Générer la décision interne de réallocation à partir du modèle.
- Gérer la mesure conservatoire d'urgence avec statut « à régulariser CA ».

### F-BUD-024 — Exécution et suivi des réallocations

| **Rubrique**      | **Contenu**                                                                                                                                                                                                                                             |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Acteurs           | SB                                                                                                                                                                                                                                                      |
| Priorité          | Haute                                                                                                                                                                                                                                                   |
| Besoins couverts  | BF-BUD-046 — Mettre à jour automatiquement les crédits des lignes source et cible après décision et produire le tableau budgétaire rectifié.<br><br>BF-BUD-047 — Produire un état consolidé des réallocations de l'exercice.                            |
| Règles de gestion | RG-BUD-019 — Toute réallocation laisse une trace : demande, analyse, décision, mise à jour budgétaire, imputation comptable, rapport d'exécution.<br><br>RG-BUD-020 — Aucune dépense n'est exécutée avant validation de la réallocation qui la finance. |

**User story :** En tant que chef du budget, je veux que les crédits soient mis à jour automatiquement après décision et que toutes les réallocations soient consolidées, afin de garder une trace complète.

**Comportements attendus :**

- Diminuer la ligne source et augmenter la ligne cible uniquement après décision validée.
- Produire le tableau budgétaire rectifié.
- Produire l'état consolidé des réallocations de l'exercice.
- Interdire l'engagement d'une dépense financée par une réallocation non validée.

# 4. Matrice de traçabilité besoins → fonctionnalités

| **Besoin** | **Libellé**                                                                                                                                                                 | **Priorité** | **Fonctionnalité(s)** |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ | --------------------- |
| BF-BUD-001 | Gérer les exercices budgétaires (ouverture, préparation, exécution, clôture) du 1er janvier au 31 décembre.                                                                 | Haute        | F-BUD-001             |
| BF-BUD-002 | Gérer la nomenclature programme → action → activité → tâche → ligne budgétaire, avec rattachement aux unités et centres de responsabilité.                                  | Haute        | F-BUD-002             |
| BF-BUD-003 | Importer et versionner le plan de codification budgétaire signé par le DG.                                                                                                  | Haute        | F-BUD-002             |
| BF-BUD-004 | Gérer plusieurs versions budgétaires par exercice (avant-projet, projet, voté, rectifié) et comparer les versions.                                                          | Haute        | F-BUD-003             |
| BF-BUD-005 | Distinguer les natures : charges de fonctionnement, produits, activités, investissements.                                                                                   | Haute        | F-BUD-002             |
| BF-BUD-006 | Rattacher chaque ligne à une source de financement (ressources propres, subvention de l'État, partenaire).                                                                  | Moyenne      | F-BUD-002             |
| BF-BUD-007 | Créer la campagne budgétaire et générer la lettre-circulaire à partir d'un modèle, avec chronogramme et jalons.                                                             | Haute        | F-BUD-004             |
| BF-BUD-008 | Enregistrer la décision de création du groupe de travail et ses membres ; gérer convocations, présences et comptes rendus.                                                  | Moyenne      | F-BUD-005             |
| BF-BUD-009 | Permettre la saisie et la révision du CDMT par programme sur trois ans avec indicateurs, cibles et coûts.                                                                   | Haute        | F-BUD-006             |
| BF-BUD-010 | Permettre à chaque chef de programme de saisir son PPA (stratégie, objectifs, activités, indicateurs, cibles, moyens).                                                      | Haute        | F-BUD-006             |
| BF-BUD-011 | Permettre à chaque unité opérationnelle de saisir ses avant-projets (fonctionnement, produits, activités, investissements) et d'y joindre les éléments de maturation (TdR). | Haute        | F-BUD-007             |
| BF-BUD-012 | Consolider automatiquement CDMT, PPA et avant-projets au niveau SODEPA.                                                                                                     | Haute        | F-BUD-008             |
| BF-BUD-013 | Pré-remplir les propositions à partir de l'historique des ventes, de l'exécution N-1 et des activités à reconduire.                                                         | Moyenne      | F-BUD-007             |
| BF-BUD-014 | Gérer les arbitrages (débats budgétaires) avec historique des montants proposés, arbitrés et retenus.                                                                       | Haute        | F-BUD-008             |
| BF-BUD-015 | Faire suivre au projet de budget le circuit : GT → DG (validation interne) → CA (approbation) → décision exécutoire.                                                        | Haute        | F-BUD-009             |
| BF-BUD-016 | Verrouiller le budget voté et le rendre seul ouvert à l'engagement après la décision exécutoire.                                                                            | Haute        | F-BUD-009             |
| BF-BUD-017 | Produire les annexes : budget d'exploitation, budget d'investissement, plan d'opérations en capital, plan de trésorerie, plan de financement.                               | Moyenne      | F-BUD-009             |
| BF-BUD-018 | Créer un bon d'engagement à partir d'une expression de besoin, d'un TdR, d'un ordre de mission, d'une fiche d'entretien, d'un état de paie ou d'un contrat.                 | Haute        | F-BUD-010             |
| BF-BUD-019 | Contrôler automatiquement à la saisie l'inscription de la dépense au budget voté, l'exactitude de l'imputation et la disponibilité du crédit.                               | Haute        | F-BUD-011             |
| BF-BUD-020 | Bloquer tout engagement dont le montant dépasse le disponible de la ligne.                                                                                                  | Haute        | F-BUD-011             |
| BF-BUD-021 | Déterminer automatiquement le circuit selon le montant TTC : menue dépense (< 500 000 FCFA) ou grosse dépense (≥ 500 000 FCFA, bon de commande obligatoire).                | Haute        | F-BUD-012             |
| BF-BUD-022 | Différencier les circuits Direction Générale et Unités opérationnelles (signataires DG ou DUO).                                                                             | Haute        | F-BUD-012             |
| BF-BUD-023 | Appliquer un visa budgétaire électronique et tracer le signataire, la date et le montant visé.                                                                              | Haute        | F-BUD-013             |
| BF-BUD-024 | Exiger les pièces obligatoires selon le type de dépense (expression de besoin, attestation de carence, pro forma, dossier fiscal, TdR).                                     | Haute        | F-BUD-012             |
| BF-BUD-025 | Générer le bon de commande lié au bon d'engagement pour les grosses dépenses et le transmettre au module Marchés.                                                           | Haute        | F-BUD-014             |
| BF-BUD-026 | Gérer les engagements non liés aux achats : dépenses de personnel, TdR d'activités, contrats, baux, conventions, indemnités des administrateurs.                            | Haute        | F-BUD-010             |
| BF-BUD-027 | Gérer les cas types missions, entretien de véhicules (commerciaux et de service) et études/consultants avec leurs circuits propres.                                         | Haute        | F-BUD-015             |
| BF-BUD-028 | Annuler ou réduire un engagement avec motif, et restituer le crédit correspondant.                                                                                          | Haute        | F-BUD-016             |
| BF-BUD-029 | Numéroter automatiquement et de façon unique les bons d'engagement par exercice et par unité.                                                                               | Haute        | F-BUD-010             |
| BF-BUD-030 | Suivre les recettes par nature (ventes de produits, ventes de services, subventions) et par unité.                                                                          | Haute        | F-BUD-017             |
| BF-BUD-031 | Rapprocher les recettes budgétaires, les encaissements (module Finance) et la comptabilité.                                                                                 | Haute        | F-BUD-017             |
| BF-BUD-032 | Produire la fiche de suivi mensuel des recettes et le taux de réalisation par rapport aux prévisions.                                                                       | Haute        | F-BUD-017             |
| BF-BUD-033 | Fournir une liste de contrôle a priori paramétrable par type de dépense, à valider avant signature.                                                                         | Haute        | F-BUD-018             |
| BF-BUD-034 | Mettre à jour en temps réel la situation d'exécution : crédit voté, engagé, liquidé, payé, disponible.                                                                      | Haute        | F-BUD-019             |
| BF-BUD-035 | Consulter l'exécution par programme, action, activité, ligne, unité, nature et source de financement.                                                                       | Haute        | F-BUD-019             |
| BF-BUD-036 | Générer automatiquement le rapport mensuel d'exécution (dépassements, économies, niveau d'engagement en valeur et en nombre).                                               | Haute        | F-BUD-020             |
| BF-BUD-037 | Permettre la saisie des explications et justificatifs des dépassements et des économies.                                                                                    | Haute        | F-BUD-020             |
| BF-BUD-038 | Générer le rapport trimestriel de performance (exécution financière et indicateurs).                                                                                        | Haute        | F-BUD-020             |
| BF-BUD-039 | Alerter le gestionnaire lorsqu'une ligne atteint un seuil de consommation paramétrable (ex. 80 %, 100 %).                                                                   | Moyenne      | F-BUD-019             |
| BF-BUD-040 | Saisir une fiche de demande de réallocation avec toutes les rubriques exigées et les pièces justificatives.                                                                 | Haute        | F-BUD-021             |
| BF-BUD-041 | Classer automatiquement la demande (simple, significative, exceptionnelle) selon les lignes, unités, natures et montants concernés.                                         | Haute        | F-BUD-022             |
| BF-BUD-042 | Contrôler la disponibilité du crédit à prélever et l'absence d'engagement sur la ligne source.                                                                              | Haute        | F-BUD-022             |
| BF-BUD-043 | Router la demande vers l'autorité compétente (DAF+DG, comité, CA, tutelle) selon le niveau.                                                                                 | Haute        | F-BUD-023             |
| BF-BUD-044 | Enregistrer l'avis du comité de réallocation et la décision du DG ; générer la décision interne de réallocation à partir du modèle.                                         | Haute        | F-BUD-023             |
| BF-BUD-045 | Gérer la mesure conservatoire du DG en cas d'urgence, avec obligation de régularisation devant le CA.                                                                       | Haute        | F-BUD-023             |
| BF-BUD-046 | Mettre à jour automatiquement les crédits des lignes source et cible après décision et produire le tableau budgétaire rectifié.                                             | Haute        | F-BUD-024             |
| BF-BUD-047 | Produire un état consolidé des réallocations de l'exercice.                                                                                                                 | Haute        | F-BUD-024             |

# 5. Validation du document

| **Rôle**                      | **Nom** | **Date** | **Visa** |
| ----------------------------- | ------- | -------- | -------- |
| Propriétaire métier           |         |          |          |
| Chef de projet ERP            |         |          |          |
| Responsable qualité / recette |         |          |          |

Les criteres d'acceptation du module gestion du budget:
**SODEPA**

_Société de Développement et d'Exploitation des Productions Animales_

**PROJET ERP SODEPA**