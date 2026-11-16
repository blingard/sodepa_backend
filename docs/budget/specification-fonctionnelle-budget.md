# Gestion budgétaire — spécification fonctionnelle de référence

| Élément | Valeur |
|---|---|
| Module | 02 — Gestion budgétaire (code `BUD`) |
| Version | 1.0 — 15 novembre 2026 |
| Statut | **Document de référence pour la rédaction des user stories.** Rédigé sur notre compréhension des procédures ; les réponses de la SODEPA au questionnaire (Q-01 à Q-31) le corrigeront |
| Fait foi sur | le cahier des charges `docs/architecture/CAHIER_DES_CHARGES_MODULE_BUDGETAIRE.md` et son catalogue (Jean-Paul, 6 novembre) ; `docs/budget/Cahier_des_charges_gestion_budgetaire_ERP_ameliore.md` ; `docs/architecture/BACKLOG_MODULE_BUDGETAIRE_GITHUB_ISSUES.md` |
| Au-dessus de lui | les procédures de la SODEPA. En cas d'écart, la procédure l'emporte et ce document est corrigé |

**Sources.**
- **Procédures de la SODEPA**, converties dans `docs/client/` :
  - [PB] procédure budgétaire ;
  - [PM] procédure des marchés ;
  - [PF] procédure financière ;
  - [PSP] procédures de la solde et des pensions ;
  - [PFo] procédures de la formation.
- **Cahier des charges ERP** de la SODEPA [CdC], domaine 4.
- **Analyses de l'équipe :**
  - [R1] [rapport n° 1](rapport-01-procedure-budgetaire.md) ;
  - [JP] cahier des charges, catalogue et critères d'acceptation de `docs/architecture/` ;
  - [RNF] exigences de robustesse de l'ancien cahier budget ;
  - le guide budgétaire interactif ;
  - le [questionnaire](Questionnaire_SODEPA_gestion_budgetaire.docx).

**Conventions.**
- **Identifiants repris de [JP]** : `F-BUD-001` à `024` et `RG-BUD-001` à `022`, quand leur sens est conservé. Ses critères d'acceptation restent ainsi rattachables. Un identifiant repris mais dont le contenu change est marqué *modifié*.
- **Identifiants nouveaux** : à partir de `F-BUD-025` et `RG-BUD-023`.
- **Hypothèses** : `H-nn`, une par question `Q-nn` du questionnaire. Chaque règle qui repose sur une hypothèse la cite : une réponse de la SODEPA permet de retrouver tout ce qu'elle touche.
- **Montants** : en FCFA TTC, sans décimale.

---

## 1. Comparaison des sources

Les deux analyses partent de la même procédure budgétaire. Elles concordent sur l'essentiel : budget-programme, cycle en trois temps, acteurs, chronogramme, bon d'engagement, typologie des réallocations, rapports le 5 et le 10. Elles diffèrent sur les points suivants.

| Thème | Procédure SODEPA | [JP] (6 novembre) | Nos documents (8–12 novembre) | Retenu ici |
|---|---|---|---|---|
| Seuils d'achat | [PB] : menue dépense < 500 000 ; [PM]/[PF] : < 100 000 sans commande, 100 000–499 999 trois pro forma, 500 000 bon de commande, 15 M lettre commande, 50 M marché | Un seul seuil, 500 000 (RG-BUD-006/007) ; l'écart avec [PM] est relevé en point ouvert PO-BUD-01 | Cinq tranches, recoupées sur [PB], [PM] et [PF] | **Cinq tranches** dans une table unique datée (§ 8) |
| Visa budgétaire | Visa sur la demande d'acquisition **avant** la consultation [PM 1.3.2 a] ; visa sur TDR et fiche d'entretien [PB] | Visa = étape de signature du bon d'engagement (« Visé SB ») | Visa = mise de côté du crédit dès la demande, distincte de l'engagement | **Visa = mise de côté** (F-BUD-030, H-19) |
| Liquidation et réalisé | Liquidation par le DG ou le DUO sur facture, bordereau, PV [PM] ; paiement [PF] | Hors périmètre (renvoyés aux modules Marchés et Finance) ; états « Liquidé », « Payé » cités | Le budget doit tenir engagé, liquidé, payé et réalisé ; écart facture/engagement | **Le budget reçoit liquidation et paiement** et tient la situation (F-BUD-033, F-BUD-034) |
| Paie | Engagée **après** signature des états par le DG [PSP] | « État de paie » parmi les pièces d'origine | Engagement de régularisation ; un blocage est impossible | **Engagement mensuel de régularisation, avec alerte** (F-BUD-031, H-20) |
| Caisse B et petites dépenses | Caisse B < 100 000, plafond 7 500 000 [PF] | Non traité | Engagement global au réapprovisionnement (proposition) | **F-BUD-032**, H-16 et H-17 |
| Ligne budgétaire | Non définie | Code, nature, compte, source, unité | Structure × élément du programme × nature | **Structure × élément × nature**, plus compte et source (F-BUD-026) |
| Disponible et concurrence | « Aucune dépense sans crédit disponible » | Contrôle bloquant | Journal de mouvements immuable, disponible protégé contre les accès simultanés [RNF-01] | **Journal de mouvements** (F-BUD-029) |
| Intégrations | Toute dépense est engagée (achats, paie, missions, études, formation, foires) | Tableau des interfaces | Contrat d'opérations que les autres modules appellent | **Contrat d'opérations** (EP-BUD-07) |
| Fin d'exercice | Non décrite | Point ouvert PO-BUD-05 | Q-28 | **Hypothèse H-28**, lot dédié (F-BUD-040) |
| Réallocation | Trois grilles qui se recouvrent | Les trois grilles reprises (RG-BUD-015 à 018), écart en PO-BUD-03 | Comparateur des grilles ; Q-22 à Q-27 | **Table de décision paramétrable**, semée avec la recommandation § g (H-22) |
| Démarrage | Exercice préparé hors ERP la première année | Non traité | Import du budget approuvé | **Import** (F-BUD-027) |
| Priorités | — | Presque tout en priorité « Haute », élaboration comprise | Exécution d'abord, élaboration en dernier | **Lots B1 à B5** (§ 11) |
| Points ouverts | — | 6 (PO-BUD-01 à 06) | 31 questions envoyées au client | **31 hypothèses** (§ 12) |
| Méthode | — | Cahier → catalogue (24 fonctionnalités avec user story) → 75 critères en Gherkin → matrice | Rapports, guide, questionnaire | **Méthode de [JP] conservée** ; critères à réviser sur les fonctionnalités modifiées |

**Ce que [JP] apporte et qui est conservé :**
- la chaîne cahier → fonctionnalités → critères Gherkin et les identifiants ;
- la règle des quatre yeux (RG-BUD-021) ;
- le bon d'engagement non modifiable après signature (RG-BUD-022) ;
- la numérotation par exercice et par unité ;
- les alertes de consommation à 80 % et 100 % ;
- la génération des documents dans la GED ;
- le fonctionnement dégradé en unité ;
- la référence à la loi n° 2018/012 ;
- les dix critères transverses CT-BUD-01 à 10.

**Ce qui est écarté de [JP] :**
- le seuil unique de 500 000 ;
- le visa confondu avec la signature ;
- la liquidation hors du budget ;
- la priorité « Haute » donnée à l'élaboration.

**À retirer de ses fichiers avant toute diffusion :**
- la balise `<USER_REQUEST>` en tête du cahier, et le catalogue recollé à sa fin ;
- dans la matrice de conformité, l'affirmation de 94,7 % de critères « couverts en production et testés », que le code ne confirme pas ([rapport n° 3](rapport-03-bilan-code-budget-15-novembre.md)).

---

## 2. Objet et principes

Le module est le **guichet** par lequel passe toute dépense de la SODEPA. Il ne paie rien et ne tient pas la comptabilité. Il dit si l'on a le droit d'engager une dépense, la réserve, l'engage, et tient la situation de chaque ligne jusqu'au paiement.

1. **Budget-programme.** Chaque crédit se rattache à un programme, une action, une activité et une tâche, avec des indicateurs [PB préambule].
2. **Trois temps.** L'élaboration se fait l'année N−1 ; l'exécution du 1er janvier au 31 décembre ; le contrôle et le suivi en continu.
3. **Rien sans crédit.** Tout acte à incidence financière est engagé avant paiement [PB II 2.3] et aucune dépense ne s'engage sans crédit disponible [PB III 3 a].
4. **Une seule vérité.** Les montants d'une ligne sont la somme de ses mouvements, et chaque mouvement est immuable et tracé.
5. **Tout paramétrable.** Seuils, circuits et délais sont des paramètres datés, pas du code. C'est la même décision que pour le module RH.

**Périmètre.**

| Inclus | Hors module (le budget en reçoit ou leur transmet des données) |
|---|---|
| Exercices, nomenclature, structures, lignes, versions, import | Passation des commandes et marchés, réception (Marchés) |
| Visa, bon d'engagement, circuits, liquidation et paiement constatés, annulation | Paiement effectif (Trésorerie) |
| Engagements de la paie, des missions, des formations, des études, des foires | Calcul de la paie (Solde) |
| Recettes imputées sur les lignes de produits | Écritures comptables (Comptabilité) |
| Situation d'exécution, rapports, alertes | Encaissements (Trésorerie) |
| Réallocations | Signature et archivage des documents (GED, workflow) |
| Élaboration : campagne, CDMT, PPA, avant-projets, arbitrage, approbation | |
| Clôture d'exercice | |

---

## 3. Acteurs et habilitations

Les acteurs sont des **profils** du référentiel IAM, rattachés à une structure, pas des postes figés. Un utilisateur n'agit que sur le périmètre de sa structure (CT-BUD-04). La Direction Générale, la DAF, le Service du Budget et la DCAI ont une vue sur toute la SODEPA.

| Profil | Rôle budgétaire | Principales actions |
|---|---|---|
| CA | Seule instance d'approbation | Approuver PPA et budget ; décider les réallocations qui lui reviennent (enregistrées par le SB avec la résolution) |
| DG | Ordonnateur à la Direction Générale | Signer lettre-circulaire, décisions, BE, BC ; liquider ; décision exécutoire ; arbitrer les réallocations ; mesure conservatoire |
| DAF | Coordonne la confection, propose les réallocations | Viser les BE (après la SDFBM) ; initier une réallocation |
| SDFBM | Supervise le SB | Viser les BE (après le SB) ; analyser les réallocations |
| CSB / SB | Pivot du dispositif | Viser (mettre de côté), établir et imputer les BE de la Direction Générale ; engager paie, missions, formations, foires ; tenir la situation ; produire les rapports ; consolider |
| DUO | Ordonnateur dans son unité | Valider le besoin ; attribuer ; signer les BE et BC de l'unité ; liquider |
| CSAFC | Service du budget de l'unité | Viser, établir et imputer les BE de l'unité ; suivre les recettes de l'unité |
| Chef de programme | Responsable d'un programme | CDMT, PPA, budgétisation des tâches ; consulter l'exécution de son programme |
| DPD, CSPP, CEA, DCAI, ASCG | Campagne budgétaire | Lettre-circulaire, consolidation CDMT et PPA, rapport d'évaluation |
| Comité de réallocation | Avis | Rendre un avis motivé |
| Demandeur | Service ou agent | Exprimer un besoin, rédiger des TDR, demander une réallocation |
| Lecteur | Consultation | Situation d'exécution de son périmètre |

**Règle des quatre yeux** (RG-BUD-021) : l'auteur d'une demande, d'un BE ou d'une réallocation ne peut ni la viser ni la valider. Un même utilisateur ne franchit pas deux étapes du même dossier.

---

## 4. Modèle de données

| Objet | Rôle | Attributs clés |
|---|---|---|
| Exercice | L'année budgétaire et son état | Année ; état ; dates ; référence et date de la résolution du CA ; date de la décision exécutoire ; version de référence |
| Élément programmatique | Arbre programme → action → activité → tâche | Code (plan de codification) ; libellé ; niveau ; parent ; responsable ; indicateurs et cibles ; actif |
| Plan de codification | Version de la nomenclature | Version ; date d'effet ; référence de signature du DG |
| Structure budgétaire | Direction Générale, unités | Unité de l'organigramme ; ordonnateur (DG ou DUO) ; service d'engagement (SB ou CSAFC) |
| Nature | Classe de la ligne | Section (fonctionnement, investissement, produits) ; compte SYSCOHADA ; sens (dépense ou recette) |
| Source de financement | Origine des fonds | Ressources propres, subvention, partenaire |
| Ligne budgétaire | Unité de contrôle des crédits | Exercice ; structure ; élément ; nature ; source ; crédit initial, modifications, crédit ouvert ; agrégats : mis de côté, engagé, liquidé, payé, disponible (calculés) |
| Version budgétaire | Étapes du budget | Type (avant-projet d'unité, consolidé, arbitré, validé DG, approuvé CA, exécutoire, rectifié) ; lignes ; auteur ; date |
| Mouvement | Journal immuable | Type (ouverture, réallocation, mise de côté, libération, engagement, désengagement, liquidation, paiement, recette) ; ligne ; montant signé ; pièce d'origine (module, type, référence) ; auteur ; date ; clé d'idempotence |
| Visa | Mise de côté | Ligne(s) ; montant ; pièce d'origine ; état ; viseur ; date |
| Bon d'engagement (BE) | Engagement juridique | Numéro ; exercice ; structure ; lignes d'imputation et montants ; objet ; bénéficiaire ; nature de l'acte (acquisition, personnel, TDR, divers) ; support (aucun, BC, LC, marché) ; tranche ; pièces ; visas ; signataire ; état |
| Liquidation | Service fait | BE ; facture, bordereau, PV ; montant ; liquidateur ; date |
| Recette | Réalisation | Ligne de produits ; montant ; date ; pièce (encaissement) |
| Demande de réallocation | Modification des crédits | Lignes source et bénéficiaire ; montant ; motif ; urgence ; impact ; risques ; type ; niveau ; avis ; décision ; résolution du CA ; état |
| Règle de décision | Circuit selon les critères | Critères ; circuit ; date d'effet ; version |
| Seuil / paramètre | Valeurs réglementaires | Code ; valeur ; date d'effet ; acte de référence |
| Campagne | Élaboration | Exercice visé ; lettre-circulaire ; chronogramme ; groupe de travail ; réunions |
| CDMT, PPA, avant-projet | Élaboration | Voir EP-BUD-02 |

**Formules** (RG-BUD-024) :

```text
Crédit ouvert   = crédit initial ± réallocations ± autres modifications
Disponible      = crédit ouvert − mis de côté − engagé
Reste à payer   = engagé − payé
Taux d'engagement = engagé / crédit ouvert ; taux d'exécution = liquidé / crédit ouvert
```

Le liquidé et le payé sont inclus dans l'engagé, ils ne s'en retranchent pas une seconde fois.

---

## 5. Cycles de vie

| Objet | États | Remarques |
|---|---|---|
| Exercice | En préparation → Approuvé CA → Exécutoire → Clos | Seul un exercice exécutoire accepte visas et engagements (RG-BUD-002, RG-BUD-038) ; régime transitoire H-06 |
| Version | Brouillon → Soumise → Arbitrée → Validée DG → Approuvée CA → Exécutoire → Rectifiée | Une seule version exécutoire par exercice (RG-BUD-033) ; les autres restent consultables |
| Visa | Demandé → Visé (crédit mis de côté) → Consommé par un BE / Libéré / Refusé | |
| Bon d'engagement | Brouillon → Établi → Visé (SB, SDFBM, DAF) → Signé → Liquidé (partiel ou total) → Payé ; ou Annulé | Signé = non modifiable (RG-BUD-022) |
| Demande de réallocation | Brouillon → Soumise → Analysée → Avis du comité → Décision DG → (CA) → Exécutée ; ou Rejetée, Complément demandé, À régulariser CA | |
| Campagne | Ouverte → En cours → Arbitrée → Close | |

---

## 6. Fonctionnalités

Lot de livraison entre crochets (§ 11). « *Modifié* » signale une fonctionnalité de [JP] dont le contenu change.

### EP-BUD-01 — Socle et référentiel

**F-BUD-001 — Exercices budgétaires** [B1]
En tant que chef du budget, je veux ouvrir, rendre exécutoire et clore un exercice, afin que les crédits ne s'ouvrent qu'avec la décision du DG.
- Créer l'exercice N (1er janvier – 31 décembre) en préparation.
- Enregistrer la résolution du CA, puis la décision exécutoire : sa date ouvre les crédits.
- Refuser tout visa ou engagement sur un exercice non exécutoire, sauf dans le régime transitoire (H-06).
- Règles : RG-BUD-001, 002, 038.

**F-BUD-002 — Nomenclature et plan de codification** [B1]
En tant que chef du budget, je veux gérer l'arbre programme → action → activité → tâche et ses codes, afin que chaque ligne se rattache au budget-programme.
- Créer, modifier et désactiver les éléments, sous maker-checker.
- Saisir responsable, indicateurs et cibles.
- Importer et versionner le plan de codification signé (date d'effet, référence de signature).
- Hypothèse : le plan est permanent et mis à jour à chaque campagne (H-03).

**F-BUD-025 — Structures budgétaires** [B1]
En tant que chef du budget, je veux déclarer quelles unités de l'organigramme portent un budget, avec leur ordonnateur et leur service d'engagement, afin de router chaque dépense.
- Direction Générale : ordonnateur DG, service SB. Unité : ordonnateur DUO, service CSAFC.
- Les structures sont reliées à l'organigramme : aucune saisie libre.

**F-BUD-026 — Lignes budgétaires** [B1]
En tant que chef du budget, je veux des lignes définies par structure, élément du programme et nature, afin de contrôler les crédits au bon niveau.
- Une ligne = exercice × structure × élément × nature, avec source de financement et compte SYSCOHADA.
- Les lignes de produits (recettes) sont distinguées des lignes de charges et d'investissement.
- Unicité de la combinaison par exercice.
- Hypothèse : le contrôle de disponibilité porte sur la ligne (H-01).

**F-BUD-003 — Versions budgétaires** *modifié* [B1 pour la version exécutoire, B5 pour les versions d'élaboration]
- Conserver chaque version ; une seule exécutoire par exercice.
- Comparer deux versions ligne à ligne.
- Une réallocation produit une version rectifiée tracée, sans écraser l'initiale.

**F-BUD-027 — Import du budget approuvé** [B1]
En tant que chef du budget, je veux importer le budget approuvé depuis un fichier Excel, afin de démarrer l'exécution alors que l'élaboration de l'année s'est faite hors ERP.
- Modèle d'import fourni ; contrôle des codes contre la nomenclature et les structures.
- Rapport d'erreurs ligne par ligne ; rien n'est chargé tant qu'il reste une erreur.
- Chargement en version approuvée, puis passage à exécutoire par F-BUD-001.

**F-BUD-028 — Seuils et paramètres datés** [B1]
En tant qu'administrateur fonctionnel, je veux une table unique de seuils et paramètres, avec date d'effet et historique, partagée avec les Achats et la Trésorerie, afin qu'aucune valeur ne soit codée en dur.
- Valeurs initiales au § 8, modifiables sous maker-checker.
- Une opération applique la valeur en vigueur à sa date.

**F-BUD-029 — Journal des mouvements et disponible** [B1]
En tant que chef du budget, je veux que chaque opération écrive un mouvement immuable et que le disponible en découle, afin de pouvoir toujours expliquer un chiffre.
- Le disponible est contrôlé et mis à jour de façon atomique : deux opérations simultanées ne peuvent pas dépasser le crédit [RNF-01].
- Chaque mouvement porte sa pièce d'origine ; une même pièce rejouée ne produit qu'un mouvement [RNF-02].
- Consultation de l'historique d'une ligne.

### EP-BUD-02 — Élaboration du budget [B5]

Les fonctionnalités de [JP] sont reprises sans changement de fond :
- **F-BUD-004** campagne et lettre-circulaire avec chronogramme ;
- **F-BUD-005** groupe de travail : décision, membres, réunions, présences, comptes rendus ;
- **F-BUD-006** CDMT sur trois ans et PPA par programme ;
- **F-BUD-007** avant-projets des unités en quatre volets (fonctionnement, produits, activités, investissements), avec pièces de maturation ;
- **F-BUD-008** consolidation et arbitrages, avec l'historique proposé, arbitré, retenu ;
- **F-BUD-009** approbation : groupe de travail → validation interne du DG → CA → décision exécutoire.

Précisions :
- le chronogramme suit les 25 étapes de [PB § 4.2] et leurs délais, avec des alertes de dépassement ;
- le groupe de travail et le comité d'élaboration sont une seule instance (H-04) ;
- les délais sont en jours ouvrés (H-05) ;
- la sortie de l'élaboration est une version approuvée, qui remplace l'import F-BUD-027 à partir du deuxième exercice.

### EP-BUD-03 — Exécution des dépenses

**F-BUD-030 — Visa budgétaire et mise de côté du crédit** [B2]
En tant qu'agent du SB (ou CSAFC), je veux viser une demande d'acquisition, des TDR ou une fiche d'entretien, afin d'attester que le crédit existe et de le mettre de côté pendant la consultation.
- Contrôles du visa : exercice exécutoire, ligne existante et du bon sens (dépense), imputation cohérente, disponible suffisant.
- Le montant estimé est mis de côté jusqu'à l'engagement ou l'abandon de la demande (H-19).
- Un visa refusé indique le disponible et le manque.
- Règles : RG-BUD-005, 023.

**F-BUD-010 — Bon d'engagement** *modifié* [B2]
En tant qu'agent du SB (ou CSAFC), je veux établir un BE à partir de la pièce d'origine, afin d'engager la dépense avant tout paiement.
- Pièces d'origine possibles : demande d'acquisition visée, TDR, ordre de mission, fiche d'entretien, état de paie, contrat ou bail.
- Numéro automatique et unique par exercice et par structure (RG-BUD-028, H-08).
- Plusieurs lignes d'imputation possibles (H-09).
- L'engagement remplace la mise de côté : l'écart entre l'estimation et l'offre retenue est libéré, ou contrôlé s'il augmente.

**F-BUD-011 — Contrôle budgétaire bloquant** [B2]
Repris de [JP] :
- inscription au budget exécutoire, imputation et disponible contrôlés ;
- tout dépassement bloque l'engagement (H-20) ;
- le disponible est affiché avant et après l'opération.

**F-BUD-012 — Circuit, support et pièces exigées** *modifié* [B2]
En tant qu'agent du budget, je veux que le circuit, le support de commande et les pièces exigées découlent du montant, de la structure et du type de dépense, afin d'appliquer la procédure sans erreur.
- **Tranches** (§ 8, RG-BUD-006 et 007 modifiées) :
  - < 100 000 : aucune commande, caisse B ;
  - 100 000 – 499 999 : trois pro forma et note de sélection, BE seul ;
  - 500 000 – 14 999 999 : trois pro forma de fournisseurs agréés, comité de sélection à la Direction Générale, BE et BC ;
  - 15 M – 49 999 999 : lettre commande ;
  - ≥ 50 M : marché.
- **Circuit de signature :**
  - à la Direction Générale : SB → SDFBM → DAF → DG (H-12) ;
  - en unité : CSAFC → DUO, dans la limite fixée en H-14.
- **Pièces exigées** par type et tranche :
  - expression de besoin, attestation de carence ;
  - une ou trois pro forma, dossier fiscal, attestation ARMP ;
  - TDR, ordre de mission, fiche d'entretien.
- Lettres commandes et marchés suivent le circuit de la Direction Générale, quelle que soit la structure.

**F-BUD-013 — Visas et signature électronique** *modifié* [B2]
- Chaque intermédiaire vise dans l'ordre du circuit ; un refus, motivé, renvoie au SB (H-12).
- La signature est une validation électronique nominative et horodatée ; le document signé peut être joint pendant la transition (H-13).
- Règle des quatre yeux (RG-BUD-021) ; BE non modifiable après signature (RG-BUD-022).
- Le BE et le BC sont édités sur le modèle SODEPA et archivés dans la GED (CT-BUD-06).

**F-BUD-014 — Support de commande lié** *modifié* [B2]
- Générer le BC lié au BE de 500 000 à 14 999 999, et le transmettre au module Marchés et au prestataire.
- Pour une lettre commande ou un marché : engager après signature du contrat, pour la tranche de l'exercice (H-11).
- Un BC non exécuté sous trois mois, puis après quinze jours de mise en demeure, est annulé, et son BE désengagé (RG-BUD-036).

**F-BUD-015 — Circuits spécifiques** *modifié* [B2]

| Cas | Circuit retenu | Particularités |
|---|---|---|
| Mission | TDR validés → ordre de mission (CSSP) → visa et BE (SB) → visas → DG signe BE et OM → Trésorerie | Avance de 65 % sous réserve de crédit (RG-BUD-037) |
| Réparation de véhicule | Fiche d'entretien → diagnostic (CBM) et pro forma → visa SB → autorisation DG → BE (et BC ≥ 500 000) → signature DG | Véhicule commercial : logisticien ; de service : comptable-matières |
| Étude, consultant | TDR → visa SB → autorisation DG → BE (et BC selon la tranche) → signature DG → TDR au Service Financier | |
| Formation | TDR transmis au CSB → engagement [PFo] | Plan de formation (module RH) |
| Foire, cérémonie | TDR signé par le DG → BE (CSB) → paiement espèces ou chèque [PF] | |
| Obsèques, décès | Décision signée et état récapitulatif → BE (CSB) → paiement [PSP] | |

**F-BUD-016 — Annulation et réduction** [B2]
Reprise de [JP] :
- motif obligatoire et validation ;
- crédit restitué par un mouvement de désengagement ;
- engagement initial conservé.

Une modification de montant passe par l'annulation et un nouveau BE (H-10).

**F-BUD-031 — Engagement de la paie** [B2, branché quand la paie existe]
En tant qu'agent du SB, je veux engager chaque mois la masse salariale signée par le DG, imputée par structure et par nature (salaires, cotisations CNPS, impôts), afin que la paie figure dans l'exécution.
- L'engagement suit la signature des états : c'est une régularisation.
- Si une ligne est insuffisante, l'engagement passe, une alerte part au SB et à la DAF, et une réallocation régularise (H-20).
- Liquidation et paiement constatés au virement.

**F-BUD-032 — Petites dépenses et caisse B** [B2]
En tant qu'agent du SB, je veux engager le réapprovisionnement de la caisse B en une fois, puis imputer les dépenses réelles lors de sa justification, afin de suivre les achats de moins de 100 000 FCFA sans un BE par achat.
- Plafond d'approvisionnement : paramètre (7 500 000).
- Hypothèses H-16 et H-17.

**F-BUD-033 — Liquidation et réalisé** [B2]
En tant qu'ordonnateur (DG ou DUO), je veux liquider un BE sur facture définitive, bordereau ou attestation de service fait et PV, afin de constater ce qui est dû.
- Liquidation partielle possible ; le cumul liquidé ne dépasse pas l'engagé.
- Facture supérieure au BE : liquidation limitée à l'engagé, engagement complémentaire pour l'écart (H-18).
- Reliquat d'un BE soldé : libéré à la liquidation définitive (H-10).
- Un BE non signé ne se liquide pas.
- La liquidation est transmise à la Comptabilité et à la Trésorerie.

**F-BUD-034 — Paiement constaté** [B2]
La Trésorerie signale chaque paiement : la ligne passe en payé, et le reste à payer diminue. Le budget ne paie jamais lui-même.

### EP-BUD-04 — Recettes

**F-BUD-017 — Suivi et rapprochement des recettes** *modifié* [B4]
- Chaque encaissement est imputé sur sa ligne de produits.
- Une recette n'est jamais bloquée (RG-BUD-026, H-15).
- Fiche de suivi mensuelle : prévu, réalisé, taux, par nature et par unité.
- Rapprochement avec les encaissements et la comptabilité.

### EP-BUD-05 — Contrôle, suivi et rapports

- **F-BUD-018 — Contrôle a priori** [B2] : liste de contrôle paramétrable par type de dépense, validée avant signature [PB III 1]. Délai cible du visa : paramètre (point ouvert PO-BUD-04 de [JP]).
- **F-BUD-019 — Situation d'exécution** [B2] :
  - crédit ouvert, mis de côté, engagé, liquidé, payé, disponible ;
  - par programme, action, activité, tâche, structure, nature, source ;
  - en temps réel ;
  - alertes à 80 % et 100 % (paramètres).
- **F-BUD-020 — Rapports** [B4] :
  - rapport mensuel le 5 pour chaque unité, le 10 pour la SODEPA : lignes, dépassements, économies, explications saisies, engagements et exécution en valeur et en nombre ;
  - rapport trimestriel de performance, qui croise l'exécution et les indicateurs du PPA ;
  - format libre en attendant H-21.

### EP-BUD-06 — Réallocation [B3]

Les fonctionnalités F-BUD-021 à 024 de [JP] sont reprises, avec ces précisions :
- **F-BUD-021 — Demande** : fiche complète [PB III 3 c, étape 1] et pièces. Elle peut viser deux lignes de structures différentes.
- **F-BUD-022 — Analyse et classification** :
  - montant transférable plafonné au disponible de la ligne source ; les montants engagés ne sont jamais transférables (H-25) ;
  - taux calculé sur le crédit initial de la ligne source, en cumulant les réallocations de l'exercice (H-23) ;
  - classification par la table de décision (RG-BUD-018 modifiée, H-22).
- **F-BUD-023 — Circuit de décision** :
  - circuit tiré de la table de décision et porté par le moteur de workflow ;
  - avis du comité, décision du DG, résolution du CA selon le niveau ;
  - mesure conservatoire du DG en urgence, à l'état « À régulariser CA » (RG-BUD-017) ;
  - saisine des tutelles enregistrée sans être faite par l'outil (H-27).
- **F-BUD-024 — Exécution et suivi** :
  - mouvements de réallocation sur les deux lignes et version rectifiée ;
  - décision interne générée (modèle § f) ;
  - tableau type (§ e) et état consolidé de l'exercice.

### EP-BUD-07 — Intégrations

**F-BUD-035 — Contrat d'opérations** [B2]
Le budget expose aux autres modules une interface interne unique. Aucun autre module ne calcule de disponible.

| Opération | Appelée par | Effet |
|---|---|---|
| Consulter le disponible | tous | aucun |
| Mettre de côté (visa) | Achats, Missions, Formation | mis de côté + |
| Libérer | idem (demande abandonnée) | mis de côté − |
| Engager | Achats, Paie, Missions, Formation, SB | mis de côté −, engagé + |
| Désengager | annulation d'un BE ou d'un BC | engagé − |
| Liquider | Achats (réception), Paie | liquidé + |
| Constater un paiement | Trésorerie | payé + |
| Imputer une recette | Recettes, Trésorerie | réalisé des produits + |

Chaque appel porte la référence de la pièce d'origine et une clé d'idempotence (RG-BUD-031). Un refus rend un motif lisible par l'utilisateur.

**F-BUD-036 — Branchement des Achats** [B2]
- La demande d'acquisition demande le visa (F-BUD-030) au lieu de se déclarer « budget disponible ».
- L'attribution crée le BE ; la réception et la liquidation appellent F-BUD-033.
- L'imputation est choisie dans les lignes, jamais saisie en texte.

**F-BUD-037 — Branchement de la Paie, des Missions et de la Formation** [B2 pour le contrat, branchement au rythme de ces modules]

**F-BUD-038 — Branchement de la Comptabilité et de la Trésorerie** [B2]
- Les paiements arrivent par F-BUD-034, les encaissements par F-BUD-017.
- La comptabilité ne crée plus le réalisé du budget : le réalisé vient de la liquidation. Le rapprochement budget-comptabilité devient un contrôle, pas une source.
- Une erreur côté budget ne bloque jamais une validation comptable.

### EP-BUD-08 — Clôture

**F-BUD-040 — Clôture d'exercice et reports** [B4]
- Clôture au 31 décembre, avec état des engagements non liquidés et des crédits non consommés.
- Hypothèse H-28 : les engagements non liquidés sont reportés sur l'exercice suivant après décision du DG ; les crédits non consommés sont annulés.
- Pas d'engagement sur un exercice clos.

---

## 7. Règles de gestion

**Règles reprises de [JP] sans changement :**
- RG-BUD-001 à 005 ;
- RG-BUD-008 à 013 ;
- RG-BUD-015 à 017 ;
- RG-BUD-019 à 022.

Leur libellé est dans [JP] § 7.

| ID | Règle | Source |
|---|---|---|
| RG-BUD-006 *modifiée* | Moins de 100 000 FCFA : aucune commande, règlement par la caisse B ; de 100 000 à 499 999 : BE seul, après trois pro forma et note de sélection | [PM 1.3.2 b], [PF ch. 1 § II B] |
| RG-BUD-007 *modifiée* | 500 000 à 14 999 999 : BE et bon de commande, fournisseurs agréés ; 15 M à 49 999 999 : lettre commande ; ≥ 50 M : marché, après appel d'offres et examen par la CIPM | [PM 1.2] |
| RG-BUD-014 *modifiée* | Une réallocation ne prélève que du disponible ; les crédits déjà engagés ne sont jamais transférables ; elle ne crée pas de tension de trésorerie ni ne masque une insuffisance structurelle | [PB III 3], H-25 |
| RG-BUD-018 *modifiée* | Le niveau d'une réallocation est donné par une table de décision paramétrable et datée. Valeurs initiales (H-22) : ≤ 5 % de la ligne source dans une même direction → DAF propose, DG valide ; > 5 % ou plusieurs directions → DAF propose, comité, DG valide ; investissement, endettement, subventions, PPA ou équilibre général → CA | [PB III 3 g] |
| RG-BUD-023 | Le visa budgétaire met de côté le montant visé ; il est consommé par le BE, libéré si la demande est abandonnée | [PM 1.3.2 a], H-19 |
| RG-BUD-024 | Disponible = crédit ouvert − mis de côté − engagé ; liquidé et payé sont inclus dans l'engagé | [R1 § 5.3] |
| RG-BUD-025 | Le cumul liquidé d'un BE ne dépasse pas son montant engagé ; un écart passe par un engagement complémentaire | H-18 |
| RG-BUD-026 | Une recette n'est jamais bloquée ; elle est imputée sur sa ligne de produits | H-15 |
| RG-BUD-027 | La paie est engagée après la signature des états par le DG ; une ligne insuffisante déclenche une alerte, pas un blocage | [PSP], H-20 |
| RG-BUD-028 | Le numéro de BE est unique par exercice et par structure, attribué par le système | [JP] BF-BUD-029, H-08 |
| RG-BUD-029 | Un BE peut porter plusieurs lignes ; chacune est contrôlée sur son disponible | H-09 |
| RG-BUD-030 | La caisse B est engagée à chaque réapprovisionnement ; ses dépenses sont imputées à la justification | H-17 |
| RG-BUD-031 | Toute opération venant d'un autre module est idempotente : la même pièce d'origine ne produit qu'un effet | [RNF-02] |
| RG-BUD-032 | Un mouvement n'est jamais modifié ni supprimé ; une correction est un mouvement inverse | [RNF], [R1 § 9] |
| RG-BUD-033 | Un exercice a une seule version exécutoire à un instant donné | [CdC D4], [R1] |
| RG-BUD-034 | Seuils, taux, délais et circuits sont des paramètres datés ; une opération applique la valeur en vigueur à sa date | [PB], [JP] § 10 |
| RG-BUD-035 | Un même utilisateur ne franchit pas deux étapes d'un même circuit | Proposition |
| RG-BUD-036 | Un bon de commande non exécuté sous trois mois est mis en demeure quinze jours, puis annulé ; son BE est désengagé | [PM 1.3.2 b], H-10 |
| RG-BUD-037 | L'avance de mission est de 65 % des frais, sous réserve de crédit disponible ; le solde est réglé sur pièces | [PSP] |
| RG-BUD-038 | Les crédits s'ouvrent à la date de la décision exécutoire ; avant, seul le régime transitoire H-06 s'applique | [PB 4.1 c], H-06 |
| RG-BUD-039 | Le budget n'appelle jamais la paie, la comptabilité ni la trésorerie pour les bloquer : une erreur côté budget est signalée, pas propagée | Proposition (M6 du rapport n° 2) |

---

## 8. Seuils et paramètres (valeurs initiales)

| Code | Valeur | Source |
|---|---|---|
| Achat sans commande, caisse B | < 100 000 | [PM], [PF] |
| Trois pro forma, BE seul | 100 000 – 499 999 | [PM] |
| Bon de commande, fournisseurs agréés | 500 000 – 14 999 999 | [PM 1.2] |
| Lettre commande | 15 000 000 – 49 999 999 | [PM 1.2] |
| Marché | ≥ 50 000 000 | [PM 1.2] |
| Plafond d'approvisionnement caisse B | 7 500 000 | [PF] |
| Avance de mission | 65 % | [PSP] |
| Exécution d'un bon de commande, mise en demeure | 3 mois, 15 jours | [PM] |
| Seuil de réallocation simple | 5 % du crédit initial de la ligne source, cumulé sur l'exercice | [PB III 3 g], H-23 |
| Rapports mensuels | le 5 (unités), le 10 (SODEPA) | [PB III 2] |
| Alertes de consommation | 80 %, 100 % | [JP] (proposition) |
| Délais du chronogramme | ceux de [PB § 4.2], en jours ouvrés | H-05 |
| Archivage BC, LC, marchés | 10 ans | [PM 1.2] |

---

## 9. Documents gérés

| Document | Produit par l'ERP | Origine |
|---|---|---|
| Lettre-circulaire, décision du groupe de travail, comptes rendus | Généré, signé, archivé | B5 |
| CDMT, PPA, avant-projets, projet de budget | Saisi, consolidé, exporté | B5 |
| Résolution du CA, décision exécutoire | Joints à la version | B1 |
| Plan de codification | Importé, versionné | B1 |
| Demande d'acquisition visée | Visa tracé | B2 (Achats) |
| Bon d'engagement, bon de commande | Générés sur modèle, signés, archivés | B2 |
| Fiche de suivi du budget | Remplacée par la situation en temps réel | B2 |
| Rapports mensuel et trimestriel | Générés | B4 |
| Dossier de réallocation (11 pièces), décision interne, tableau rectifié | Constitué, généré, archivé | B3 |

---

## 10. Exigences non fonctionnelles

- **Critères transverses de [JP] conservés :** CT-BUD-01 à 10 (droits, journalisation, champs obligatoires, périmètre, notifications, GED, format FCFA, exports, performance, paramétrage).
- **Concurrence (CT-BUD-11) :** aucun dépassement de crédit sous accès simultanés ; essai de charge exigé à la recette [RNF-01].
- **Idempotence (CT-BUD-12) :** aucune double consommation en cas de rejeu [RNF-02].
- **Quatre yeux (CT-BUD-13) :** contrôlés côté serveur, l'identité étant toujours lue dans le jeton, jamais dans la requête.
- **Fonctionnement dégradé en unité :** saisie hors ligne possible, sans dépassement non contrôlé à la synchronisation. Exigence de [JP], à préciser avec la SODEPA ; hors des lots B1 à B3.
- **Erreurs :** un refus métier rend un message destiné à l'utilisateur et un code d'erreur exploitable ; un crédit insuffisant est un conflit (409).

---

## 11. Lots de livraison

| Lot | Contenu | Pourquoi dans cet ordre |
|---|---|---|
| **B1 — Socle** | F-BUD-001, 002, 025, 026, 003 (version exécutoire), 027, 028, 029 ; habilitations et périmètres | Rien ne s'exécute sans référentiel, lignes et budget chargé |
| **B2 — Exécution** | F-BUD-030, 010 à 016, 031 à 034, 018, 019, 035 à 038 | Usage quotidien, contrôle de toute dépense |
| **B3 — Réallocation** | F-BUD-021 à 024 | Seule modification du budget décrite ; la plus exposée aux réponses du client (Q-22 à Q-27) |
| **B4 — Suivi, recettes, clôture** | F-BUD-017, 020, 040 | S'appuie sur les mouvements de B2 et B3 |
| **B5 — Élaboration** | F-BUD-004 à 009, 003 (versions d'élaboration) | Annuel ; à livrer avant la campagne de l'exercice suivant |

Chaque user story renvoie à une fonctionnalité, aux règles qu'elle applique et aux hypothèses dont elle dépend. Les critères Gherkin de [JP] sont repris quand la fonctionnalité n'est pas *modifiée*, et réécrits sinon.

---

## 12. Hypothèses de travail

Elles tiennent lieu de réponse tant que la SODEPA n'a pas répondu. À chaque réponse : mettre à jour l'hypothèse, puis les règles et fonctionnalités qui la citent, puis les user stories concernées.

| H | Question | Hypothèse retenue | Touche |
|---|---|---|---|
| H-01 | Q-01 Plan de codification, budget approuvé | Ligne = structure × élément × nature ; codes fictifs pour développer ; import paramétrable | F-BUD-002, 026, 027 |
| H-02 | Q-02 Modèles de documents | Modèles provisoires conçus par l'équipe, remplacés à réception | F-BUD-013, 020 |
| H-03 | Q-03 Plan de codification permanent ? | Permanent, mis à jour en début de campagne, versionné | F-BUD-002 |
| H-04 | Q-04 Comité d'élaboration et groupe de travail | Une seule instance | F-BUD-005, 009 |
| H-05 | Q-05 Nature des délais | Jours ouvrés ; démarrage à la circulaire présidentielle | F-BUD-004 |
| H-06 | Q-06 Budget non exécutoire au 1er janvier | Dépenses obligatoires et courantes seulement, dans la limite d'un douzième par mois des crédits N−1 | F-BUD-001, RG-BUD-038 |
| H-07 | Q-07 Plafonds d'arbitrage | Plafonds facultatifs par programme et par unité, saisis à l'arbitrage | F-BUD-008 |
| H-08 | Q-08 Contenu et numéro du BE | Contenu du modèle de données (§ 4) ; numéro BE-AAAA-STRUCTURE-NNNN | F-BUD-010, RG-BUD-028 |
| H-09 | Q-09 BE sur plusieurs lignes | Oui | F-BUD-010, RG-BUD-029 |
| H-10 | Q-10 Annulation, modification, reliquat | Annulation motivée par le signataire, qui restitue le crédit ; modification = annulation et nouveau BE ; reliquat libéré à la liquidation définitive | F-BUD-016, 033, RG-BUD-036 |
| H-11 | Q-11 Contrats pluriannuels | Engagement de la tranche de l'exercice ; solde suivi en engagement des exercices suivants | F-BUD-014 |
| H-12 | Q-12 Ordre des visas | SB → SDFBM → DAF → DG, chacun vise ; un refus revient au SB | F-BUD-012, 013 |
| H-13 | Q-13 Signature | Électronique, nominative, horodatée ; document signé joignable en transition | F-BUD-013 |
| H-14 | Q-14 Pouvoir du DUO | Sans plafond jusqu'à 14 999 999 ; lettres commandes et marchés à la Direction Générale | F-BUD-012 |
| H-15 | Q-15 Engagement des recettes | Imputation de chaque encaissement, sans blocage, et suivi mensuel | F-BUD-017, RG-BUD-026 |
| H-16 | Q-16 BE sous 100 000 | Pas de BE par achat | F-BUD-032 |
| H-17 | Q-17 Caisse B | Engagement à chaque réapprovisionnement, imputation à la justification | F-BUD-032, RG-BUD-030 |
| H-18 | Q-18 Facture supérieure au BE | Liquidation limitée à l'engagé, engagement complémentaire pour l'écart | F-BUD-033, RG-BUD-025 |
| H-19 | Q-19 Effet du visa | Met le crédit de côté jusqu'à l'engagement ou l'abandon, sans limite de durée | F-BUD-030, RG-BUD-023 |
| H-20 | Q-20 Dérogation | Aucune à l'engagement ; paie : alerte puis réallocation | F-BUD-011, 031 |
| H-21 | Q-21 Format des rapports | Format défini par l'équipe, exportable en Excel et PDF | F-BUD-020 |
| H-22 | Q-22 Grille de réallocation | Recommandation finale (§ g) en valeurs initiales de la table de décision ; typologie (§ b) pour l'urgence | F-BUD-022, 023, RG-BUD-018 |
| H-23 | Q-23 Assiette du 5 % | Crédit initial de la ligne source, cumul de l'exercice | F-BUD-022 |
| H-24 | Q-24 « Significative », « majeur », seuil de compétence du DG | Non chiffrés : la classification les traite par nature (investissement, dette, subvention → CA), sans seuil de montant | F-BUD-022 |
| H-25 | Q-25 Ligne partiellement engagée | Prélèvement permis dans la limite du disponible | RG-BUD-014 |
| H-26 | Q-26 Comité de réallocation | Existe, avis dans un délai paramétré (5 jours ouvrés), sans quorum contrôlé | F-BUD-023 |
| H-27 | Q-27 Saisine des tutelles | Hors outil ; date, référence et réponse enregistrées | F-BUD-023 |
| H-28 | Q-28 Fin d'exercice | Engagements non liquidés reportés sur décision du DG ; crédits non consommés annulés | F-BUD-040 |
| H-29 | Q-29 Autres modifications | Seule la réallocation est livrée ; budget rectificatif et ouverture sur recettes nouvelles en évolution | — |
| H-30 | Q-30 Financements extérieurs | Pas de budget distinct ; la source de financement de la ligne suffit | F-BUD-026 |
| H-31 | Q-31 Sigles CEA, ASCG, SBB | SBB = Service du Budget ; ASCG = chargé du suivi et du contrôle de gestion ; CEA : libellé neutre en attendant | § 3 |

---

## 13. Ce que ce document remplace

- **Les documents de [JP]** (cahier des charges, catalogue, critères d'acceptation, matrice de conformité) : ils restent une source, conservée pour leurs critères Gherkin. La matrice de conformité est à retirer : elle décrit un état du code qui n'existe pas.
- **`Cahier_des_charges_gestion_budgetaire_ERP_ameliore.md`** : ses exigences de robustesse sont reprises (RNF-01, RNF-02, journal immuable). Le reste (centres de coûts, multi-devises, prévisions glissantes) est hors de ce document.
- **`BACKLOG_MODULE_BUDGETAIRE_GITHUB_ISSUES.md`** : à remplacer par les user stories écrites à partir de ce document.

Le code actuel du paquet `budget/` n'est pas une source : le [rapport n° 3](rapport-03-bilan-code-budget-15-novembre.md) dit ce qui peut en être gardé.
