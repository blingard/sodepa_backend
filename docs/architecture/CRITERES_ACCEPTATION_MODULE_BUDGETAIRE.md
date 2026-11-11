# CRITÈRES D'ACCEPTATION — MODULE 02 : GESTION BUDGÉTAIRE

**SODEPA — Société de Développement et d'Exploitation des Productions Animales**  
**PROJET ERP SODEPA**

| **Élément** | **Valeur** |
| :--- | :--- |
| Code du module | **BUD** |
| Documents de référence | CdC Module 02 (v1.0) ; Catalogue des fonctionnalités Module 02 (v1.0) |
| Fonctionnalités couvertes | 24 / 24 |
| Critères d'acceptation | **75** (nominal : 42, erreur/refus : 17, cas limite : 10, sécurité/contrôle : 6) |
| Règles de gestion vérifiées | **22 / 22** |
| Version / date | 1.0 — Projet — 06/11/2026 |
| Statut | Validé / Référence de Recette |
| Propriétaire métier | SDFBM — Service du Budget (en lien avec la DPD/CSPP) |

---

## 1. Critères Transverses (CT-BUD-01 à CT-BUD-10)

| **ID** | **Critère transverse** |
| :--- | :--- |
| **CT-BUD-01** | Un utilisateur sans le droit requis ne voit pas l'action (menu, bouton) et reçoit un refus explicite s'il tente d'y accéder directement. |
| **CT-BUD-02** | Toute création, modification, validation, rejet ou suppression est journalisée avec l'auteur, la date et l'heure, la nature de l'action et les valeurs avant/après. |
| **CT-BUD-03** | Les champs obligatoires sont signalés ; l'enregistrement est refusé avec un message clair indiquant le champ en cause. |
| **CT-BUD-04** | Un utilisateur ne voit et ne traite que les données de son périmètre (structure, unité, exercice) défini dans le Référentiel. |
| **CT-BUD-05** | Chaque étape d'un circuit de validation notifie l'acteur suivant ; un rejet exige un motif et renvoie le dossier à l'émetteur. |
| **CT-BUD-06** | Les documents générés reprennent le modèle officiel SODEPA et sont archivés automatiquement dans la GED, liés à la transaction. |
| **CT-BUD-07** | Les montants sont exprimés en FCFA, sans décimale, avec séparateur de milliers ; les dates au format JJ/MM/AAAA. |
| **CT-BUD-08** | Les listes et états sont exportables en Excel et en PDF ; l'export respecte les filtres et le périmètre de l'utilisateur. |
| **CT-BUD-09** | Les écrans courants s'affichent en moins de 3 secondes et les recherches en moins de 5 secondes dans les conditions nominales. |
| **CT-BUD-10** | Les seuils, taux et délais utilisés proviennent du paramétrage (avec date d'effet) et non de valeurs codées en dur. |

---

## 2. Critères d'Acceptation par Fonctionnalité (75 CA)

### 2.1 EP-BUD-01 — Structure et nomenclature budgétaire

#### F-BUD-001 — Gestion des exercices budgétaires
- **CA-BUD-001-01 (Nominal, RG-001)** : Étant donné aucun exercice 2027 n'existe, Quand le chef du budget crée l'exercice 2027, Alors l'exercice est créé du 01/01/2027 au 31/12/2027 au statut « En préparation ».
- **CA-BUD-001-02 (Erreur/refus, RG-002)** : Étant donné l'exercice 2027 au statut « Voté », Quand un agent tente de créer un bon d'engagement sur 2027, Alors l'engagement est refusé : « Budget non exécutoire ».
- **CA-BUD-001-03 (Cas limite)** : Étant donné l'exercice 2026 clôturé, Quand un agent tente de saisir une opération datée du 31/12/2026, Alors la saisie est refusée.

#### F-BUD-002 — Nomenclature et plan de codification budgétaire
- **CA-BUD-002-01 (Nominal)** : Étant donné l'exercice 2027, Quand le CSB crée un programme, une action, une activité, une tâche et une ligne rattachée à une unité, Alors la ligne est disponible pour l'imputation avec sa hiérarchie complète.
- **CA-BUD-002-02 (Nominal)** : Étant donné un plan de codification signé par le DG, Quand le CSB l'importe, Alors les codes sont créés et le plan est archivé comme version 1 avec la date de signature.
- **CA-BUD-002-03 (Erreur/refus)** : Étant donné une ligne en création, Quand elle est enregistrée sans nature ou sans source de financement, Alors l'enregistrement est refusé.

#### F-BUD-003 — Versions budgétaires et comparaison
- **CA-BUD-003-01 (Nominal)** : Étant donné la version « Projet » du budget 2027, Quand le CSB crée la version « Voté » à partir du projet, Alors la version « Voté » reprend les montants et la version « Projet » reste inchangée.
- **CA-BUD-003-02 (Nominal)** : Étant donné deux versions d'un même exercice, Quand le DAF les compare, Alors l'écart en valeur et en pourcentage est affiché ligne par ligne.

---

### 2.2 EP-BUD-02 — Préparation et élaboration du budget

#### F-BUD-004 — Campagne budgétaire et lettre-circulaire
- **CA-BUD-004-01 (Nominal)** : Étant donné la campagne 2027 créée, Quand la DPD génère la lettre-circulaire et le chronogramme, Alors la lettre est produite sur le modèle officiel et soumise à la signature du DG.
- **CA-BUD-004-02 (Cas limite)** : Étant donné un jalon du chronogramme fixé au 15/07, Quand la date du 13/07 est atteinte sans réalisation, Alors le responsable du jalon reçoit une alerte.
- **CA-BUD-004-03 (Nominal)** : Étant donné la lettre-circulaire signée, Quand le secrétariat la diffuse, Alors chaque service destinataire la reçoit et la diffusion est tracée.

#### F-BUD-005 — Groupe de travail budgétaire
- **CA-BUD-005-01 (Nominal)** : Étant donné la décision de création signée, Quand la DPD enregistre la décision et les membres, Alors les membres sont convocables et la décision est archivée.
- **CA-BUD-005-02 (Nominal)** : Étant donné une réunion de concertation tenue, Quand la DPD enregistre la feuille de présence et le compte rendu, Alors ils sont rattachés à la campagne budgétaire.

#### F-BUD-006 — CDMT et Projet de Performance Annuel par programme
- **CA-BUD-006-01 (Nominal)** : Étant donné un chef de programme, Quand il saisit le CDMT de son programme pour 2027 à 2029, Alors les montants par activité, indicateurs et cibles sont enregistrés pour les trois années.
- **CA-BUD-006-02 (Erreur/refus)** : Étant donné un PPA sans indicateur pour une activité, Quand le chef de programme le soumet, Alors la soumission est refusée et l'activité en cause est signalée.
- **CA-BUD-006-03 (Sécurité/contrôle)** : Étant donné un chef de programme A, Quand il tente de modifier le PPA du programme B, Alors la modification est refusée.

#### F-BUD-007 — Avant-projets des unités opérationnelles
- **CA-BUD-007-01 (Nominal)** : Étant donné le chef de l'abattoir de Yaoundé, Quand il saisit les budgets de fonctionnement, produits, activités et investissements et joint les TdR, Alors l'avant-projet est soumis au groupe de travail avec ses pièces.
- **CA-BUD-007-02 (Nominal)** : Étant donné l'exécution 2026 disponible, Quand le chef d'unité initialise son avant-projet 2027, Alors les montants sont pré-remplis à partir de l'exécution N-1 et restent modifiables.

#### F-BUD-008 — Consolidation et arbitrages budgétaires
- **CA-BUD-008-01 (Nominal)** : Étant donné les avant-projets de toutes les unités soumis, Quand le Service du Budget lance la consolidation, Alors le total SODEPA par programme, unité et nature est égal à la somme des avant-projets.
- **CA-BUD-008-02 (Nominal)** : Étant donné une ligne proposée à 10 000 000 FCFA, Quand le groupe de travail l'arbitre à 8 000 000 FCFA avec commentaire, Alors les montants proposé, arbitré et retenu et le commentaire sont conservés.

#### F-BUD-009 — Approbation et mise en exécution du budget
- **CA-BUD-009-01 (Nominal, RG-002)** : Étant donné le projet de budget validé par le groupe de travail, Quand il est validé par le DG, approuvé par le CA (résolution jointe) puis rendu exécutoire par décision, Alors le budget passe au statut « Exécutoire » et les crédits sont ouverts à l'engagement.
- **CA-BUD-009-02 (Erreur/refus, RG-002)** : Étant donné un projet validé par le DG sans résolution du CA, Quand le CSB tente de le rendre exécutoire, Alors l'action est refusée.
- **CA-BUD-009-03 (Sécurité/contrôle)** : Étant donné le budget exécutoire, Quand un utilisateur tente de modifier un montant directement, Alors la modification est refusée ; seule une réallocation est possible.
- **CA-BUD-009-04 (Nominal)** : Étant donné le budget exécutoire, Quand le CSB édite les annexes, Alors le budget d'exploitation, d'investissement, le plan de trésorerie et le plan de financement sont produits.

---

### 2.3 EP-BUD-03 — Engagement des dépenses

#### F-BUD-010 — Création du bon d'engagement
- **CA-BUD-010-01 (Nominal, RG-003)** : Étant donné une expression de besoin validée, Quand l'agent du SB crée le BE à partir de celle-ci, Alors le BE reprend objet, montant et bénéficiaire, et reçoit un numéro unique par exercice et unité.
- **CA-BUD-010-02 (Nominal, RG-011)** : Étant donné un état de paie mensuel signé, Quand le CSB crée l'engagement correspondant, Alors le montant de la paie est engagé sur les lignes de personnel.
- **CA-BUD-010-03 (Nominal, RG-011)** : Étant donné un TdR d'activité de 3 000 000 FCFA, Quand l'agent l'engage, Alors le montant global de l'activité est engagé.

#### F-BUD-011 — Contrôle budgétaire bloquant
- **CA-BUD-011-01 (Nominal, RG-004)** : Étant donné une ligne disponible de 2 000 000 FCFA, Quand un BE de 1 500 000 FCFA est saisi, Alors l'engagement est accepté Et le disponible passe à 500 000 FCFA.
- **CA-BUD-011-02 (Erreur/refus, RG-004)** : Étant donné une ligne disponible de 1 000 000 FCFA, Quand un BE de 1 000 001 FCFA est saisi, Alors l'engagement est bloqué avec le message « Crédit insuffisant » et le disponible affiché.
- **CA-BUD-011-03 (Cas limite, RG-004)** : Étant donné une ligne disponible de 1 000 000 FCFA, Quand un BE de 1 000 000 FCFA est saisi, Alors l'engagement est accepté et le disponible devient 0.
- **CA-BUD-011-04 (Erreur/refus, RG-005)** : Étant donné une dépense de nature investissement, Quand l'agent l'impute sur une ligne de fonctionnement, Alors l'engagement est refusé pour imputation incorrecte.

#### F-BUD-012 — Détermination du circuit et pièces obligatoires
- **CA-BUD-012-01 (Cas limite, RG-006)** : Étant donné une dépense de 499 999 FCFA TTC, Quand le BE est créé, Alors le circuit « menue dépense » est appliqué sans bon de commande.
- **CA-BUD-012-02 (Cas limite, RG-007)** : Étant donné une dépense de 500 000 FCFA TTC, Quand le BE est créé, Alors le circuit « grosse dépense » est appliqué et un bon de commande est exigé.
- **CA-BUD-012-03 (Erreur/refus, RG-008)** : Étant donné une grosse dépense en unité avec 2 pro forma, Quand le CSAFC soumet le dossier, Alors la soumission est refusée : 3 pro forma exigées.
- **CA-BUD-012-04 (Erreur/refus, RG-009)** : Étant donné une demande d'acquisition sans attestation de carence, Quand l'agent soumet le BE, Alors la soumission est refusée.
- **CA-BUD-012-05 (Erreur/refus, RG-010)** : Étant donné une grosse dépense sans dossier fiscal du prestataire, Quand l'agent soumet le BE, Alors la soumission est refusée.
- **CA-BUD-012-06 (Nominal)** : Étant donné un BE créé à l'unité de Ndokayo, Quand il est soumis, Alors il est routé au DUO de Ndokayo et non au DG.

#### F-BUD-013 — Visa budgétaire et signature
- **CA-BUD-013-01 (Nominal)** : Étant donné un BE contrôlé, Quand le CSB appose son visa, Alors le visa est enregistré avec signataire, date et montant.
- **CA-BUD-013-02 (Sécurité/contrôle, RG-021)** : Étant donné un BE créé par l'agent A, Quand A tente de le signer, Alors la signature est refusée.
- **CA-BUD-013-03 (Sécurité/contrôle, RG-022)** : Étant donné un BE signé, Quand un utilisateur tente d'en modifier le montant, Alors la modification est refusée ; seule l'annulation tracée est possible.
- **CA-BUD-013-04 (Nominal, RG-012)** : Étant donné un BE signé, Quand le circuit est terminé, Alors une copie est archivée au SB et l'original est transmis au SMP.

#### F-BUD-014 — Bon de commande lié à l'engagement
- **CA-BUD-014-01 (Nominal, RG-007)** : Étant donné un BE signé de 750 000 FCFA, Quand l'agent génère le BC, Alors le BC est lié au BE et transmis au module Marchés et au prestataire.
- **CA-BUD-014-02 (Erreur/refus, RG-007)** : Étant donné aucun BE signé, Quand l'agent tente de générer un BC, Alors la génération est refusée.

#### F-BUD-015 — Circuits spécifiques : missions, véhicules, études
- **CA-BUD-015-01 (Nominal, RG-003)** : Étant donné un TdR validé, une note de service et des ordres de mission, Quand le SB vise et établit le BE, Alors le BE est transmis au SFR.
- **CA-BUD-015-02 (Cas limite, RG-007)** : Étant donné un devis d'entretien de 499 999 FCFA TTC puis un autre de 500 000 FCFA TTC, Quand les BE sont établis, Alors le premier n'a pas de BC, le second exige un BC.
- **CA-BUD-015-03 (Erreur/refus)** : Étant donné une fiche d'entretien sans diagnostic de la Brigade de Maintenance, Quand l'agent la soumet, Alors la soumission est refusée.
- **CA-BUD-015-04 (Nominal)** : Étant donné un TdR d'étude et l'autorisation du DG, Quand le SB établit le BE, Alors le TdR est transmis au CSFR avec le BE.

#### F-BUD-016 — Annulation et réduction d'un engagement
- **CA-BUD-016-01 (Nominal, RG-022)** : Étant donné un BE de 800 000 FCFA non liquidé, Quand le CSB l'annule avec motif et validation, Alors le crédit de 800 000 FCFA est restitué à la ligne Et le BE initial reste consultable au statut « Annulé ».
- **CA-BUD-016-02 (Erreur/refus)** : Étant donné un BE signé, Quand l'utilisateur l'annule sans motif, Alors l'annulation est refusée.
- **CA-BUD-016-03 (Cas limite)** : Étant donné un BE de 1 000 000 FCFA, Quand il est réduit à 600 000 FCFA, Alors 400 000 FCFA sont restitués au disponible.

---

### 2.4 EP-BUD-04 — Recettes budgétaires

#### F-BUD-017 — Suivi et rapprochement des recettes
- **CA-BUD-017-01 (Nominal)** : Étant donné des encaissements validés dans le module Finance, Quand le CSB consulte le suivi des recettes, Alors les recettes réalisées sont affichées par nature et par unité.
- **CA-BUD-017-02 (Nominal)** : Étant donné une prévision de 10 000 000 FCFA et 7 500 000 FCFA réalisés, Quand le suivi mensuel est produit, Alors le taux de réalisation affiché est de 75 %.
- **CA-BUD-017-03 (Erreur/refus)** : Étant donné un encaissement non comptabilisé, Quand le rapprochement est lancé, Alors l'écart entre budget, finance et comptabilité est listé.

---

### 2.5 EP-BUD-05 — Contrôle, suivi et reporting

#### F-BUD-018 — Contrôle a priori des pièces
- **CA-BUD-018-01 (Nominal, RG-005)** : Étant donné un BE de type « achat de fournitures », Quand l'agent ouvre le contrôle a priori, Alors la liste des pièces à contrôler pour ce type s'affiche.
- **CA-BUD-018-02 (Erreur/refus, RG-005)** : Étant donné un point obligatoire non coché, Quand l'agent soumet à la signature, Alors la soumission est refusée.

#### F-BUD-019 — Situation d'exécution en temps réel
- **CA-BUD-019-01 (Nominal)** : Étant donné une ligne votée de 5 000 000 FCFA avec 2 000 000 engagés, Quand un nouvel engagement de 1 000 000 FCFA est signé, Alors la situation affiche 3 000 000 engagés et 2 000 000 disponibles sans délai.
- **CA-BUD-019-02 (Nominal)** : Étant donné la situation d'exécution, Quand le DAF filtre par programme puis par unité, Alors les montants voté, engagé, liquidé, payé et disponible sont affichés pour chaque axe.
- **CA-BUD-019-03 (Cas limite)** : Étant donné une ligne de 10 000 000 FCFA engagée à 7 900 000, Quand un engagement de 100 000 FCFA est signé, Alors une alerte de consommation à 80 % est envoyée au gestionnaire.

#### F-BUD-020 — Rapports mensuels et trimestriels
- **CA-BUD-020-01 (Nominal, RG-013)** : Étant donné le mois d'octobre clôturé, Quand le rapport est généré, Alors il présente dépassements, économies et niveau d'engagement en valeur et en nombre.
- **CA-BUD-020-02 (Cas limite, RG-013)** : Étant donné le 05 du mois pour une unité et le 10 pour la DG, Quand le rapport n'est pas transmis à l'échéance, Alors une alerte est envoyée au responsable.
- **CA-BUD-020-03 (Nominal)** : Étant donné une ligne en dépassement, Quand le CSB saisit l'explication et joint un justificatif, Alors l'explication apparaît dans le rapport.

---

### 2.6 EP-BUD-06 — Réallocation des crédits

#### F-BUD-021 — Demande de réallocation
- **CA-BUD-021-01 (Nominal)** : Étant donné un demandeur, Quand il saisit objet, lignes source et cible, montant, justification, urgence, impact, risques et pièces, Alors la demande est soumise.
- **CA-BUD-021-02 (Erreur/refus)** : Étant donné une demande sans justification, Quand le demandeur la soumet, Alors la soumission est refusée.
- **CA-BUD-021-03 (Sécurité/contrôle, RG-019)** : Étant donné une dépense financée par une réallocation non validée, Quand l'agent tente de l'engager, Alors l'engagement est refusé.

#### F-BUD-022 — Analyse et classification de la réallocation
- **CA-BUD-022-01 (Nominal, RG-015, RG-018)** : Étant donné une demande de réallocation portant sur 4 % du crédit d'une ligne de fonctionnement au sein d'une même direction, Quand la DAF l'analyse, Alors la demande est classée « Simple » et orientée vers le circuit DAF + DG.
- **CA-BUD-022-02 (Nominal, RG-016, RG-018)** : Étant donné une réallocation de 6 % d'une ligne ou entre investissement et fonctionnement, Quand la DAF l'analyse, Alors la demande est classée « Significative » et orientée vers le circuit Comité + CA.
- **CA-BUD-022-03 (Erreur/refus, RG-014)** : Étant donné une ligne source dont le disponible non engagé est inférieur au montant à prélever, Quand la demande est soumise, Alors la demande est refusée pour crédit source insuffisant ou engagé.
- **CA-BUD-022-04 (Cas limite)** : Étant donné une réallocation exactement égale à 5,00 % de la ligne source, Quand elle est analysée, Alors elle reste classée dans le seuil « Simple » (DAF + DG).

#### F-BUD-023 — Circuit de décision de la réallocation
- **CA-BUD-023-01 (Nominal, RG-015)** : Étant donné une réallocation simple validée par la DAF, Quand le DG signe la décision, Alors la décision interne de réallocation est émise et le statut passe à « Exécutée ».
- **CA-BUD-023-02 (Nominal, RG-016)** : Étant donné une réallocation significative avec avis favorable du comité, Quand la résolution du CA est enregistrée, Alors la réallocation est approuvée et rendue exécutoire.
- **CA-BUD-023-03 (Cas limite, RG-017)** : Étant donné une urgence sanitaire ou vétérinaire avérée sur un ranch, Quand le DG prend une mesure conservatoire, Alors les crédits sont transférés immédiatement avec statut « À régulariser CA ».
- **CA-BUD-023-04 (Sécurité/contrôle, RG-021)** : Étant donné un demandeur membre du comité de réallocation, Quand sa propre demande est examinée, Alors il ne peut pas voter ni approuver sa demande.

#### F-BUD-024 — Exécution et suivi des réallocations
- **CA-BUD-024-01 (Nominal, RG-019, RG-020)** : Étant donné une réallocation approuvée, Quand elle est exécutée, Alors le crédit de la ligne source est diminué et celui de la ligne cible est augmenté du montant exact.
- **CA-BUD-024-02 (Nominal, RG-019)** : Étant donné des réallocations validées sur l'exercice, Quand le CSB édite le budget rectifié, Alors les montants initiaux, rectifiés et le détail des transferts sont édités avec leurs références décisionnelles.
- **CA-BUD-024-03 (Erreur/refus, RG-020)** : Étant donné une demande de réallocation en cours d'instruction, Quand un service tente d'émettre un bon d'engagement sur la ligne cible avant la décision finale, Alors le système bloque l'engagement pour défaut de couverture budgétaire.
