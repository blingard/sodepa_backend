# CAHIER DES CHARGES FONCTIONNEL — MODULE 02 : GESTION BUDGÉTAIRE

**SODEPA — Société de Développement et d'Exploitation des Productions Animales**  
**PROJET ERP SODEPA**

| **Élément** | **Valeur** |
| :--- | :--- |
| Code du module | **BUD** |
| Document source analysé | Procédure BUDGETAIRE OK.pdf (20 pages) |
| Version | 1.0 — Projet |
| Date | 06/11/2026 |
| Statut | Validé / Référence d'Implémentation |
| Propriétaire métier pressenti | SDFBM — Service du Budget (en lien avec la DPD/CSPP) |

---

## Sommaire
1. Contexte et objet du module
2. Objectifs du module
3. Périmètre
4. Acteurs et profils utilisateurs
5. Processus métier couverts (P1 à P10)
6. Besoins fonctionnels (BF-BUD-001 à BF-BUD-047)
7. Règles de gestion (RG-BUD-001 à RG-BUD-022)
8. Données et documents gérés
9. Cycles de vie et statuts
10. Paramètres et seuils de gestion
11. Interfaces avec les autres modules
12. Exigences non fonctionnelles spécifiques
13. États, tableaux de bord et indicateurs
14. Points ouverts et arbitrages

---

## 1. Contexte et objet du module

La SODEPA s'arrime à la politique gouvernementale de budgétisation par programmes (budget-programme) et non par moyens. Le budget est construit autour de trois principes : la vision (ce que l'on veut faire), l'ancrage et la déclinaison (la source et la manière de le faire), la codification et le suivi (comment cela doit se faire et a été fait).

La préparation du budget prend la forme d'un Projet de Performance Annuel (PPA) qui présente les réalisations de l'année écoulée (Rapport de Performance Annuel), la revue des projets d'investissement et, pour chaque programme, la stratégie, les objectifs de performance sur trois ans, les activités, les indicateurs, les cibles et les moyens. Le budget couvre l'exercice du 1er janvier au 31 décembre.

Le présent cahier des charges décrit le module ERP « Gestion budgétaire », qui doit couvrir les trois grands volets de la procédure : l'élaboration du budget, son exécution (engagement des recettes et des dépenses) et son contrôle et suivi, y compris la réallocation des crédits. Le module est la clé de voûte du contrôle des dépenses : aucune dépense ne peut être engagée sans crédit disponible.

---

## 2. Objectifs du module

- Élaborer un PPA budgétisé réaliste, précis et concis, décliné du plan stratégique quinquennal.
- Outiller le cycle de préparation : lettre-circulaire, groupe de travail, CDMT, avant-projets par unité, consolidation, arbitrage, approbation par le Conseil d'Administration.
- Gérer la nomenclature budgétaire (programmes, actions, activités, tâches, lignes) et le plan de codification budgétaire.
- Contrôler a priori chaque engagement de dépense : inscription au budget, imputation correcte, disponibilité des crédits, conformité des pièces.
- Distinguer et outiller les circuits de menues dépenses et de grosses dépenses, au siège et dans les unités opérationnelles.
- Suivre en temps réel l'exécution budgétaire par programme, par unité, par ligne et par nature.
- Encadrer les réallocations de crédits selon leur niveau, avec traçabilité décisionnelle complète.
- Produire automatiquement les rapports mensuels et trimestriels d'exécution budgétaire.

---

## 3. Périmètre

### 3.1 Inclus dans le périmètre
- Préparation du budget : lettre-circulaire, chronogramme, décision de création du groupe de travail, réunions de concertation.
- Élaboration du PPA et du CDMT par programme ; consolidation par le CSPP.
- Budgétisation des tâches par programme et des avant-projets des unités opérationnelles (fonctionnement, produits, activités, investissements).
- Arbitrage, validation interne, approbation par le CA, décision exécutoire du DG, diffusion du budget.
- Plan de codification budgétaire.
- Engagement des recettes (suivi mensuel des recettes par nature).
- Engagement des dépenses : bon d'engagement, visa budgétaire, imputation, mise à jour de la fiche de suivi, cas pratiques (missions, entretien de véhicules, études/consultants).
- Contrôle a priori et a posteriori ; rapports mensuels et trimestriels.
- Réallocation des lignes budgétaires (simple, significative, exceptionnelle).

### 3.2 Hors périmètre / traité par un autre module
- Passation des commandes et des marchés, réception des biens (module 04 Marchés et Patrimoine).
- Paiement effectif des dépenses (module 03 Finance et Trésorerie).
- Comptabilisation générale des opérations (module 05 Comptabilité).
- Calcul de la paie (module 06 Solde et Pensions) ; seul l'engagement de la masse salariale est traité ici.

---

## 4. Acteurs et profils utilisateurs

| **Sigle** | **Désignation** | **Rôle dans le module** |
| :--- | :--- | :--- |
| **DG** | Directeur Général | Signe la lettre-circulaire, les décisions, les bons d'engagement et de commande ; arbitre les réallocations ; valide le projet de budget en interne. |
| **CA** | Conseil d'Administration | Unique instance d'approbation du PPA et du budget ; approuve les réallocations significatives. |
| **DAF** | Directeur Administratif et Financier | Programme et coordonne la confection du budget ; propose les réallocations ; signe par voie hiérarchique. |
| **SDFBM** | Sous-Directeur Financier, du Budget et du Matériel | Supervise le Service du Budget ; analyse les demandes de réallocation. |
| **CSB / SB** | Chef Service du Budget / Service du Budget | Centralise et consolide les budgets ; établit les bons d'engagement ; appose le visa budgétaire ; tient la fiche de suivi ; produit les rapports. |
| **DPD** | Chef Division Prospective et Développement | Organise la concertation sur le PPA ; prépare la lettre-circulaire et la décision du groupe de travail avec le CSPP. |
| **CSPP** | Cellule Statistiques, Programmation & Projets | Prépare la lettre-circulaire ; consolide les CDMT et le PPA. |
| **DCAI** | Division Contrôle & Audit Interne | Présente le rapport d'évaluation de l'exécution ; membre du comité de réallocation. |
| **Chef de programme** | Responsable d'un programme budgétaire | Révise le CDMT, élabore le PPA de son programme, décline et budgétise les tâches. |
| **DUO** | Directeur d'Unité Opérationnelle | Valide les expressions de besoin, attribue les commandes et signe les bons d'engagement de son unité. |
| **CSAFC** | Chef Service Admin, Financier & Comptable (UO) | Établit les bons d'engagement en unité, impute et met à jour la fiche de suivi. |
| **GT** | Groupe de travail budgétaire | Valide les CDMT et l'avant-projet de PPA ; consolide et valide les avant-projets des unités. |
| **Demandeur** | Service ou agent demandeur | Exprime le besoin ; initie les TdR et les demandes de réallocation. |

---

## 5. Processus métier couverts (P1 à P10)

- **P1 — Préparation et lancement du cycle budgétaire** : Publication de la lettre-circulaire, décision du groupe de travail, réunion de concertation, chronogramme.
- **P2 — Élaboration du PPA et du CDMT** : Évaluation N-1, révision CDMT triennal par programme, consolidation CSPP, avant-projet de PPA validé par le GT.
- **P3 — Budgétisation, consolidation et approbation** : Déclinaison en tâches chiffrées, avant-projets des unités opérationnelles, débats budgétaires, validation DG, approbation CA, décision exécutoire.
- **P4 — Plan de codification budgétaire** : Structure des codes programmes, actions, activités, tâches, lignes signée par le DG et importée.
- **P5 — Engagement des menues dépenses (< 500 000 FCFA TTC)** : Bon d'engagement sans bon de commande, imputation, contrôle disponibilité, visa budgétaire.
- **P6 — Engagement des grosses dépenses (≥ 500 000 FCFA TTC)** : Bon d'engagement + bon de commande obligatoire, consultation 3 fournisseurs en unité, dossier fiscal, attestation de carence.
- **P7 — Cas pratiques d'engagement** : Missions (TdR + OM), entretien de véhicules (diagnostic Brigade de Maintenance), études/consultants.
- **P8 — Engagement des recettes et suivi mensuel** : Recettes par nature (ventes produits/services, subventions) rapprochées des encaissements Finance et de la comptabilité.
- **P9 — Contrôle a priori et a posteriori, reporting** : Liste de contrôle a priori des pièces, situation d'exécution en temps réel, rapport mensuel (05 UO / 10 DG), rapport trimestriel.
- **P10 — Réallocation des crédits** : Demande motivée, analyse disponibilité ligne source, classification (simple ≤5%, significative >5% ou investissement/CA, exceptionnelle), avis comité, décision DG ou CA, tableau rectifié.

---

## 6. Besoins fonctionnels (BF-BUD-001 à BF-BUD-047)

### 6.1 Référentiel et structure budgétaire
- **BF-BUD-001** : Gérer les exercices budgétaires (ouverture, préparation, exécution, clôture) du 1er janvier au 31 décembre.
- **BF-BUD-002** : Gérer la nomenclature programme → action → activité → tâche → ligne budgétaire, avec rattachement aux unités et centres de responsabilité.
- **BF-BUD-003** : Importer et versionner le plan de codification budgétaire signé par le DG.
- **BF-BUD-004** : Gérer plusieurs versions budgétaires par exercice (avant-projet, projet, voté, rectifié) et comparer les versions.
- **BF-BUD-005** : Distinguer les natures : charges de fonctionnement, produits, activités, investissements.
- **BF-BUD-006** : Rattacher chaque ligne à une source de financement (ressources propres, subvention de l'État, partenaire).

### 6.2 Préparation et élaboration du budget
- **BF-BUD-007** : Créer la campagne budgétaire et générer la lettre-circulaire à partir d'un modèle, avec chronogramme et jalons.
- **BF-BUD-008** : Enregistrer la décision de création du groupe de travail et ses membres ; gérer convocations, présences et comptes rendus.
- **BF-BUD-009** : Permettre la saisie et la révision du CDMT par programme sur trois ans avec indicateurs, cibles et coûts.
- **BF-BUD-010** : Permettre à chaque chef de programme de saisir son PPA (stratégie, objectifs, activités, indicateurs, cibles, moyens).
- **BF-BUD-011** : Permettre à chaque unité opérationnelle de saisir ses avant-projets (fonctionnement, produits, activités, investissements) et d'y joindre les éléments de maturation (TdR).
- **BF-BUD-012** : Consolider automatiquement CDMT, PPA et avant-projets au niveau SODEPA.
- **BF-BUD-013** : Pré-remplir les propositions à partir de l'historique des ventes, de l'exécution N-1 et des activités à reconduire.
- **BF-BUD-014** : Gérer les arbitrages (débats budgétaires) avec historique des montants proposés, arbitrés et retenus.
- **BF-BUD-015** : Faire suivre au projet de budget le circuit : GT → DG (validation interne) → CA (approbation) → décision exécutoire.
- **BF-BUD-016** : Verrouiller le budget voté et le rendre seul ouvert à l'engagement après la décision exécutoire.
- **BF-BUD-017** : Produire les annexes : budget d'exploitation, budget d'investissement, plan d'opérations en capital, plan de trésorerie, plan de financement.

### 6.3 Engagement des dépenses
- **BF-BUD-018** : Créer un bon d'engagement à partir d'une expression de besoin, d'un TdR, d'un ordre de mission, d'une fiche d'entretien, d'un état de paie ou d'un contrat.
- **BF-BUD-019** : Contrôler automatiquement à la saisie l'inscription de la dépense au budget voté, l'exactitude de l'imputation et la disponibilité du crédit.
- **BF-BUD-020** : Bloquer tout engagement dont le montant dépasse le disponible de la ligne.
- **BF-BUD-021** : Déterminer automatiquement le circuit selon le montant TTC : menue dépense (< 500 000 FCFA) ou grosse dépense (≥ 500 000 FCFA, bon de commande obligatoire).
- **BF-BUD-022** : Différencier les circuits Direction Générale et Unités opérationnelles (signataires DG ou DUO).
- **BF-BUD-023** : Appliquer un visa budgétaire électronique et tracer le signataire, la date et le montant visé.
- **BF-BUD-024** : Exiger les pièces obligatoires selon le type de dépense (expression de besoin, attestation de carence, pro forma, dossier fiscal, TdR).
- **BF-BUD-025** : Générer le bon de commande lié au bon d'engagement pour les grosses dépenses et le transmettre au module Marchés.
- **BF-BUD-026** : Gérer les engagements non liés aux achats : dépenses de personnel, TdR d'activités, contrats, baux, conventions, indemnités des administrateurs.
- **BF-BUD-027** : Gérer les cas types missions, entretien de véhicules (commerciaux et de service) et études/consultants avec leurs circuits propres.
- **BF-BUD-028** : Annuler ou réduire un engagement avec motif, et restituer le crédit correspondant.
- **BF-BUD-029** : Numéroter automatiquement et de façon unique les bons d'engagement par exercice et par unité.

### 6.4 Exécution des recettes
- **BF-BUD-030** : Suivre les recettes par nature (ventes de produits, ventes de services, subventions) et par unité.
- **BF-BUD-031** : Rapprocher les recettes budgétaires, les encaissements (module Finance) et la comptabilité.
- **BF-BUD-032** : Produire la fiche de suivi mensuel des recettes et le taux de réalisation par rapport aux prévisions.

### 6.5 Contrôle et suivi
- **BF-BUD-033** : Fournir une liste de contrôle a priori paramétrable par type de dépense, à valider avant signature.
- **BF-BUD-034** : Mettre à jour en temps réel la situation d'exécution : crédit voté, engagé, liquidé, payé, disponible.
- **BF-BUD-035** : Consulter l'exécution par programme, action, activité, ligne, unité, nature et source de financement.
- **BF-BUD-036** : Générer automatiquement le rapport mensuel d'exécution (dépassements, économies, niveau d'engagement en valeur et en nombre).
- **BF-BUD-037** : Permettre la saisie des explications et justificatifs des dépassements et des économies.
- **BF-BUD-038** : Générer le rapport trimestriel de performance (exécution financière et indicateurs).
- **BF-BUD-039** : Alerter le gestionnaire lorsqu'une ligne atteint un seuil de consommation paramétrable (ex. 80 %, 100 %).

### 6.6 Réallocation des crédits
- **BF-BUD-040** : Saisir une fiche de demande de réallocation avec toutes les rubriques exigées et les pièces justificatives.
- **BF-BUD-041** : Classer automatiquement la demande (simple, significative, exceptionnelle) selon les lignes, unités, natures et montants concernés.
- **BF-BUD-042** : Contrôler la disponibilité du crédit à prélever et l'absence d'engagement sur la ligne source.
- **BF-BUD-043** : Router la demande vers l'autorité compétente (DAF+DG, comité, CA, tutelle) selon le niveau.
- **BF-BUD-044** : Enregistrer l'avis du comité de réallocation et la décision du DG ; générer la décision interne de réallocation à partir du modèle.
- **BF-BUD-045** : Gérer la mesure conservatoire du DG en cas d'urgence, avec obligation de régularisation devant le CA.
- **BF-BUD-046** : Mettre à jour automatiquement les crédits des lignes source et cible après décision et produire le tableau budgétaire rectifié.
- **BF-BUD-047** : Produire un état consolidé des réallocations de l'exercice.

---

## 7. Règles de gestion (RG-BUD-001 à RG-BUD-022)

| **ID** | **Règle de gestion** |
| :--- | :--- |
| **RG-BUD-001** | Le budget couvre la période du 1er janvier au 31 décembre de l'année N. |
| **RG-BUD-002** | Le Conseil d'Administration est l'unique instance d'approbation du budget ; le budget n'est exécutoire qu'après décision signée du DG. |
| **RG-BUD-003** | Tout acte de gestion ayant une incidence financière sur le budget doit être engagé avant paiement. |
| **RG-BUD-004** | Aucune dépense ne peut être engagée sans crédit disponible (régularité budgétaire stricte). |
| **RG-BUD-005** | Avant engagement, le SB vérifie : inscription au budget approuvé, exactitude de l'imputation, conformité et régularité des pièces. |
| **RG-BUD-006** | Une dépense de 0 à 499 999 FCFA TTC est une menue dépense : bon d'engagement seul. |
| **RG-BUD-007** | Une dépense ≥ 500 000 FCFA TTC est une grosse dépense : bon d'engagement et bon de commande obligatoires. |
| **RG-BUD-008** | En unité, pour une grosse dépense, trois fournisseurs sont consultés et la commande est attribuée à la meilleure offre financière. |
| **RG-BUD-009** | Une attestation de carence du stock (magasinier/CSAFC ou SMP) accompagne toute demande d'acquisition. |
| **RG-BUD-010** | Le dossier fiscal du prestataire (patente, carte de contribuable, RCCM) est exigé pour les grosses dépenses. |
| **RG-BUD-011** | L'engagement couvre aussi les dépenses de personnel, les TdR (montant global de l'activité) et les dépenses diverses (contrats, baux, conventions, indemnités de session). |
| **RG-BUD-012** | Le SB conserve une copie de chaque bon d'engagement signé et transmet les originaux au SMP. |
| **RG-BUD-013** | Le rapport mensuel d'exécution est produit au plus tard le 05 du mois pour les unités et le 10 du mois pour la Direction Générale. |
| **RG-BUD-014** | Une réallocation ne doit ni créer de tension de trésorerie, ni prélever sur une ligne déjà engagée, ni masquer une insuffisance structurelle. |
| **RG-BUD-015** | Réallocation simple (même direction, même nature, sans modification du global ni du plan de passation) : proposée par la DAF, autorisée par le DG. |
| **RG-BUD-016** | Réallocation entre fonctionnement et investissement, entre unités, entre projets stratégiques, ou affectant subventions, emprunts, trésorerie ou budget voté : note motivée au DG et résolution du CA si l'incidence est significative. |
| **RG-BUD-017** | En urgence (panne grave, urgence sanitaire/vétérinaire, continuité de service, obligation réglementaire), le DG peut prendre une mesure conservatoire, régularisée devant le CA si elle dépasse son seuil de compétence. |
| **RG-BUD-018** | Seuils recommandés : ≤ 5 % de la ligne → DAF + DG ; > 5 % ou plusieurs directions → DAF + comité + DG ; investissement, endettement, subventions, PPA ou équilibre général → CA. |
| **RG-BUD-019** | Toute réallocation laisse une trace : demande, analyse, décision, mise à jour budgétaire, imputation comptable, rapport d'exécution. |
| **RG-BUD-020** | Aucune dépense n'est exécutée avant validation de la réallocation qui la finance. |
| **RG-BUD-021** | Un demandeur ne peut pas valider son propre engagement ni sa propre demande de réallocation (Séparation 4-Yeux). |
| **RG-BUD-022** | Un bon d'engagement signé ne peut être modifié ; toute correction passe par une annulation tracée et un nouvel engagement. |
