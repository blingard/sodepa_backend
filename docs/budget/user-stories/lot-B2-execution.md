# Lot B2 — Exécution des dépenses

Objectif du lot : qu'aucune dépense ne s'engage sans crédit, que chaque engagement suive le circuit de la procédure, et que la situation d'une ligne soit juste du visa au paiement. Les branchements avec les autres modules se font par le contrat d'opérations (US-BUD-240).

Conventions : voir [README](README.md). Référence : [spécification](../specification-fonctionnelle-budget.md). Exemples chiffrés : ligne « ranch de Ndokayo × achat des vaccins », crédit ouvert 10 000 000.

---

## Visa budgétaire

### US-BUD-201 — Viser une demande et mettre le crédit de côté

**En tant qu'**agent du Service du Budget (ou CSAFC en unité), **je veux** viser une demande d'acquisition, des TDR ou une fiche d'entretien, **afin d'**attester que le crédit existe et de le mettre de côté pendant la consultation des fournisseurs.

- Fonctionnalité : F-BUD-030 · Règles : RG-BUD-004, 005, 023 · Hypothèses : H-19 · Dépend de : US-BUD-103, 113 · Taille : L

```gherkin
Scénario: Visa accordé
  Étant donné une ligne exécutoire de disponible 10 000 000
  Et une demande d'acquisition de vaccins estimée à 2 400 000, imputée sur cette ligne
  Quand le CSAFC du ranch vise la demande
  Alors la ligne affiche mis de côté 2 400 000 et disponible 7 600 000
  Et la demande porte le visa, son auteur, sa date et le montant visé

Scénario: Crédit insuffisant
  Étant donné une ligne de disponible 1 000 000
  Quand le CSAFC vise une demande estimée à 2 400 000
  Alors le visa est refusé en conflit (409) avec le message « Crédit insuffisant : disponible 1 000 000, demandé 2 400 000 »
  Et rien n'est mis de côté

Scénario: Mauvais sens de ligne
  Quand le CSAFC impute une dépense sur une ligne de produits
  Alors le visa est refusé : une dépense ne s'impute pas sur une ligne de recette

Scénario: Auteur de la demande
  Étant donné une demande créée par le CSAFC lui-même
  Quand il tente de la viser
  Alors le visa est refusé (règle des quatre yeux)
```

### US-BUD-202 — Libérer une mise de côté

**En tant qu'**agent du budget, **je veux** libérer le crédit mis de côté quand une demande est abandonnée ou rejetée, **afin de** le rendre disponible.

- Fonctionnalité : F-BUD-030 · Règles : RG-BUD-023 · Hypothèses : H-19 · Dépend de : US-BUD-201 · Taille : S

```gherkin
Scénario: Demande abandonnée
  Étant donné une demande visée pour 2 400 000
  Quand le demandeur l'abandonne, ou le DUO la rejette
  Alors un mouvement de libération de 2 400 000 est enregistré et le disponible remonte d'autant

Scénario: Demandes visées anciennes
  Quand le chef du budget affiche les mises de côté de plus de 60 jours
  Alors il voit la liste des demandes concernées avec leur demandeur, pour relance ou libération
```

---

## Bon d'engagement

### US-BUD-203 — Établir un bon d'engagement à partir d'un visa

**En tant qu'**agent du budget (ou CSAFC), **je veux** établir le BE de l'offre retenue à partir de la demande visée, **afin d'**engager juridiquement la dépense.

- Fonctionnalité : F-BUD-010, F-BUD-011 · Règles : RG-BUD-003, 004, 024, 028 · Hypothèses : H-08 · Dépend de : US-BUD-201, 205 · Taille : L

```gherkin
Scénario: Offre inférieure à l'estimation
  Étant donné une demande visée pour 2 400 000 et une offre retenue de 2 300 000
  Quand le CSAFC établit le BE
  Alors la mise de côté de 2 400 000 est consommée, l'engagé passe à 2 300 000
  Et 100 000 redeviennent disponibles

Scénario: Offre supérieure à l'estimation, crédit suffisant
  Étant donné une demande visée pour 2 400 000, une offre de 2 600 000 et un disponible de 7 600 000
  Quand le CSAFC établit le BE
  Alors l'engagé passe à 2 600 000 et le disponible à 7 400 000

Scénario: Offre supérieure, crédit insuffisant
  Étant donné une demande visée pour 2 400 000, une offre de 3 000 000 et un disponible de 300 000
  Quand le CSAFC établit le BE
  Alors l'établissement est refusé en conflit (409) : il manque 300 000

Scénario: Numéro attribué par le système
  Quand le BE est enregistré
  Alors il reçoit le numéro suivant de sa structure pour l'exercice, par exemple BE-2027-NDOKAYO-0007
  Et aucun numéro n'est saisissable par l'utilisateur
```

### US-BUD-204 — Établir un bon d'engagement sans visa préalable

**En tant qu'**agent du budget, **je veux** établir un BE directement à partir d'un contrat, d'un bail, d'une convention ou d'une décision, sur une ou plusieurs lignes, **afin d'**engager les dépenses qui ne passent pas par une demande d'acquisition.

- Fonctionnalité : F-BUD-010 · Règles : RG-BUD-011, 029 · Hypothèses : H-09 · Dépend de : US-BUD-113 · Taille : M

```gherkin
Scénario: Bail sur deux lignes
  Quand l'agent du budget établit un BE de 6 000 000 pour un bail, réparti 4 000 000 sur la ligne « loyers DG » et 2 000 000 sur « charges locatives DG »
  Alors chaque ligne est contrôlée sur son disponible
  Et les deux lignes sont engagées dans une seule opération, ou aucune ne l'est

Scénario: Pièce d'origine obligatoire
  Quand l'agent du budget établit un BE sans pièce d'origine jointe
  Alors l'enregistrement est refusé
```

### US-BUD-205 — Déterminer la tranche, le support et les pièces exigées

**En tant qu'**agent du budget, **je veux** que la tranche, le support de commande et la liste des pièces exigées découlent automatiquement du montant, de la structure et du type de dépense, **afin d'**appliquer la procédure sans erreur.

- Fonctionnalité : F-BUD-012 · Règles : RG-BUD-006, 007, 008, 009, 010, 034 · Hypothèses : H-14 · Dépend de : US-BUD-112 · Taille : M

```gherkin
Plan du scénario: Support selon le montant TTC
  Quand une acquisition de <montant> est engagée
  Alors la tranche est « <tranche> » et le support « <support> »
  Exemples:
    | montant     | tranche              | support                        |
    | 60 000      | < 100 000            | aucune commande, caisse B      |
    | 300 000     | 100 000 – 499 999    | BE seul                        |
    | 2 300 000   | 500 000 – 14 999 999 | BE et bon de commande          |
    | 20 000 000  | 15 M – 49 999 999    | lettre commande                |
    | 80 000 000  | ≥ 50 M               | marché                         |

Scénario: Pièces exigées
  Étant donné une acquisition de 2 300 000 à la Direction Générale
  Alors les pièces exigées sont : expression de besoin, attestation de carence, demande d'acquisition visée, trois pro forma de fournisseurs agréés, rapport du comité de sélection, dossier fiscal
  Et le BE ne peut pas passer à la signature tant qu'une pièce manque

Scénario: Lettre commande demandée par une unité
  Étant donné le ranch de Ndokayo et une acquisition de 20 000 000
  Alors le circuit est celui de la Direction Générale
```

### US-BUD-206 — Contrôle a priori des pièces

**En tant qu'**agent du budget, **je veux** une liste de contrôle par type de dépense, que je coche avant de transmettre le BE à la signature, **afin de** vérifier la régularité des pièces comme le demande la procédure.

- Fonctionnalité : F-BUD-018 · Règles : RG-BUD-005 · Hypothèses : — · Dépend de : US-BUD-205 · Taille : S

```gherkin
Scénario: Contrôle complet
  Étant donné un BE de 2 300 000 et sa liste de contrôle
  Quand l'agent du budget coche chaque point (régularité de l'expression de besoin, du BE, de l'attestation de carence, des pro forma, du dossier fiscal, de l'attestation ARMP)
  Alors le BE peut être transmis à la signature et le contrôle est tracé

Scénario: Liste paramétrable
  Quand l'administrateur fonctionnel ajoute un point à la liste des missions
  Alors les BE de mission suivants l'affichent
```

---

## Circuits de signature

### US-BUD-207 — Circuit de signature à la Direction Générale

**En tant que** SDFBM, DAF puis DG, **je veux** recevoir dans ma boîte de réception les BE de la Direction Générale dans l'ordre SB → SDFBM → DAF → DG, **afin de** les viser puis les signer.

- Fonctionnalité : F-BUD-013 · Règles : RG-BUD-021, 022, 035 · Hypothèses : H-12, H-13 · Dépend de : US-BUD-203, 206 · Taille : L

```gherkin
Scénario: Circuit complet
  Étant donné un BE établi par le SB à la Direction Générale
  Quand la SDFBM vise, puis la DAF vise, puis le DG signe
  Alors le BE passe à l'état « signé », non modifiable
  Et chaque visa est tracé avec son auteur, sa date et l'heure

Scénario: Ordre imposé
  Quand la DAF tente de viser avant la SDFBM
  Alors la tâche n'est pas dans sa boîte de réception

Scénario: Une personne, une étape
  Étant donné que la même personne a visé en tant que SDFBM par intérim
  Quand elle tente de viser l'étape DAF du même BE
  Alors l'action est refusée

Scénario: Signature électronique
  Quand le DG signe
  Alors la validation est nominative et horodatée
  Et le document signé peut être joint, pendant la période de transition
```

### US-BUD-208 — Circuit de signature en unité

**En tant que** DUO, **je veux** signer les BE et bons de commande que le CSAFC de mon unité a établis, **afin d'**engager les dépenses de mon unité.

- Fonctionnalité : F-BUD-013 · Règles : RG-BUD-010 (procédure), 021, 022 · Hypothèses : H-14 · Dépend de : US-BUD-203, 206 · Taille : M

```gherkin
Scénario: Signature du DUO
  Étant donné un BE de 2 300 000 établi par le CSAFC du ranch de Ndokayo
  Quand le DUO du ranch le signe
  Alors le BE et son bon de commande passent à l'état « signé »

Scénario: DUO d'une autre unité
  Quand le DUO de l'abattoir de Yaoundé ouvre ce BE
  Alors il ne le voit pas : il est hors de son périmètre
```

### US-BUD-209 — Refuser un bon d'engagement

**En tant que** signataire du circuit, **je veux** refuser un BE avec un motif, **afin qu'**il revienne au service qui l'a établi pour correction.

- Fonctionnalité : F-BUD-013 · Règles : RG-BUD-022 · Hypothèses : H-12 · Dépend de : US-BUD-207 · Taille : S

```gherkin
Scénario: Refus motivé
  Quand la DAF refuse un BE avec le motif « pro forma périmée »
  Alors le BE revient au SB à l'état « établi », avec le motif
  Et le crédit reste engagé jusqu'à la correction ou l'annulation

Scénario: Motif obligatoire
  Quand un signataire refuse sans motif
  Alors le refus n'est pas enregistré
```

### US-BUD-210 — Éditer et archiver le bon d'engagement et le bon de commande

**En tant qu'**agent du budget, **je veux** éditer le BE et le BC sur le modèle SODEPA et les retrouver dans la GED, **afin de** transmettre les originaux au Service des Marchés et au prestataire.

- Fonctionnalité : F-BUD-013, F-BUD-014 · Règles : RG-BUD-012 · Hypothèses : H-02, H-08 · Dépend de : US-BUD-207 ou 208 · Taille : M

```gherkin
Scénario: Édition
  Étant donné un BE signé de 2 300 000
  Quand l'agent du budget l'édite
  Alors le PDF porte le numéro, l'exercice, la structure, les lignes et montants, le bénéficiaire, l'objet, les visas et la signature
  Et il est archivé dans la GED, lié au BE

Scénario: BE non signé
  Quand l'agent tente d'éditer un BE non signé
  Alors seul un aperçu marqué « projet » est produit
```

---

## Commandes, annulation, liquidation, paiement

### US-BUD-211 — Générer le bon de commande et le transmettre aux Marchés

**En tant qu'**agent du budget, **je veux** que tout BE de 500 000 à 14 999 999 génère son bon de commande, transmis au module Marchés à la signature, **afin que** la commande parte au fournisseur.

- Fonctionnalité : F-BUD-014 · Règles : RG-BUD-007 · Hypothèses : — · Dépend de : US-BUD-205, 207, 208 · Taille : M

```gherkin
Scénario: BC généré
  Étant donné un BE signé de 2 300 000
  Alors un bon de commande lié est généré et transmis au module Marchés avec la date de signature

Scénario: Pas de BC sous 500 000
  Étant donné un BE signé de 300 000
  Alors aucun bon de commande n'est généré
```

### US-BUD-212 — Engager une lettre commande ou un marché

**En tant qu'**agent du budget, **je veux** engager une lettre commande ou un marché dès sa signature par le DG, pour la tranche de l'exercice, **afin que** le crédit soit engagé avant toute exécution.

- Fonctionnalité : F-BUD-014 · Règles : RG-BUD-007 · Hypothèses : H-11 · Dépend de : US-BUD-203, 240 · Taille : M

```gherkin
Scénario: Marché sur deux exercices
  Étant donné un marché de 80 000 000 signé, dont 50 000 000 prévus en 2027 et 30 000 000 en 2028
  Quand le module Marchés transmet le marché signé au CSB
  Alors un BE de 50 000 000 est établi sur 2027
  Et 30 000 000 sont enregistrés comme engagement à reprendre sur 2028
```

### US-BUD-213 — Annuler ou réduire un bon d'engagement

**En tant que** chef du budget, **je veux** annuler ou réduire un BE avec un motif, **afin de** restituer le crédit non utilisé.

- Fonctionnalité : F-BUD-016 · Règles : RG-BUD-022, 032, 036 · Hypothèses : H-10 · Dépend de : US-BUD-203 · Taille : M

```gherkin
Scénario: Annulation
  Étant donné un BE signé de 2 300 000, non liquidé
  Quand l'ordonnateur l'annule avec un motif
  Alors un mouvement de désengagement de 2 300 000 est enregistré et le BE passe à « annulé »
  Et le BE initial reste consultable

Scénario: Bon de commande expiré
  Étant donné un BC non exécuté depuis 3 mois et 15 jours après mise en demeure
  Quand le module Marchés annule le BC
  Alors le BE lié est désengagé automatiquement

Scénario: BE liquidé
  Étant donné un BE entièrement liquidé
  Quand l'ordonnateur tente de l'annuler
  Alors l'annulation est refusée
```

### US-BUD-214 — Liquider un bon d'engagement

**En tant que** DG ou DUO, **je veux** liquider un BE sur facture définitive, bordereau ou attestation de service fait et procès-verbal, **afin de** constater ce que la SODEPA doit.

- Fonctionnalité : F-BUD-033 · Règles : RG-BUD-025 · Hypothèses : H-10, H-18 · Dépend de : US-BUD-207 ou 208 · Taille : M

```gherkin
Scénario: Liquidation totale
  Étant donné un BE signé de 2 300 000 et une facture de 2 300 000 avec bordereau et PV
  Quand le DUO liquide
  Alors la ligne affiche liquidé 2 300 000 et le disponible reste inchangé
  Et la liquidation est transmise à la Comptabilité et à la Trésorerie

Scénario: Liquidation partielle puis définitive
  Étant donné un BE de 2 300 000
  Quand le DUO liquide une première facture de 1 500 000, puis une seconde de 700 000 marquée « définitive »
  Alors le liquidé vaut 2 200 000
  Et le reliquat de 100 000 est désengagé et redevient disponible

Scénario: BE non signé
  Quand l'ordonnateur tente de liquider un BE non signé
  Alors la liquidation est refusée

Scénario: Pièces manquantes
  Quand l'ordonnateur liquide sans procès-verbal de réception
  Alors la liquidation est refusée et la pièce manquante est indiquée
```

### US-BUD-215 — Facture supérieure au bon d'engagement

**En tant qu'**ordonnateur, **je veux** que l'écart entre une facture et le BE passe par un engagement complémentaire, **afin de** ne jamais liquider au-delà de ce qui a été engagé.

- Fonctionnalité : F-BUD-033 · Règles : RG-BUD-025 · Hypothèses : H-18 · Dépend de : US-BUD-214 · Taille : M

```gherkin
Scénario: Écart couvert
  Étant donné un BE de 2 300 000 et une facture de 2 450 000
  Quand l'ordonnateur liquide
  Alors la liquidation est limitée à 2 300 000
  Et un engagement complémentaire de 150 000 est proposé, avec le même circuit de visa que le BE initial
  Et le solde de la facture est liquidé une fois le complément signé

Scénario: Écart non couvert
  Étant donné un disponible de 50 000 sur la ligne
  Alors l'engagement complémentaire de 150 000 est refusé en conflit (409)
  Et la facture reste en attente avec le motif « crédit insuffisant pour l'écart »
```

### US-BUD-216 — Constater un paiement

**En tant que** chef du budget, **je veux** que chaque paiement fait par la Trésorerie passe la dépense en « payé », **afin de** suivre le reste à payer.

- Fonctionnalité : F-BUD-034 · Règles : RG-BUD-024, 031 · Hypothèses : — · Dépend de : US-BUD-214, 240 · Taille : S

```gherkin
Scénario: Paiement
  Étant donné un BE liquidé de 2 300 000
  Quand la Trésorerie signale un virement de 2 300 000 pour ce BE
  Alors la ligne affiche payé 2 300 000 et le reste à payer du BE vaut 0

Scénario: Paiement en double
  Quand la Trésorerie renvoie le même paiement
  Alors il n'est pris en compte qu'une fois

Scénario: Paiement supérieur au liquidé
  Quand la Trésorerie signale un paiement supérieur au liquidé du BE
  Alors le paiement est refusé et signalé au chef du budget
```

---

## Dépenses particulières

### US-BUD-217 — Engager une mission

**En tant qu'**agent du Service du Budget, **je veux** viser et engager une mission à partir des TDR validés et de l'ordre de mission, avec son avance, **afin que** la mission soit payée sur un crédit engagé.

- Fonctionnalité : F-BUD-015 · Règles : RG-BUD-003, 037 · Hypothèses : — · Dépend de : US-BUD-201, 203, 207 · Taille : M

```gherkin
Scénario: Mission avec avance
  Étant donné des TDR validés de 1 200 000 et l'ordre de mission établi par le CSSP
  Quand le SB vise et établit le BE, puis le DG signe BE et ordre de mission
  Alors 1 200 000 sont engagés
  Et une avance de 780 000 (65 %) est proposée au paiement
  Et le solde de 420 000 attend les pièces justificatives

Scénario: Pas de crédit, pas d'avance
  Étant donné un disponible de 500 000
  Quand le SB vise des TDR de 1 200 000
  Alors le visa est refusé et aucune avance n'est possible
```

### US-BUD-218 — Engager une réparation de véhicule

**En tant qu'**agent du Service du Budget, **je veux** viser une fiche d'entretien et engager la réparation après autorisation du DG, **afin de** suivre le circuit propre aux véhicules.

- Fonctionnalité : F-BUD-015 · Règles : RG-BUD-007 · Hypothèses : — · Dépend de : US-BUD-201, 203, 205 · Taille : M

```gherkin
Scénario: Réparation de 650 000
  Étant donné une fiche d'entretien d'un véhicule de service, le diagnostic de la Brigade de Maintenance et une pro forma de 650 000
  Quand le SB vise, que le DG autorise, puis que le SB établit le BE
  Alors un BE et un bon de commande sont établis, le montant dépassant 499 999
  Et le dossier porte le comptable-matières comme initiateur

Scénario: Réparation de 300 000
  Alors seul un BE est établi
```

### US-BUD-219 — Engager une étude ou un consultant

**En tant qu'**agent du Service du Budget, **je veux** viser les TDR d'une étude, obtenir l'autorisation du DG et engager, **afin de** transmettre le dossier au prestataire et au Service Financier.

- Fonctionnalité : F-BUD-015 · Règles : RG-BUD-003 · Hypothèses : — · Dépend de : US-BUD-201, 203, 205 · Taille : S

```gherkin
Scénario: Étude
  Étant donné des TDR d'étude de 8 000 000
  Quand le SB vise, que le DG autorise l'engagement, puis que le SB établit le BE
  Alors un BE et un bon de commande sont établis
  Et les TDR sont transmis au Service Financier
```

### US-BUD-220 — Engager une formation, une foire ou des frais d'obsèques

**En tant qu'**agent du Service du Budget, **je veux** engager ces dépenses sur leur pièce propre (TDR de formation, TDR signé d'une foire, décision et état récapitulatif d'obsèques), **afin que** toute dépense soit engagée avant paiement.

- Fonctionnalité : F-BUD-015 · Règles : RG-BUD-003, 011 · Hypothèses : — · Dépend de : US-BUD-204 · Taille : M

```gherkin
Scénario: Foire
  Étant donné des TDR de foire signés par le DG pour 3 000 000
  Quand le CSB établit le BE et qu'il est signé
  Alors 3 000 000 sont engagés et le dossier part au Service Financier pour paiement en espèces ou par chèque

Scénario: Obsèques
  Étant donné une décision signée et un état récapitulatif de 1 500 000
  Quand le CSB établit le BE
  Alors la dépense est engagée sur la ligne des dépenses sociales
```

### US-BUD-221 — Engager la paie du mois

**En tant qu'**agent du Service du Budget, **je veux** engager la masse salariale du mois, une fois les états signés par le DG, répartie par structure et par nature, **afin que** la paie figure dans l'exécution.

- Fonctionnalité : F-BUD-031 · Règles : RG-BUD-027, 039 · Hypothèses : H-20 · Dépend de : US-BUD-204, 240 · Taille : L

```gherkin
Scénario: Paie engagée
  Étant donné les états de paie de mars 2027 signés par le DG : salaires 85 000 000, cotisations CNPS 12 000 000, impôts 9 000 000, répartis sur 14 structures
  Quand le CSB engage la paie de mars
  Alors un BE de régularisation par structure et par nature est établi, puis signé par le DG avec les titres de paiement
  Et les lignes concernées sont engagées

Scénario: Ligne insuffisante
  Étant donné que la ligne « salaires — abattoir de Yaoundé » n'a plus que 2 000 000 de disponible pour 6 500 000 de salaires
  Quand le CSB engage la paie
  Alors l'engagement passe quand même
  Et une alerte part au SB et à la DAF : « Ligne salaires abattoir de Yaoundé dépassée de 4 500 000, réallocation à prévoir »

Scénario: Pas de double engagement
  Quand la paie de mars est engagée une seconde fois
  Alors la seconde opération est sans effet
```

### US-BUD-222 — Caisse B : engager le réapprovisionnement et imputer les dépenses

**En tant qu'**agent du budget (ou CSAFC), **je veux** engager chaque réapprovisionnement de la caisse B, puis répartir ses dépenses réelles sur les lignes lors de sa justification, **afin de** suivre les achats de moins de 100 000 sans un BE par achat.

- Fonctionnalité : F-BUD-032 · Règles : RG-BUD-006, 030 · Hypothèses : H-16, H-17 · Dépend de : US-BUD-204 · Taille : M

```gherkin
Scénario: Réapprovisionnement
  Étant donné une ligne « caisse B — ranch de Ndokayo » de disponible 4 000 000
  Quand le CSAFC engage un réapprovisionnement de 1 500 000
  Alors 1 500 000 sont engagés sur cette ligne

Scénario: Plafond
  Quand le CSAFC engage un réapprovisionnement qui porterait la caisse au-delà de 7 500 000
  Alors l'engagement est refusé

Scénario: Justification
  Étant donné une justification de 1 380 000 répartie en fournitures 400 000, petit entretien 600 000, réception 380 000
  Quand le CSAFC la valide
  Alors chaque montant est imputé sur sa ligne
  Et l'engagement de la caisse est liquidé à hauteur de 1 380 000
```

### US-BUD-223 — Régime transitoire avant la décision exécutoire

**En tant que** chef du budget, **je veux** autoriser, avant la décision exécutoire, les seules dépenses obligatoires dans la limite d'un douzième par mois des crédits de l'exercice précédent, **afin que** la SODEPA fonctionne si le budget n'est pas exécutoire au 1er janvier.

- Fonctionnalité : F-BUD-001 · Règles : RG-BUD-038 · Hypothèses : H-06 · Dépend de : US-BUD-103, 204 · Taille : M

```gherkin
Scénario: Salaires de janvier
  Étant donné l'exercice 2027 non exécutoire au 10/01/2027 et une ligne salaires 2026 de 960 000 000
  Quand le CSB engage la paie de janvier pour 78 000 000
  Alors l'engagement est accepté dans la limite de 80 000 000 (un douzième)

Scénario: Dépense non obligatoire
  Quand un agent engage un achat d'équipement en régime transitoire
  Alors l'engagement est refusé

Scénario: Reprise
  Quand le budget 2027 devient exécutoire
  Alors les engagements du régime transitoire sont rattachés aux lignes 2027 correspondantes
```

---

## Situation et alertes

### US-BUD-224 — Situation d'exécution en temps réel

**En tant que** chef du budget, directeur ou chef de programme, **je veux** voir pour chaque ligne et chaque regroupement le crédit, le mis de côté, l'engagé, le liquidé, le payé et le disponible, **afin de** piloter l'exécution chaque jour.

- Fonctionnalité : F-BUD-019 · Règles : RG-BUD-024 · Hypothèses : — · Dépend de : US-BUD-113 · Taille : L

```gherkin
Scénario: Regroupements
  Quand l'utilisateur affiche la situation 2027
  Alors il peut regrouper par programme, action, activité, tâche, structure, nature ou source de financement
  Et chaque total est la somme exacte de ses lignes

Scénario: Temps réel
  Étant donné un BE signé à 10 h 02
  Quand l'utilisateur actualise la situation à 10 h 03
  Alors le BE y figure

Scénario: Taux
  Alors chaque ligne affiche le taux d'engagement (engagé / crédit ouvert) et le taux d'exécution (liquidé / crédit ouvert)

Scénario: Export
  Quand l'utilisateur exporte la situation filtrée
  Alors l'export Excel ou PDF respecte les filtres et son périmètre
```

### US-BUD-225 — Alertes de consommation

**En tant que** responsable d'une ligne (chef du budget, CSAFC, chef de programme), **je veux** être alerté quand une ligne atteint 80 % puis 100 % de consommation, **afin d'**anticiper une réallocation.

- Fonctionnalité : F-BUD-019 · Règles : RG-BUD-034 · Hypothèses : — · Dépend de : US-BUD-224, US-BUD-112 · Taille : S

```gherkin
Scénario: Seuil de 80 %
  Étant donné une ligne de 10 000 000 engagée à 7 900 000
  Quand un BE de 200 000 est signé
  Alors une notification « Ligne vaccins Ndokayo à 81 % » part au CSAFC et au chef du budget, une seule fois par seuil
```

---

## Intégrations

### US-BUD-240 — Contrat d'opérations pour les autres modules

**En tant que** développeur d'un module consommateur (Achats, Paie, Missions, Formation, Trésorerie), **je veux** une interface unique pour consulter le disponible, mettre de côté, libérer, engager, désengager, liquider, constater un paiement et imputer une recette, **afin de** ne jamais recalculer le budget dans mon module.

- Fonctionnalité : F-BUD-035 · Règles : RG-BUD-031, 039 · Hypothèses : — · Dépend de : US-BUD-113 · Taille : L

```gherkin
Scénario: Appel idempotent
  Quand le module Achats appelle « engager » deux fois avec la même pièce d'origine et la même clé
  Alors un seul engagement existe et les deux appels reçoivent le même résultat

Scénario: Refus lisible
  Quand un engagement est refusé faute de crédit
  Alors le module appelant reçoit un code d'erreur stable et un message destiné à l'utilisateur, avec le disponible

Scénario: Pas d'effet de bord
  Quand le budget refuse une opération
  Alors la transaction du module appelant n'est pas annulée par le budget : c'est l'appelant qui décide de la suite
```

### US-BUD-241 — Brancher la demande d'acquisition sur le visa budgétaire

**En tant que** CSMP ou magasinier, **je veux** que la demande d'acquisition choisisse sa ligne dans le budget et soit visée par le SB ou le CSAFC, **afin qu'**aucun achat ne démarre sans crédit.

- Fonctionnalité : F-BUD-036 · Règles : RG-BUD-004, 023 · Hypothèses : H-19 · Dépend de : US-BUD-201, 240 · Taille : M

```gherkin
Scénario: Ligne choisie
  Quand le CSMP prépare une demande d'acquisition
  Alors il choisit la ligne dans une liste filtrée sur sa structure et l'exercice exécutoire
  Et aucune imputation ne se saisit en texte libre

Scénario: Plus de crédit déclaré d'office
  Quand la demande est créée
  Alors elle est à l'état « à viser » et ne devient « visée » que par US-BUD-201
```

### US-BUD-242 — Brancher l'attribution et la réception des achats

**En tant qu'**agent du budget, **je veux** que l'attribution d'une commande établisse le BE et que la réception déclenche la liquidation, **afin que** le dossier d'achat et l'engagement restent liés.

- Fonctionnalité : F-BUD-036 · Règles : RG-BUD-003, 025 · Hypothèses : — · Dépend de : US-BUD-203, 214, 241 · Taille : M

```gherkin
Scénario: Du dossier d'achat au BE
  Étant donné une demande visée pour 2 400 000
  Quand le DUO attribue la commande à l'offre de 2 300 000
  Alors un BE de 2 300 000 est proposé à l'établissement, lié au dossier d'achat

Scénario: Réception
  Quand la commission de réception signe le PV et que la facture définitive est enregistrée
  Alors le dossier est proposé à la liquidation de l'ordonnateur (US-BUD-214)
```

### US-BUD-243 — Brancher la Trésorerie et la Comptabilité

**En tant que** chef du budget, **je veux** que les paiements et encaissements arrivent de la Trésorerie, et que la Comptabilité reçoive les liquidations sans pouvoir être bloquée par le budget, **afin que** chaque module garde son rôle.

- Fonctionnalité : F-BUD-038 · Règles : RG-BUD-039 · Hypothèses : — · Dépend de : US-BUD-216, 240 · Taille : M

```gherkin
Scénario: Réalisé issu de la liquidation
  Quand une écriture comptable de charge est validée
  Alors elle ne modifie pas les montants du budget
  Et elle est rapprochée de la liquidation correspondante dans l'état de rapprochement (US-BUD-402)

Scénario: Budget indisponible
  Étant donné que l'opération budgétaire échoue
  Quand le comptable valide une écriture
  Alors la validation comptable aboutit et l'anomalie est signalée au chef du budget
```
