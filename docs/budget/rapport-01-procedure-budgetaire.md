# Gestion budgétaire — analyse des procédures de la SODEPA (rapport n° 1)

Rédigé le 8 novembre 2026. Premier rapport du chantier « gestion budgétaire » : il décrit le fonctionnement budgétaire de la SODEPA tel que ses procédures le prescrivent, en tire les règles et les besoins pour l'ERP, et le confronte à ce qui existe déjà (cahier des charges, documents de conception, code).

**Sources.** Procédure budgétaire remise par la SODEPA (`Procédure BUDGETAIRE OK.pdf`, 20 pages, document Word du 9 octobre 2026), convertie dans [docs/client/procedure-budgetaire.md](../client/procedure-budgetaire.md). Recoupée avec les procédures des marchés, financières, de la solde et des pensions et comptables (même dossier), et avec le cahier des charges ERP v1.0 (domaine 4 « Gestion budgétaire », module 4 « Comptabilité budgétaire » du domaine 3). Existant : [Cahier_des_charges_gestion_budgetaire_ERP_ameliore.md](Cahier_des_charges_gestion_budgetaire_ERP_ameliore.md), [BACKLOG_MODULE_BUDGETAIRE_GITHUB_ISSUES.md](../architecture/BACKLOG_MODULE_BUDGETAIRE_GITHUB_ISSUES.md), code du dépôt.

**Renvois.** [PB] procédure budgétaire, [PM] marchés, [PF] financière, [PSP] solde et pensions, [PC] comptable, [CdC] cahier des charges ERP. Ce que la procédure dit est rapporté comme tel ; ce que j'en déduis est signalé (« lecture », « à confirmer »).

---

## 1. Synthèse

1. **La SODEPA budgétise en mode programme, pas en mode moyens** [PB, préambule]. Le budget part d'un plan stratégique quinquennal, se décline en Cadre de Dépenses à Moyen Terme (CDMT, trois ans) puis en Projet de Performance Annuel (PPA) : programmes → actions → activités → tâches, avec indicateurs et cibles. Le modèle « centres de coûts et enveloppes » du cahier budget déjà rédigé dans `docs/budget/` ne correspond pas à ce cadre.
2. **Le cycle a trois temps** : élaboration (25 étapes, 85 jours de délais cumulés plus une étape « ouverte »), exécution (engagement des recettes et des dépenses), contrôle et suivi (contrôle a priori, a posteriori, réallocation des crédits).
3. **Seul le Conseil d'Administration approuve le budget** ; il devient exécutoire par décision signée du Directeur Général. Exercice civil : 1er janvier – 31 décembre.
4. **Chaque unité opérationnelle (ranch, abattoir, station…) prépare son budget** en quatre volets : charges de fonctionnement, produits, activités, investissements. Le Service du Budget consolide.
5. **Le pivot de l'exécution est le bon d'engagement** (BE), établi par le Service du Budget à la Direction Générale, par le CSAFC dans une unité opérationnelle, et signé par le DG ou le Directeur d'unité. Règle cardinale : *tout acte ayant une incidence financière est engagé avant paiement*. Achats, missions, paie, études, réparations, obsèques, foires : toutes ces dépenses passent par un engagement, au Service du Budget ou chez le CSAFC de l'unité.
6. **Le visa budgétaire précède l'engagement** dans plusieurs circuits (TDR, demande d'acquisition, fiche d'entretien) : le Service du Budget vérifie la disponibilité des crédits et vise. Dans l'ERP, c'est une **réservation** de crédit ; le BE est l'**engagement** ; viennent ensuite liquidation (DG ou Directeur d'unité, sur facture définitive et procès-verbal) et paiement (Service Financier et des Recettes).
7. **Les seuils structurent les circuits** : 100 000 FCFA (caisse ou banque, achat sans commande), 500 000 FCFA (bon de commande), 15 000 000 FCFA (lettre commande), 50 000 000 FCFA (marché). Les procédures ne nomment pas toutes la même chose « menue dépense » (moins de 500 000 FCFA en [PB], moins de 100 000 FCFA en [PM] et [PF]).
8. **La réallocation des crédits est la seule modification du budget décrite.** Elle est rédigée comme une *proposition* (« circuit proposé », « il est recommandé ») et superpose trois grilles de compétence qui ne concordent pas (§ 6.3). Elle demande une décision de la SODEPA avant d'être codée.
9. **Le suivi est quotidien, mensuel et trimestriel** : situation d'exécution par programme et par unité mise à jour chaque jour, rapport mensuel le 5 (unités) et le 10 (Direction Générale), rapport trimestriel de performance.
10. **Beaucoup reste à préciser** : contenu du bon d'engagement, désengagement, écart entre facture et engagement, fin d'exercice, budget non encore approuvé au 1er janvier, plan de codification. 30 questions sont listées au § 10.
11. **Le module budget du dépôt ne peut pas porter cette procédure tel quel** : une ligne y est un compte comptable et une section, sans programme, sans structure ni exercice ; les visas sont codés en dur (500 000 et 5 000 000 FCFA) ; la liquidation libère le crédit sans enregistrer le réalisé ; le maker-checker budget n'est pas utilisable de bout en bout. Le cœur est à reconstruire (§ 8.3).
12. **Proposition de démarche** : commencer par l'exécution (référentiels, budget approuvé importé, visa, bon d'engagement, disponible, suivi), puis la réallocation, et seulement ensuite l'élaboration (campagne, CDMT, PPA), qui ne sert qu'une fois par an.

---

## 2. Le modèle budgétaire de la SODEPA

### 2.1 Budget-programme

La SODEPA « s'arrime à la politique gouvernementale » et base son action sur le **budget programme et non le budget de moyens** [PB, préambule]. Trois principes :

| Principe | Énoncé de la procédure | Traduction pour l'ERP (lecture) |
|---|---|---|
| Vision | « on veut faire ceci » | objectifs et indicateurs de performance par programme |
| Ancrage et déclinaison | « la source et comment le faire » | rattachement de chaque crédit à un programme, une action, une activité, une tâche |
| Codification et suivi | « comment cela doit se faire et a été fait » | plan de codification budgétaire, suivi d'exécution par code |

### 2.2 Documents budgétaires

| Document | Rôle | Produit par | Source |
|---|---|---|---|
| Plan stratégique quinquennal | Cadre d'origine du PPA | — | [PB ch. 1 § 1] |
| Note de cadrage économique | Accompagne les orientations du DG | — | [PB, préambule] |
| Lettre-circulaire (avec chronogramme) | Lance la campagne, fixe les orientations | DPD/CSPP, signée par le DG | [PB § 4.2] |
| Rapport d'évaluation de l'exécution technique et financière | Référence de départ | DCAI, CSPP, ASCG, CSB | [PB § 4.2] |
| Cadre de Dépenses à Moyen Terme (CDMT) | Programmation sur trois ans, par programme, avec indicateurs | Chefs de programme, consolidé par CSPP/CEA | [PB § 3, § 4.2] |
| Projet de Performance Annuel (PPA) | Stratégie, objectifs, activités, indicateurs, cibles, moyens pour l'année | Chefs de programme, consolidé par CSPP/CEA | [PB § 2, § 4.2] |
| Rapport de Performance Annuel (RAP) | Réalisations et résultats de l'année écoulée | — | [PB, préambule] |
| Termes de référence (TDR) | « Maturation » des actions, activités, tâches ; pièce d'engagement des activités | Services initiateurs | [PB, préambule ; ch. II § 2.2] |
| Avant-projets de budget des unités | Budgets des unités opérationnelles | Chefs d'unité | [PB § 4.1 b, § 4.2] |
| Budget approuvé et décision exécutoire | Résolution du CA, décision du DG | CA, DG | [PB § 4.1 c, § 4.2] |
| Plan de codification budgétaire | Nomenclature des codes | CSB/SDFBM/DPD/DAF, signé par le DG | [PB § 4.2] |
| Annexes | Budget d'exploitation ; budget d'investissement avec plan d'opérations en capital, plan de trésorerie et plan de financement | — | [PB § 4.2, NB] |

### 2.3 Les axes d'une ligne budgétaire (lecture)

La procédure ne définit pas la ligne budgétaire. Trois axes ressortent du texte :

- **programmatique** : programme → action → activité → tâche [PB, préambule, § 4.2] ;
- **organisationnel** : Direction Générale et unités opérationnelles, chacune avec son budget [PB § 4.1 b] ; le cahier des charges ajoute directions, sous-directions, services, ranchs, abattoirs, entrepôts frigorifiques, boucheries pilotes, projets et sources de financement [CdC D4 module 2] ;
- **par nature** : fonctionnement, produits, activités, investissements [PB § 4.1 b] ; le tableau type de réallocation désigne les lignes par des numéros de comptes de charges et d'immobilisations (6021, 6153, 2182, 6051) [PB ch. III § 3 e], ce qui suggère une nomenclature calée sur le plan comptable SYSCOHADA.

Une ligne budgétaire serait donc l'intersection *structure × élément programmatique × nature*. Le plan de codification budgétaire de la SODEPA dira lequel de ces axes porte le contrôle de disponibilité : c'est la première pièce à obtenir (Q-01).

---

## 3. Acteurs

Les développements marqués « organigramme » viennent de `SodepaScrapedDataSeeder` (référentiel semé dans le code) ; ceux marqués « à confirmer » sont déduits.

| Sigle | Développement | Rôle budgétaire |
|---|---|---|
| CA | Conseil d'Administration | Seule instance d'approbation du budget et du PPA ; autorise les réallocations lourdes |
| DG | Directeur Général | Lance la campagne, préside, valide en interne, rend le budget exécutoire ; signe les BE de la Direction Générale ; arbitre les réallocations ; peut prendre une mesure conservatoire |
| DAF | Direction Administrative et Financière | Appuie les chefs de programme, coordonne la confection ; propose les réallocations |
| SDFBM | Sous-Direction des Finances, du Budget et du Matériel | Visa intermédiaire des BE ; analyse des réallocations |
| SB / CSB | Service du Budget / son chef | Consolide les budgets ; vérifie, impute, établit les BE ; tient la fiche de suivi ; contrôle a priori et a posteriori ; rapports |
| DPD | Division Prospective et Développement (organigramme) | Prépare la lettre-circulaire et la décision du groupe de travail ; organise la concertation |
| CSPP | Cellule Statistiques, Programmation et Projets, rattachée à la DPD (organigramme) | Consolide CDMT et PPA ; membre du comité de réallocation |
| CEA | à confirmer | Consolide CDMT et PPA avec la CSPP |
| DCAI | Division du Contrôle et de l'Audit Interne | Rapport d'évaluation de l'exécution ; membre du comité de réallocation |
| ASCG | à confirmer (l'organigramme semé a un « Chargé du Suivi et Contrôle de Gestion ») | Rapport d'évaluation de l'exécution |
| Chefs de programme | — | Élaborent CDMT, PPA et budget de leur programme ; débats budgétaires |
| DUO | Directeur d'Unité Opérationnelle | Valide les besoins, attribue les commandes, signe les BE et liquide dans son unité |
| CSAFC (aussi « C.S.A.F ») | Chef de Service Administratif, Financier et Comptable d'une unité [PF] | Établit les BE de l'unité, impute, tient la fiche de suivi, comptabilise les recettes |
| CSMP / SMP | Chef du Service des Marchés et du Patrimoine | Demandes d'acquisition, consultations, réception ; reçoit les originaux des BE |
| CSFR / SFR | Chef du Service Financier et des Recettes | Paiement ; reçoit les BE des missions et études |
| CSC | Chef du Service de la Comptabilité | Comptabilise ; membre du comité de réallocation |
| CSSP | Chef du Service de la Solde et des Pensions | Paie, ordres de mission ; transmet au Service du Budget pour engagement [PSP] |
| CSCo | Service du Courrier (organigramme : « Service du Courrier & Liaison ») | Formalise et ventile les actes |
| CBM | Chef de la Brigade de Maintenance | Diagnostic des réparations de véhicules |
| SBB | à confirmer, probablement une coquille pour SB | Contrôle a priori des dépenses [PB ch. III § 1] |

**Lecture de la notation « A/B/C/D ».** Les tableaux de circuits écrivent « DG/DAF/SDFBM/SB reçoit la liasse » puis « SB/SDFBM/DAF/DG signe le bon d'engagement ». Je lis la première forme comme la cotation descendante (le dossier part du DG et descend jusqu'au SB) et la seconde comme la remontée des visas jusqu'au signataire. Cette lecture fixe l'ordre des étapes du circuit dans le moteur de workflow ; elle est à confirmer (Q-12).

---

## 4. Élaboration du budget

### 4.1 Objectifs et règles

Objectifs [PB ch. 1 § 1] : un PPA budgétisé réaliste, précis et concis, issu du plan stratégique quinquennal ; coordonner les actions ; programmer et chiffrer les activités de l'année ; distribuer les responsabilités ; maîtriser l'exécution.

Règles de gestion [PB § 3] : la campagne débute par la circulaire du DG ; un groupe de travail est créé par décision du DG ; le DG lance les travaux devant les chefs de programme et les responsables d'unité ; l'exécution en cours est évaluée ; programmes, actions et orientations sont déclinés en CDMT avec des indicateurs mesurables ; le PPA est consolidé ; l'avant-projet de PPA est validé en présence du DG.

### 4.2 Trois phases [PB § 4.1]

- **Préparation** : le DG fixe une politique de gestion par objectifs mesurables. Éléments pris en compte : historique des ventes, programmes d'activités, plan stratégique, planifications précédentes des activités reconduites.
- **Confection** : chaque chef de programme élabore son budget avec l'appui de la DAF, qui programme et coordonne ; le budget est segmenté ; chaque unité opérationnelle prépare quatre budgets (charges de fonctionnement, produits, activités, investissements) ; le Service du Budget centralise et consolide en un projet de budget global annuel.
- **Approbation** : l'avant-projet passe à l'arbitrage du comité d'élaboration du budget (créé par décision du DG), qui le corrige ; le projet corrigé va au CA, **unique instance d'approbation** ; le budget approuvé devient exécutoire par décision signée du DG.

### 4.3 Chronogramme [PB § 4.2]

| # | Phase | Intervenant | Tâche | Livrable | Délai |
|---|---|---|---|---|---|
| 1 | Lettre-circulaire | DPD/CSPP | Prépare la lettre-circulaire (orientations générales) | Projet de lettre-circulaire et chronogramme | 15 j après la circulaire du Président de la République |
| 2 | | DG | Signe | Lettre-circulaire signée, chronogramme validé | 2 j |
| 3 | | Secrétariat DG/CSCo | Formalise et ventile aux services | Lettre-circulaire diffusée | 1 j |
| 4 | Groupe de travail | DPD/CSPP | Prépare la décision créant le groupe de travail | Projet de décision | 2 j |
| 5 | | DG | Signe | Décision signée et transmise | 2 j |
| 6 | | DPD | Organise la réunion de concertation | Convocation, projet de chronogramme | ouvert |
| 7 | | DG | Préside, précise les orientations, valide le chronogramme | Fiche de présence, compte rendu, chronogramme validé | 1 j |
| 8 | PPA | DCAI, CSPP, ASCG, CSB | Rapport d'évaluation de l'exécution technique et financière | Rapport d'évaluation | 5 j |
| 9 | | Chefs de programme | Révisent et présentent le CDMT de leur programme | CDMT validé par le groupe de travail | 5 j |
| 10 | | CSPP/CEA | Consolident les CDMT | Projet de CDMT consolidé | 5 j |
| 11 | | Chefs de programme | Élaborent le projet de performance par programme | Projet de performance | 5 j |
| 12 | | CSPP/CEA et secrétariat technique du groupe | Consolident | Projet de performance consolidé | 3 j |
| 13 | | Groupe de travail | Valide l'avant-projet de PPA | Avant-projet de PPA validé | 1 j |
| 14 | Budgétisation | Chefs de programme | Déclinent les activités du CDMT en tâches avec indicateurs | Projet de PPA | 5 j |
| 15 | | Chefs de programme | Budgétisent les tâches | Projet de PPA chiffré | 5 j |
| 16 | | Chefs d'unité opérationnelle | Transmettent les avant-projets de budget et les éléments de maturation à la Direction Générale | Copies électroniques des avant-projets | 2 j |
| 17 | | Groupe de travail | Reçoit, consolide, renvoie aux chefs de programme | Avant-projets consolidés | 3 j |
| 18 | | Groupe de travail | Valide et intègre dans le projet de budget | Avant-projet de budget validé | 3 j |
| 19 | | Chefs de programme | Débats budgétaires, consolidation | Projet de budget | 3 j |
| 20 | Approbation | DG | Reçoit et valide en interne | Projet de budget validé en interne | 1 j |
| 21 | | CA | Examine et approuve le PPA et le budget | Résolution signée | 1 j |
| 22 | | DG | Signe la décision rendant le budget exécutoire | Décision signée | 2 j après le CA |
| 23 | | DG/Secrétariat | Transmet le budget aux chefs de programme et au SB | Budget diffusé | 1 j après la décision |
| 24 | Codification | CSB/SDFBM/DPD/DAF | Prépare le plan de codification budgétaire | Projet de plan | 10 j |
| 25 | | DG | Signe | Plan de codification signé | 2 j |

Délais cumulés : 18 j (lettre-circulaire) + 5 j et une étape ouverte (groupe de travail) + 24 j (PPA) + 21 j (budgétisation) + 5 j (approbation) + 12 j (codification) = **85 jours** plus la concertation. Le texte ne dit pas s'il s'agit de jours ouvrés ou calendaires (Q-05), ni combien de temps s'écoule entre la validation interne et la session du CA.

### 4.4 Ce que l'ERP doit couvrir pour l'élaboration

| Réf. | Besoin | Source |
|---|---|---|
| EL-01 | Campagne budgétaire par exercice, avec chronogramme : étapes, responsables, échéances, relances | [PB § 4.2] ; [CdC D4 module 3, « calendrier budgétaire »] |
| EL-02 | Nomenclature programme → action → activité → tâche, avec responsable (chef de programme), indicateurs et cibles | [PB, préambule, § 4.2] |
| EL-03 | CDMT : montants et indicateurs sur trois ans par programme | [PB § 3, § 4.2] ; [CdC D4 module 1, « budgets pluriannuels »] |
| EL-04 | Saisie des avant-projets par unité, en quatre volets (fonctionnement, produits, activités, investissements) | [PB § 4.1 b] |
| EL-05 | Consolidation automatique des avant-projets par le SB et le groupe de travail | [PB § 4.1 b, § 4.2] ; [CdC D4 module 1] |
| EL-06 | Versions successives conservées : avant-projets des unités, avant-projet consolidé, projet arbitré, projet validé en interne, budget approuvé, budget exécutoire | [PB § 4.1 c, § 4.2] ; [CdC D4 module 1, « historisation des versions »] |
| EL-07 | Pièces de la campagne rattachées : lettre-circulaire, décision, comptes rendus, rapport d'évaluation, résolution du CA, décision exécutoire | [PB § 4.2] |
| EL-08 | Ouverture des crédits à la date de la décision exécutoire, pas avant | [PB § 4.1 c] |
| EL-09 | Annexes : budget d'exploitation, budget d'investissement avec plan d'opérations en capital, plan de trésorerie, plan de financement | [PB § 4.2, NB] ; [CdC D4 module 3] |
| EL-10 | Import des budgets préparés hors ERP (Excel) : la campagne de l'exercice de mise en service se fera vraisemblablement hors outil | lecture |

### 4.5 Points flous de l'élaboration

- « Comité d'élaboration du budget » [§ 4.1 c] et « groupe de travail » [§ 4.2] : une ou deux instances ? (Q-04)
- Le plan de codification est produit **après** l'approbation (étapes 24-25), alors que les tâches sont budgétisées aux étapes 14-15 : il faut des codes avant. Le plan est-il permanent et seulement mis à jour chaque année ? (Q-03)
- Aucune règle si le budget n'est pas exécutoire au 1er janvier (Q-06).
- Les débats budgétaires (étape 19) et l'arbitrage n'ont pas de règle écrite (enveloppes plafonds par programme ? par unité ?) (Q-07).

---

## 5. Exécution du budget

### 5.1 Principes

- *Tout acte de gestion ayant une incidence financière sur le budget doit être engagé avant paiement* [PB ch. II § 2.3].
- L'acte d'engagement est entendu largement [PB ch. II § 2.2] : acquisitions (biens, services), **dépenses de personnel** (salaires, gratifications, primes, frais funéraires, contributions sociales), **termes de référence** (montant global de l'activité inscrite au budget), **dépenses diverses** (contrats, baux, conventions, indemnités de session des administrateurs).
- Avant tout engagement, le Service du Budget vérifie trois points [PB ch. II § 2.1] : la dépense est inscrite au budget approuvé ; l'imputation est correcte ; les pièces sont conformes et régulières.
- Toutes les acquisitions se font « par référence au budget approuvé par le CA » [PM ch. I § 1.2].

### 5.2 Engagement des recettes [PB ch. II § 1]

La procédure demande : la mise à jour des outils de suivi des recettes, présentés par nature et comptabilisés par les CSAFC ; la comparaison des documents budgétaires, financiers et comptables ; la tenue d'un fichier de suivi mensuel des recettes. Les recettes propres naissent dans les unités (vente de bovins, de viande, de semences fourragères, prestations d'abattage, d'entreposage, de transhumance, boucherie) et transitent par la caisse A puis la banque [PF ch. 1 § II]. Le terme « engagement des recettes » ne décrit pas d'acte précis : dans l'ERP, il s'agit au minimum d'imputer chaque recette réalisée sur une ligne du budget des produits (Q-15).

### 5.3 Chaîne de la dépense (consolidée sur les quatre procédures)

```mermaid
flowchart LR
  A[Expression du besoin<br>ou TDR, OM, fiche d'entretien] --> B[Visa budgétaire<br>SB / CSAFC]
  B --> C[Autorisation<br>DG / DUO]
  C --> D[Consultation<br>et attribution]
  D --> E[Bon d'engagement<br>+ imputation + fiche de suivi]
  E --> F[Signature du BE<br>DG / DUO]
  F --> G[Bon de commande,<br>lettre commande ou marché]
  G --> H[Réception<br>PV, service fait]
  H --> I[Liquidation<br>DG / DUO]
  I --> J[Comptabilisation<br>CSC / CSAFC]
  J --> K[Paiement<br>CSFR : chèque, virement, caisse]
```

Correspondance avec les états budgétaires de l'ERP (lecture, cohérente avec [CdC D4 module 4]) :

| Étape de la procédure | État budgétaire | Effet sur le disponible |
|---|---|---|
| Visa budgétaire | Réservé (pré-engagement) | Diminue |
| Bon d'engagement signé | Engagé | Remplace la réservation |
| Liquidation | Liquidé | Aucun (le montant passe d'engagé à liquidé ; l'écart avec l'engagement est à traiter, Q-18) |
| Paiement | Payé | Aucun |

Disponible = crédit ouvert − réservé − engagé non liquidé − liquidé. Restes à engager et à payer en découlent [CdC D4 module 4].

### 5.4 Seuils

| Montant TTC (FCFA) | Support de la commande | Mise en concurrence | Attribution | Règlement | Sources |
|---|---|---|---|---|---|
| moins de 100 000 | aucune commande ; un agent désigné achète et présente produit et facture | non | autorisation du DG ou du Directeur d'unité | caisse B (espèces) | [PM § 1.3.2 b], [PF ch. 1 § II B] |
| 100 000 à 499 999 | aucun bon de commande ; bon d'engagement en trois exemplaires | trois factures pro forma, note de sélection | DG ou DUO, puis DAF | chèque ou virement (à partir de 100 000) | [PM § 1.3.2 b], [PF ch. 1 § I b] |
| 500 000 à 14 999 999 | **bon de commande** et bon d'engagement | trois pro forma, fournisseurs agréés (appel à manifestation d'intérêt annuel), comité de sélection à la Direction Générale | DG sur rapport du comité ; DUO dans l'unité | chèque ou virement, programmation FIFO | [PM § 1.2, § 1.3.2 b], [PB ch. II § 2.3 a], [PF ch. 2] |
| 15 000 000 à 49 999 999 | **lettre commande** | règles des marchés publics | — | idem | [PM § 1.2] |
| 50 000 000 et plus | **marché** | règles des marchés publics | — | idem | [PM § 1.2] |

Autres valeurs : plafond d'approvisionnement de la caisse B 7 500 000 FCFA, fixé par décision du DG ; certaines dépenses de plus de 100 000 FCFA peuvent passer par la caisse (carburant, indemnités, salaires à vue, primes de stage, transport) [PF ch. 1 § II B] ; avance de mission de 65 % « sous réserve de la disponibilité des crédits » [PSP, missions] ; seuil de 5 % pour la réallocation (§ 6.3). Achats de bétail sur pied : une commission ad hoc [PM § 1.2].

**Incohérence de vocabulaire.** [PB] appelle « menues dépenses » celles de moins de 500 000 FCFA ; [PM] et [PF] réservent ce terme à celles de moins de 100 000 FCFA. Et [PB] exige un BE pour toute menue dépense, alors que [PM] ne mentionne aucun BE en dessous de 100 000 FCFA. Les achats de caisse B sont-ils engagés un par un, ou l'approvisionnement de la caisse est-il engagé globalement ? (Q-16, Q-17)

### 5.5 Circuits d'engagement

Délais : somme des délais chiffrés du circuit ; « n.d. » si une étape n'en a pas.

| Cas | Déclencheur | Visa ou contrôle de disponibilité | BE établi par | BE signé par | Suite | Délai | Source |
|---|---|---|---|---|---|---|---|
| Dépense de moins de 500 000, Direction Générale | Liasse de dépense | SB (imputation, fiche de suivi) | SB | DG, après visas SB → SDFBM → DAF | SB archive une copie, originaux au SMP | 4 j | [PB ch. II § 2.3 a] |
| Dépense de moins de 500 000, unité | Expression de besoin et attestation de carence | — | CSAFC | DUO | — | 4 j | idem |
| Dépense d'au moins 500 000, Direction Générale | Pro forma retenue | SB | SB, avec le bon de commande | DG, après visas | SB transmet le bon de commande au prestataire pour enregistrement | 4 j | idem |
| Dépense d'au moins 500 000, unité | Expression de besoin, trois fournisseurs consultés | — | CSAFC, avec le bon de commande | DUO, qui attribue à la meilleure offre financière | CSAFC transmet le bon de commande | 5 j | idem |
| Mission | TDR validés, projet de note de service, ordres de mission | Visa budgétaire SB | SB | DG | Transmis au SFR pour paiement | 3 j et n.d. | [PB cas 1], [PSP], [PF ch. 2] |
| Réparation d'un véhicule commercial | Fiche d'entretien, diagnostic et pro forma (logisticien, CBM, prestataire) | Visa budgétaire SB | SB (et bon de commande au-delà de 499 999) | DG | SB archive, originaux au SMP | 9 j et n.d. | [PB cas 2 a] |
| Réparation d'un véhicule de service | Idem, porté par le comptable-matières | Visa budgétaire SB | SB | DG | idem | 9 j et n.d. | [PB cas 2 b] |
| Étude, consultant | TDR | SB vérifie la disponibilité et vise | CSB, avec un bon de commande éventuel | DG | Bon de commande au prestataire, TDR au CSFR | 6 j et n.d. | [PB cas 3] |
| Demande d'acquisition | Expression de besoin, attestation de carence | CSB ou CSAFC « vérifie les disponibilités budgétaires et appose son visa » | DAF/SDFBM/CSB selon le seuil | DG ou DUO | Consultation, réception, liquidation | selon seuil | [PM § 1.3.2] |
| Paie mensuelle | États de paie signés par le DG | — | CSB, « engagement et imputation » | DG, avec les titres de paiement | Comptabilisation, puis paiement | n.d. | [PSP ch. 1] |
| Obsèques, décès | Décision signée, état récapitulatif | — | CSB | — | CSFR pour paiement | n.d. | [PSP] |
| Foires et cérémonies officielles | TDR signé par le DG | — | CSB | DG | CSFR paie (espèces ou chèque) | 4 j | [PF ch. 2] |

Remarques :

- La paie est engagée **après** que le DG a signé les états : l'engagement y est une régularisation, pas un contrôle préalable. Dans l'ERP, le module de paie produirait l'engagement mensuel, imputé par nature et par structure.
- Le bon d'engagement est établi en trois exemplaires [PM] ; le SB en archive une copie [PB].
- Aucune procédure ne décrit le contenu du BE, sa numérotation, son annulation, sa modification, ni les engagements sur plusieurs exercices (Q-08 à Q-11, Q-19).
- Les signatures sont manuscrites. Le circuit dans l'ERP peut soit remplacer ces signatures par des validations électroniques, soit enregistrer le document signé et numérisé (Q-13).

### 5.6 Ce que l'ERP doit faire pour l'exécution

| Réf. | Besoin | Source |
|---|---|---|
| EX-01 | Réserver le crédit au visa budgétaire, avec contrôle de disponibilité bloquant | [PB ch. II § 2.1], [PM § 1.3.2], [CdC D4 module 5] |
| EX-02 | Établir le BE (Direction Générale ou unité) : ligne d'imputation, montant TTC, tiers, objet, nature de l'acte, pièces ; contrôle des trois vérifications préalables | [PB ch. II § 2.1-2.3] |
| EX-03 | Router le BE selon la structure et le seuil : SB → SDFBM → DAF → DG, ou CSAFC → DUO ; délais par étape | [PB], [PM] |
| EX-04 | Exiger les pièces selon le type de dépense et le seuil (expression de besoin, attestation de carence, pro forma, trois pro forma, note de sélection, TDR, ordre de mission, fiche d'entretien, dossier fiscal…) | [PB ch. III § 1], [PM] |
| EX-05 | Générer les documents : BE (trois exemplaires), bon de commande | [PB], [PM] |
| EX-06 | Mettre à jour la situation d'exécution à chaque mouvement (la « fiche de suivi du budget » actuelle) | [PB ch. II § 2.3], [CdC D4 module 4] |
| EX-07 | Liquider sur pièces (facture définitive timbrée, bordereau de livraison ou attestation de service fait, procès-verbal) et transmettre au paiement | [PM], [PF ch. 2] |
| EX-08 | Recevoir du module de paie l'engagement mensuel, et des autres modules (missions, achats) leurs demandes de visa | [PSP], [PF] |
| EX-09 | Imputer les recettes réalisées sur le budget des produits ; fiche de suivi mensuelle des recettes | [PB ch. II § 1, ch. III § 2] |
| EX-10 | Engager depuis une unité opérationnelle sur les crédits de cette unité | [PB ch. II § 2.3 a] |

---

## 6. Contrôle et suivi

### 6.1 Contrôle a priori [PB ch. III § 1]

Le service du budget vérifie l'existence et la régularité des pièces :

| Objet | Pièces |
|---|---|
| Pièce ayant déclenché la dépense | expression du besoin ou TDR ; projets de contrats, conventions, baux ; bon d'engagement ; attestation de carence ; bon de commande ; cachets composteurs |
| Pièces justificatives | facture pro forma ou devis estimatif ; facture définitive ; bordereau de livraison ; attestation de non-exclusion des marchés publics (ARMP) ; dossier fiscal |

Aucun délai n'est fixé (« non défini » partout).

### 6.2 Contrôle a posteriori et rapports [PB ch. III § 2]

Le texte attend un « programme de comptabilité intégrant un volet gestion budgétaire » : c'est l'ERP.

| Tâche du SB | Livrable | Fréquence ou échéance |
|---|---|---|
| Mise à jour de la situation d'exécution par programme et par unité | Fiche de suivi des engagements | quotidienne |
| Exploitation des rapports d'activité des unités, notes | — | mensuelle |
| Rapport d'activité mensuel « rédigé automatiquement » : analyse des lignes, dépassements et économies avec leurs causes, niveau des engagements et de l'exécution en valeur et en nombre | Rapport d'activité | le 5 du mois (unités), le 10 (Direction Générale) |
| Niveau d'exécution budgétaire | Rapport trimestriel de performance | trimestrielle |
| Suivi des recettes | Fiche de suivi mensuelle des recettes | mensuelle |
| Consolidation | Budgets des produits, des charges, d'investissement consolidés | non défini |

Le rapport mensuel doit expliquer des **dépassements** : la procédure admet donc qu'une ligne puisse être dépassée en réalisé (recettes inférieures, liquidation supérieure à l'engagement), alors que le cahier des charges exige le blocage de tout dépassement [CdC D4 § 4]. Les deux se concilient si le blocage porte sur l'engagement et le rapport sur les écarts de réalisation (Q-20).

### 6.3 Réallocation des crédits [PB ch. III § 3]

**Principe.** Déplacer des crédits disponibles d'une ligne, activité, unité ou projet vers une autre jugée prioritaire, sans compromettre l'équilibre financier, les engagements pris ni les objectifs. Trois exigences : régularité (aucune dépense sans crédit disponible), soutenabilité (pas de tension de trésorerie), traçabilité (toute modification significative justifiée, validée, archivée).

**Typologie (§ b).**

| Type | Champ | Autorité |
|---|---|---|
| Simple (virement interne) | même direction, même nature de dépense, sans changer le total, sans toucher les investissements majeurs, le plan de passation des marchés, ni une ressource nouvelle | DG, sur proposition de la DAF, après vérification des crédits |
| Significative | entre fonctionnement et investissement ; entre unités ; entre projets stratégiques ; subventions, emprunts, investissements, engagements pluriannuels ; plan de trésorerie ; budget approuvé par le CA | note motivée au DG, puis résolution du CA « lorsque l'incidence financière est significative » |
| Exceptionnelle | urgences : panne grave d'un équipement stratégique, urgence sanitaire, sécuritaire ou vétérinaire, continuité du service, dépense imprévue indispensable, obligation réglementaire ou judiciaire | mesure conservatoire du DG, régularisée devant le CA si elle dépasse son seuil de compétence |

**Circuit proposé (§ c).**

1. Le service demandeur adresse une *fiche de demande de réallocation* à la Direction Générale, sous couvert de la DAF : objet, ligne à renforcer, ligne à diminuer, montant, justification technique, degré d'urgence, impact attendu, risques en cas de refus, pièces.
2. La DAF (SDFBM, SB) vérifie : crédits disponibles sur la ligne source, absence d'engagement déjà pris sur cette ligne, conformité à l'objet social, impact sur la trésorerie, sur le plan de passation des marchés, sur les objectifs de performance, conformité au budget voté. Elle produit une **note technique et financière**.
3. Un **comité interne de réallocation** (« il est recommandé de créer ») donne un avis sur l'opportunité, la régularité, la soutenabilité, la priorité, la source de financement interne. Membres : DAF, chef de la DPD, chef de la DCAI, SDFBM, CSPP, sous-directeur concerné, CSB, CSC, CSFR.
4. Le DG approuve, rejette, demande des compléments, saisit le CA ou la tutelle.
5. Validation selon le niveau (grille ci-dessous).

**Grille d'autorité (§ c, étape 5).**

| Réallocation | Autorité proposée |
|---|---|
| Mineure, dans une même ligne ou activité | DAF/SDFBM/SB, validation du DG |
| Entre services ou unités | DG |
| Entre fonctionnement et investissement | CA |
| Touchant emprunt, subvention, dette ou investissement majeur | CA, avec information ou avis des tutelles selon les statuts |
| Modifiant fortement le budget approuvé | CA |
| Ayant une incidence sur la stratégie globale | CA, tutelle technique et tutelle financière |

**Recommandation finale (§ g).** Trois niveaux, à adopter par note de service du DG ou dans le manuel de procédures :

1. au plus 5 % de la ligne concernée : la DAF initie, le DG valide ;
2. plus de 5 %, ou plusieurs directions : la DAF initie, le comité approuve, le DG valide ;
3. investissement, endettement, subventions, plan de performance ou équilibre général : le CA.

**Dossier (§ d), onze pièces** : demande, note explicative, état d'exécution actualisé, situation de trésorerie, tableau des lignes diminuées et augmentées, avis de la DAF, avis du comité, projet de décision du DG, projet de résolution du CA le cas échéant, tableau budgétaire rectifié, pièces techniques (pro forma, devis, rapport de panne, note d'urgence…).

**Tableau type (§ e)** : ligne source, libellé, montant initial, montant consommé, crédit disponible, montant à transférer, ligne bénéficiaire, crédit initial, nouveau crédit. **Décision type (§ f)** : quatre articles (montant et lignes, objet, ajustements par la DAF, exécution).

**Précautions** : pas de réallocation non formalisée, pas de dépense avant validation, pas de prélèvement sur des lignes déjà engagées, pas de réallocation masquant une insuffisance structurelle, pas de modifications répétées sans rapport consolidé, pas de dépense hors objet social, pas de modification indirecte du plan d'investissement sans information du CA. Toute réallocation laisse une trace : demande, analyse, décision, mise à jour budgétaire, imputation comptable, rapport d'exécution.

**Ce qui ne concorde pas** (à trancher par la SODEPA avant tout développement, Q-22 à Q-27) :

- Trois grilles se recouvrent (typologie, grille d'autorité, recommandation à 5 %). Exemple : une réallocation entre deux unités relève du DG dans la grille d'autorité, du CA dans la typologie si elle est « significative », et du comité puis du DG dans la recommandation si elle touche plusieurs directions.
- « Significative », « fortement », « investissement majeur » ne sont pas chiffrés ; seul le seuil de 5 % l'est, et on ignore s'il s'apprécie sur la ligne source ou la ligne bénéficiaire.
- Le tableau type transfère 10 000 000 FCFA du compte 2182 « Matériel roulant » (investissement) vers le compte 6051 « Pneumatiques » (fonctionnement) : c'est précisément le cas réservé au CA par la grille d'autorité.
- « Absence d'engagement déjà pris sur la ligne source » (étape 2) interdirait de prélever sur toute ligne déjà entamée ; la précaution correspondante parle seulement de ne pas prélever sur des crédits déjà engagés. Je retiens provisoirement : montant transférable ≤ disponible de la ligne source.
- Le comité n'a ni quorum ni délai.

**Conséquence pour l'ERP.** Le choix du circuit doit être une **table de décision paramétrable** (critères : pourcentage, structures concernées, changement de section fonctionnement ou investissement, nature de la ligne, urgence ; résultat : circuit), versionnée avec date d'effet, plutôt qu'une règle codée. C'est conforme à la décision « tout paramétrable » prise pour le module RH.

---

## 7. Règles de gestion consolidées

| Réf. | Règle | Source |
|---|---|---|
| RG-01 | L'exercice budgétaire va du 1er janvier au 31 décembre | [PB § 4.1] |
| RG-02 | Le CA est la seule instance d'approbation ; le budget est exécutoire par décision du DG | [PB § 4.1 c] |
| RG-03 | Toute dépense est inscrite au budget approuvé ; aucune dépense sans crédit disponible | [PB ch. II § 2.1, ch. III § 3 a], [PM § 1.2] |
| RG-04 | Tout acte à incidence financière est engagé avant paiement | [PB ch. II § 2.3] |
| RG-05 | Avant engagement : inscription au budget, imputation correcte, pièces conformes | [PB ch. II § 2.1] |
| RG-06 | Un visa budgétaire (vérification de disponibilité) précède l'engagement des missions, réparations, études et acquisitions | [PB], [PM § 1.3.2 a] |
| RG-07 | À partir de 500 000 FCFA TTC : bon de commande ; à partir de 15 000 000 : lettre commande ; à partir de 50 000 000 : marché | [PM § 1.2], [PB] |
| RG-08 | À partir de 100 000 FCFA : trois pro forma ; à partir de 500 000 : fournisseurs agréés et comité de sélection (Direction Générale) | [PM § 1.3.2 b] |
| RG-09 | Moins de 100 000 FCFA : pas de commande, règlement par caisse B | [PM], [PF] |
| RG-10 | Le BE est signé par le DG (Direction Générale) ou le DUO (unité) | [PB], [PM] |
| RG-11 | La liquidation est faite par le DG ou le DUO sur facture définitive, bordereau de livraison et procès-verbal | [PM], [PF ch. 2] |
| RG-12 | Paiement par chèque ou virement privilégié ; programmation des factures par ancienneté (FIFO) | [PF ch. 2] |
| RG-13 | La situation d'exécution par programme et par unité est mise à jour chaque jour | [PB ch. III § 2] |
| RG-14 | Rapport mensuel le 5 (unités) et le 10 (Direction Générale) ; rapport trimestriel de performance | [PB ch. III § 2] |
| RG-15 | Une réallocation ne prélève que du crédit disponible, sans compromettre l'équilibre ni les engagements | [PB ch. III § 3] |
| RG-16 | Réallocation entre fonctionnement et investissement, ou touchant dette, subvention, investissement majeur : CA | [PB ch. III § 3 c, g] |
| RG-17 | En urgence, le DG peut prendre une mesure conservatoire, régularisée devant le CA | [PB ch. III § 3 b.3] |
| RG-18 | Toute réallocation est tracée de la demande au rapport d'exécution | [PB ch. III § 3, précautions] |
| RG-19 | Avance de mission de 65 % sous réserve de crédits disponibles | [PSP, missions] |
| RG-20 | Bons de commande, lettres commandes et marchés archivés dix ans | [PM § 1.2] |

Les montants (100 000, 500 000, 15 000 000, 50 000 000, 7 500 000, 5 %, 65 %) changent par décision : ils vont en paramètres datés, pas dans le code.

---

## 8. Confrontation avec l'existant

### 8.1 Cahier des charges ERP (domaine 4)

Le domaine 4 est cohérent avec la procédure mais plus large et moins précis :

- **Couvert par les deux** : élaboration et consolidation, versions, budgets par structure (le CdC liste ranchs, abattoirs, entrepôts, boucheries, projets, sources de financement), engagements, réservations, liquidations, ordonnancements, paiements, restes à engager et à payer, contrôle bloquant avant engagement, réaménagements, tableaux de bord.
- **Apporté par la procédure** : la logique programme (PPA, CDMT, chefs de programme, indicateurs), le bon d'engagement et le visa budgétaire, l'engagement décentralisé dans les unités, les seuils, le chronogramme, les circuits nominatifs, les grilles de réallocation, les échéances de rapport.
- **Demandé par le CdC sans appui dans la procédure** : scénarios et simulations, budgets pluriannuels au-delà du CDMT, ouvertures complémentaires et annulations de crédits, dérogations aux dépassements, centres de profit, prévisions de fin d'exercice. À garder en évolution, ou à confirmer avec la SODEPA.

### 8.2 Cahier budget « amélioré » et backlog (`docs/budget/`, `docs/architecture/`)

Rédigés avant la réception de la procédure, ces deux documents décrivent un module budgétaire d'entreprise privée générique : axes analytiques et **centres de coûts** comme colonne vertébrale, enveloppes hiérarchiques, reforecast et rolling forecast, consolidation multi-entités et multi-devises, CRM, recommandations par IA.

- **À garder** : les exigences de robustesse — réservation atomique sans double consommation, cycle engagement → réception → facture sans double comptage, journal de mouvements immuable, séparation maker-checker, idempotence des intégrations, piste d'audit avant/après, imports contrôlés. Elles restent justes et la procédure ne les contredit pas.
- **À refonder** : la structure (programme → action → activité → tâche × structure × nature, au lieu des centres de coûts), le vocabulaire (BE, visa budgétaire, liquidation, réallocation, PPA, CDMT), les circuits (ceux du § 5.5 et du § 6.3), les états (§ 6.2).
- **À reporter** : multi-entités, multi-devises, CRM, rolling forecast, IA. La SODEPA est une seule entité, en FCFA.

Je recommande de réécrire le cahier budget sur la base de la procédure, en reprenant en annexe les exigences de robustesse, puis de réaligner le backlog.

### 8.3 Code du dépôt

État au commit `9d03fc9` (6 novembre 2026), relevé par lecture du code. Les points marqués « vérifié » ont été relus dans le source pour ce rapport.

| Notion de la procédure | Dans le code | Écart |
|---|---|---|
| Exercice budgétaire | un entier `annee` sur le plan ; ni entité, ni état, ni clôture | à créer |
| Programme → action → activité → tâche | absent. Une ligne (`BudgetItemEntity`) = un compte SYSCOHADA de classe 6 ou 7 et une section analytique facultative | à créer : c'est la colonne vertébrale du budget de la SODEPA |
| Structure (Direction Générale, unités) | absente de la ligne ; `BudgetDemandeEntity.departementId` est un UUID sans cible | à relier à l'organigramme |
| Recettes et dépenses | aucune distinction : un compte de classe 7 est contrôlé et bloqué comme une charge | à séparer |
| Versions | un « plan » par version, sans copie des lignes ni lien avec la précédente ; plusieurs plans publiés possibles pour la même année | une seule version de référence par exercice |
| Visa budgétaire | `/pre-engager` réserve aussitôt et ajoute au montant engagé | réservation à distinguer de l'engagement |
| Circuit des visas | codé en dur dans `RapprochementAdapter` : chef de service jusqu'à 500 000, directeur financier jusqu'à 5 000 000, DG au-delà ; le rôle de l'approbateur est lu dans la requête (vérifié) | ne correspond à aucun circuit des procédures (§ 5.5) |
| Bon d'engagement | `BudgetEngagementEntity` : numéro saisi librement, ni tiers, ni pièces, ni édition, ni lien avec le dossier d'achat | à compléter (EX-02, EX-05) |
| Liquidation | changement d'état : l'engagé diminue, le réalisé ne bouge pas (vérifié, `BudgetPlanAdapter.liquiderEngagement`) ; un pré-engagement non visé peut être liquidé | le crédit paraît libre entre la liquidation et la validation de l'écriture |
| Réalisé | alimenté à la validation d'une écriture comptable : compte exact seulement, montant toujours positif, ajouté à toutes les lignes de même compte et section, tous plans de l'année confondus (vérifié) | sous-comptes ignorés, avoirs comptés en plus, doublons entre versions, double comptage possible avec l'engagé |
| Disponible | prévu − engagé − réalisé, bloquant sur un plan publié (vérifié) | juste dans son principe, faussé par les deux lignes précédentes |
| Réallocation | entre lignes d'un même plan, journal sans état, valideur lu dans la requête | ni circuit par niveau, ni pièces, ni comité, ni CA (§ 6.3) |
| Seuils d'achat | table paramétrable `achat_seuils_config`, semée avec un « achat direct caisse » de 0 à 499 999 FCFA et une seule pro forma | les procédures fixent : sans commande sous 100 000, trois pro forma de 100 000 à 499 999. La table est la bonne idée, le contenu est à corriger |
| Achats → budget | `ExpressionBesoinEntity.budgetDisponible` toujours `true` ; la route `engager-budget` des achats ne touche pas les lignes budgétaires ; imputation en texte libre | aucun contrôle réel de disponibilité à l'achat |
| Élaboration | « budget collaboratif » : demandes par département, cadrage par coefficient, génération depuis l'historique, consolidation qui avale ses erreurs | logique de budget de moyens, sans programme ni unité |
| Workflow | `WorkflowSeeder` déclare WF-DAF-06 (réallocation : CSB puis DAF), WF-DG-01 (budget annuel), WF-DG-04 (dépassement), WF-DAF-01 (commande) ; le paquet budget ne les appelle jamais et l'étape de contrôle automatique n'est pas implémentée | trois mécanismes d'approbation parallèles : maker-checker, visas codés en dur, circuits déclaratifs |
| Second budget | `comptabilite/analytique` a sa propre table `budgets` et ses routes, sans lien avec `budget_plan` | deux budgets concurrents |

Défauts transverses :

- **Maker-checker du budget inutilisable** : les créations rendent 200 et le DTO au lieu de 202 et `SubmissionOutput` (vérifié pour `POST /plans`), il n'existe pas de liste des demandes en attente, et selon l'inventaire l'auto-validation est refusée (même utilisateur) avec une erreur avalée : `POST /plans` renvoie un plan qui n'existe pas en base.
- **Habilitations** : 6 méthodes sur 51 routes contrôlent une permission ; aucun rôle métier ne détient ces permissions, absentes aussi d'`APP_PERMISSIONS` côté front ; l'identifiant de l'utilisateur est lu dans le corps de la requête (vérifié pour `POST /plans`). Le pré-engagement et l'approbation directe d'un plan contournent tout contrôle.
- **Erreurs** : les refus métier lèvent `IllegalArgumentException` ou `IllegalStateException` (vérifié dans la liquidation), donc un 500 au lieu d'un 400 ou 409.
- **Données** : `TestDataSeeder` tourne dans tous les profils (`@Profile("test")` commenté, vérifié) ; sa création de plans échoue vraisemblablement. Le dépôt ne contient que des données semées : rien à migrer.
- **Front** : écrans plans, engagements, demandes ; aucun écran de visa ni de validation maker-checker ; le message « plan créé » s'affiche alors que le plan n'est qu'en attente ; `api.json` est périmé.

**Conclusion.** Le module existant ne peut pas accueillir la procédure par ajouts : son unité de base (compte et section) n'a ni programme, ni structure, ni exercice, et ses trois mécanismes d'approbation ne correspondent à aucun circuit de la SODEPA. Je recommande de reconstruire le cœur (référentiels, ligne, mouvements, visa, BE, réallocation) dans le paquet `budget`, en conservant : la table de seuils paramétrable, l'écoute des écritures validées (imputation à corriger), la piste d'audit ; trésorerie et financement restent à part et hors de ce chantier.

---

## 9. Modèle de données cible (proposition à discuter)

| Entité | Rôle | Attributs clés |
|---|---|---|
| Exercice budgétaire | Année et son état | année, dates, état (préparation, approuvé, exécutoire, clos), référence de la résolution du CA, date de la décision exécutoire |
| Élément programmatique | Arbre programme → action → activité → tâche | code (plan de codification), libellé, niveau, parent, responsable, indicateurs, cibles |
| Structure budgétaire | Direction Générale et unités qui portent un budget | référence vers l'unité de l'organigramme, ordonnateur (DG ou DUO), service d'engagement (SB ou CSAFC) |
| Nature budgétaire | Section et compte | section (fonctionnement, investissement, produits), compte SYSCOHADA |
| Ligne budgétaire | Unité de contrôle des crédits | exercice, structure, élément programmatique, nature ; crédit initial, modifications, crédit ouvert ; agrégats réservé, engagé, liquidé, payé, disponible |
| Version budgétaire | Étapes de la campagne | type (avant-projet d'unité, consolidé, arbitré, validé, approuvé, exécutoire), lignes, auteur, date |
| CDMT | Programmation sur trois ans | programme, années N+1 à N+3, montants, indicateurs |
| Visa budgétaire | Réservation | ligne, montant, pièce d'origine (TDR, demande d'acquisition, fiche d'entretien, ordre de mission), état, échéance éventuelle |
| Bon d'engagement | Engagement | numéro, exercice, structure, lignes d'imputation, montant TTC, tiers, objet, nature de l'acte (acquisition, personnel, TDR, divers), support (bon de commande, lettre commande, marché), pièces, état, signataire |
| Liquidation | Service fait et montant dû | BE, facture définitive, procès-verbal, montant, liquidateur, date |
| Mouvement budgétaire | Journal immuable | type (ouverture, réallocation, réservation, libération, engagement, désengagement, liquidation, paiement), ligne, montant, référence, auteur, date |
| Demande de réallocation | Modification des crédits | type, lignes source et bénéficiaire, montants, motif, urgence, impact, risques, pièces, note de la DAF, avis du comité, décision du DG, résolution du CA, état |
| Règle de circuit | Table de décision paramétrable | critères, circuit cible, date d'effet, version |
| Seuil | Valeurs réglementaires | code, valeur, date d'effet, acte de référence |
| Recette | Réalisation imputée | ligne de produits, montant, date, pièce d'origine (caisse A, banque) |

Paiements et écritures restent dans les modules trésorerie et comptabilité ; le budget en reçoit les montants. Montants en `BigDecimal`, comme décidé pour le module RH.

---

## 10. Questions pour la SODEPA

**Documents à obtenir**

- Q-01 Le plan de codification budgétaire en vigueur, et le budget approuvé de l'exercice en cours, ligne par ligne (fichier Excel si possible).
- Q-02 Les modèles : bon d'engagement, bon de commande, fiche de suivi du budget, fiche de demande de réallocation, rapport mensuel, rapport trimestriel de performance, PPA et CDMT d'un exercice passé.

**Élaboration**

- Q-03 Le plan de codification est-il permanent et mis à jour chaque année, ou refait après chaque approbation ?
- Q-04 Le comité d'élaboration du budget et le groupe de travail sont-ils la même instance ?
- Q-05 Les délais du chronogramme sont-ils en jours ouvrés ou calendaires ? À quelle date la campagne démarre-t-elle en pratique ?
- Q-06 Que se passe-t-il si le budget n'est pas exécutoire au 1er janvier (dépenses autorisées, plafond) ?
- Q-07 Les arbitrages fixent-ils des plafonds par programme ou par unité ?

**Engagement**

- Q-08 Quelles informations porte le bon d'engagement ? Comment est-il numéroté (par exercice, par structure) ?
- Q-09 Un BE peut-il porter plusieurs lignes d'imputation ?
- Q-10 Comment annule-t-on ou modifie-t-on un BE signé ?
- Q-11 Les marchés et contrats sur plusieurs exercices sont-ils engagés en totalité ou par tranche annuelle ?
- Q-12 La lecture de la notation « DG/DAF/SDFBM/SB » (cotation descendante, puis visas remontants) est-elle juste ? Chaque intermédiaire vise-t-il réellement ?
- Q-13 Les signatures doivent-elles rester manuscrites (document numérisé dans l'ERP) ou une validation électronique dans l'ERP suffit-elle ?
- Q-14 Un Directeur d'unité peut-il engager sans limite sur les crédits de son unité ?
- Q-15 Que recouvre concrètement « l'engagement des recettes » ?
- Q-16 Les dépenses de moins de 100 000 FCFA donnent-elles lieu à un BE ?
- Q-17 La caisse B est-elle engagée à l'approvisionnement (globalement) ou dépense par dépense ?
- Q-18 Si la facture définitive dépasse le BE, que fait-on (tolérance, engagement complémentaire, nouveau BE) ?
- Q-19 Le visa budgétaire réserve-t-il le crédit ? A-t-il une durée de validité ?

**Suivi**

- Q-20 Un dépassement doit-il bloquer l'engagement sans exception, ou une dérogation est-elle possible ? Par qui ?
- Q-21 Les rapports mensuels et trimestriels ont-ils un format imposé (tutelle, CA) ?

**Réallocation**

- Q-22 Laquelle des trois grilles fait foi (typologie, grille d'autorité, recommandation à 5 %) ? La recommandation a-t-elle été adoptée par note de service ?
- Q-23 Le seuil de 5 % s'apprécie-t-il sur la ligne source, la ligne bénéficiaire, ou par cumul sur l'exercice ?
- Q-24 Quels seuils chiffrent « significative », « fortement », « investissement majeur » ?
- Q-25 Peut-on prélever sur une ligne déjà partiellement engagée, dans la limite de son disponible ?
- Q-26 Le comité de réallocation existe-t-il ? Quorum, délai, périodicité ?
- Q-27 Le DG doit-il saisir la tutelle via l'ERP, ou hors outil ?

**Fin d'exercice et périmètre**

- Q-28 Que deviennent au 31 décembre les engagements non liquidés et les crédits non consommés (report, annulation) ?
- Q-29 Le budget est-il modifié autrement que par réallocation (budget rectificatif, ouverture de crédits sur recettes nouvelles, subvention reçue) ?
- Q-30 Les projets sur financement extérieur ont-ils un budget distinct ?

---

## 11. Découpage proposé

| Lot | Contenu | Pourquoi dans cet ordre |
|---|---|---|
| B1 — Socle | Exercice, nomenclature programmatique, natures, structures (depuis l'organigramme), seuils datés ; import du budget approuvé ; décision exécutoire ; situation des crédits | Sans référentiel ni budget chargé, rien ne s'exécute. L'exercice de mise en service sera préparé hors ERP |
| B2 — Engagement | Visa et réservation, BE à la Direction Générale et dans les unités, contrôle bloquant, pièces requises par type, circuits du § 5.5 dans le moteur de workflow, impression du BE, liquidation, réception des paiements, situation quotidienne | Cœur de l'usage quotidien et du contrôle |
| B3 — Réallocation | Demande, table de décision du circuit, comité, décision, tableau type, mise à jour des crédits, journal | Seule modification du budget décrite ; dépend des réponses Q-22 à Q-27 |
| B4 — Suivi et rapports | Rapports mensuels (5 et 10), trimestriel, recettes, alertes, tableaux de bord du CdC | S'appuie sur les mouvements de B2 et B3 |
| B5 — Élaboration | Campagne et chronogramme, CDMT, PPA, avant-projets des unités, consolidation, arbitrage, versions jusqu'à l'approbation | Utile une fois par an ; à livrer avant la campagne de l'exercice suivant |

Les intégrations (paie, achats et marchés, trésorerie, comptabilité) se branchent sur B2 au fur et à mesure que ces modules existent.
