<USER_REQUEST>
le cahier de charge du module budget:
**SODEPA**

_Société de Développement et d'Exploitation des Productions Animales_

**PROJET ERP SODEPA**

**CAHIER DES CHARGES FONCTIONNEL**

**Module 02 — Gestion budgétaire**

| **Élément**                   | **Valeur**                                                            |
| ----------------------------- | --------------------------------------------------------------------- |
| Code du module                | BUD                                                                   |
| Document source analysé       | Procédure BUDGETAIRE OK.pdf (20 pages)                                |
| Version                       | 1.0 — Projet                                                          |
| Date                          | 06/11/2026                                                            |
| Statut                        | À valider par le propriétaire métier                                  |
| Propriétaire métier pressenti | SDFBM — Service du Budget (en lien avec la DPD/CSPP)                  |
| Étape suivante                | Extraction des fonctionnalités → critères d'acceptation → cas de test |

**Sommaire**

1\. Contexte et objet du module

2\. Objectifs du module

3\. Périmètre

4\. Acteurs et profils utilisateurs

5\. Processus métier couverts

6\. Besoins fonctionnels

7\. Règles de gestion

8\. Données et documents gérés

9\. Cycles de vie et statuts

10\. Paramètres et seuils de gestion

11\. Interfaces avec les autres modules

12\. Exigences non fonctionnelles spécifiques

13\. États, tableaux de bord et indicateurs

14\. Points ouverts et incohérences relevées

15\. Validation du document

# 1. Contexte et objet du module

La SODEPA s'arrime à la politique gouvernementale de budgétisation par programmes (budget-programme) et non par moyens. Le budget est construit autour de trois principes : la vision (ce que l'on veut faire), l'ancrage et la déclinaison (la source et la manière de le faire), la codification et le suivi (comment cela doit se faire et a été fait).

La préparation du budget prend la forme d'un Projet de Performance Annuel (PPA) qui présente les réalisations de l'année écoulée (Rapport de Performance Annuel), la revue des projets d'investissement et, pour chaque programme, la stratégie, les objectifs de performance sur trois ans, les activités, les indicateurs, les cibles et les moyens. Le budget couvre l'exercice du 1er janvier au 31 décembre.

Le présent cahier des charges décrit le module ERP « Gestion budgétaire », qui doit couvrir les trois grands volets de la procédure : l'élaboration du budget, son exécution (engagement des recettes et des dépenses) et son contrôle et suivi, y compris la réallocation des crédits. Le module est la clé de voûte du contrôle des dépenses : aucune dépense ne peut être engagée sans crédit disponible.

# 2. Objectifs du module

- Élaborer un PPA budgétisé réaliste, précis et concis, décliné du plan stratégique quinquennal.
- Outiller le cycle de préparation : lettre-circulaire, groupe de travail, CDMT, avant-projets par unité, consolidation, arbitrage, approbation par le Conseil d'Administration.
- Gérer la nomenclature budgétaire (programmes, actions, activités, tâches, lignes) et le plan de codification budgétaire.
- Contrôler a priori chaque engagement de dépense : inscription au budget, imputation correcte, disponibilité des crédits, conformité des pièces.
- Distinguer et outiller les circuits de menues dépenses et de grosses dépenses, au siège et dans les unités opérationnelles.
- Suivre en temps réel l'exécution budgétaire par programme, par unité, par ligne et par nature.
- Encadrer les réallocations de crédits selon leur niveau, avec traçabilité décisionnelle complète.
- Produire automatiquement les rapports mensuels et trimestriels d'exécution budgétaire.

# 3. Périmètre

## 3.1 Inclus dans le périmètre

- Préparation du budget : lettre-circulaire, chronogramme, décision de création du groupe de travail, réunions de concertation.
- Élaboration du PPA et du CDMT par programme ; consolidation par le CSPP.
- Budgétisation des tâches par programme et des avant-projets des unités opérationnelles (fonctionnement, produits, activités, investissements).
- Arbitrage, validation interne, approbation par le CA, décision exécutoire du DG, diffusion du budget.
- Plan de codification budgétaire.
- Engagement des recettes (suivi mensuel des recettes par nature).
- Engagement des dépenses : bon d'engagement, visa budgétaire, imputation, mise à jour de la fiche de suivi, cas pratiques (missions, entretien de véhicules, études/consultants).
- Contrôle a priori et a posteriori ; rapports mensuels et trimestriels.
- Réallocation des lignes budgétaires (simple, significative, exceptionnelle).

## 3.2 Hors périmètre / traité par un autre module

- Passation des commandes et des marchés, réception des biens (module 04 Marchés et Patrimoine).
- Paiement effectif des dépenses (module 03 Finance et Trésorerie).
- Comptabilisation générale des opérations (module 05 Comptabilité).
- Calcul de la paie (module 06 Solde et Pensions) ; seul l'engagement de la masse salariale est traité ici.

## 3.3 Documents et textes de référence

- Procédures budgétaires SODEPA (document source).
- Circulaire présidentielle annuelle relative à la préparation du budget de l'État.
- Loi n° 2018/012 du 11 juillet 2018 portant régime financier de l'État et des autres entités publiques.
- Plan stratégique quinquennal de la SODEPA.
- Référentiel ERP SODEPA — REF-04 (référentiel budgétaire) et REF-07 (workflows).

# 4. Acteurs et profils utilisateurs

Les acteurs ci-dessous sont repris du document source. Dans l'ERP, ils correspondent à des profils fonctionnels paramétrables (module Référentiel – IAM) et non à des postes organiques figés.

| **Sigle**             | **Désignation**                                              | **Rôle dans le module**                                                                                                                              |
| --------------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| DG                    | Directeur Général                                            | Signe la lettre-circulaire, les décisions, les bons d'engagement et de commande ; arbitre les réallocations ; valide le projet de budget en interne. |
| CA                    | Conseil d'Administration                                     | Unique instance d'approbation du PPA et du budget ; approuve les réallocations significatives.                                                       |
| DAF                   | Directeur Administratif et Financier                         | Programme et coordonne la confection du budget ; propose les réallocations ; signe par voie hiérarchique.                                            |
| SDFBM                 | Sous-Directeur Financier, du Budget et du Matériel           | Supervise le Service du Budget ; analyse les demandes de réallocation.                                                                               |
| CSB / SB              | Chef Service du Budget / Service du Budget                   | Centralise et consolide les budgets ; établit les bons d'engagement ; appose le visa budgétaire ; tient la fiche de suivi ; produit les rapports.    |
| DPD                   | Chef de la Division de la Prospective et du Développement    | Organise la concertation sur le PPA ; prépare la lettre-circulaire et la décision du groupe de travail avec le CSPP.                                 |
| CSPP                  | Cellule des Statistiques, de la Programmation et des Projets | Prépare la lettre-circulaire ; consolide les CDMT et le PPA.                                                                                         |
| CEA                   | Cellule d'appui à la consolidation (sigle à confirmer)       | Consolide les CDMT et le PPA avec le CSPP.                                                                                                           |
| DCAI                  | Division du Contrôle et de l'Audit Interne                   | Présente le rapport d'évaluation de l'exécution ; membre du comité de réallocation.                                                                  |
| ASCG                  | Attaché chargé du contrôle de gestion (sigle à confirmer)    | Contribue au rapport d'évaluation technique et financière.                                                                                           |
| Chef de programme     | Responsable d'un programme budgétaire                        | Révise le CDMT, élabore le PPA de son programme, décline et budgétise les tâches.                                                                    |
| DUO                   | Directeur d'Unité Opérationnelle                             | Valide les expressions de besoin, attribue les commandes et signe les bons d'engagement de son unité.                                                |
| CSAFC                 | Chef Service Administratif, Financier et Comptable (unité)   | Établit les bons d'engagement en unité, impute et met à jour la fiche de suivi.                                                                      |
| GT                    | Groupe de travail budgétaire                                 | Valide les CDMT et l'avant-projet de PPA ; consolide et valide les avant-projets des unités.                                                         |
| Secrétariat DG / CSCo | Secrétariat du DG / Chef Service du Courrier                 | Formalise et ventile les documents signés.                                                                                                           |
| Demandeur             | Service ou agent demandeur                                   | Exprime le besoin ; initie les TdR et les demandes de réallocation.                                                                                  |

# 5. Processus métier couverts

Chaque processus est décrit tel qu'il ressort de la procédure SODEPA, puis traduit en exigences pour l'ERP. Les délais indiqués sont les délais cibles de la procédure ; ils deviennent des délais de traitement (SLA) suivis par le moteur de workflow.

## 5.1 P1 — Préparation et lancement du cycle budgétaire

Le cycle démarre après la publication de la circulaire du Président de la République. La DPD/CSPP prépare la lettre-circulaire du DG portant orientations générales et chronogramme ; un groupe de travail est institué par décision du DG puis réuni en séance de concertation.

**Déclencheur :** Publication de la circulaire présidentielle relative à la préparation du budget.

| **N°** | **Intervenant**       | **Action**                                                               | **Document / interface**                             | **Délai**                |
| ------ | --------------------- | ------------------------------------------------------------------------ | ---------------------------------------------------- | ------------------------ |
| 1      | DPD/CSPP              | Prépare la lettre-circulaire (orientations générales) et le chronogramme | Projet de lettre-circulaire + chronogramme           | 15 j après la circulaire |
| 2      | DG                    | Signe la lettre-circulaire et valide le chronogramme                     | Lettre-circulaire signée                             | 2 j                      |
| 3      | Secrétariat DG / CSCo | Formalise et ventile la lettre-circulaire aux services                   | Lettre-circulaire diffusée                           | 1 j                      |
| 4      | DPD/CSPP              | Prépare le projet de décision de mise en place du groupe de travail      | Projet de décision                                   | 2 j                      |
| 5      | DG                    | Signe la décision portant organisation du groupe de travail              | Décision signée et notifiée                          | 2 j                      |
| 6      | DPD                   | Organise la réunion de concertation sur l'élaboration du PPA             | Convocation, projet de chronogramme                  | Ouvert                   |
| 7      | DG                    | Préside la réunion, précise les orientations, valide le chronogramme     | Fiche de présence, compte rendu, chronogramme validé | 1 j                      |

**Résultat attendu :** Exercice budgétaire N+1 ouvert en préparation, orientations et calendrier publiés, groupe de travail constitué.

## 5.2 P2 — Élaboration du Projet de Performance Annuel (PPA) et du CDMT

Le groupe de travail s'appuie sur l'évaluation de l'exécution en cours pour réviser le Cadre de Dépenses à Moyen Terme (CDMT) de chaque programme, puis élaborer et consolider le PPA.

**Déclencheur :** Chronogramme validé.

| **N°** | **Intervenant**              | **Action**                                                                          | **Document / interface**    | **Délai** |
| ------ | ---------------------------- | ----------------------------------------------------------------------------------- | --------------------------- | --------- |
| 1      | DCAI, CSPP, ASCG, CSB        | Présentent le rapport d'évaluation de l'exécution technique et financière du budget | Rapport d'évaluation du PPA | 5 j       |
| 2      | Chefs de programme           | Révisent et présentent le CDMT par programme                                        | CDMT validé par le GT       | 5 j       |
| 3      | CSPP/CEA                     | Consolident les CDMT des programmes                                                 | Projet de CDMT consolidé    | 5 j       |
| 4      | Chefs de programme           | Élaborent le projet de performance annuel par programme                             | Projet de PPA par programme | 5 j       |
| 5      | CSPP/CEA + secrétariat du GT | Consolident le projet de performance                                                | PPA consolidé               | 3 j       |
| 6      | Groupe de travail            | Valide l'avant-projet de PPA                                                        | Avant-projet de PPA validé  | 1 j       |

**Résultat attendu :** Avant-projet de PPA validé, avec indicateurs de résultats mesurables et cibles.

## 5.3 P3 — Budgétisation, consolidation et approbation

Les activités du CDMT sont déclinées en tâches budgétisées. Chaque unité opérationnelle prépare son budget des charges de fonctionnement, des produits, des activités et des investissements. Le Service du Budget et le groupe de travail consolident ; le DG valide en interne, le CA approuve, le DG rend le budget exécutoire.

**Déclencheur :** Avant-projet de PPA validé.

| **N°** | **Intervenant**                | **Action**                                                             | **Document / interface**      | **Délai**             |
| ------ | ------------------------------ | ---------------------------------------------------------------------- | ----------------------------- | --------------------- |
| 1      | Chefs de programme             | Déclinent les activités de l'année en tâches avec indicateurs précis   | Projet de PPA                 | 5 j                   |
| 2      | Chefs de programme             | Budgétisent les tâches déclinées par programme                         | Projet de PPA budgétisé       | 5 j                   |
| 3      | Chefs d'unités opérationnelles | Transmettent les avant-projets de budget et les éléments de maturation | Avant-projets électroniques   | 2 j                   |
| 4      | Groupe de travail              | Réceptionne, consolide et retourne aux chefs de programme              | Avant-projets consolidés      | 3 j                   |
| 5      | Groupe de travail              | Valide et intègre les avant-projets dans le projet de budget           | Avant-projet de budget validé | 3 j                   |
| 6      | Chefs de programme             | Procèdent aux débats budgétaires et consolident le projet de budget    | Projet de budget              | 3 j                   |
| 7      | DG                             | Reçoit et valide le projet de budget en interne                        | Projet de budget validé       | 1 j                   |
| 8      | CA                             | Examine et approuve le PPA et le budget                                | Résolution signée             | 1 j                   |
| 9      | DG                             | Signe la décision rendant le budget exécutoire                         | Décision signée               | 2 j après le CA       |
| 10     | DG / Secrétariat               | Transmet le budget aux chefs de programme et au SB                     | Budget diffusé                | 1 j après la décision |

**Résultat attendu :** Budget approuvé, exécutoire, chargé dans l'ERP comme version de référence et ouvert à l'engagement.

## 5.4 P4 — Plan de codification budgétaire

Après approbation, le Service du Budget prépare le plan de codification budgétaire (codes programmes, actions, activités, tâches, lignes), validé et signé par le DG. Ce plan alimente le référentiel budgétaire de l'ERP.

**Déclencheur :** Budget rendu exécutoire.

| **N°** | **Intervenant**   | **Action**                                                 | **Document / interface**       | **Délai** |
| ------ | ----------------- | ---------------------------------------------------------- | ------------------------------ | --------- |
| 1      | CSB/SDFBM/DPD/DAF | Préparent le plan de codification et le transmettent au DG | Projet de plan de codification | 10 j      |
| 2      | DG                | Signe le plan de codification                              | Plan de codification signé     | 2 j       |

**Résultat attendu :** Nomenclature budgétaire de l'exercice publiée et active dans le référentiel.

## 5.5 P5 — Engagement des dépenses — menues dépenses (< 500 000 FCFA TTC)

Pour les dépenses de 0 à 499 999 FCFA TTC, un bon d'engagement est établi avec imputation budgétaire. Au siège, le dossier est traité par le SB et signé par voie hiérarchique ; en unité, l'expression de besoin est validée par le DUO et le CSAFC établit le bon d'engagement.

**Déclencheur :** Liasse de dépense reçue (siège) ou expression de besoin du demandeur (unité).

| **N°** | **Intervenant**         | **Action**                                                                          | **Document / interface**                             | **Délai** |
| ------ | ----------------------- | ----------------------------------------------------------------------------------- | ---------------------------------------------------- | --------- |
| 1      | Demandeur / CSAFC (UO)  | Exprime le besoin, joint l'attestation de carence du magasinier                     | Fiche d'expression de besoin, attestation de carence | 1 j       |
| 2      | DUO (UO)                | Valide et signe l'expression de besoin                                              | Expression de besoin signée, pro forma / devis       | 1 j       |
| 3      | SB (siège) / CSAFC (UO) | Établit le bon d'engagement, impute, met à jour la fiche de suivi                   | Bon d'engagement + liasse                            | 1 j       |
| 4      | SB/SDFBM/DAF/DG ou DUO  | Signe le bon d'engagement                                                           | Bon d'engagement signé                               | 1 à 2 j   |
| 5      | SB                      | Archive une copie et transmet les originaux au Service des Marchés et du Patrimoine | Bon d'engagement signé + liasse                      | 1 j       |

**Résultat attendu :** Crédit engagé, disponible budgétaire diminué, dossier transmis pour exécution.

## 5.6 P6 — Engagement des dépenses — grosses dépenses (≥ 500 000 FCFA TTC)

Pour les dépenses supérieures ou égales à 500 000 FCFA TTC, un bon d'engagement et un bon de commande sont établis sur la base de la facture pro forma retenue (trois fournisseurs consultés en unité), du dossier fiscal du fournisseur et de l'attestation de carence.

**Déclencheur :** Facture pro forma retenue pour engagement.

| **N°** | **Intervenant**        | **Action**                                                                          | **Document / interface**                          | **Délai** |
| ------ | ---------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------- | --------- |
| 1      | Demandeur/CSAFC        | Exprime le besoin ; consultation de 3 fournisseurs                                  | Expression de besoin, attestation de carence      | 1 j       |
| 2      | DUO (UO)               | Attribue la commande à la meilleure offre financière                                | Pro forma, dossier fiscal                         | 1 j       |
| 3      | SB / CSAFC             | Établit bon d'engagement et bon de commande ; impute ; met à jour la fiche de suivi | BE + BC + liasse (TdR, pro forma, dossier fiscal) | 1 j       |
| 4      | SB/SDFBM/DAF/DG ou DUO | Signe le bon d'engagement et le bon de commande                                     | BE et BC signés                                   | 1 à 2 j   |
| 5      | SB / CSAFC             | Archive ; transmet les originaux au SMP et l'original du BC au prestataire          | BC signé remis au prestataire                     | 1 j       |

**Résultat attendu :** Engagement juridique et budgétaire constitué, commande notifiée au prestataire.

## 5.7 P7 — Cas pratiques d'engagement : missions, entretien de véhicules, études

La procédure détaille trois cas types. Missions : le SB reçoit les TdR validés, la note de service et les ordres de mission, appose le visa budgétaire et établit le bon d'engagement transmis au SFR. Entretien/réparation de véhicule (commercial via le logisticien, de service via le comptable-matières) : diagnostic par la Brigade de Maintenance, visa budgétaire, autorisation du DG, bon d'engagement et, si le montant TTC dépasse 499 999 FCFA, bon de commande. Études/consultants : TdR élaborés par le service initiateur, vérification de la ligne et visa budgétaire, autorisation du DG, bon d'engagement et éventuellement bon de commande, TdR transmis au CSFR.

**Déclencheur :** TdR validés, fiche d'entretien ou besoin d'étude.

| **N°** | **Intervenant**                                       | **Action**                                                            | **Document / interface**          | **Délai**  |
| ------ | ----------------------------------------------------- | --------------------------------------------------------------------- | --------------------------------- | ---------- |
| 1      | Service initiateur / logisticien / comptable-matières | Constitue le dossier (TdR, fiche d'entretien, pro forma)              | TdR, fiche d'entretien, pro forma | Non défini |
| 2      | SB                                                    | Vérifie la disponibilité de la ligne et appose le visa budgétaire     | Dossier visé                      | 1 j        |
| 3      | DG/DAF/SDFBM                                          | Autorise la dépense                                                   | Dossier autorisé                  | 2 j        |
| 4      | SB                                                    | Établit le bon d'engagement (+ bon de commande si > 499 999 FCFA TTC) | BE / BC                           | 1 à 2 j    |
| 5      | DG                                                    | Signe BE et BC                                                        | BE / BC signés                    | 2 j        |
| 6      | SB / CSB                                              | Archive, transmet au SMP / au prestataire / au SFR selon le cas       | Dossier transmis                  | 1 j        |

**Résultat attendu :** Engagement réalisé selon le circuit propre au type de dépense.

## 5.8 P8 — Engagement des recettes et suivi mensuel

Les recettes sont suivies par nature, comptabilisées par les CSAF des unités, et rapprochées des documents financiers et comptables. Le fichier de suivi mensuel des recettes est mis à jour.

**Déclencheur :** Encaissements de la période (module Finance).

| **N°** | **Intervenant** | **Action**                                                  | **Document / interface**      | **Délai** |
| ------ | --------------- | ----------------------------------------------------------- | ----------------------------- | --------- |
| 1      | CSAFC           | Met à jour les outils de suivi des recettes par nature      | Fiche de suivi des recettes   | Mensuel   |
| 2      | SB              | Compare les documents budgétaires, financiers et comptables | État de rapprochement         | Mensuel   |
| 3      | SB              | Met à jour le fichier de suivi mensuel des recettes         | Budget des produits consolidé | Mensuel   |

**Résultat attendu :** Taux de réalisation des recettes connu par nature, par unité et par mois.

## 5.9 P9 — Contrôle a priori et a posteriori, reporting

Le contrôle a priori vérifie la régularité des pièces ayant déclenché la dépense et des pièces justificatives. Le contrôle a posteriori met à jour quotidiennement la situation d'exécution, exploite les rapports d'activité des unités et produit le rapport mensuel (le 10 du mois pour la DG, le 05 pour les unités) et le rapport trimestriel de performance.

**Déclencheur :** Chaque engagement (a priori) ; échéances quotidiennes, mensuelles et trimestrielles (a posteriori).

| **N°** | **Intervenant** | **Action**                                                                                                       | **Document / interface**       | **Délai**               |
| ------ | --------------- | ---------------------------------------------------------------------------------------------------------------- | ------------------------------ | ----------------------- |
| 1      | SB              | Vérifie la régularité de l'expression de besoin/TdR, contrats, BE, attestation de carence, BC, cachets           | Liste de contrôle a priori     | Non défini              |
| 2      | SB              | Vérifie pro forma, facture définitive, bordereau de livraison, attestation de non-exclusion ARMP, dossier fiscal | Liste de contrôle a priori     | Non défini              |
| 3      | SB              | Met à jour quotidiennement la situation d'exécution par programme et par unité                                   | Fiche de suivi des engagements | Quotidien               |
| 4      | SB              | Produit le rapport mensuel d'exécution (dépassements, économies, justifications)                                 | Rapport d'activités mensuel    | Le 05 (UO) / le 10 (DG) |
| 5      | SB              | Produit le rapport trimestriel de performance                                                                    | Rapport trimestriel            | Trimestriel             |

**Résultat attendu :** Situation d'exécution fiable et à jour, écarts expliqués.

## 5.10 P10 — Réallocation des lignes budgétaires

La réallocation déplace des crédits disponibles d'une ligne, activité, unité ou projet vers une autre ligne prioritaire, sans compromettre l'équilibre global ni les engagements pris. Trois types : simple (virement interne), significative, exceptionnelle (urgence). Le niveau d'autorité dépend du type et des seuils.

**Déclencheur :** Fiche de demande de réallocation émise par une direction, une unité ou un service.

| **N°** | **Intervenant**        | **Action**                                                                                                                       | **Document / interface**         | **Délai** |
| ------ | ---------------------- | -------------------------------------------------------------------------------------------------------------------------------- | -------------------------------- | --------- |
| 1      | Demandeur              | Adresse une fiche de demande (objet, lignes source et cible, montant, justification, urgence, impact, risques, pièces)           | Fiche de demande de réallocation | —         |
| 2      | DAF/SDFBM/SB           | Analyse : disponibilité, absence d'engagement sur la ligne source, conformité, impact trésorerie, plan de passation, performance | Note technique et financière     | —         |
| 3      | Comité de réallocation | Donne un avis sur l'opportunité, la régularité, la soutenabilité, la priorité, la source interne                                 | Avis du comité                   | —         |
| 4      | DG                     | Approuve, rejette, demande des compléments, saisit le CA ou la tutelle                                                           | Décision interne de réallocation | —         |
| 5      | CA (si requis)         | Approuve les réallocations significatives                                                                                        | Résolution du CA                 | —         |
| 6      | SB                     | Met à jour les crédits (tableau budgétaire rectifié) et trace l'opération                                                        | Tableau de réallocation          | —         |

**Résultat attendu :** Crédits modifiés avec trace complète : demande, analyse, décision, mise à jour budgétaire, imputation, rapport.

# 6. Besoins fonctionnels

Les besoins sont identifiés « BF-BUD-nnn ». Priorité : **Haute** = indispensable au démarrage, **Moyenne** = attendue en lot 1 si possible, **Basse** = évolution ultérieure. Ces besoins serviront de base à l'extraction des fonctionnalités (étape 2).

## 6.1 Référentiel et structure budgétaire

| **ID**     | **Besoin fonctionnel**                                                                                                                     | **Priorité** |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------ |
| BF-BUD-001 | Gérer les exercices budgétaires (ouverture, préparation, exécution, clôture) du 1er janvier au 31 décembre.                                | Haute        |
| BF-BUD-002 | Gérer la nomenclature programme → action → activité → tâche → ligne budgétaire, avec rattachement aux unités et centres de responsabilité. | Haute        |
| BF-BUD-003 | Importer et versionner le plan de codification budgétaire signé par le DG.                                                                 | Haute        |
| BF-BUD-004 | Gérer plusieurs versions budgétaires par exercice (avant-projet, projet, voté, rectifié) et comparer les versions.                         | Haute        |
| BF-BUD-005 | Distinguer les natures : charges de fonctionnement, produits, activités, investissements.                                                  | Haute        |
| BF-BUD-006 | Rattacher chaque ligne à une source de financement (ressources propres, subvention de l'État, partenaire).                                 | Moyenne      |

## 6.2 Préparation et élaboration du budget

| **ID**     | **Besoin fonctionnel**                                                                                                                                                      | **Priorité** |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ |
| BF-BUD-007 | Créer la campagne budgétaire et générer la lettre-circulaire à partir d'un modèle, avec chronogramme et jalons.                                                             | Haute        |
| BF-BUD-008 | Enregistrer la décision de création du groupe de travail et ses membres ; gérer convocations, présences et comptes rendus.                                                  | Moyenne      |
| BF-BUD-009 | Permettre la saisie et la révision du CDMT par programme sur trois ans avec indicateurs, cibles et coûts.                                                                   | Haute        |
| BF-BUD-010 | Permettre à chaque chef de programme de saisir son PPA (stratégie, objectifs, activités, indicateurs, cibles, moyens).                                                      | Haute        |
| BF-BUD-011 | Permettre à chaque unité opérationnelle de saisir ses avant-projets (fonctionnement, produits, activités, investissements) et d'y joindre les éléments de maturation (TdR). | Haute        |
| BF-BUD-012 | Consolider automatiquement CDMT, PPA et avant-projets au niveau SODEPA.                                                                                                     | Haute        |
| BF-BUD-013 | Pré-remplir les propositions à partir de l'historique des ventes, de l'exécution N-1 et des activités à reconduire.                                                         | Moyenne      |
| BF-BUD-014 | Gérer les arbitrages (débats budgétaires) avec historique des montants proposés, arbitrés et retenus.                                                                       | Haute        |
| BF-BUD-015 | Faire suivre au projet de budget le circuit : GT → DG (validation interne) → CA (approbation) → décision exécutoire.                                                        | Haute        |
| BF-BUD-016 | Verrouiller le budget voté et le rendre seul ouvert à l'engagement après la décision exécutoire.                                                                            | Haute        |
| BF-BUD-017 | Produire les annexes : budget d'exploitation, budget d'investissement, plan d'opérations en capital, plan de trésorerie, plan de financement.                               | Moyenne      |

## 6.3 Engagement des dépenses

| **ID**     | **Besoin fonctionnel**                                                                                                                                       | **Priorité** |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------ |
| BF-BUD-018 | Créer un bon d'engagement à partir d'une expression de besoin, d'un TdR, d'un ordre de mission, d'une fiche d'entretien, d'un état de paie ou d'un contrat.  | Haute        |
| BF-BUD-019 | Contrôler automatiquement à la saisie l'inscription de la dépense au budget voté, l'exactitude de l'imputation et la disponibilité du crédit.                | Haute        |
| BF-BUD-020 | Bloquer tout engagement dont le montant dépasse le disponible de la ligne.                                                                                   | Haute        |
| BF-BUD-021 | Déterminer automatiquement le circuit selon le montant TTC : menue dépense (< 500 000 FCFA) ou grosse dépense (≥ 500 000 FCFA, bon de commande obligatoire). | Haute        |
| BF-BUD-022 | Différencier les circuits Direction Générale et Unités opérationnelles (signataires DG ou DUO).                                                              | Haute        |
| BF-BUD-023 | Appliquer un visa budgétaire électronique et tracer le signataire, la date et le montant visé.                                                               | Haute        |
| BF-BUD-024 | Exiger les pièces obligatoires selon le type de dépense (expression de besoin, attestation de carence, pro forma, dossier fiscal, TdR).                      | Haute        |
| BF-BUD-025 | Générer le bon de commande lié au bon d'engagement pour les grosses dépenses et le transmettre au module Marchés.                                            | Haute        |
| BF-BUD-026 | Gérer les engagements non liés aux achats : dépenses de personnel, TdR d'activités, contrats, baux, conventions, indemnités des administrateurs.             | Haute        |
| BF-BUD-027 | Gérer les cas types missions, entretien de véhicules (commerciaux et de service) et études/consultants avec leurs circuits propres.                          | Haute        |
| BF-BUD-028 | Annuler ou réduire un engagement avec motif, et restituer le crédit correspondant.                                                                           | Haute        |
| BF-BUD-029 | Numéroter automatiquement et de façon unique les bons d'engagement par exercice et par unité.                                                                | Haute        |

## 6.4 Exécution des recettes

| **ID**     | **Besoin fonctionnel**                                                                                | **Priorité** |
| ---------- | ----------------------------------------------------------------------------------------------------- | ------------ |
| BF-BUD-030 | Suivre les recettes par nature (ventes de produits, ventes de services, subventions) et par unité.    | Haute        |
| BF-BUD-031 | Rapprocher les recettes budgétaires, les encaissements (module Finance) et la comptabilité.           | Haute        |
| BF-BUD-032 | Produire la fiche de suivi mensuel des recettes et le taux de réalisation par rapport aux prévisions. | Haute        |

## 6.5 Contrôle et suivi

| **ID**     | **Besoin fonctionnel**                                                                                                        | **Priorité** |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------- | ------------ |
| BF-BUD-033 | Fournir une liste de contrôle a priori paramétrable par type de dépense, à valider avant signature.                           | Haute        |
| BF-BUD-034 | Mettre à jour en temps réel la situation d'exécution : crédit voté, engagé, liquidé, payé, disponible.                        | Haute        |
| BF-BUD-035 | Consulter l'exécution par programme, action, activité, ligne, unité, nature et source de financement.                         | Haute        |
| BF-BUD-036 | Générer automatiquement le rapport mensuel d'exécution (dépassements, économies, niveau d'engagement en valeur et en nombre). | Haute        |
| BF-BUD-037 | Permettre la saisie des explications et justificatifs des dépassements et des économies.                                      | Haute        |
| BF-BUD-038 | Générer le rapport trimestriel de performance (exécution financière et indicateurs).                                          | Haute        |
| BF-BUD-039 | Alerter le gestionnaire lorsqu'une ligne atteint un seuil de consommation paramétrable (ex. 80 %, 100 %).                     | Moyenne      |

## 6.6 Réallocation des crédits

| **ID**     | **Besoin fonctionnel**                                                                                                              | **Priorité** |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------- | ------------ |
| BF-BUD-040 | Saisir une fiche de demande de réallocation avec toutes les rubriques exigées et les pièces justificatives.                         | Haute        |
| BF-BUD-041 | Classer automatiquement la demande (simple, significative, exceptionnelle) selon les lignes, unités, natures et montants concernés. | Haute        |
| BF-BUD-042 | Contrôler la disponibilité du crédit à prélever et l'absence d'engagement sur la ligne source.                                      | Haute        |
| BF-BUD-043 | Router la demande vers l'autorité compétente (DAF+DG, comité, CA, tutelle) selon le niveau.                                         | Haute        |
| BF-BUD-044 | Enregistrer l'avis du comité de réallocation et la décision du DG ; générer la décision interne de réallocation à partir du modèle. | Haute        |
| BF-BUD-045 | Gérer la mesure conservatoire du DG en cas d'urgence, avec obligation de régularisation devant le CA.                               | Haute        |
| BF-BUD-046 | Mettre à jour automatiquement les crédits des lignes source et cible après décision et produire le tableau budgétaire rectifié.     | Haute        |
| BF-BUD-047 | Produire un état consolidé des réallocations de l'exercice.                                                                         | Haute        |

# 7. Règles de gestion

Les règles sont identifiées « RG-BUD-nnn ». La colonne « Source » renvoie au document SODEPA ou au texte d'où la règle est tirée ; « Proposition » signale une règle ajoutée pour rendre la procédure exploitable dans l'ERP.

| **ID**     | **Règle de gestion**                                                                                                                                                                                                                    | **Source**                 |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------- |
| RG-BUD-001 | Le budget couvre la période du 1er janvier au 31 décembre de l'année N.                                                                                                                                                                 | Procédure, ch. 1, §4.1     |
| RG-BUD-002 | Le Conseil d'Administration est l'unique instance d'approbation du budget ; le budget n'est exécutoire qu'après décision signée du DG.                                                                                                  | Ch. 1, §4.1 c              |
| RG-BUD-003 | Tout acte de gestion ayant une incidence financière sur le budget doit être engagé avant paiement.                                                                                                                                      | Ch. 2, §2.3                |
| RG-BUD-004 | Aucune dépense ne peut être engagée sans crédit disponible (régularité budgétaire).                                                                                                                                                     | Ch. 3, §3 a                |
| RG-BUD-005 | Avant engagement, le SB vérifie : inscription au budget approuvé, exactitude de l'imputation, conformité et régularité des pièces.                                                                                                      | Ch. 2, §2.1                |
| RG-BUD-006 | Une dépense de 0 à 499 999 FCFA TTC est une menue dépense : bon d'engagement seul.                                                                                                                                                      | Ch. 2, §2.3 a              |
| RG-BUD-007 | Une dépense ≥ 500 000 FCFA TTC est une grosse dépense : bon d'engagement et bon de commande.                                                                                                                                            | Ch. 2, §2.3 a              |
| RG-BUD-008 | En unité, pour une grosse dépense, trois fournisseurs sont consultés et la commande est attribuée à la meilleure offre financière.                                                                                                      | Ch. 2, UO grosses dépenses |
| RG-BUD-009 | Une attestation de carence du stock (magasinier/CSAFC ou SMP) accompagne toute demande d'acquisition.                                                                                                                                   | Ch. 2, circuits            |
| RG-BUD-010 | Le dossier fiscal du prestataire (patente, carte de contribuable, RCCM) est exigé pour les grosses dépenses.                                                                                                                            | Ch. 2, grosses dépenses    |
| RG-BUD-011 | L'engagement couvre aussi les dépenses de personnel, les TdR (montant global de l'activité) et les dépenses diverses (contrats, baux, conventions, indemnités de session).                                                              | Ch. 2, §2.2                |
| RG-BUD-012 | Le SB conserve une copie de chaque bon d'engagement signé et transmet les originaux au SMP.                                                                                                                                             | Ch. 2, circuits            |
| RG-BUD-013 | Le rapport mensuel d'exécution est produit au plus tard le 05 du mois pour les unités et le 10 du mois pour la Direction Générale.                                                                                                      | Ch. 3, §2                  |
| RG-BUD-014 | Une réallocation ne doit ni créer de tension de trésorerie, ni prélever sur une ligne déjà engagée, ni masquer une insuffisance structurelle.                                                                                           | Ch. 3, §3                  |
| RG-BUD-015 | Réallocation simple (même direction, même nature, sans modification du global ni du plan de passation) : proposée par la DAF, autorisée par le DG.                                                                                      | Ch. 3, §3 b.1              |
| RG-BUD-016 | Réallocation entre fonctionnement et investissement, entre unités, entre projets stratégiques, ou affectant subventions, emprunts, trésorerie ou budget voté : note motivée au DG et résolution du CA si l'incidence est significative. | Ch. 3, §3 b.2              |
| RG-BUD-017 | En urgence (panne grave, urgence sanitaire/vétérinaire, continuité de service, obligation réglementaire), le DG peut prendre une mesure conservatoire, régularisée devant le CA si elle dépasse son seuil de compétence.                | Ch. 3, §3 b.3              |
| RG-BUD-018 | Seuils recommandés : ≤ 5 % de la ligne → DAF + DG ; > 5 % ou plusieurs directions → DAF + comité + DG ; investissement, endettement, subventions, PPA ou équilibre général → CA.                                                        | Ch. 3, §3 g                |
| RG-BUD-019 | Toute réallocation laisse une trace : demande, analyse, décision, mise à jour budgétaire, imputation comptable, rapport d'exécution.                                                                                                    | Ch. 3, précautions         |
| RG-BUD-020 | Aucune dépense n'est exécutée avant validation de la réallocation qui la finance.                                                                                                                                                       | Ch. 3, précautions         |
| RG-BUD-021 | Un demandeur ne peut pas valider son propre engagement ni sa propre demande de réallocation.                                                                                                                                            | Proposition (SoD — RG REF) |
| RG-BUD-022 | Un bon d'engagement signé ne peut être modifié ; toute correction passe par une annulation tracée et un nouvel engagement.                                                                                                              | Proposition                |

# 8. Données et documents gérés

## 8.1 Objets de données principaux

| **Objet**                             | **Attributs principaux**                                                                                                                                          |
| ------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Exercice budgétaire                   | Année, dates d'ouverture/clôture, statut, version de référence                                                                                                    |
| Programme / action / activité / tâche | Code, libellé, responsable (chef de programme), indicateurs, cibles, unité, période                                                                               |
| Ligne budgétaire                      | Code (plan de codification), libellé, nature, compte comptable associé, source de financement, unité, crédit initial, rectifié, engagé, liquidé, payé, disponible |
| CDMT                                  | Programme, années N+1 à N+3, montants par activité, indicateurs                                                                                                   |
| PPA                                   | Programme, stratégie, objectifs, activités, indicateurs, cibles, moyens, statut                                                                                   |
| Avant-projet d'unité                  | Unité, nature (fonctionnement, produits, activités, investissements), lignes, montants, pièces de maturation                                                      |
| Bon d'engagement                      | N°, date, exercice, unité, ligne, montant TTC, objet, bénéficiaire, pièce d'origine, type de circuit, visa, signataires, statut                                   |
| Demande de réallocation               | N°, demandeur, lignes source/cible, montant, justification, urgence, impact, risques, type, avis, décision, statut                                                |
| Rapport d'exécution                   | Période, unité, lignes, taux d'engagement, dépassements, économies, explications                                                                                  |

## 8.2 Documents produits ou exploités

| **Document**                                                                                 | **Usage dans l'ERP**                                                   |
| -------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Lettre-circulaire et chronogramme                                                            | Générés à partir d'un modèle, signés par le DG, diffusés aux services. |
| Décision de mise en place du groupe de travail                                               | Générée et archivée dans la GED.                                       |
| CDMT, PPA, avant-projets, projet de budget                                                   | Saisis et consolidés dans l'ERP ; exportables (Excel/PDF).             |
| Résolution du CA et décision exécutoire                                                      | Jointes à la version votée du budget.                                  |
| Plan de codification budgétaire                                                              | Importé dans le référentiel budgétaire.                                |
| Fiche d'expression de besoin, attestation de carence                                         | Pièces amont de l'engagement (module Marchés).                         |
| Bon d'engagement, bon de commande                                                            | Générés par l'ERP, signés électroniquement, liés à la liasse.          |
| Fiche de suivi du budget / des engagements                                                   | Remplacée par la situation d'exécution en temps réel.                  |
| Rapports mensuel et trimestriel                                                              | Générés automatiquement.                                               |
| Fiche de demande, note technique, avis du comité, décision de réallocation, tableau rectifié | Constituent le dossier de réallocation archivé.                        |

# 9. Cycles de vie et statuts

| **Objet**               | **Statuts successifs**                                                                                      | **Commentaire**                                      |
| ----------------------- | ----------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| Exercice budgétaire     | En préparation → Voté → Exécutoire → Clôturé                                                                | Seul un exercice exécutoire accepte des engagements. |
| Version budgétaire      | Brouillon → Soumise → Arbitrée → Validée DG → Approuvée CA → Rectifiée                                      | Chaque version est conservée.                        |
| Bon d'engagement        | Brouillon → Visé SB → Signé → Transmis → Liquidé → Payé / Annulé                                            | Annulation = restitution du crédit.                  |
| Demande de réallocation | Brouillon → Soumise → Analysée → Avis comité → Décision DG → (CA) → Exécutée / Rejetée / Complément demandé | Mesure conservatoire : statut « À régulariser CA ».  |

# 10. Paramètres et seuils de gestion

Ces valeurs ne doivent pas être codées en dur : elles sont paramétrables avec date d'effet et historisation (règle RG-REF-007 du Référentiel).

| **Paramètre**                                         | **Valeur de référence**                     | **Source**          |
| ----------------------------------------------------- | ------------------------------------------- | ------------------- |
| Seuil menue / grosse dépense                          | 500 000 FCFA TTC                            | Procédure, ch. 2    |
| Nombre de pro forma exigées (grosse dépense en unité) | 3                                           | Procédure, ch. 2    |
| Échéance du rapport mensuel                           | 05 du mois (UO), 10 du mois (DG)            | Procédure, ch. 3    |
| Seuil de réallocation DAF + DG                        | ≤ 5 % du crédit de la ligne                 | Procédure, ch. 3 §g |
| Délai de préparation de la lettre-circulaire          | 15 jours après la circulaire présidentielle | Procédure, §4.2     |
| Seuils d'alerte de consommation                       | 80 % et 100 % (proposition)                 | Proposition         |

# 11. Interfaces avec les autres modules

| **Module**               | **Sens**      | **Données échangées**                                                                                                                               |
| ------------------------ | ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01 Référentiel           | Entrée        | Structures, unités, centres de coûts, programmes, projets, sources de financement, utilisateurs, workflows.                                         |
| 04 Marchés et Patrimoine | Entrée/Sortie | Expressions de besoin, attestations de carence, demandes d'acquisition (visa budgétaire), bons de commande, lettres commandes et marchés à engager. |
| 03 Finance et Trésorerie | Sortie/Entrée | Engagements liquidés à payer ; paiements effectués (statut payé) ; encaissements pour le suivi des recettes ; plan de trésorerie.                   |
| 05 Comptabilité          | Sortie/Entrée | Correspondance ligne budgétaire ↔ compte ; imputation comptable des engagements ; réalisations comptables pour rapprochement.                       |
| 06 Solde et Pensions     | Entrée        | États de paie mensuels, ordres de mission, droits de départ à engager.                                                                              |
| 08 Formation             | Entrée        | TdR de formation à engager (plan de formation).                                                                                                     |

# 12. Exigences non fonctionnelles spécifiques

| **Exigence**           | **Description**                                                                                                             |
| ---------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| Contrôle bloquant      | Le contrôle de disponibilité est effectué en temps réel et ne peut être contourné que par un profil habilité, avec trace.   |
| Traçabilité            | Toute modification de crédit, d'engagement ou de version est journalisée (auteur, date, avant/après).                       |
| Multisite              | Les unités saisissent leurs engagements en local ; la consolidation est immédiate au siège.                                 |
| Fonctionnement dégradé | En cas de coupure réseau en unité, la saisie est possible et synchronisée ensuite, sans risque de dépassement non contrôlé. |
| Performance            | Situation d'exécution consolidée affichée en moins de 5 secondes.                                                           |
| Export                 | Tous les états exportables en Excel et PDF.                                                                                 |

# 13. États, tableaux de bord et indicateurs

| **État / indicateur**               | **Description**                                                      |
| ----------------------------------- | -------------------------------------------------------------------- |
| Taux d'engagement / d'exécution     | Engagé (ou payé) / crédit voté, par programme, unité, ligne, nature. |
| Disponible budgétaire               | Crédit rectifié – engagé, en temps réel.                             |
| Taux de réalisation des recettes    | Recettes réalisées / prévisions, par nature et par unité.            |
| Dépassements et économies           | Lignes en dépassement ou en sous-consommation, avec justification.   |
| Nombre et montant des réallocations | Par type et par niveau d'autorité.                                   |
| Délai moyen d'engagement            | De la réception de la liasse à la signature du bon d'engagement.     |
| Indicateurs de performance du PPA   | Réalisé / cible par indicateur et par programme.                     |

# 14. Points ouverts et incohérences relevées

Points relevés lors de l'analyse du document source, à arbitrer par le propriétaire métier avant la validation du présent cahier des charges.

| **ID**    | **Constat**                                                                                                                                                                   | **Recommandation / décision attendue**                                                              |
| --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| PO-BUD-01 | Le seuil « grosse dépense » (≥ 500 000 FCFA) diffère des seuils du module Marchés (< 100 000, 100 000–499 999, 500 000–14 999 999, ≥ 15 000 000) et de la caisse (< 100 000). | Retenir une table de seuils unique, partagée entre Budget, Marchés et Finance, dans le Référentiel. |
| PO-BUD-02 | Les sigles CEA et ASCG ne sont pas définis dans la procédure.                                                                                                                 | Confirmer leur signification et leur rôle.                                                          |
| PO-BUD-03 | Les seuils de réallocation existent sous deux formes (tableau par type au §e et règle des 5 % au §g) et doivent être « adoptés par note de service ».                         | Arrêter la règle définitive et la référence de la note de service à paramétrer.                     |
| PO-BUD-04 | Le contrôle a priori et a posteriori n'a pas de délais définis.                                                                                                               | Fixer des délais cibles (SLA) pour le visa budgétaire.                                              |
| PO-BUD-05 | La procédure ne précise pas le traitement des engagements non soldés en fin d'exercice (reports).                                                                             | Définir la règle de report ou d'annulation des engagements en fin d'exercice.                       |
| PO-BUD-06 | L'abréviation « SBB » apparaît dans le contrôle a priori.                                                                                                                     | Vérifier s'il s'agit du Service du Budget (SB).                                                     |

# 15. Validation du document

| **Rôle**                      | **Nom** | **Date** | **Visa** |
| ----------------------------- | ------- | -------- | -------- |
| Propriétaire métier           |         |          |          |
| Chef de projet ERP            |         |          |          |
| Responsable qualité / recette |         |          |          |



Les features de la gestion du budget:
**SODEPA**

_Société de Développement et d'Exploitation des Productions Animales_

**PROJET ERP SODEPA**