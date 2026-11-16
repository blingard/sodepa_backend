# Lot B4 — Suivi, recettes et clôture

Objectif du lot : rendre compte (rapports du 5, du 10 et trimestriel), suivre les recettes et clore l'exercice. Le lot s'appuie sur les mouvements des lots B2 et B3.

Conventions : voir [README](README.md). Référence : [spécification](../specification-fonctionnelle-budget.md), EP-BUD-04, EP-BUD-05, EP-BUD-08.

---

## Recettes

### US-BUD-401 — Imputer les encaissements sur les lignes de produits

**En tant que** CSAFC (ou agent du budget au siège), **je veux** que chaque encaissement soit imputé sur sa ligne de produits, **afin de** connaître le réalisé des recettes par nature et par unité.

- Fonctionnalité : F-BUD-017 · Règles : RG-BUD-026, 031 · Hypothèses : H-15 · Dépend de : US-BUD-240 · Taille : M

```gherkin
Scénario: Vente de bovins
  Étant donné la ligne « ranch de Ndokayo × ventes de bovins », prévision 120 000 000
  Quand la Trésorerie enregistre un encaissement de 4 500 000 en caisse A, nature « ventes de bovins »
  Alors la ligne affiche un réalisé de 4 500 000 et un taux de réalisation de 3,75 %

Scénario: Jamais bloquée
  Étant donné que le réalisé dépasse déjà la prévision
  Quand un nouvel encaissement arrive
  Alors il est imputé normalement et la ligne est marquée « au-delà des prévisions »

Scénario: Nature inconnue
  Quand un encaissement arrive sans ligne de produits correspondante
  Alors il est placé dans une file « à imputer » visible du CSAFC et du SB
```

### US-BUD-402 — Fiche de suivi mensuelle des recettes et rapprochement

**En tant qu'**agent du Service du Budget, **je veux** la fiche mensuelle des recettes et un état de rapprochement entre budget, encaissements et comptabilité, **afin de** respecter l'engagement des recettes de la procédure et de détecter les écarts.

- Fonctionnalité : F-BUD-017, F-BUD-038 · Règles : RG-BUD-026 · Hypothèses : H-15 · Dépend de : US-BUD-401, 243 · Taille : M

```gherkin
Scénario: Fiche mensuelle
  Quand le SB édite la fiche des recettes de mars 2027
  Alors elle présente, par nature et par unité, la prévision, le réalisé du mois, le cumul et le taux de réalisation

Scénario: Rapprochement
  Quand le SB édite l'état de rapprochement de mars
  Alors il voit, pour les recettes et pour les dépenses liquidées, les montants du budget, de la trésorerie et de la comptabilité
  Et chaque écart est listé avec ses pièces
```

---

## Rapports

### US-BUD-403 — Expliquer les dépassements et les économies

**En tant que** CSAFC, chef de programme ou agent du budget, **je veux** saisir l'explication et les justificatifs d'un dépassement ou d'une économie, **afin qu'**ils figurent dans le rapport mensuel.

- Fonctionnalité : F-BUD-020 · Règles : — · Hypothèses : H-21 · Dépend de : US-BUD-224 · Taille : S

```gherkin
Scénario: Liste des écarts à expliquer
  Quand le CSAFC ouvre les écarts de mars 2027 de son unité
  Alors il voit les lignes liquidées au-delà de leur crédit et celles consommées sous un seuil paramétré
  Et il saisit pour chacune une cause et joint ses justificatifs

Scénario: Rapport incomplet
  Quand le rapport est généré alors qu'un écart n'est pas expliqué
  Alors l'écart y figure avec la mention « explication non fournie »
```

### US-BUD-404 — Rapport mensuel d'une unité, le 5

**En tant que** DUO (ou CSAFC), **je veux** obtenir le rapport d'activité budgétaire de mon unité au plus tard le 5 du mois suivant, **afin de** rendre compte à la Direction Générale.

- Fonctionnalité : F-BUD-020 · Règles : RG-BUD-013 · Hypothèses : H-21 · Dépend de : US-BUD-224, 403 · Taille : M

```gherkin
Scénario: Génération automatique
  Quand arrive le 1er avril 2027
  Alors le rapport de mars de chaque unité est généré
  Et il présente l'analyse des lignes, les dépassements et les économies avec leurs causes, le niveau des engagements et de l'exécution en valeur et en nombre
  Et une notification part au DUO, au CSAFC et au SB

Scénario: Échéance
  Étant donné qu'un écart reste sans explication le 4 avril
  Alors un rappel part au CSAFC

Scénario: Formats
  Alors le rapport s'exporte en PDF et en Excel
```

### US-BUD-405 — Rapport mensuel de la SODEPA, le 10

**En tant que** chef du Service du Budget, **je veux** le rapport d'activité consolidé de la SODEPA au plus tard le 10 du mois, **afin de** le remettre à la Direction Générale.

- Fonctionnalité : F-BUD-020 · Règles : RG-BUD-013 · Hypothèses : H-21 · Dépend de : US-BUD-404 · Taille : M

```gherkin
Scénario: Consolidation
  Quand le SB génère le rapport de mars 2027
  Alors il consolide les rapports des unités et la Direction Générale
  Et il ajoute les réallocations du mois et les alertes de consommation

Scénario: Rapport d'une unité manquant
  Étant donné qu'une unité n'a pas expliqué ses écarts
  Alors le rapport consolidé la signale
```

### US-BUD-406 — Rapport trimestriel de performance

**En tant que** chef du Service du Budget, **je veux** un rapport trimestriel qui croise l'exécution financière et les indicateurs du PPA, **afin de** mesurer la performance des programmes.

- Fonctionnalité : F-BUD-020 · Règles : — · Hypothèses : H-21 · Dépend de : US-BUD-105, 224 · Taille : M

```gherkin
Scénario: Indicateurs
  Étant donné l'indicateur « part du cheptel vacciné », cible 95 %
  Quand le chef de programme saisit la valeur atteinte au 31 mars : 41 %
  Alors le rapport du 1er trimestre affiche, pour l'activité, le taux d'exécution financière et le taux d'atteinte de l'indicateur

Scénario: Valeur non saisie
  Alors l'indicateur figure avec la mention « non renseigné »
```

---

## Clôture

### US-BUD-407 — Clore un exercice

**En tant que** chef du Service du Budget, **je veux** clore l'exercice au 31 décembre après un état des restes, **afin d'**arrêter les comptes budgétaires de l'année.

- Fonctionnalité : F-BUD-040 · Règles : — · Hypothèses : H-28 · Dépend de : US-BUD-224 · Taille : M

```gherkin
Scénario: État des restes
  Quand le chef du budget prépare la clôture de 2027
  Alors il obtient la liste des mises de côté encore ouvertes, des engagements non liquidés et des liquidations non payées, avec leurs montants

Scénario: Clôture
  Étant donné que chaque reste a été reporté, annulé ou libéré
  Quand le DG valide la clôture
  Alors l'exercice 2027 passe à l'état « clos » et n'accepte plus aucune opération

Scénario: Restes non traités
  Quand la clôture est demandée alors que des restes ne sont pas traités
  Alors elle est refusée et les restes sont listés
```

### US-BUD-408 — Reporter les engagements non liquidés

**En tant que** chef du Service du Budget, **je veux** proposer au DG le report des engagements non liquidés sur l'exercice suivant, **afin que** les commandes en cours restent couvertes.

- Fonctionnalité : F-BUD-040 · Règles : RG-BUD-032 · Hypothèses : H-28 · Dépend de : US-BUD-407 · Taille : M

```gherkin
Scénario: Report décidé
  Étant donné un BE 2027 de 2 300 000 liquidé à 1 500 000
  Quand le DG décide le report de son reste
  Alors 800 000 sont désengagés en 2027 et engagés en 2028 sur la ligne correspondante, avec la référence du BE d'origine

Scénario: Ligne 2028 absente
  Étant donné que la ligne correspondante n'existe pas en 2028
  Alors le report est bloqué jusqu'à ce qu'une ligne soit désignée

Scénario: Crédits non consommés
  Quand l'exercice est clos
  Alors les crédits non engagés sont annulés et n'alimentent pas 2028
```
