# Lot B5 — Élaboration du budget

Objectif du lot : préparer dans l'ERP le budget de l'exercice suivant, de la lettre-circulaire à la décision exécutoire. À partir du deuxième exercice, ce lot remplace l'import du budget (US-BUD-111). À livrer avant la campagne de l'exercice visé.

Conventions : voir [README](README.md). Référence : [spécification](../specification-fonctionnelle-budget.md), EP-BUD-02 ; chronogramme en 25 étapes de [PB § 4.2].

---

## Campagne

### US-BUD-501 — Ouvrir la campagne et diffuser la lettre-circulaire

**En tant que** DPD (avec la CSPP), **je veux** créer la campagne de l'exercice N+1, préparer la lettre-circulaire et son chronogramme à partir d'un modèle, puis la faire signer par le DG et diffuser, **afin de** lancer la préparation du budget.

- Fonctionnalité : F-BUD-004 · Règles : — · Hypothèses : H-02, H-05 · Dépend de : US-BUD-101 · Taille : M

```gherkin
Scénario: Lancement
  Étant donné la circulaire présidentielle reçue le 01/07/2027
  Quand la DPD crée la campagne 2028 et rédige la lettre-circulaire avec les orientations du DG
  Alors un chronogramme est proposé avec les 25 étapes de la procédure et leurs échéances en jours ouvrés, la première au 22/07/2027
  Et après signature du DG, la lettre est archivée et notifiée aux chefs de programme et aux chefs d'unité

Scénario: Échéances modifiables
  Quand le DG modifie une échéance lors de la validation du chronogramme
  Alors les échéances suivantes sont recalculées
```

### US-BUD-502 — Suivre le chronogramme de la campagne

**En tant que** DPD ou DG, **je veux** suivre l'avancement des 25 étapes, avec leurs responsables, livrables et échéances, **afin de** relancer à temps.

- Fonctionnalité : F-BUD-004 · Règles : — · Hypothèses : H-05 · Dépend de : US-BUD-501 · Taille : M

```gherkin
Scénario: Étape en retard
  Étant donné l'étape 9 « Révision des CDMT », échéance 12/09/2027
  Quand la date est dépassée sans livrable déposé
  Alors l'étape passe en retard et une relance part aux chefs de programme concernés et à la DPD

Scénario: Vue d'ensemble
  Quand le DG ouvre la campagne
  Alors il voit les étapes faites, en cours et en retard, et le livrable de chacune
```

### US-BUD-503 — Constituer le groupe de travail et tenir ses réunions

**En tant que** DPD, **je veux** enregistrer la décision du DG créant le groupe de travail, ses membres, ses convocations, ses présences et ses comptes rendus, **afin de** tracer les travaux de la campagne.

- Fonctionnalité : F-BUD-005 · Règles : — · Hypothèses : H-04 · Dépend de : US-BUD-501 · Taille : M

```gherkin
Scénario: Création du groupe
  Quand la DPD prépare la décision avec la liste des membres et le DG la signe
  Alors le groupe de travail 2028 existe et ses membres reçoivent leurs droits sur la campagne

Scénario: Réunion
  Quand la DPD convoque la réunion de concertation, puis enregistre la feuille de présence et le compte rendu
  Alors la réunion figure dans la campagne avec ses pièces
```

## Programmation

### US-BUD-504 — Produire le rapport d'évaluation de l'exécution

**En tant que** DCAI, CSPP, ASCG ou CSB, **je veux** partir d'un rapport d'évaluation pré-rempli avec l'exécution de l'exercice en cours, **afin de** fonder la programmation sur des chiffres justes.

- Fonctionnalité : F-BUD-006 · Règles : — · Hypothèses : — · Dépend de : US-BUD-224, 406 · Taille : M

```gherkin
Scénario: Pré-remplissage
  Quand la CSPP génère le rapport d'évaluation de la campagne 2028
  Alors il contient, par programme, les crédits, engagements, liquidations et taux de 2027 à la date du jour, et les indicateurs saisis
  Et les auteurs y ajoutent leur analyse avant de le déposer comme livrable de l'étape 8
```

### US-BUD-505 — Réviser le CDMT de mon programme

**En tant que** chef de programme, **je veux** réviser le CDMT de mon programme sur trois ans, avec montants par activité, indicateurs et cibles, **afin de** le présenter au groupe de travail.

- Fonctionnalité : F-BUD-006 · Règles : — · Hypothèses : — · Dépend de : US-BUD-105, 503 · Taille : L

```gherkin
Scénario: Révision
  Étant donné le CDMT 2027-2029 du programme P01
  Quand le chef de programme le révise pour 2028-2030
  Alors les années 2028 et 2029 sont reprises comme point de départ, 2030 est à saisir
  Et chaque activité porte ses montants annuels et ses indicateurs avec leurs cibles

Scénario: Validation par le groupe de travail
  Quand le groupe de travail valide le CDMT
  Alors il est figé pour la campagne et la CSPP peut le consolider (US-BUD-510)
```

### US-BUD-506 — Élaborer le projet de performance de mon programme

**En tant que** chef de programme, **je veux** saisir le projet de performance annuel de mon programme (stratégie, objectifs, activités, indicateurs, cibles, moyens), **afin de** contribuer au PPA.

- Fonctionnalité : F-BUD-006 · Règles : — · Hypothèses : — · Dépend de : US-BUD-505 · Taille : M

```gherkin
Scénario: Saisie
  Quand le chef de programme remplit son projet de performance 2028
  Alors les activités et indicateurs sont repris du CDMT validé
  Et il ne peut pas soumettre tant qu'une activité n'a pas d'indicateur avec une cible
```

### US-BUD-507 — Décliner les activités en tâches et les budgétiser

**En tant que** chef de programme, **je veux** décliner chaque activité de l'année en tâches avec indicateurs, puis chiffrer chaque tâche par structure et nature, **afin de** construire le projet de PPA budgétisé.

- Fonctionnalité : F-BUD-006 · Règles : — · Hypothèses : H-01, H-03 · Dépend de : US-BUD-506, 106 · Taille : L

```gherkin
Scénario: Budgétisation d'une tâche
  Étant donné la tâche « Achat des vaccins » de l'activité « Campagne de vaccination »
  Quand le chef de programme la chiffre à 10 000 000 pour le ranch de Ndokayo, nature produits vétérinaires
  Alors une ligne proposée est créée dans la version « projet de PPA » de 2028

Scénario: Écart avec le CDMT
  Quand le total budgétisé d'une activité dépasse le montant 2028 de son CDMT
  Alors l'écart est signalé au chef de programme et à la CSPP
```

### US-BUD-508 — Préparer l'avant-projet de budget de mon unité

**En tant que** DUO (avec le CSAFC), **je veux** saisir l'avant-projet de mon unité en quatre volets et joindre les éléments de maturation, **afin de** le transmettre à la Direction Générale.

- Fonctionnalité : F-BUD-007 · Règles : — · Hypothèses : — · Dépend de : US-BUD-501 · Taille : L

```gherkin
Scénario: Quatre volets
  Quand le DUO du ranch de Ndokayo prépare l'avant-projet 2028
  Alors il saisit les charges de fonctionnement, les produits, les activités et les investissements, chaque montant rattaché à un élément du programme et à une nature
  Et il joint les TDR et autres éléments de maturation

Scénario: Transmission
  Quand le DUO transmet l'avant-projet
  Alors il devient une version « avant-projet d'unité », non modifiable par l'unité, et le groupe de travail est notifié
```

### US-BUD-509 — Partir de l'exécution précédente

**En tant que** DUO ou chef de programme, **je veux** pré-remplir mes propositions à partir de l'exécution de l'année en cours et de l'historique des ventes, **afin de** gagner du temps et de partir de chiffres réels.

- Fonctionnalité : F-BUD-008 (BF-BUD-013 de [JP]) · Règles : — · Hypothèses : — · Dépend de : US-BUD-508, 224 · Taille : M · Priorité : moyenne

```gherkin
Scénario: Reprise
  Quand le DUO choisit « partir de 2027 »
  Alors chaque ligne reprend le crédit ouvert 2027, avec en regard l'engagé et le liquidé à date
  Et les lignes de produits reprennent le réalisé des ventes des douze derniers mois
```

## Consolidation et approbation

### US-BUD-510 — Consolider CDMT, PPA et avant-projets

**En tant que** CSPP ou Service du Budget, **je veux** obtenir automatiquement les CDMT, le PPA et le projet de budget consolidés au niveau de la SODEPA, **afin de** les soumettre au groupe de travail.

- Fonctionnalité : F-BUD-008 · Règles : — · Hypothèses : — · Dépend de : US-BUD-505, 507, 508 · Taille : L

```gherkin
Scénario: Projet de budget consolidé
  Quand le SB consolide les avant-projets des 14 structures et le PPA budgétisé
  Alors une version « consolidée » est créée, avec les totaux par programme, structure et section
  Et deux propositions portant sur la même ligne sont additionnées, avec leur origine conservée

Scénario: Avant-projet manquant
  Quand une unité n'a pas transmis son avant-projet
  Alors la consolidation la signale et peut être relancée après réception
```

### US-BUD-511 — Arbitrer le projet de budget

**En tant que** membre du groupe de travail ou chef de programme lors des débats budgétaires, **je veux** ramener les propositions aux moyens disponibles en gardant la trace des montants proposés, arbitrés et retenus, **afin de** produire le projet de budget.

- Fonctionnalité : F-BUD-008 · Règles : — · Hypothèses : H-07 · Dépend de : US-BUD-510 · Taille : L

```gherkin
Scénario: Plafonds
  Étant donné un plafond de 1 200 000 000 fixé pour le programme P01
  Quand les propositions du programme totalisent 1 450 000 000
  Alors le dépassement de 250 000 000 est affiché tant qu'il n'est pas résorbé

Scénario: Trace des arbitrages
  Quand un arbitre ramène une ligne de 12 000 000 à 10 000 000 avec un commentaire
  Alors la ligne garde les montants proposé (12 000 000), arbitré (10 000 000) et le commentaire
  Et la version « arbitrée » est enregistrée à la fin des débats
```

### US-BUD-512 — Faire valider, approuver et rendre exécutoire le budget

**En tant que** chef du Service du Budget, **je veux** faire suivre au projet de budget le circuit groupe de travail → validation interne du DG → approbation du CA → décision exécutoire, **afin que** la version approuvée devienne la base de l'exécution.

- Fonctionnalité : F-BUD-009 · Règles : RG-BUD-002, 033, 038 · Hypothèses : H-04 · Dépend de : US-BUD-511, 102, 103 · Taille : M

```gherkin
Scénario: Circuit
  Quand le groupe de travail valide, puis que le DG valide en interne
  Alors la version passe à « validée DG »
  Et l'enregistrement de la résolution du CA (US-BUD-102) puis la décision exécutoire (US-BUD-103) la rendent exécutoire

Scénario: Retour en arbitrage
  Quand le DG refuse la validation interne avec un motif
  Alors la version revient à l'état « arbitrée » avec le motif
```

### US-BUD-513 — Comparer deux versions du budget

**En tant que** DG, DAF ou chef du budget, **je veux** comparer deux versions ligne par ligne, **afin de** voir ce que chaque étape a changé.

- Fonctionnalité : F-BUD-003 · Règles : — · Hypothèses : — · Dépend de : US-BUD-510 · Taille : M

```gherkin
Scénario: Comparaison
  Quand l'utilisateur compare la version « consolidée » et la version « approuvée CA » de 2028
  Alors il voit, par ligne et par regroupement, les deux montants, l'écart en valeur et en pourcentage
  Et la comparaison s'exporte en Excel
```

### US-BUD-514 — Produire les annexes du budget

**En tant que** chef du Service du Budget, **je veux** produire le budget d'exploitation et le budget d'investissement, avec plan d'opérations en capital, plan de trésorerie et plan de financement, **afin de** joindre au budget les annexes demandées par la procédure.

- Fonctionnalité : F-BUD-009 (BF-BUD-017 de [JP]) · Règles : — · Hypothèses : H-02 · Dépend de : US-BUD-510 · Taille : L · Priorité : moyenne

```gherkin
Scénario: Annexes
  Quand le SB génère les annexes de la version validée DG
  Alors le budget d'exploitation et le budget d'investissement sont produits à partir des lignes, par section
  Et les plans de trésorerie et de financement sont proposés à compléter, puis joints à la version
```
