# CATALOGUE DES FONCTIONNALITÉS — MODULE 02 : GESTION BUDGÉTAIRE

**SODEPA — Société de Développement et d'Exploitation des Productions Animales**  
**PROJET ERP SODEPA**

| **Élément** | **Valeur** |
| :--- | :--- |
| Code du module | **BUD** |
| Cahier des charges de référence | CdC Module 02 — Gestion budgétaire (v1.0) |
| Nombre d'epics / de fonctionnalités | 6 / 24 |
| Couverture des besoins fonctionnels | 47 / 47 BF couverts (100 %) |
| Version / date | 1.0 — Projet — 06/11/2026 |
| Statut | Validé / Référence d'Implémentation |
| Propriétaire métier | SDFBM — Service du Budget (en lien avec la DPD/CSPP) |

---

## 1. Vue d'ensemble des 6 Épics

| **ID** | **Epic** | **Nb** | **Fonctionnalités** |
| :--- | :--- | :---: | :--- |
| **EP-BUD-01** | Structure et nomenclature budgétaire | 3 | F-BUD-001, F-BUD-002, F-BUD-003 |
| **EP-BUD-02** | Préparation et élaboration du budget | 6 | F-BUD-004, F-BUD-005, F-BUD-006, F-BUD-007, F-BUD-008, F-BUD-009 |
| **EP-BUD-03** | Engagement des dépenses | 7 | F-BUD-010, F-BUD-011, F-BUD-012, F-BUD-013, F-BUD-014, F-BUD-015, F-BUD-016 |
| **EP-BUD-04** | Recettes budgétaires | 1 | F-BUD-017 |
| **EP-BUD-05** | Contrôle, suivi et reporting | 3 | F-BUD-018, F-BUD-019, F-BUD-020 |
| **EP-BUD-06** | Réallocation des crédits | 4 | F-BUD-021, F-BUD-022, F-BUD-023, F-BUD-024 |

---

## 2. Liste des 24 Fonctionnalités

| **ID** | **Fonctionnalité** | **Epic** | **Priorité** | **BF couverts** |
| :--- | :--- | :--- | :---: | :--- |
| **F-BUD-001** | Gestion des exercices budgétaires | EP-BUD-01 | Haute | BF-BUD-001 |
| **F-BUD-002** | Nomenclature et plan de codification budgétaire | EP-BUD-01 | Haute | BF-BUD-002, 003, 005, 006 |
| **F-BUD-003** | Versions budgétaires et comparaison | EP-BUD-01 | Haute | BF-BUD-004 |
| **F-BUD-004** | Campagne budgétaire et lettre-circulaire | EP-BUD-02 | Haute | BF-BUD-007 |
| **F-BUD-005** | Groupe de travail budgétaire | EP-BUD-02 | Moyenne | BF-BUD-008 |
| **F-BUD-006** | CDMT et Projet de Performance Annuel par programme | EP-BUD-02 | Haute | BF-BUD-009, 010 |
| **F-BUD-007** | Avant-projets des unités opérationnelles | EP-BUD-02 | Haute | BF-BUD-011, 013 |
| **F-BUD-008** | Consolidation et arbitrages budgétaires | EP-BUD-02 | Haute | BF-BUD-012, 014 |
| **F-BUD-009** | Approbation et mise en exécution du budget | EP-BUD-02 | Haute | BF-BUD-015, 016, 017 |
| **F-BUD-010** | Création du bon d'engagement | EP-BUD-03 | Haute | BF-BUD-018, 026, 029 |
| **F-BUD-011** | Contrôle budgétaire bloquant | EP-BUD-03 | Haute | BF-BUD-019, 020 |
| **F-BUD-012** | Détermination du circuit et pièces obligatoires | EP-BUD-03 | Haute | BF-BUD-021, 022, 024 |
| **F-BUD-013** | Visa budgétaire et signature | EP-BUD-03 | Haute | BF-BUD-023 |
| **F-BUD-014** | Bon de commande lié à l'engagement | EP-BUD-03 | Haute | BF-BUD-025 |
| **F-BUD-015** | Circuits spécifiques : missions, véhicules, études | EP-BUD-03 | Haute | BF-BUD-027 |
| **F-BUD-016** | Annulation et réduction d'un engagement | EP-BUD-03 | Haute | BF-BUD-028 |
| **F-BUD-017** | Suivi et rapprochement des recettes | EP-BUD-04 | Haute | BF-BUD-030, 031, 032 |
| **F-BUD-018** | Contrôle a priori des pièces | EP-BUD-05 | Haute | BF-BUD-033 |
| **F-BUD-019** | Situation d'exécution en temps réel | EP-BUD-05 | Haute | BF-BUD-034, 035, 039 |
| **F-BUD-020** | Rapports mensuels et trimestriels | EP-BUD-05 | Haute | BF-BUD-036, 037, 038 |
| **F-BUD-021** | Demande de réallocation | EP-BUD-06 | Haute | BF-BUD-040 |
| **F-BUD-022** | Analyse et classification de la réallocation | EP-BUD-06 | Haute | BF-BUD-041, 042 |
| **F-BUD-023** | Circuit de décision de la réallocation | EP-BUD-06 | Haute | BF-BUD-043, 044, 045 |
| **F-BUD-024** | Exécution et suivi des réallocations | EP-BUD-06 | Haute | BF-BUD-046, 047 |

---

## 3. Fiches Détaillées des Fonctionnalités

### 3.1 EP-BUD-01 — Structure et nomenclature budgétaire
- **F-BUD-001 (Gestion des exercices budgétaires)** : Création d'exercice (01/01 au 31/12), cycle « En préparation » → « Voté » → « Exécutoire » → « Clôturé ». Seul l'exercice exécutoire autorise les engagements.
- **F-BUD-002 (Nomenclature et codification budgétaire)** : Hiérarchie Programme → Action → Activité → Tâche → Ligne, distinction des natures (fonctionnement, produits, activités, investissements) et sources de financement. Import du plan officiel signé.
- **F-BUD-003 (Versions budgétaires et comparaison)** : Création de versions (avant-projet, projet, voté, rectifié), comparaison ligne par ligne en valeur et %.

### 3.2 EP-BUD-02 — Préparation et élaboration du budget
- **F-BUD-004 (Campagne budgétaire et lettre-circulaire)** : Modèle de lettre-circulaire soumis au DG, chronogramme et alertes de jalons.
- **F-BUD-005 (Groupe de travail budgétaire)** : Décision de mise en place, gestion des membres, convocations, feuilles de présence et PV de séances.
- **F-BUD-006 (CDMT et PPA par programme)** : Saisie CDMT triennal N+1 à N+3, stratégie, indicateurs chiffrés, objectifs et cibles mesurables.
- **F-BUD-007 (Avant-projets des unités opérationnelles)** : Budgétisation décentralisée (ranchs, abattoirs), éléments de maturation (TdR), pré-remplissage basé sur l'historique N-1.
- **F-BUD-008 (Consolidation et arbitrages budgétaires)** : Débats budgétaires, conservation des montants proposés, arbitrés et retenus avec motifs.
- **F-BUD-009 (Approbation et mise en exécution du budget)** : Circuit GT → DG → CA (résolution) → Décision exécutoire. Verrouillage du voté, ouverture des engagements et génération des annexes réglementaires.

### 3.3 EP-BUD-03 — Engagement des dépenses
- **F-BUD-010 (Création du bon d'engagement)** : Numérotation unique par exercice et unité, reprise de l'expression de besoin, TdR, OM, paie ou contrat.
- **F-BUD-011 (Contrôle budgétaire bloquant)** : Vérification en temps réel de l'imputation et de la disponibilité du crédit ; blocage strict si dépassement.
- **F-BUD-012 (Détermination du circuit et pièces obligatoires)** : Aiguillage menue dépense (< 500 000 FCFA TTC) vs grosse dépense (≥ 500 000 FCFA TTC avec BC). Exigence attestation de carence, dossier fiscal et consultation 3 fournisseurs en unité.
- **F-BUD-013 (Visa budgétaire et signature)** : Visa électronique horodaté, interdiction d'auto-validation (4-yeux), inviolabilité du BE signé.
- **F-BUD-014 (Bon de commande lié à l'engagement)** : Génération automatique du BC lié au BE pour dépenses ≥ 500 000 FCFA et transmission Marchés.
- **F-BUD-015 (Circuits spécifiques : missions, véhicules, études)** : Validation spécifique OM/SFR, diagnostic Brigade de Maintenance pour véhicules, visa TdR études.
- **F-BUD-016 (Annulation et réduction d'engagement)** : Annulation motivée et restitution immédiate du crédit au disponible.

### 3.4 EP-BUD-04 — Recettes budgétaires
- **F-BUD-017 (Suivi et rapprochement des recettes)** : Réception des encaissements Finance, suivi par nature et unité, rapprochement comptable et calcul du taux de réalisation.

### 3.5 EP-BUD-05 — Contrôle, suivi et reporting
- **F-BUD-018 (Contrôle a priori des pièces)** : Checklist paramétrable par nature bloquant la signature en cas d'incomplétude.
- **F-BUD-019 (Situation d'exécution en temps réel)** : Tableau de bord dynamique Voté / Engagé / Liquidé / Payé / Disponible et alertes de seuils (80%, 100%).
- **F-BUD-020 (Rapports mensuels et trimestriels)** : Génération automatique le 05 pour les unités et le 10 pour la DG ; justifications des écarts.

### 3.6 EP-BUD-06 — Réallocation des crédits
- **F-BUD-021 (Demande de réallocation)** : Formulaire normé (lignes source/cible, justification, impact, urgence, risques).
- **F-BUD-022 (Analyse et classification)** : Classification automatique simple (≤ 5%), significative (> 5% ou investissement), exceptionnelle ; contrôle disponibilité source.
- **F-BUD-023 (Circuit de décision de la réallocation)** : DAF+DG, comité, résolution CA ; gestion de la mesure conservatoire d'urgence.
- **F-BUD-024 (Exécution et suivi des réallocations)** : Mise à jour automatique des soldes budgétaires, édition du tableau rectifié et historique annuel.
