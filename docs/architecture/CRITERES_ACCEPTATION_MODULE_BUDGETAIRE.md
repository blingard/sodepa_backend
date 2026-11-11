**CRITÈRES D'ACCEPTATION**

**Module 02 — Gestion budgétaire**

| **Élément**                   | **Valeur**                                                                   |
| ----------------------------- | ---------------------------------------------------------------------------- |
| Code du module                | BUD                                                                          |
| Documents de référence        | CdC Module 02 (v1.0) ; Catalogue des fonctionnalités Module 02 (v1.0)        |
| Fonctionnalités couvertes     | 24 / 24                                                                      |
| Critères d'acceptation        | 75 (nominal : 42, erreur/refus : 17, cas limite : 10, sécurité/contrôle : 6) |
| Règles de gestion vérifiées   | 22 / 22                                                                      |
| Version / date                | 1.0 — Projet — 06/11/2026                                                    |
| Statut                        | À valider par le propriétaire métier                                         |
| Propriétaire métier pressenti | SDFBM — Service du Budget (en lien avec la DPD/CSPP)                         |
| Étape suivante                | Cas de test (étape 4, Excel)                                                 |

**Sommaire**

1\. Objet et conventions

2\. Critères transverses applicables à toutes les fonctionnalités

3\. Synthèse par fonctionnalité

4\. Critères d'acceptation par fonctionnalité

5\. Couverture des règles de gestion

6\. Validation du document

# 1. Objet et conventions

Ce document constitue l'étape 3 de la démarche qualité du projet ERP SODEPA. Il définit, pour chacune des 24 fonctionnalités du module 02, les critères d'acceptation qui permettront de prononcer sa conformité lors de la recette. Une fonctionnalité est acceptée lorsque tous ses critères, ainsi que les critères transverses de la section 2, sont satisfaits.

Les critères sont rédigés au format Gherkin :

- **Étant donné** : le contexte et les données de départ ;
- **Quand** : l'action réalisée par l'utilisateur ou l'événement déclencheur ;
- **Alors** : le résultat observable attendu ; **Et** ajoute une condition supplémentaire.

Chaque critère est typé afin d'équilibrer la recette :

- **Nominal** : le fonctionnement attendu dans le cas courant ;
- **Erreur / refus** : le système bloque ou refuse une action non conforme ;
- **Cas limite** : comportement aux seuils, échéances et valeurs frontières ;
- **Sécurité / contrôle** : habilitations, séparation des tâches, traçabilité, contrôle interne.

Identifiant : **CA-BUD-nnn-kk**, où nnn est le numéro de la fonctionnalité F-BUD-nnn et kk le rang du critère. La colonne « Règles » renvoie aux règles RG-BUD-nnn du cahier des charges. Les valeurs chiffrées (seuils, délais) sont celles des procédures SODEPA ; elles sont paramétrables et doivent être recettées avec les valeurs en vigueur au moment des tests.

# 2. Critères transverses applicables à toutes les fonctionnalités

Ces critères ne sont pas répétés dans chaque fiche ; ils sont vérifiés pour chaque fonctionnalité du module.

| **ID**    | **Critère transverse**                                                                                                                                              |
| --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| CT-BUD-01 | Un utilisateur sans le droit requis ne voit pas l'action (menu, bouton) et reçoit un refus explicite s'il tente d'y accéder directement.                            |
| CT-BUD-02 | Toute création, modification, validation, rejet ou suppression est journalisée avec l'auteur, la date et l'heure, la nature de l'action et les valeurs avant/après. |
| CT-BUD-03 | Les champs obligatoires sont signalés ; l'enregistrement est refusé avec un message clair indiquant le champ en cause.                                              |
| CT-BUD-04 | Un utilisateur ne voit et ne traite que les données de son périmètre (structure, unité, exercice) défini dans le Référentiel.                                       |
| CT-BUD-05 | Chaque étape d'un circuit de validation notifie l'acteur suivant ; un rejet exige un motif et renvoie le dossier à l'émetteur.                                      |
| CT-BUD-06 | Les documents générés reprennent le modèle officiel SODEPA et sont archivés automatiquement dans la GED, liés à la transaction.                                     |
| CT-BUD-07 | Les montants sont exprimés en FCFA, sans décimale, avec séparateur de milliers ; les dates au format JJ/MM/AAAA.                                                    |
| CT-BUD-08 | Les listes et états sont exportables en Excel et en PDF ; l'export respecte les filtres et le périmètre de l'utilisateur.                                           |
| CT-BUD-09 | Les écrans courants s'affichent en moins de 3 secondes et les recherches en moins de 5 secondes dans les conditions nominales.                                      |
| CT-BUD-10 | Les seuils, taux et délais utilisés proviennent du paramétrage (avec date d'effet) et non de valeurs codées en dur.                                                 |

# 3. Synthèse par fonctionnalité

| **Fonctionnalité** | **Libellé**                                        | **Nb CA** | **N** | **E** | **L** | **S** |
| ------------------ | -------------------------------------------------- | --------- | ----- | ----- | ----- | ----- |
| F-BUD-001          | Gestion des exercices budgétaires                  | 3         | 1     | 1     | 1     | 0     |
| F-BUD-002          | Nomenclature et plan de codification budgétaire    | 3         | 2     | 1     | 0     | 0     |
| F-BUD-003          | Versions budgétaires et comparaison                | 2         | 2     | 0     | 0     | 0     |
| F-BUD-004          | Campagne budgétaire et lettre-circulaire           | 3         | 2     | 0     | 1     | 0     |
| F-BUD-005          | Groupe de travail budgétaire                       | 2         | 2     | 0     | 0     | 0     |
| F-BUD-006          | CDMT et Projet de Performance Annuel par programme | 3         | 1     | 1     | 0     | 1     |
| F-BUD-007          | Avant-projets des unités opérationnelles           | 2         | 2     | 0     | 0     | 0     |
| F-BUD-008          | Consolidation et arbitrages budgétaires            | 2         | 2     | 0     | 0     | 0     |
| F-BUD-009          | Approbation et mise en exécution du budget         | 4         | 2     | 1     | 0     | 1     |
| F-BUD-010          | Création du bon d'engagement                       | 3         | 3     | 0     | 0     | 0     |
| F-BUD-011          | Contrôle budgétaire bloquant                       | 4         | 1     | 2     | 1     | 0     |
| F-BUD-012          | Détermination du circuit et pièces obligatoires    | 6         | 1     | 3     | 2     | 0     |
| F-BUD-013          | Visa budgétaire et signature                       | 4         | 2     | 0     | 0     | 2     |
| F-BUD-014          | Bon de commande lié à l'engagement                 | 2         | 1     | 1     | 0     | 0     |
| F-BUD-015          | Circuits spécifiques : missions, véhicules, études | 4         | 2     | 1     | 1     | 0     |
| F-BUD-016          | Annulation et réduction d'un engagement            | 3         | 1     | 1     | 1     | 0     |
| F-BUD-017          | Suivi et rapprochement des recettes                | 3         | 2     | 1     | 0     | 0     |
| F-BUD-018          | Contrôle a priori des pièces                       | 2         | 1     | 1     | 0     | 0     |
| F-BUD-019          | Situation d'exécution en temps réel                | 3         | 2     | 0     | 1     | 0     |
| F-BUD-020          | Rapports mensuels et trimestriels                  | 3         | 2     | 0     | 1     | 0     |
| F-BUD-021          | Demande de réallocation                            | 3         | 1     | 1     | 0     | 1     |
| F-BUD-022          | Analyse et classification de la réallocation       | 4         | 2     | 1     | 1     | 0     |
| F-BUD-023          | Circuit de décision de la réallocation             | 4         | 3     | 0     | 0     | 1     |
| F-BUD-024          | Exécution et suivi des réallocations               | 3         | 2     | 1     | 0     | 0     |

# 4. Critères d'acceptation par fonctionnalité

## 4.1 EP-BUD-01 — Structure et nomenclature budgétaire

### F-BUD-001 — Gestion des exercices budgétaires

_En tant que chef du budget, je veux ouvrir, faire évoluer et clôturer les exercices budgétaires, afin que chaque opération soit rattachée au bon exercice (1er janvier – 31 décembre)._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                             | **Règles** |
| ------------- | -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-001-01 | Nominal        | **Ouverture d'un exercice**<br><br>**Étant donné** aucun exercice 2027 n'existe<br><br>**Quand** le chef du budget crée l'exercice 2027<br><br>**Alors** l'exercice est créé du 01/01/2027 au 31/12/2027 au statut « En préparation »           | 001        |
| CA-BUD-001-02 | Erreur / refus | **Engagement sur exercice non exécutoire**<br><br>**Étant donné** l'exercice 2027 au statut « Voté »<br><br>**Quand** un agent tente de créer un bon d'engagement sur 2027<br><br>**Alors** l'engagement est refusé : « Budget non exécutoire » | 002        |
| CA-BUD-001-03 | Cas limite     | **Exercice clôturé**<br><br>**Étant donné** l'exercice 2026 clôturé<br><br>**Quand** un agent tente de saisir une opération datée du 31/12/2026<br><br>**Alors** la saisie est refusée                                                          | —          |

### F-BUD-002 — Nomenclature et plan de codification budgétaire

_En tant que chef du budget, je veux gérer la nomenclature programme → action → activité → tâche → ligne et importer le plan de codification signé, afin que toutes les imputations utilisent les mêmes codes._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                                                             | **Règles** |
| ------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-002-01 | Nominal        | **Création de la nomenclature**<br><br>**Étant donné** l'exercice 2027<br><br>**Quand** le CSB crée un programme, une action, une activité, une tâche et une ligne rattachée à une unité<br><br>**Alors** la ligne est disponible pour l'imputation avec sa hiérarchie complète | —          |
| CA-BUD-002-02 | Nominal        | **Import du plan de codification signé**<br><br>**Étant donné** un plan de codification signé par le DG<br><br>**Quand** le CSB l'importe<br><br>**Alors** les codes sont créés et le plan est archivé comme version 1 avec la date de signature                                | —          |
| CA-BUD-002-03 | Erreur / refus | **Ligne sans nature ou source**<br><br>**Étant donné** une ligne en création<br><br>**Quand** elle est enregistrée sans nature ou sans source de financement<br><br>**Alors** l'enregistrement est refusé                                                                       | —          |

### F-BUD-003 — Versions budgétaires et comparaison

_En tant que chef du budget, je veux gérer plusieurs versions (avant-projet, projet, voté, rectifié) et les comparer, afin de suivre l'évolution du budget._

| **ID**        | **Type** | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                                     | **Règles** |
| ------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-003-01 | Nominal  | **Nouvelle version**<br><br>**Étant donné** la version « Projet » du budget 2027<br><br>**Quand** le CSB crée la version « Voté » à partir du projet<br><br>**Alors** la version « Voté » reprend les montants et la version « Projet » reste inchangée | —          |
| CA-BUD-003-02 | Nominal  | **Comparaison de versions**<br><br>**Étant donné** deux versions d'un même exercice<br><br>**Quand** le DAF les compare<br><br>**Alors** l'écart en valeur et en pourcentage est affiché ligne par ligne                                                | —          |

## 4.2 EP-BUD-02 — Préparation et élaboration du budget

### F-BUD-004 — Campagne budgétaire et lettre-circulaire

_En tant que chef de la DPD, je veux lancer la campagne budgétaire avec la lettre-circulaire et le chronogramme, afin que tous les services connaissent les orientations et les jalons._

| **ID**        | **Type**   | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                               | **Règles** |
| ------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-004-01 | Nominal    | **Lettre-circulaire générée**<br><br>**Étant donné** la campagne 2027 créée<br><br>**Quand** la DPD génère la lettre-circulaire et le chronogramme<br><br>**Alors** la lettre est produite sur le modèle officiel et soumise à la signature du DG | —          |
| CA-BUD-004-02 | Cas limite | **Alerte de jalon**<br><br>**Étant donné** un jalon du chronogramme fixé au 15/07<br><br>**Quand** la date du 13/07 est atteinte sans réalisation<br><br>**Alors** le responsable du jalon reçoit une alerte                                      | —          |
| CA-BUD-004-03 | Nominal    | **Diffusion**<br><br>**Étant donné** la lettre-circulaire signée<br><br>**Quand** le secrétariat la diffuse<br><br>**Alors** chaque service destinataire la reçoit et la diffusion est tracée                                                     | —          |

### F-BUD-005 — Groupe de travail budgétaire

_En tant que chef de la DPD, je veux enregistrer la décision de création du groupe de travail, ses membres et ses réunions, afin de tracer la concertation._

| **ID**        | **Type** | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                            | **Règles** |
| ------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- |
| CA-BUD-005-01 | Nominal  | **Groupe de travail**<br><br>**Étant donné** la décision de création signée<br><br>**Quand** la DPD enregistre la décision et les membres<br><br>**Alors** les membres sont convocables et la décision est archivée            | —          |
| CA-BUD-005-02 | Nominal  | **Compte rendu de réunion**<br><br>**Étant donné** une réunion de concertation tenue<br><br>**Quand** la DPD enregistre la feuille de présence et le compte rendu<br><br>**Alors** ils sont rattachés à la campagne budgétaire | —          |

### F-BUD-006 — CDMT et Projet de Performance Annuel par programme

_En tant que chef de programme, je veux saisir et réviser le CDMT sur trois ans et le PPA de mon programme, afin de décliner la stratégie en activités, indicateurs et cibles._

| **ID**        | **Type**            | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                          | **Règles** |
| ------------- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-006-01 | Nominal             | **Saisie du CDMT**<br><br>**Étant donné** un chef de programme<br><br>**Quand** il saisit le CDMT de son programme pour 2027 à 2029<br><br>**Alors** les montants par activité, indicateurs et cibles sont enregistrés pour les trois années | —          |
| CA-BUD-006-02 | Erreur / refus      | **PPA incomplet**<br><br>**Étant donné** un PPA sans indicateur pour une activité<br><br>**Quand** le chef de programme le soumet<br><br>**Alors** la soumission est refusée et l'activité en cause est signalée                             | —          |
| CA-BUD-006-03 | Sécurité / contrôle | **Périmètre du chef de programme**<br><br>**Étant donné** un chef de programme A<br><br>**Quand** il tente de modifier le PPA du programme B<br><br>**Alors** la modification est refusée                                                    | —          |

### F-BUD-007 — Avant-projets des unités opérationnelles

_En tant que chef d'unité, je veux saisir les avant-projets de mon unité et y joindre les éléments de maturation, afin qu'ils soient consolidés au siège._

| **ID**        | **Type** | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                                                                | **Règles** |
| ------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-007-01 | Nominal  | **Avant-projet d'unité**<br><br>**Étant donné** le chef de l'abattoir de Yaoundé<br><br>**Quand** il saisit les budgets de fonctionnement, produits, activités et investissements et joint les TdR<br><br>**Alors** l'avant-projet est soumis au groupe de travail avec ses pièces | —          |
| CA-BUD-007-02 | Nominal  | **Pré-remplissage**<br><br>**Étant donné** l'exécution 2026 disponible<br><br>**Quand** le chef d'unité initialise son avant-projet 2027<br><br>**Alors** les montants sont pré-remplis à partir de l'exécution N-1 et restent modifiables                                         | —          |

### F-BUD-008 — Consolidation et arbitrages budgétaires

_En tant que membre du groupe de travail, je veux consolider automatiquement CDMT, PPA et avant-projets et tracer les arbitrages, afin de produire un projet de budget global cohérent._

| **ID**        | **Type** | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                                                    | **Règles** |
| ------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-008-01 | Nominal  | **Consolidation automatique**<br><br>**Étant donné** les avant-projets de toutes les unités soumis<br><br>**Quand** le Service du Budget lance la consolidation<br><br>**Alors** le total SODEPA par programme, unité et nature est égal à la somme des avant-projets  | —          |
| CA-BUD-008-02 | Nominal  | **Traçabilité des arbitrages**<br><br>**Étant donné** une ligne proposée à 10 000 000 FCFA<br><br>**Quand** le groupe de travail l'arbitre à 8 000 000 FCFA avec commentaire<br><br>**Alors** les montants proposé, arbitré et retenu et le commentaire sont conservés | —          |

### F-BUD-009 — Approbation et mise en exécution du budget

_En tant que DG, je veux faire valider le projet de budget en interne, le soumettre au CA puis le rendre exécutoire, afin que seul le budget approuvé soit ouvert à l'engagement._

| **ID**        | **Type**            | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                                                                                                           | **Règles** |
| ------------- | ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-009-01 | Nominal             | **Circuit d'approbation**<br><br>**Étant donné** le projet de budget validé par le groupe de travail<br><br>**Quand** il est validé par le DG, approuvé par le CA (résolution jointe) puis rendu exécutoire par décision<br><br>**Alors** le budget passe au statut « Exécutoire » et les crédits sont ouverts à l'engagement | 002        |
| CA-BUD-009-02 | Erreur / refus      | **Exécution sans résolution du CA**<br><br>**Étant donné** un projet validé par le DG sans résolution du CA<br><br>**Quand** le CSB tente de le rendre exécutoire<br><br>**Alors** l'action est refusée                                                                                                                       | 002        |
| CA-BUD-009-03 | Sécurité / contrôle | **Verrouillage de la version votée**<br><br>**Étant donné** le budget exécutoire<br><br>**Quand** un utilisateur tente de modifier un montant directement<br><br>**Alors** la modification est refusée ; seule une réallocation est possible                                                                                  | —          |
| CA-BUD-009-04 | Nominal             | **Annexes budgétaires**<br><br>**Étant donné** le budget exécutoire<br><br>**Quand** le CSB édite les annexes<br><br>**Alors** le budget d'exploitation, d'investissement, le plan de trésorerie et le plan de financement sont produits                                                                                      | —          |

## 4.3 EP-BUD-03 — Engagement des dépenses

### F-BUD-010 — Création du bon d'engagement

_En tant qu'agent du Service du Budget, je veux créer un bon d'engagement à partir de la pièce d'origine (besoin, TdR, OM, fiche d'entretien, état de paie, contrat), afin d'engager la dépense avant tout paiement._

| **ID**        | **Type** | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                                                           | **Règles** |
| ------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-010-01 | Nominal  | **BE depuis une expression de besoin**<br><br>**Étant donné** une expression de besoin validée<br><br>**Quand** l'agent du SB crée le BE à partir de celle-ci<br><br>**Alors** le BE reprend objet, montant et bénéficiaire, et reçoit un numéro unique par exercice et unité | 003        |
| CA-BUD-010-02 | Nominal  | **Engagement de la masse salariale**<br><br>**Étant donné** un état de paie mensuel signé<br><br>**Quand** le CSB crée l'engagement correspondant<br><br>**Alors** le montant de la paie est engagé sur les lignes de personnel                                               | 011        |
| CA-BUD-010-03 | Nominal  | **Engagement d'un TdR global**<br><br>**Étant donné** un TdR d'activité de 3 000 000 FCFA<br><br>**Quand** l'agent l'engage<br><br>**Alors** le montant global de l'activité est engagé                                                                                       | 011        |

### F-BUD-011 — Contrôle budgétaire bloquant

_En tant que chef du budget, je veux que l'ERP contrôle automatiquement l'inscription au budget, l'imputation et la disponibilité des crédits, afin qu'aucune dépense ne soit engagée sans crédit._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                              | **Règles** |
| ------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- |
| CA-BUD-011-01 | Nominal        | **Crédit suffisant**<br><br>**Étant donné** une ligne disponible de 2 000 000 FCFA<br><br>**Quand** un BE de 1 500 000 FCFA est saisi<br><br>**Alors** l'engagement est accepté<br><br>**Et** le disponible passe à 500 000 FCFA                 | 004        |
| CA-BUD-011-02 | Erreur / refus | **Crédit insuffisant**<br><br>**Étant donné** une ligne disponible de 1 000 000 FCFA<br><br>**Quand** un BE de 1 000 001 FCFA est saisi<br><br>**Alors** l'engagement est bloqué avec le message « Crédit insuffisant » et le disponible affiché | 004        |
| CA-BUD-011-03 | Cas limite     | **Montant égal au disponible**<br><br>**Étant donné** une ligne disponible de 1 000 000 FCFA<br><br>**Quand** un BE de 1 000 000 FCFA est saisi<br><br>**Alors** l'engagement est accepté et le disponible devient 0                             | 004        |
| CA-BUD-011-04 | Erreur / refus | **Imputation incohérente**<br><br>**Étant donné** une dépense de nature investissement<br><br>**Quand** l'agent l'impute sur une ligne de fonctionnement<br><br>**Alors** l'engagement est refusé pour imputation incorrecte                     | 005        |

### F-BUD-012 — Détermination du circuit et pièces obligatoires

_En tant qu'agent du budget, je veux que le circuit (menue ou grosse dépense, DG ou unité) et les pièces obligatoires soient déterminés automatiquement, afin d'appliquer la procédure sans erreur._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                | **Règles** |
| ------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- |
| CA-BUD-012-01 | Cas limite     | **Seuil menue dépense**<br><br>**Étant donné** une dépense de 499 999 FCFA TTC<br><br>**Quand** le BE est créé<br><br>**Alors** le circuit « menue dépense » est appliqué sans bon de commande                     | 006        |
| CA-BUD-012-02 | Cas limite     | **Seuil grosse dépense**<br><br>**Étant donné** une dépense de 500 000 FCFA TTC<br><br>**Quand** le BE est créé<br><br>**Alors** le circuit « grosse dépense » est appliqué et un bon de commande est exigé        | 007        |
| CA-BUD-012-03 | Erreur / refus | **Moins de 3 pro forma en unité**<br><br>**Étant donné** une grosse dépense en unité avec 2 pro forma<br><br>**Quand** le CSAFC soumet le dossier<br><br>**Alors** la soumission est refusée : 3 pro forma exigées | 008        |
| CA-BUD-012-04 | Erreur / refus | **Pièce obligatoire manquante**<br><br>**Étant donné** une demande d'acquisition sans attestation de carence<br><br>**Quand** l'agent soumet le BE<br><br>**Alors** la soumission est refusée                      | 009        |
| CA-BUD-012-05 | Erreur / refus | **Dossier fiscal absent**<br><br>**Étant donné** une grosse dépense sans dossier fiscal du prestataire<br><br>**Quand** l'agent soumet le BE<br><br>**Alors** la soumission est refusée                            | 010        |
| CA-BUD-012-06 | Nominal        | **Circuit unité**<br><br>**Étant donné** un BE créé à l'unité de Ndokayo<br><br>**Quand** il est soumis<br><br>**Alors** il est routé au DUO de Ndokayo et non au DG                                               | —          |

### F-BUD-013 — Visa budgétaire et signature

_En tant que signataire, je veux viser et signer électroniquement le bon d'engagement, afin que l'engagement soit valide et tracé._

| **ID**        | **Type**            | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                 | **Règles** |
| ------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-013-01 | Nominal             | **Visa électronique**<br><br>**Étant donné** un BE contrôlé<br><br>**Quand** le CSB appose son visa<br><br>**Alors** le visa est enregistré avec signataire, date et montant                                        | —          |
| CA-BUD-013-02 | Sécurité / contrôle | **Auto-validation**<br><br>**Étant donné** un BE créé par l'agent A<br><br>**Quand** A tente de le signer<br><br>**Alors** la signature est refusée                                                                 | 021        |
| CA-BUD-013-03 | Sécurité / contrôle | **BE signé non modifiable**<br><br>**Étant donné** un BE signé<br><br>**Quand** un utilisateur tente d'en modifier le montant<br><br>**Alors** la modification est refusée ; seule l'annulation tracée est possible | 022        |
| CA-BUD-013-04 | Nominal             | **Archivage et transmission**<br><br>**Étant donné** un BE signé<br><br>**Quand** le circuit est terminé<br><br>**Alors** une copie est archivée au SB et l'original est transmis au SMP                            | 012        |

### F-BUD-014 — Bon de commande lié à l'engagement

_En tant qu'agent du budget, je veux générer le bon de commande lié au bon d'engagement pour les grosses dépenses, afin de le transmettre au prestataire et au module Marchés._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                         | **Règles** |
| ------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-014-01 | Nominal        | **BC généré**<br><br>**Étant donné** un BE signé de 750 000 FCFA<br><br>**Quand** l'agent génère le BC<br><br>**Alors** le BC est lié au BE et transmis au module Marchés et au prestataire | 007        |
| CA-BUD-014-02 | Erreur / refus | **BC sans BE**<br><br>**Étant donné** aucun BE signé<br><br>**Quand** l'agent tente de générer un BC<br><br>**Alors** la génération est refusée                                             | 007        |

### F-BUD-015 — Circuits spécifiques : missions, véhicules, études

_En tant qu'agent du budget, je veux appliquer les circuits propres aux missions, à l'entretien des véhicules et aux études, afin de respecter les cas pratiques de la procédure._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                        | **Règles** |
| ------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- |
| CA-BUD-015-01 | Nominal        | **Mission**<br><br>**Étant donné** un TdR validé, une note de service et des ordres de mission<br><br>**Quand** le SB vise et établit le BE<br><br>**Alors** le BE est transmis au SFR                                                     | 003        |
| CA-BUD-015-02 | Cas limite     | **Entretien de véhicule au seuil**<br><br>**Étant donné** un devis d'entretien de 499 999 FCFA TTC puis un autre de 500 000 FCFA TTC<br><br>**Quand** les BE sont établis<br><br>**Alors** le premier n'a pas de BC, le second exige un BC | 007        |
| CA-BUD-015-03 | Erreur / refus | **Entretien sans diagnostic**<br><br>**Étant donné** une fiche d'entretien sans diagnostic de la Brigade de Maintenance<br><br>**Quand** l'agent la soumet<br><br>**Alors** la soumission est refusée                                      | —          |
| CA-BUD-015-04 | Nominal        | **Étude ou consultant**<br><br>**Étant donné** un TdR d'étude et l'autorisation du DG<br><br>**Quand** le SB établit le BE<br><br>**Alors** le TdR est transmis au CSFR avec le BE                                                         | —          |

### F-BUD-016 — Annulation et réduction d'un engagement

_En tant que chef du budget, je veux annuler ou réduire un engagement avec motif, afin de restituer le crédit non utilisé._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                                                     | **Règles** |
| ------------- | -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-016-01 | Nominal        | **Annulation**<br><br>**Étant donné** un BE de 800 000 FCFA non liquidé<br><br>**Quand** le CSB l'annule avec motif et validation<br><br>**Alors** le crédit de 800 000 FCFA est restitué à la ligne<br><br>**Et** le BE initial reste consultable au statut « Annulé » | 022        |
| CA-BUD-016-02 | Erreur / refus | **Annulation sans motif**<br><br>**Étant donné** un BE signé<br><br>**Quand** l'utilisateur l'annule sans motif<br><br>**Alors** l'annulation est refusée                                                                                                               | —          |
| CA-BUD-016-03 | Cas limite     | **Réduction partielle**<br><br>**Étant donné** un BE de 1 000 000 FCFA<br><br>**Quand** il est réduit à 600 000 FCFA<br><br>**Alors** 400 000 FCFA sont restitués au disponible                                                                                         | —          |

## 4.4 EP-BUD-04 — Recettes budgétaires

### F-BUD-017 — Suivi et rapprochement des recettes

_En tant que chef du budget, je veux suivre les recettes par nature et par unité et les rapprocher des encaissements et de la comptabilité, afin de mesurer le taux de réalisation des prévisions._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                         | **Règles** |
| ------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-017-01 | Nominal        | **Alimentation des recettes**<br><br>**Étant donné** des encaissements validés dans le module Finance<br><br>**Quand** le CSB consulte le suivi des recettes<br><br>**Alors** les recettes réalisées sont affichées par nature et par unité | —          |
| CA-BUD-017-02 | Nominal        | **Taux de réalisation**<br><br>**Étant donné** une prévision de 10 000 000 FCFA et 7 500 000 FCFA réalisés<br><br>**Quand** le suivi mensuel est produit<br><br>**Alors** le taux de réalisation affiché est de 75 %                        | —          |
| CA-BUD-017-03 | Erreur / refus | **Écart de rapprochement**<br><br>**Étant donné** un encaissement non comptabilisé<br><br>**Quand** le rapprochement est lancé<br><br>**Alors** l'écart entre budget, finance et comptabilité est listé                                     | —          |

## 4.5 EP-BUD-05 — Contrôle, suivi et reporting

### F-BUD-018 — Contrôle a priori des pièces

_En tant qu'agent du budget, je veux une liste de contrôle paramétrable par type de dépense à valider avant signature, afin de vérifier la régularité des pièces._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                      | **Règles** |
| ------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- |
| CA-BUD-018-01 | Nominal        | **Liste de contrôle par type**<br><br>**Étant donné** un BE de type « achat de fournitures »<br><br>**Quand** l'agent ouvre le contrôle a priori<br><br>**Alors** la liste des pièces à contrôler pour ce type s'affiche | 005        |
| CA-BUD-018-02 | Erreur / refus | **Contrôle incomplet**<br><br>**Étant donné** un point obligatoire non coché<br><br>**Quand** l'agent soumet à la signature<br><br>**Alors** la soumission est refusée                                                   | 005        |

### F-BUD-019 — Situation d'exécution en temps réel

_En tant que responsable, je veux consulter à tout moment le voté, l'engagé, le liquidé, le payé et le disponible selon tous les axes, afin de piloter l'exécution._

| **ID**        | **Type**   | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                                                         | **Règles** |
| ------------- | ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-019-01 | Nominal    | **Mise à jour en temps réel**<br><br>**Étant donné** une ligne votée de 5 000 000 FCFA avec 2 000 000 engagés<br><br>**Quand** un nouvel engagement de 1 000 000 FCFA est signé<br><br>**Alors** la situation affiche 3 000 000 engagés et 2 000 000 disponibles sans délai | —          |
| CA-BUD-019-02 | Nominal    | **Axes d'analyse**<br><br>**Étant donné** la situation d'exécution<br><br>**Quand** le DAF filtre par programme puis par unité<br><br>**Alors** les montants voté, engagé, liquidé, payé et disponible sont affichés pour chaque axe                                        | —          |
| CA-BUD-019-03 | Cas limite | **Alerte à 80 %**<br><br>**Étant donné** une ligne de 10 000 000 FCFA engagée à 7 900 000<br><br>**Quand** un engagement de 100 000 FCFA est signé<br><br>**Alors** une alerte de consommation à 80 % est envoyée au gestionnaire                                           | —          |

### F-BUD-020 — Rapports mensuels et trimestriels

_En tant que chef du budget, je veux générer automatiquement les rapports mensuels et trimestriels et y saisir les justifications des écarts, afin de respecter les échéances du 05 et du 10 du mois._

| **ID**        | **Type**   | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                | **Règles** |
| ------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- |
| CA-BUD-020-01 | Nominal    | **Rapport mensuel**<br><br>**Étant donné** le mois d'octobre clôturé<br><br>**Quand** le rapport est généré<br><br>**Alors** il présente dépassements, économies et niveau d'engagement en valeur et en nombre     | 013        |
| CA-BUD-020-02 | Cas limite | **Échéance du rapport**<br><br>**Étant donné** le 05 du mois pour une unité et le 10 pour la DG<br><br>**Quand** le rapport n'est pas transmis à l'échéance<br><br>**Alors** une alerte est envoyée au responsable | 013        |
| CA-BUD-020-03 | Nominal    | **Justification des écarts**<br><br>**Étant donné** une ligne en dépassement<br><br>**Quand** le CSB saisit l'explication et joint un justificatif<br><br>**Alors** l'explication apparaît dans le rapport         | —          |

## 4.6 EP-BUD-06 — Réallocation des crédits

### F-BUD-021 — Demande de réallocation

_En tant que demandeur, je veux saisir une fiche de demande de réallocation complète, afin de solliciter le transfert de crédits vers une ligne prioritaire._

| **ID**        | **Type**            | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                   | **Règles** |
| ------------- | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| CA-BUD-021-01 | Nominal             | **Demande complète**<br><br>**Étant donné** un demandeur<br><br>**Quand** il saisit objet, lignes source et cible, montant, justification, urgence, impact, risques et pièces<br><br>**Alors** la demande est soumise | —          |
| CA-BUD-021-02 | Erreur / refus      | **Rubrique manquante**<br><br>**Étant donné** une demande sans justification<br><br>**Quand** le demandeur la soumet<br><br>**Alors** la soumission est refusée                                                       | —          |
| CA-BUD-021-03 | Sécurité / contrôle | **Pas d'exécution avant validation**<br><br>**Étant donné** une dépense financée par une réallocation non validée<br><br>**Quand** l'agent tente de l'engager<br><br>**Alors** l'engagement est refusé                | 019        |

### F-BUD-022 — Analyse et classification de la réallocation

_En tant qu'analyste de la DAF, je veux que la demande soit classée automatiquement et que la disponibilité de la ligne source soit contrôlée, afin de déterminer le circuit applicable._

| **ID**        | **Type**       | **Critère d'acceptation (Gherkin)**                                                                                                                                                                                                                            | **Règles** |
| ------------- | -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------
<truncated 13842 bytes>

NOTE: The output was truncated because it was too long. Use a more targeted query or a smaller range to get the information you need.