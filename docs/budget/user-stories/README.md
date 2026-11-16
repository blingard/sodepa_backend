# User stories — module Gestion budgétaire

75 user stories écrites le 15 novembre 2026 à partir de la [spécification fonctionnelle de référence](../specification-fonctionnelle-budget.md) (v1.0). Elles en suivent les lots de livraison :

| Lot | Fichier | Stories | Tailles (S / M / L) |
|---|---|---|---|
| B1 — Socle | [lot-B1-socle.md](lot-B1-socle.md) | US-BUD-101 à 115 (15) | 6 / 7 / 2 (L : l'import et le journal des mouvements) |
| B2 — Exécution des dépenses | [lot-B2-execution.md](lot-B2-execution.md) | US-BUD-201 à 225, 240 à 243 (29) | 6 / 17 / 6 |
| B3 — Réallocation | [lot-B3-reallocation.md](lot-B3-reallocation.md) | US-BUD-301 à 309 (9) | 2 / 5 / 2 |
| B4 — Suivi, recettes, clôture | [lot-B4-suivi-recettes-cloture.md](lot-B4-suivi-recettes-cloture.md) | US-BUD-401 à 408 (8) | 1 / 7 / 0 |
| B5 — Élaboration | [lot-B5-elaboration.md](lot-B5-elaboration.md) | US-BUD-501 à 514 (14) | 0 / 8 / 6 |

Les réponses de la SODEPA au questionnaire ne sont pas encore arrivées. Les stories reposent sur les hypothèses H-01 à H-31 de la spécification ; la table « Hypothèses → stories » ci-dessous dit quoi reprendre à chaque réponse.

---

## Conventions

- **Identifiant** : `US-BUD-LNN`, où L est le lot (1 à 5) et NN le rang. Les intégrations du lot B2 commencent à 240. Un numéro n'est jamais réutilisé ; une story ajoutée prend le premier numéro libre de son lot.
- **Gabarit** : titre ; « En tant que…, je veux…, afin de… » ; une ligne de métadonnées (fonctionnalité, règles, hypothèses, dépendances, taille) ; critères d'acceptation en Gherkin français (`Étant donné`, `Quand`, `Alors`, `Et`).
- **Exemples chiffrés** : ceux du guide (ranch de Ndokayo, ligne « achat des vaccins » de 10 000 000). Ils sont fictifs.
- **Taille** : relative. S = quelques jours, M = un sprint au plus, L = à découper si elle ne tient pas dans un sprint.
- **Règles** : `RG-BUD-nnn` de la spécification § 7. **Hypothèses** : `H-nn` de la spécification § 12.

## Définition de « prêt »

Une story entre en sprint quand :

1. ses dépendances sont livrées ou prévues dans le même sprint ;
2. les hypothèses qu'elle cite sont relues, et aucune réponse de la SODEPA ne les contredit ;
3. les maquettes d'écran existent quand la story a une interface ;
4. l'équipe a estimé la story ; une story L qui dépasse un sprint est découpée.

## Définition de « terminé »

Valable pour toutes les stories, sans être répétée dans leurs critères. Reprend les critères transverses CT-BUD-01 à 13 de la spécification § 10.

- **Droits** : une action sans le profil requis renvoie 403 et n'apparaît pas dans l'interface (CT-BUD-01). Chaque nouvelle permission est ajoutée à `Permissions` côté serveur et à `APP_PERMISSIONS` côté front.
- **Périmètre** : un utilisateur ne voit et ne traite que sa structure, sauf les profils à vue globale (CT-BUD-04).
- **Identité** : l'auteur d'une opération est lu dans le jeton, jamais dans la requête (CT-BUD-13).
- **Quatre yeux** : l'auteur d'une demande ne la vise ni ne la valide ; une personne ne franchit pas deux étapes d'un même circuit (RG-BUD-021, RG-BUD-035).
- **Référentiels** : toute création ou modification d'un référentiel (nomenclature, structures, natures, seuils, table de décision) passe par le maker-checker et rend 202 avec l'identifiant de la demande.
- **Traçabilité** : toute écriture est journalisée avec auteur, date, avant et après (CT-BUD-02) ; tout montant budgétaire provient d'un mouvement (RG-BUD-032).
- **Concurrence et rejeu** : aucun dépassement de crédit sous accès simultanés ; une opération rejouée n'a qu'un effet (CT-BUD-11, CT-BUD-12). Toute story qui touche un montant a un test d'intégration qui le vérifie.
- **Erreurs** : un refus métier rend un message en français destiné à l'utilisateur et un code stable ; crédit insuffisant = 409, ressource absente = 404, données invalides = 400.
- **Paramètres** : aucun seuil, taux, délai ni circuit codé en dur (CT-BUD-10).
- **Formats** : montants en FCFA sans décimale, dates JJ/MM/AAAA ; listes paginées (100 au plus) et exportables en Excel et PDF avec les filtres appliqués (CT-BUD-07, CT-BUD-08).
- **Schéma** : toute table ou colonne a son changelog Liquibase.
- **Contrat** : toute route ajoutée a sa règle de limitation de débit, et `api.json` du front est régénéré.
- **Tests** : critères d'acceptation automatisés (tests d'intégration côté backend, scénarios AQA pour les parcours d'écran) ; la CI exécute les tests.

---

## Ordre de réalisation suggéré

Le chemin critique va de l'exercice au premier engagement signé :

```text
101 → 104 → 105 → 106 ┐
107 → 108 ────────────┼→ 109 → 113 → 111 → 102 → 103 → 201 → 203 → 207/208 → 214 → 216
112 ──────────────────┘                                  ↑
                                                  205 → 206
```

Ensuite, dans le lot B2 : les dépenses particulières (217 à 223), puis le contrat d'opérations et les branchements (240 à 243) au rythme des modules Achats, Paie et Trésorerie. Les lots B3 et B4 peuvent avancer en parallèle une fois 113 et 224 livrées. Le lot B5 vient en dernier, avant la campagne de l'exercice suivant.

---

## Traçabilité

### Fonctionnalités → stories

| Fonctionnalité | Stories (US-BUD-) |
|---|---|
| F-BUD-001 Exercices | 101, 102, 103, 223 |
| F-BUD-002 Nomenclature, plan de codification | 104, 105, 106 |
| F-BUD-003 Versions | 102, 103, 513 |
| F-BUD-004 Campagne | 501, 502 |
| F-BUD-005 Groupe de travail | 503 |
| F-BUD-006 CDMT et PPA | 504, 505, 506, 507 |
| F-BUD-007 Avant-projets des unités | 508 |
| F-BUD-008 Consolidation et arbitrages | 509, 510, 511 |
| F-BUD-009 Approbation et annexes | 512, 514 |
| F-BUD-010 Bon d'engagement | 203, 204 |
| F-BUD-011 Contrôle bloquant | 203 (et 113, 201) |
| F-BUD-012 Circuit, support, pièces | 205 |
| F-BUD-013 Visas et signature | 207, 208, 209, 210 |
| F-BUD-014 Support de commande | 210, 211, 212 |
| F-BUD-015 Circuits spécifiques | 217, 218, 219, 220 |
| F-BUD-016 Annulation, réduction | 213 |
| F-BUD-017 Recettes | 401, 402 |
| F-BUD-018 Contrôle a priori | 206 |
| F-BUD-019 Situation d'exécution | 110, 224, 225 |
| F-BUD-020 Rapports | 403, 404, 405, 406 |
| F-BUD-021 Demande de réallocation | 301 |
| F-BUD-022 Analyse et classification | 302, 303 |
| F-BUD-023 Circuit de décision | 304, 305, 306, 309 |
| F-BUD-024 Exécution des réallocations | 305, 307, 308 |
| F-BUD-025 Structures | 107 |
| F-BUD-026 Lignes, natures, sources | 108, 109, 110 |
| F-BUD-027 Import du budget | 111 |
| F-BUD-028 Seuils et paramètres | 112 |
| F-BUD-029 Journal des mouvements | 113, 114 |
| F-BUD-030 Visa et mise de côté | 201, 202 |
| F-BUD-031 Paie | 221 |
| F-BUD-032 Caisse B | 222 |
| F-BUD-033 Liquidation | 214, 215 |
| F-BUD-034 Paiement constaté | 216 |
| F-BUD-035 Contrat d'opérations | 240 |
| F-BUD-036 Branchement Achats | 241, 242 |
| F-BUD-037 Branchement Paie, Missions, Formation | 217, 220, 221 (par le contrat 240) |
| F-BUD-038 Branchement Comptabilité, Trésorerie | 243, 402 |
| F-BUD-040 Clôture et reports | 407, 408 |
| Habilitations (spécification § 3) | 115 |

### Hypothèses → stories

À chaque réponse de la SODEPA : corriger l'hypothèse dans la spécification, puis les stories listées ici, puis leurs critères.

| Hypothèse (question) | Stories (US-BUD-) |
|---|---|
| H-01 (Q-01) Plan de codification, budget approuvé | 104, 105, 106, 109, 111, 507 |
| H-02 (Q-02) Modèles de documents | 210, 305, 501, 514 |
| H-03 (Q-03) Plan de codification permanent | 104, 106, 507 |
| H-04 (Q-04) Comité d'élaboration et groupe de travail | 503, 512 |
| H-05 (Q-05) Délais du chronogramme | 501, 502 |
| H-06 (Q-06) Budget non exécutoire au 1er janvier | 103, 223 |
| H-07 (Q-07) Plafonds d'arbitrage | 511 |
| H-08 (Q-08) Contenu et numéro du BE | 203, 210 |
| H-09 (Q-09) BE sur plusieurs lignes | 204 |
| H-10 (Q-10) Annulation, reliquat | 213, 214 |
| H-11 (Q-11) Contrats pluriannuels | 212 |
| H-12 (Q-12) Ordre des visas | 207, 209 |
| H-13 (Q-13) Signature électronique | 207 |
| H-14 (Q-14) Pouvoir du DUO | 107, 205, 208 |
| H-15 (Q-15) Engagement des recettes | 401, 402 |
| H-16, H-17 (Q-16, Q-17) Petites dépenses, caisse B | 222 |
| H-18 (Q-18) Facture supérieure au BE | 214, 215 |
| H-19 (Q-19) Effet du visa | 201, 202, 241 |
| H-20 (Q-20) Dérogation, paie | 221 |
| H-21 (Q-21) Format des rapports | 403, 404, 405, 406 |
| H-22 (Q-22) Grille de réallocation | 303, 304, 305 |
| H-23 (Q-23) Assiette du 5 % | 112, 303 |
| H-24 (Q-24) Seuils non chiffrés | 303, 306 |
| H-25 (Q-25) Ligne partiellement engagée | 302, 307 |
| H-26 (Q-26) Comité de réallocation | 304 |
| H-27 (Q-27) Saisine des tutelles | 309 |
| H-28 (Q-28) Fin d'exercice | 407, 408 |
| H-29 (Q-29) Autres modifications du budget | aucune (évolution hors lots) |
| H-30 (Q-30) Financements extérieurs | 108 |
| H-31 (Q-31) Sigles | 115 |

### Reprise des critères de Jean-Paul

Les 75 critères d'acceptation de `docs/architecture/CRITERES_ACCEPTATION_MODULE_BUDGETAIRE.md` restent une source pour les fonctionnalités non modifiées (spécification § 6). Les critères Gherkin de ces stories les remplacent pour F-BUD-003, 010, 012, 013, 014, 015, 017 et 022 à 024, dont le contenu a changé.
