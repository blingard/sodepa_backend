# Lot B3 — Réallocation des crédits

Objectif du lot : déplacer des crédits disponibles d'une ligne à une autre selon le circuit décidé par la SODEPA, avec un dossier complet et une trace de bout en bout.

**C'est le lot le plus exposé aux réponses du client** (Q-22 à Q-27). Le circuit est porté par une table de décision paramétrable : une réponse différente change ses valeurs, pas le code. Relire les hypothèses H-22 à H-27 avant de démarrer le lot.

Conventions : voir [README](README.md). Référence : [spécification](../specification-fonctionnelle-budget.md), EP-BUD-06.

---

### US-BUD-301 — Demander une réallocation

**En tant que** responsable d'un service ou d'une unité, **je veux** remplir une fiche de demande de réallocation et y joindre les pièces, **afin de** renforcer une ligne prioritaire à partir d'une ligne qui ne sera pas consommée.

- Fonctionnalité : F-BUD-021 · Règles : RG-BUD-019 · Hypothèses : — · Dépend de : US-BUD-109 · Taille : M

```gherkin
Scénario: Demande complète
  Quand le DUO du ranch de Ndokayo demande le transfert de 1 500 000 de la ligne « carburant » vers la ligne « vaccins »
  Et renseigne l'objet, la justification technique, le degré d'urgence, l'impact attendu, les risques en cas de refus, et joint le rapport vétérinaire
  Alors la demande est soumise à la Direction Générale sous couvert de la DAF
  Et elle apparaît dans la boîte de réception de l'analyse (DAF, SDFBM, SB)

Scénario: Rubrique manquante
  Quand le demandeur soumet sans justification ni impact
  Alors la soumission est refusée et les rubriques manquantes sont signalées

Scénario: Même ligne
  Quand la ligne source et la ligne bénéficiaire sont identiques
  Alors la demande est refusée
```

### US-BUD-302 — Analyser la demande et produire la note technique

**En tant qu'**agent du Service du Budget (pour la DAF et la SDFBM), **je veux** voir automatiquement la situation des deux lignes et l'impact de la demande, puis rédiger la note technique et financière, **afin d'**éclairer l'avis et la décision.

- Fonctionnalité : F-BUD-022 · Règles : RG-BUD-014 · Hypothèses : H-25 · Dépend de : US-BUD-301, 224 · Taille : M

```gherkin
Scénario: Contrôle du disponible
  Étant donné une ligne source de crédit 8 000 000, engagée à 5 000 000, sans mise de côté
  Quand le SB ouvre une demande de 1 500 000
  Alors l'écran affiche un disponible de 3 000 000 sur la ligne source et le marque « suffisant »

Scénario: Prélèvement sur des crédits engagés
  Étant donné une demande de 3 500 000 sur cette même ligne
  Alors la demande est marquée « non recevable » : au plus 3 000 000 sont transférables

Scénario: Note jointe
  Quand le SB enregistre la note technique et financière, signée par la SDFBM et la DAF
  Alors la demande passe à l'état « analysée » et la note rejoint le dossier
```

### US-BUD-303 — Classer la réallocation par la table de décision

**En tant que** chef du budget, **je veux** que l'ERP détermine le niveau de la réallocation à partir d'une table de décision paramétrable, **afin que** chaque demande suive le circuit fixé par la Direction Générale.

- Fonctionnalité : F-BUD-022 · Règles : RG-BUD-018, RG-BUD-034 · Hypothèses : H-22, H-23, H-24 · Dépend de : US-BUD-112 · Taille : L

```gherkin
Plan du scénario: Valeurs initiales de la table
  Étant donné une ligne source de crédit initial 10 000 000, sans réallocation antérieure dans l'exercice
  Quand une demande de <montant> va de <source> vers <cible>
  Alors son niveau est « <niveau> »
  Exemples:
    | montant    | source                            | cible                              | niveau                                  |
    | 400 000    | fonctionnement, direction A       | fonctionnement, direction A        | 1 — DAF propose, DG valide              |
    | 800 000    | fonctionnement, direction A       | fonctionnement, direction A        | 2 — DAF propose, comité, DG valide      |
    | 300 000    | fonctionnement, direction A       | fonctionnement, direction B        | 2 — DAF propose, comité, DG valide      |
    | 1 000 000  | investissement                    | fonctionnement                     | 3 — Conseil d'Administration            |

Scénario: Cumul sur l'exercice
  Étant donné qu'une réallocation de 300 000 a déjà été prélevée sur la ligne source (3 %)
  Quand une nouvelle demande de 300 000 arrive sur la même ligne
  Alors le taux pris en compte est 6 % et la demande est de niveau 2

Scénario: Table modifiée
  Quand l'administrateur fonctionnel porte le seuil du niveau 1 à 10 %, avec une date d'effet et la référence de la note de service
  Alors les demandes soumises après cette date sont classées avec la nouvelle valeur
  Et les demandes déjà soumises gardent leur niveau
```

### US-BUD-304 — Faire suivre le circuit de décision

**En tant que** DAF, membre du comité ou DG, **je veux** recevoir la demande dans ma boîte de réception selon son niveau, **afin de** rendre mon avis ou ma décision dans l'ordre prévu.

- Fonctionnalité : F-BUD-023 · Règles : RG-BUD-015, 016, 018, 020, 021 · Hypothèses : H-22, H-26 · Dépend de : US-BUD-302, 303 · Taille : L

```gherkin
Scénario: Niveau 1
  Étant donné une demande analysée de niveau 1
  Alors la DAF la propose, puis le DG la valide ou la rejette

Scénario: Niveau 2 avec avis du comité
  Étant donné une demande analysée de niveau 2
  Quand le secrétaire du comité enregistre l'avis motivé (opportunité, régularité, soutenabilité, priorité, financement) et la liste des membres présents
  Alors la demande passe au DG avec l'avis

Scénario: Délai du comité
  Étant donné un délai d'avis paramétré à 5 jours ouvrés
  Quand le délai est dépassé
  Alors une relance part au président du comité et au DAF

Scénario: Complément demandé
  Quand le DG demande un complément
  Alors la demande revient au demandeur avec la question, puis reprend le circuit au même niveau

Scénario: Aucune dépense avant décision
  Tant que la demande n'est pas exécutée
  Alors la ligne bénéficiaire n'est pas augmentée et aucun visa ne peut s'appuyer sur le montant demandé
```

### US-BUD-305 — Enregistrer la décision du DG et la résolution du CA

**En tant que** DG (ou SB pour le compte du CA), **je veux** enregistrer la décision interne de réallocation, et la résolution du CA pour le niveau 3, **afin que** la réallocation soit exécutable.

- Fonctionnalité : F-BUD-023, F-BUD-024 · Règles : RG-BUD-016, 018, 019 · Hypothèses : H-02, H-22 · Dépend de : US-BUD-304 · Taille : M

```gherkin
Scénario: Décision du DG
  Quand le DG approuve une demande de niveau 1 ou 2
  Alors la décision interne est générée sur le modèle en quatre articles (montant et lignes, objet, ajustements par la DAF, exécution), numérotée, signée et archivée

Scénario: Niveau 3
  Étant donné une demande de niveau 3 approuvée par le DG
  Alors elle attend la résolution du CA
  Quand le SB enregistre la résolution n° RES-CA-2027-04 et la joint
  Alors la demande devient exécutable
```

### US-BUD-306 — Mesure conservatoire d'urgence

**En tant que** Directeur Général, **je veux** autoriser une réallocation d'urgence avant tout avis, avec une justification, **afin de** faire face à une épizootie, une panne grave ou une obligation légale, sous réserve de régularisation.

- Fonctionnalité : F-BUD-023 · Règles : RG-BUD-017 · Hypothèses : H-24 · Dépend de : US-BUD-301, 307 · Taille : M

```gherkin
Scénario: Urgence vétérinaire
  Étant donné une demande marquée « urgence vétérinaire » avec la justification « foyer de fièvre aphteuse au ranch de Faro »
  Quand le DG prend la mesure conservatoire
  Alors la réallocation est exécutée immédiatement
  Et elle passe à l'état « à régulariser CA » si son niveau est 3, ou « exécutée » sinon

Scénario: Justification obligatoire
  Quand le DG prend une mesure conservatoire sans justification
  Alors l'opération est refusée

Scénario: Réservée au DG
  Quand un autre profil tente une mesure conservatoire
  Alors l'action est refusée (403)

Scénario: Suivi de la régularisation
  Quand le chef du budget affiche les réallocations à régulariser
  Alors il voit chaque mesure conservatoire, sa date et son montant, jusqu'à l'enregistrement de la résolution du CA
```

### US-BUD-307 — Exécuter la réallocation

**En tant que** chef du Service du Budget, **je veux** que l'exécution d'une réallocation décidée mette à jour les deux lignes et produise le tableau rectifié, **afin que** les crédits ouverts reflètent la décision.

- Fonctionnalité : F-BUD-024 · Règles : RG-BUD-014, 032, 033 · Hypothèses : H-25 · Dépend de : US-BUD-113, 305 · Taille : M

```gherkin
Scénario: Exécution
  Étant donné une décision de transfert de 1 500 000 de « carburant » (disponible 3 000 000) vers « vaccins »
  Quand la réallocation est exécutée
  Alors un mouvement de −1 500 000 est écrit sur la ligne source et un mouvement de +1 500 000 sur la ligne bénéficiaire
  Et une version rectifiée de l'exercice est enregistrée, sans écraser la version initiale

Scénario: Disponible changé entre-temps
  Étant donné que la ligne source n'a plus que 1 000 000 de disponible au moment de l'exécution
  Quand la réallocation de 1 500 000 est exécutée
  Alors l'exécution est refusée en conflit (409) et la demande revient au SB

Scénario: Tableau type
  Alors le tableau rectifié présente ligne source, libellé, montant initial, consommé, disponible, montant transféré, ligne bénéficiaire, crédit initial et nouveau crédit
```

### US-BUD-308 — Consulter les réallocations de l'exercice

**En tant que** DG, DAF ou DCAI, **je veux** la liste et l'état consolidé des réallocations de l'exercice, **afin de** suivre les modifications du budget et de rendre compte au CA.

- Fonctionnalité : F-BUD-024 · Règles : RG-BUD-019 · Hypothèses : — · Dépend de : US-BUD-307 · Taille : S

```gherkin
Scénario: État consolidé
  Quand l'utilisateur affiche les réallocations 2027
  Alors il voit, par niveau et par état, le nombre et le montant des demandes
  Et chaque demande ouvre son dossier complet : demande, note, avis, décision, résolution, mouvements

Scénario: Modifications répétées
  Étant donné qu'une même ligne a reçu ou cédé des crédits plus de trois fois dans l'exercice
  Alors elle est signalée dans l'état consolidé
```

### US-BUD-309 — Enregistrer la saisine des tutelles

**En tant que** secrétariat du DG, **je veux** enregistrer la saisine des tutelles technique ou financière et leur réponse, **afin que** le dossier de réallocation reste complet.

- Fonctionnalité : F-BUD-023 · Règles : RG-BUD-019 · Hypothèses : H-27 · Dépend de : US-BUD-304 · Taille : S

```gherkin
Scénario: Saisine
  Quand le secrétariat enregistre la date, la référence du courrier et la tutelle saisie
  Alors la demande affiche « en attente de la tutelle »
  Et quand la réponse est enregistrée et jointe, le circuit reprend
```
