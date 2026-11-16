# Lot B1 — Socle budgétaire

Objectif du lot : charger le budget approuvé de l'exercice, ouvrir les crédits et disposer d'une situation fiable ligne par ligne. Aucune dépense ne s'exécute avant ce lot.

Conventions, définition de « prêt » et de « terminé » : voir [README](README.md). Référence : [spécification](../specification-fonctionnelle-budget.md).

---

## Exercice budgétaire

### US-BUD-101 — Créer un exercice budgétaire

**En tant que** chef du Service du Budget, **je veux** créer l'exercice N à l'état « en préparation », **afin de** pouvoir y rattacher la nomenclature, les lignes et le budget à charger.

- Fonctionnalité : F-BUD-001 · Règles : RG-BUD-001 · Hypothèses : — · Dépend de : — · Taille : S

```gherkin
Scénario: Création d'un exercice
  Étant donné qu'aucun exercice 2027 n'existe
  Quand le chef du budget crée l'exercice 2027
  Alors une demande de création est soumise au maker-checker
  Et, une fois validée par un second utilisateur, l'exercice 2027 existe à l'état « en préparation »
  Et ses dates sont le 01/01/2027 et le 31/12/2027

Scénario: Exercice en double
  Étant donné que l'exercice 2027 existe
  Quand le chef du budget crée à nouveau l'exercice 2027
  Alors la demande est refusée avec le message « L'exercice 2027 existe déjà »
```

### US-BUD-102 — Enregistrer l'approbation du Conseil d'Administration

**En tant que** chef du Service du Budget, **je veux** enregistrer la résolution du CA qui approuve le budget, **afin que** la version approuvée devienne la base de l'exécution.

- Fonctionnalité : F-BUD-001, F-BUD-003 · Règles : RG-BUD-002 · Hypothèses : — · Dépend de : US-BUD-101, US-BUD-111 · Taille : S

```gherkin
Scénario: Approbation enregistrée
  Étant donné l'exercice 2027 « en préparation » avec une version budgétaire chargée
  Quand le chef du budget enregistre la résolution du CA n° RES-CA-2026-12 du 15/12/2026 et joint le document signé
  Alors l'exercice passe à l'état « approuvé CA »
  Et la version chargée passe à l'état « approuvée CA »
  Et la résolution est archivée dans la GED, liée à l'exercice

Scénario: Résolution manquante
  Quand le chef du budget tente l'approbation sans numéro ni date de résolution
  Alors l'opération est refusée et les champs manquants sont signalés
```

### US-BUD-103 — Rendre le budget exécutoire

**En tant que** Directeur Général, **je veux** signer la décision qui rend le budget exécutoire, **afin que** les crédits s'ouvrent à sa date et que les engagements deviennent possibles.

- Fonctionnalité : F-BUD-001, F-BUD-003 · Règles : RG-BUD-002, RG-BUD-033, RG-BUD-038 · Hypothèses : H-06 · Dépend de : US-BUD-102, US-BUD-113 · Taille : M

```gherkin
Scénario: Décision exécutoire
  Étant donné l'exercice 2027 « approuvé CA »
  Quand le DG valide la décision exécutoire datée du 17/12/2026
  Alors l'exercice passe à l'état « exécutoire »
  Et la version approuvée devient la version exécutoire de l'exercice
  Et chaque ligne reçoit un mouvement d'ouverture égal à son crédit initial, daté du 17/12/2026

Scénario: Une seule version exécutoire
  Étant donné que l'exercice 2027 a déjà une version exécutoire
  Quand on tente de rendre exécutoire une autre version de 2027 sans passer par une rectification
  Alors l'opération est refusée

Scénario: Pas d'engagement avant la décision
  Étant donné l'exercice 2027 « approuvé CA » mais pas encore exécutoire
  Quand un agent du budget tente de viser une dépense sur 2027
  Alors le visa est refusé avec le message « Le budget 2027 n'est pas encore exécutoire »
  # Exception : régime transitoire, voir US-BUD-223
```

---

## Nomenclature budgétaire

### US-BUD-104 — Gérer les programmes et les actions

**En tant que** chef du Service du Budget, **je veux** créer, modifier et désactiver les programmes et leurs actions avec leur code, **afin de** poser les deux premiers niveaux du budget-programme.

- Fonctionnalité : F-BUD-002 · Règles : — · Hypothèses : H-01, H-03 · Dépend de : — · Taille : M

```gherkin
Scénario: Création d'un programme
  Quand le chef du budget crée le programme « P01 — Production animale » avec un responsable
  Alors la création est soumise au maker-checker
  Et, une fois validée, le programme est actif dans le plan de codification courant

Scénario: Action rattachée à un programme
  Étant donné le programme P01 actif
  Quand le chef du budget crée l'action « P01-A01 — Santé du cheptel » rattachée à P01
  Alors l'action apparaît sous P01 dans l'arbre

Scénario: Code en double
  Quand le chef du budget crée une seconde action de code « P01-A01 »
  Alors la création est refusée

Scénario: Désactivation d'un élément utilisé
  Étant donné l'action P01-A01 portée par une ligne d'un exercice exécutoire
  Quand le chef du budget désactive P01-A01
  Alors la désactivation est refusée tant que l'exercice n'est pas clos
```

### US-BUD-105 — Gérer les activités et les tâches avec leurs indicateurs

**En tant que** chef du Service du Budget, **je veux** compléter l'arbre avec les activités et les tâches, leurs indicateurs et leurs cibles, **afin que** chaque crédit se rattache au niveau où la procédure le chiffre.

- Fonctionnalité : F-BUD-002 · Règles : — · Hypothèses : H-01 · Dépend de : US-BUD-104 · Taille : M

```gherkin
Scénario: Tâche avec indicateur
  Étant donné l'action P01-A01
  Quand le chef du budget crée l'activité « P01-A01-01 — Campagne de vaccination » avec l'indicateur « part du cheptel vacciné », cible 95 %
  Et crée sous cette activité la tâche « P01-A01-01-T01 — Achat des vaccins »
  Alors l'arbre affiche les quatre niveaux programme → action → activité → tâche

Scénario: Rattachement impossible au mauvais niveau
  Quand le chef du budget rattache une tâche directement à un programme
  Alors la création est refusée : une tâche se rattache à une activité
```

### US-BUD-106 — Importer et versionner le plan de codification

**En tant que** chef du Service du Budget, **je veux** importer le plan de codification signé par le DG et en conserver chaque version, **afin que** la nomenclature de l'ERP soit celle de la SODEPA.

- Fonctionnalité : F-BUD-002 · Règles : RG-BUD-034 · Hypothèses : H-01, H-03 · Dépend de : US-BUD-104, US-BUD-105 · Taille : M

```gherkin
Scénario: Import d'un plan valide
  Étant donné un fichier Excel au modèle fourni par l'ERP
  Quand le chef du budget l'importe avec la référence de signature et la date d'effet du 01/01/2027
  Alors un rapport liste les éléments créés, modifiés et désactivés
  Et après validation par un second utilisateur, la version 2027 du plan devient active à sa date d'effet

Scénario: Fichier en erreur
  Étant donné un fichier où une tâche référence une activité inconnue
  Quand le chef du budget l'importe
  Alors rien n'est chargé
  Et le rapport indique, ligne par ligne, chaque erreur et sa cause

Scénario: Historique
  Quand un utilisateur consulte le plan de codification
  Alors il peut choisir la version 2026 ou 2027 et voir les différences
```

---

## Structures, natures et lignes

### US-BUD-107 — Déclarer les structures budgétaires

**En tant que** chef du Service du Budget, **je veux** désigner les unités de l'organigramme qui portent un budget, avec leur ordonnateur et leur service d'engagement, **afin que** chaque dépense soit routée vers les bons signataires.

- Fonctionnalité : F-BUD-025 · Règles : — · Hypothèses : H-14 · Dépend de : — · Taille : S

```gherkin
Scénario: Direction Générale
  Quand le chef du budget déclare la Direction Générale comme structure budgétaire
  Alors son ordonnateur est le profil DG et son service d'engagement le Service du Budget

Scénario: Unité opérationnelle
  Quand le chef du budget déclare le ranch de Ndokayo comme structure budgétaire
  Alors son ordonnateur est le DUO du ranch et son service d'engagement le CSAFC du ranch

Scénario: Unité inexistante
  Quand le chef du budget saisit une structure qui n'est pas une unité de l'organigramme
  Alors la saisie est impossible : la structure se choisit dans l'organigramme
```

### US-BUD-108 — Gérer les natures et les sources de financement

**En tant que** chef du Service du Budget, **je veux** gérer les natures (section, compte SYSCOHADA, sens dépense ou recette) et les sources de financement, **afin de** qualifier chaque ligne.

- Fonctionnalité : F-BUD-026 · Règles : — · Hypothèses : H-30 · Dépend de : — · Taille : S

```gherkin
Scénario: Nature de produits
  Quand le chef du budget crée la nature « Ventes de bovins », section produits, compte 7011, sens recette
  Alors, après validation maker-checker, la nature est disponible pour les lignes de produits

Scénario: Compte inconnu
  Quand le chef du budget saisit un compte absent du plan comptable
  Alors la création est refusée
```

### US-BUD-109 — Créer et consulter les lignes budgétaires

**En tant que** chef du Service du Budget, **je veux** créer les lignes d'un exercice par structure, élément du programme et nature, et les consulter avec leurs montants, **afin de** contrôler les crédits au bon niveau.

- Fonctionnalité : F-BUD-026 · Règles : RG-BUD-024, RG-BUD-033 · Hypothèses : H-01 · Dépend de : US-BUD-101, 104, 105, 107, 108 · Taille : M

```gherkin
Scénario: Création d'une ligne
  Étant donné l'exercice 2027 en préparation
  Quand le chef du budget crée la ligne « ranch de Ndokayo × P01-A01-01-T01 × produits vétérinaires », source ressources propres, crédit 10 000 000
  Alors la ligne existe dans la version en préparation de 2027

Scénario: Ligne en double
  Quand le chef du budget crée une seconde ligne avec la même structure, le même élément et la même nature en 2027
  Alors la création est refusée

Scénario: Lecture d'une ligne
  Étant donné une ligne exécutoire
  Quand un utilisateur habilité l'ouvre
  Alors il voit crédit initial, crédit ouvert, mis de côté, engagé, liquidé, payé, disponible
  Et le disponible vaut crédit ouvert − mis de côté − engagé

Scénario: Ligne verrouillée
  Étant donné l'exercice 2027 exécutoire
  Quand le chef du budget modifie le crédit d'une ligne directement
  Alors la modification est refusée : seule une réallocation modifie un crédit exécutoire
```

### US-BUD-110 — Consulter le budget par axe

**En tant que** chef de programme ou directeur, **je veux** parcourir le budget par programme, structure, nature ou source, **afin de** voir l'enveloppe qui me concerne.

- Fonctionnalité : F-BUD-026, F-BUD-019 · Règles : — · Hypothèses : — · Dépend de : US-BUD-109, US-BUD-115 · Taille : S

```gherkin
Scénario: Totaux par programme
  Quand l'utilisateur affiche le budget 2027 par programme
  Alors chaque programme montre la somme des crédits de ses lignes, dépliable jusqu'à la tâche

Scénario: Périmètre
  Étant donné le DUO du ranch de Ndokayo
  Quand il affiche le budget 2027
  Alors il ne voit que les lignes de son unité
```

---

## Chargement du budget

### US-BUD-111 — Importer le budget approuvé depuis Excel

**En tant que** chef du Service du Budget, **je veux** importer le budget de l'exercice préparé hors ERP, **afin de** démarrer l'exécution sans ressaisie.

- Fonctionnalité : F-BUD-027 · Règles : RG-BUD-033 · Hypothèses : H-01 · Dépend de : US-BUD-106, 107, 108 · Taille : L

```gherkin
Scénario: Import valide
  Étant donné l'exercice 2027 en préparation, le plan de codification 2027 actif, les structures et natures déclarées
  Et un fichier au modèle fourni, de 640 lignes, totalisant 4 250 000 000 en charges et 4 600 000 000 en produits
  Quand le chef du budget l'importe
  Alors un aperçu affiche le nombre de lignes et les totaux par section
  Et après confirmation et validation par un second utilisateur, 640 lignes sont créées dans une version « importée »

Scénario: Un code inconnu bloque tout l'import
  Étant donné un fichier dont la ligne 37 référence la tâche « P09-A01-01-T07 », absente du plan
  Quand le chef du budget l'importe
  Alors aucune ligne n'est créée
  Et le rapport indique « Ligne 37 : tâche P09-A01-01-T07 inconnue »

Scénario: Doublons dans le fichier
  Étant donné un fichier contenant deux fois la même combinaison structure × élément × nature
  Quand le chef du budget l'importe
  Alors l'import est refusé et les deux lignes en conflit sont signalées

Scénario: Montants
  Étant donné une ligne avec un montant décimal ou négatif
  Quand le chef du budget importe le fichier
  Alors la ligne est signalée en erreur : les crédits sont des entiers positifs en FCFA
```

---

## Paramètres et journal

### US-BUD-112 — Gérer la table des seuils et paramètres

**En tant qu'**administrateur fonctionnel, **je veux** modifier les seuils et paramètres avec une date d'effet, **afin qu'**aucune valeur réglementaire ne soit codée en dur.

- Fonctionnalité : F-BUD-028 · Règles : RG-BUD-034 · Hypothèses : H-23 · Dépend de : — · Taille : M

```gherkin
Scénario: Valeurs initiales
  Quand l'administrateur consulte la table des seuils
  Alors il trouve les tranches < 100 000, 100 000–499 999, 500 000–14 999 999, 15 000 000–49 999 999, ≥ 50 000 000
  Et le plafond de la caisse B 7 500 000, l'avance de mission 65 %, le seuil de réallocation 5 %, les alertes 80 % et 100 %

Scénario: Nouvelle valeur datée
  Quand l'administrateur porte le seuil du bon de commande à 750 000 au 01/03/2027 et cite la note de service
  Alors la modification est soumise au maker-checker
  Et une dépense datée du 28/02/2027 applique encore 500 000, une dépense du 01/03/2027 applique 750 000

Scénario: Table partagée
  Quand le module Achats détermine la procédure d'une commande
  Alors il lit la même table, à la même date
```

### US-BUD-113 — Tenir le journal des mouvements et un disponible sûr

**En tant que** chef du Service du Budget, **je veux** que chaque opération sur une ligne écrive un mouvement immuable et que le disponible soit contrôlé sans risque d'accès simultanés, **afin que** tout montant s'explique et qu'aucun crédit ne soit dépassé.

- Fonctionnalité : F-BUD-029 · Règles : RG-BUD-024, RG-BUD-031, RG-BUD-032 · Hypothèses : — · Dépend de : US-BUD-109 · Taille : L

```gherkin
Scénario: Montants issus des mouvements
  Étant donné une ligne avec une ouverture de 10 000 000, une mise de côté de 2 400 000, puis un engagement de 2 300 000
  Alors la ligne affiche mis de côté 0, engagé 2 300 000, disponible 7 700 000
  Et la somme de ses mouvements donne exactement ces montants

Scénario: Opérations simultanées
  Étant donné une ligne dont le disponible est 1 000 000
  Quand dix demandes de 600 000 arrivent en même temps
  Alors une seule est acceptée
  Et les neuf autres sont refusées en conflit (409), avec le disponible restant
  Et le disponible final vaut 400 000

Scénario: Rejeu
  Étant donné une mise de côté déjà enregistrée pour la demande d'acquisition DA-2027-0042
  Quand la même opération est rejouée avec la même clé
  Alors aucun nouveau mouvement n'est créé et le résultat initial est renvoyé

Scénario: Immuabilité
  Quand un utilisateur, même administrateur, tente de modifier ou supprimer un mouvement
  Alors l'opération n'existe pas : une correction passe par un mouvement inverse
```

### US-BUD-114 — Consulter l'historique d'une ligne

**En tant que** chef du Service du Budget, **je veux** voir la liste des mouvements d'une ligne avec leur pièce d'origine, **afin de** justifier chaque chiffre devant le DG, la DCAI ou un auditeur.

- Fonctionnalité : F-BUD-029 · Règles : RG-BUD-032 · Hypothèses : — · Dépend de : US-BUD-113 · Taille : S

```gherkin
Scénario: Historique
  Quand l'utilisateur ouvre l'historique d'une ligne
  Alors il voit, du plus récent au plus ancien, date, type, montant, auteur, pièce d'origine (module, type, référence) et solde après opération
  Et un lien ouvre la pièce d'origine
  Et l'historique s'exporte en Excel et en PDF
```

---

## Habilitations

### US-BUD-115 — Profils budgétaires et périmètre par structure

**En tant qu'**administrateur IAM, **je veux** attribuer les profils budgétaires (DG, DAF, SDFBM, chef du budget, agent du budget, DUO, CSAFC, chef de programme, lecteur), **afin que** chacun n'agisse que sur ce que la procédure lui confie, dans sa structure.

- Fonctionnalité : § 3 de la spécification · Règles : RG-BUD-021, RG-BUD-035 · Hypothèses : H-31 · Dépend de : US-BUD-107 · Taille : M

```gherkin
Scénario: Action non autorisée
  Étant donné un utilisateur sans profil budgétaire
  Quand il appelle l'engagement d'une dépense
  Alors il reçoit un refus explicite (403) et l'action n'apparaît pas dans son interface

Scénario: Périmètre d'une unité
  Étant donné le CSAFC du ranch de Ndokayo
  Quand il tente de viser une dépense sur une ligne de l'abattoir de Yaoundé
  Alors le visa est refusé : la ligne est hors de son périmètre

Scénario: Vue globale
  Étant donné un agent du Service du Budget
  Quand il consulte la situation d'exécution
  Alors il voit toutes les structures

Scénario: Identité
  Quand une requête porte un identifiant d'utilisateur différent de celui du jeton
  Alors l'identifiant de la requête est ignoré : l'auteur enregistré est celui du jeton
```
