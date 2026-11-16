# Module budgétaire — bilan du code au 15 novembre 2026 (rapport n° 3)

Rédigé le 15 novembre 2026. Le [rapport n° 2](rapport-02-bilan-code-budget.md) analysait le commit `9d03fc9` (6 novembre). Depuis, dix-neuf commits ont été poussés, dont trois sur le budget le 11 novembre (`805b9f2`, `7e9b89c`, `fb0c397`) et cinq sur le moteur de workflow (`5d32212` à `7efffe0`). Ce rapport dit ce qui a changé, ce qui reste à refaire au regard de la procédure de la SODEPA (guide budgétaire, [rapport n° 1](rapport-01-procedure-budgetaire.md)), et donne un avis.

**Périmètre.** `sodepa_backend` au commit `b0b69a0` (11 novembre), paquet `budget/` (≈ 9 400 lignes) et ses points de contact : achats, comptabilité, paie, régies, formation, moteur de workflow, habilitations. `sodepa-frontend`, fonctionnalité budget, au 14 novembre.

**Méthode.** J'ai lu le cœur budgétaire : adaptateur, contrôleur, entités, service de sécurité, référentiel et tests. Deux revues ont été menées en parallèle, l'une sur le moteur de workflow et le maker-checker, l'autre sur les intégrations et le front. J'en ai revérifié dans le source les affirmations les plus lourdes. Le code compile et les dix tests unitaires du budget passent (JDK 21, 15 novembre). Je n'ai pas fait d'appels réels : la version versionnée ne démarre pas (N1).

Les chemins `Bud/…` désignent `src/main/java/com/sodepa/erp/budget/…`.

---

## 1. En bref

1. **La version versionnée de `main` ne démarre pas.** Le changelog 25 contient une ligne YAML mal formée que Liquibase refuse, quel que soit l'état de la base (N1). Une correction existe sur ce poste, non versionnée.
2. **Le module a gagné le vocabulaire de la procédure, pas son fonctionnement.** Campagne, groupe de travail, réunions, quinze statuts de plan, champs de visa, profils DG, DAF, CSB, DUO et CSAFC, typologie des réallocations : tout cela est déclaré, mais presque rien n'est utilisé. Aucune route ne sert la campagne. Aucun visa n'est jamais écrit. Deux des trois nouvelles permissions ne sont contrôlées nulle part. Les circuits budgétaires du moteur ne sont jamais démarrés.
3. **Aucune dépense ne peut encore suivre la chaîne besoin → visa → bon d'engagement → liquidation → paiement → réalisé.** Les ruptures relevées le 9 novembre sont toutes là (§ 5).
4. **Les défauts bloquants du 9 novembre sont presque tous présents** (B1, B2, B3, B4, B7, B8, B9). B0 a disparu par un effet de bord. Le 500 s'est déplacé de la soumission à l'approbation : quand le DAF approuve un engagement ou une réallocation dans le moteur, la décision est perdue (§ 3).
5. **La ligne budgétaire est toujours un compte et une section analytique.** Elle n'a ni programme, ni structure, ni distinction entre recettes et dépenses. Des colonnes `programme_code` et `activite_code` existent en base, mais le code ne les lit pas.
6. **Les documents surestiment l'état du code.** La matrice de conformité de `docs/architecture/` déclare 71 critères d'acceptation sur 75 « conformes, couverts en production et testés ». La documentation du moteur déclare les écarts D1 à D12 « résolus ». Le code ne le confirme pas (§ 6).

---

## 2. Ce qui a changé depuis le 9 novembre

| Apport | Ce qui existe | Utilisé ? |
|---|---|---|
| **Référentiel** programmes, activités, natures de dépense, sources de financement (`5d32212`) | Tables, semeur, routes `GET`/`POST /api/budget/referentiels/…`. Deux niveaux sur quatre (pas d'action ni de tâche), sans indicateurs ni cibles | Consulté par le front. Les créations enregistrent l'entité JPA reçue telle quelle, sans habilitation, sans maker-checker ni cas d'utilisation (`Bud/presentation/rest/ReferentielBudgetaireRestController.java:61-97`). **Non relié aux lignes budgétaires** |
| **Campagne, groupe de travail, réunion** (`805b9f2`, changelog 25) | Entités, tables, dépôts | **Non** : aucun service ni route n'injecte ces dépôts |
| **Statuts du plan** (15 valeurs, dont des doublons français et anglais) | `Bud/infrastructure/entities/StatutBudget.java` | Seuls `DRAFT`, `SUBMITTED`, `PUBLISHED` et `REJECTED` sont écrits. L'approbation passe directement à `PUBLISHED` : ni arbitrage, ni CA, ni décision exécutoire (`Bud/infrastructure/adapter/BudgetPlanAdapter.java:486-506`) |
| **Réallocation typée** : simple, significative, urgence ; taux ; numéro de résolution du CA ; statut « à régulariser » | `BudgetPlanAdapter.java:71-112`, `:595-680` | Testée en isolation, mais **inaccessible par l'API** : la requête REST n'a ni urgence ni numéro de résolution (`Bud/presentation/requests/ReallocationRequest.java`). Toute réallocation significative est donc refusée. Le circuit calculé n'est jamais transmis au moteur (§ 3) |
| **Champs d'engagement** : visa, date de visa, agent, pièces validées, unité (changelog 25) | `Bud/infrastructure/entities/BudgetEngagementEntity.java:94-125` | Visa, date et pièces ne sont **jamais écrits**. Le circuit `MENUE_DEPENSE` / `GROSSE_DEPENSE` est une étiquette posée à 500 000 FCFA en dur (`BudgetPlanAdapter.java:792`) |
| **Cloisonnement par unité** (ABAC) | `Bud/application/BudgetSecurityService.java` | Le filtre de la liste des engagements fonctionne. Le contrôle d'imputation est **inopérant** (N2). La portée globale se décide par sous-chaîne du code de poste (« DG », « DAF », « BUDGET », « DCAI », `:147-149`) |
| **Permissions et profils** (`7e9b89c`) | 3 nouvelles permissions ; 6 profils budget (DG, DAF, chef de service budget, DUO, CSAFC, DCAI) | Seule `BUDGET_REALLOCATE_URGENCE` est contrôlée ; `BUDGET_VISA_SB` et `BUDGET_CLOTURE_EXERCICE` ne le sont nulle part. Les profils ne sont attribués à personne ; le plafond d'engagement par profil n'est jamais lu |
| **Écouteur d'événements budget** | `Bud/application/BudgetWorkflowEventListener.java` | Il écrit dans le journal « Libération des réservations de crédits » sans rien libérer |
| **Tests unitaires** (10) | `src/test/java/com/sodepa/erp/budget/BudgetModuleRulesTest.java` | Verts. Ils appellent des méthodes privées par réflexion, avec des simulacres. Ils ne couvrent ni la soumission, ni la validation, ni le vrai contrôle par unité, qui est simulé. Ils n'auraient détecté ni B8, ni B9, ni N2 |
| **Moteur de workflow** (`7efffe0`) | Identité lue dans le jeton, permissions sur les décisions, initiateur exclu des tâches, révision, `@Version` | Progrès réels côté moteur. **Le budget n'y est pas branché** (§ 3) |

---

## 3. Le budget et le moteur de workflow

- **Soumission.** Le maker-checker a été déplacé dans `workflow/application/WorkflowService.java` (`submitChange`, `:1793`). Aucun circuit `WF-METIER-BUDGET_*` n'existe. Le moteur se replie donc sur un « circuit standard » créé à la volée, avec une seule étape assignée au poste `POSTE_DIRECTEUR_DAF` (`:343-419`). La soumission ne tombe plus en 500 : l'appel interne échappe à la transaction (auto-invocation), et non grâce à l'isolation que revendique la documentation.
- **Approbation.** Quand le DAF approuve la tâche d'un engagement ou d'une réallocation, le moteur appelle l'adaptateur budget dans sa propre transaction. B9, puis B1, y lèvent une exception : `UnexpectedRollbackException`, 500, et la décision du DAF est perdue.
- **Circuits.** `WF-DAF-06`, `WF-BUD-REALLOC-SIGNIFICATIVE`, `WF-BUD-REALLOC-URGENCE`, `WF-DG-01` et `WF-DG-04` sont semés mais **jamais démarrés**. Le circuit de réallocation calculé est seulement rangé dans la charge utile (`BudgetPlanAdapter.java:556`).
- **Seuils par montant.** Aucune règle par montant dans le moteur : il enchaîne les étapes de façon linéaire. Les conditions de transition ne sont lues nulle part.
- **Contrôle de disponibilité.** L'étape `DAF_01_DISPO_BUDGET`, « Contrôle automatique de disponibilité de crédit », est approuvée sans aucune vérification (`WorkflowService.java:904-912`).
- **Deux chemins de validation coexistent.** D'un côté la tâche du moteur, de l'autre le `PUT …/validate_or_reject/{id}` du budget, qui ignore l'instance de workflow. Ils ne se synchronisent pas.
- **Visas de pré-engagement.** Le circuit de `RapprochementAdapter` (chef de service jusqu'à 500 000 FCFA, directeur financier jusqu'à 5 000 000) n'a pas changé. Il fait toujours confiance au rôle envoyé par le client (`:251-298`).

---

## 4. Défauts du 9 novembre : où en est-on

| Réf. | Défaut (rapport n° 2) | État au 15 novembre |
|---|---|---|
| B0 | Toute soumission maker-checker → 500 | **Disparu par effet de bord** (§ 3) ; le 500 réapparaît à l'approbation |
| B1 | Engagement et réallocation validés : échec à l'insertion | **Présent**, masqué par B9 |
| B2 | Maker-checker budgétaire inutilisable : auto-validation toujours refusée, 200 au lieu de 202, pas de `/pending`, objets fictifs renvoyés | **Présent** ; s'y ajoute un second chemin de validation |
| B3 | Disponible sans verrou | **Présent** (`BudgetItemEntity` sans `@Version`, aucun `@Lock`) |
| B4 | Réalisé faux : liquidation sans réalisé, avoirs comptés en plus, compte exact seulement, toutes versions | **Présent**, inchangé |
| B5 | Habilitations et identité | **Partiel** : quatre contrôles de plus. Toujours sans habilitation : soumettre, approuver et rejeter un plan ; ajouter une ligne ; liquider ; annuler ; pré-engager ; viser. Identité lue dans la requête (`userId`, `responsableId`, `roleApprobateur`). `UserSeeder` non corrigé |
| B6 | Modèle sans rapport avec le budget de la SODEPA | **Présent** (§ 1, point 5) |
| B7 | `maker_checker_requests` sans changelog, contrainte figée | **Présent, aggravé** : nouvelle colonne `workflow_instance_id`, sans changelog non plus |
| B8 | Chaque ligne budgétaire enregistrée deux fois | **Présent** (`BudgetPlanAdapter.java:454-458`) |
| B9 | `id` refusé à la relecture de la demande | **Présent, aggravé** : la réallocation porte maintenant trois champs inconnus (`id`, `circuitWorkflow`, `typeReallocation`) |
| M1, M2 | Crédit libéré deux fois ; liquidation sans visa | **Présents** |
| M5 | Erreurs métier en 400 ou 500 au lieu de 409 | **Présent** |
| M6 | Le budget peut bloquer la comptabilité (écouteur synchrone) | **Présent** |
| M7 | Seuils faux et codés en dur | **Aggravé** : trois sources contradictoires (N5) |
| M8 | Mécanismes d'approbation parallèles | **Présent** : maker-checker greffé dans le moteur, visas codés en dur, circuits semés jamais démarrés |
| M9 | Deux budgets concurrents (analytique) | **Présent** |

**Défauts nouveaux**

- **N1 — La version versionnée ne démarre pas.** `db-changelog-25.0.yaml:119` écrit `type: NUMERIC(7, 4)` dans un mapping YAML en flux : la virgule coupe la valeur en `NUMERIC(7` plus une clé orpheline `4)`. Liquibase 5 lève `ChangeLogParseException` au démarrage. La copie de travail de ce poste corrige le point (valeur entre guillemets), mais la correction n'est pas versionnée. La branche `origin/fix/demarrage-local` d'Ian ne la contient pas.
- **N2 — Le contrôle d'imputation par unité ne refuse jamais rien.** `validateAccessToUnite` reçoit l'identifiant de **section analytique** de la ligne (`BudgetPlanAdapter.java:548`, `:606`, `:787`) et le cherche parmi les **unités organisationnelles**. Ne le trouvant pas, il laisse passer. Le test unitaire correspondant simule ce service et ne le voit pas.
- **N3 — Référentiel budgétaire sans garde.** Création de programmes, activités, natures et sources sans habilitation ni maker-checker. L'entité JPA sert de corps de requête : un client peut fixer l'identifiant et écraser un enregistrement existant.
- **N4 — Réallocation significative impossible par l'API** (§ 2).
- **N5 — Trois sources de seuils incompatibles**, toutes différentes de la procédure :
  - `achat_seuils_config` : semée comme le 9 novembre, modifiable sans habilitation, lue par personne ;
  - paramètres `achats.seuil_*` : 500 000 / 15 M / 50 M en dur, seule source réellement utilisée par les achats ;
  - `SeuilGestionEntity` de la fiscalité : bon de commande jusqu'à 4 999 999, lettre commande dès 5 M, utilisée par personne.

  Aucune ne connaît les tranches de 100 000 (caisse B) ni de trois pro forma.
- **N6 — Front.**
  - La boîte de réception du workflow appelle deux routes renommées par `7efffe0` (`mes-taches` devenu `my-tasks`, `decision` devenu `task/decision`). C'est là qu'arrivent désormais les validations budgétaires.
  - `api.json` date du 27 août.
  - Aucune permission budget ni workflow dans `APP_PERMISSIONS`.
  - Aucun écran de visa, de pré-engagement, de validation en attente ni de campagne.

---

## 5. Bilan au regard du guide budgétaire

| Domaine | Ce que demande la procédure | Ce que fait le code | À faire |
|---|---|---|---|
| Exercice et états | Exercice ; états en préparation, approuvé par le CA, exécutoire, clos ; décision exécutoire datée | Entier `annee` ; quinze statuts déclarés, quatre utilisés ; approbation directe | **Refaire** |
| Nomenclature | Programme → action → activité → tâche, responsables, indicateurs, cibles | Programme et activité, sans indicateurs, non reliés aux lignes | **Reprendre** la base, compléter |
| Structures | Budget par structure (DG, unités), ordonnateur, service d'engagement (SB, CSAFC) | Ligne sans structure ; l'activité porte une unité ; cloisonnement par unité d'affectation | **Refaire** la ligne, **garder** l'idée du cloisonnement |
| Ligne budgétaire | Structure × élément du programme × nature ; recettes séparées | Compte et section analytique | **Refaire** |
| Élaboration | Campagne, chronogramme, CDMT, PPA, avant-projets en quatre volets, arbitrage, versions | Entités sans service ; ancien « budget collaboratif » (M4) | **Construire** (dernier lot) |
| Visa budgétaire | Visa avant consultation, crédit mis de côté | Rien : champs jamais écrits, étape automatique sans contrôle | **Construire** |
| Bon d'engagement | Contenu, numérotation, circuit SB → SDFBM → DAF → DG ou CSAFC → DUO, pièces exigées | Numéro libre ; deux chemins, l'un cassé (maker-checker), l'autre codé en dur avec le rôle déclaré | **Refaire** |
| Seuils | Table datée : 100 000, 500 000, 15 M, 50 M FCFA TTC | Trois sources contradictoires | **Unifier** |
| Liquidation, paiement | Service fait, facture, liquidation par le DG ou le DUO ; paiement par ancienneté | Changement d'état sans réalisé ni pièce ; pas de paiement | **Refaire** |
| Recettes | Imputation sur les lignes de produits | Aucune distinction | **Construire** |
| Suivi | Situation quotidienne ; rapports le 5 et le 10 ; rapport trimestriel | Indicateurs génériques de trésorerie ; aucun rapport de la procédure | **Construire** |
| Réallocation | Typologie, circuit selon la décision de la SODEPA, dossier, décision | Typologie codée sur un choix non validé : tout dépassement de 5 %, ou tout transfert entre sections, exige le CA, ce que ne dit aucune des trois grilles ; transfert réalisé | **Reprendre** après Q-22 à Q-27 ; la mécanique de transfert est réutilisable |
| Acteurs et habilitations | DG, DAF, SDFBM, CSB, DUO, CSAFC, CA | Six profils semés, non attribués ; une permission sur trois contrôlée | **Garder** les profils, les brancher |
| Intégrations | Achats, paie, missions, comptabilité appellent le budget | Inchangé : rien n'appelle le budget | **Construire** le contrat d'opérations |

**À garder :**
- le service de cloisonnement, une fois N2 corrigé et les sous-chaînes retirées ;
- les profils budgétaires ;
- le référentiel programme et activité, à compléter et à protéger ;
- la mécanique de transfert de la réallocation ;
- les progrès du moteur de workflow (identité, quatre yeux, révision) ;
- les lectures paginées, la validation des requêtes, la piste d'audit ;
- la démarche de tests, à étendre en tests d'intégration.

---

## 6. Écart entre la documentation et le code

`docs/architecture/` contient depuis le 11 novembre un second cahier des charges budgétaire (`CAHIER_DES_CHARGES_MODULE_BUDGETAIRE.md`), un catalogue de fonctionnalités, des critères d'acceptation et une matrice de conformité.

- **La matrice déclare 71 critères sur 75 « conformes, couverts en production et testés » (94,7 %).** Le § 4 montre l'inverse : l'application versionnée ne démarre pas, et aucun engagement ne peut aboutir.
- **La documentation du moteur affirme des corrections absentes du code** : `saveAndFlush` et isolation transactionnelle, contrôle de `WORKFLOW_READ`, assignation par N+1 ou par rôle, exécution automatique des étapes de contrôle.
- **Le cahier des charges commence par une balise `<USER_REQUEST>`.** C'est un reste de rédaction à retirer avant toute diffusion au client.

Il existe désormais deux référentiels de besoin budgétaire : `docs/budget/` (rapports n° 1 à 3, guide, questionnaire) et `docs/architecture/`. Il faut les fusionner avant d'écrire les user stories.

---

## 7. Avis

**L'intention est bonne, la méthode non.** Les commits du 11 novembre montrent une lecture attentive de la procédure : vocabulaire, typologie des réallocations, acteurs, cloisonnement par unité. Mais ils ajoutent des déclarations (entités, statuts, champs, permissions, circuits, journaux) sans les relier à un parcours qui fonctionne. Ils les accompagnent de tests et de matrices qui donnent l'apparence de la conformité. Le principal risque n'est plus technique : il touche à la confiance. Un document qui affirme 94,7 % de conformité peut partir chez le client alors que l'application ne démarre pas.

**Ma recommandation du 9 novembre tient, renforcée : reconstruire le cœur plutôt que le corriger.** Corriger B1, B3, B8 et B9 sur une ligne « compte et section » donnerait un module qui fonctionne, mais qui ne serait toujours pas le budget de la SODEPA. Les ajouts du 11 novembre ne changent pas ce constat : ils n'ont pas touché la ligne, les mouvements, le visa ni le bon d'engagement.

**Ordre proposé :**

1. **Tout de suite, pour toute l'équipe :**
   - versionner la correction du changelog 25 ;
   - donner un changelog à `maker_checker_requests` (B7) ;
   - régler l'`ObjectMapper` partagé pour qu'il tolère les champs inconnus (B9) ;
   - faire compléter les permissions par `UserSeeder` sur les comptes existants ;
   - corriger les deux routes du front renommées par le moteur.

   Ce sont de petits correctifs qui débloquent tous les modules.
2. **Décider qui porte le module budget**, et sur quel document de référence. Deux personnes y travaillent en parallèle depuis le 8 novembre, sur deux cahiers des charges distincts. Sans arbitrage, les user stories et le code se contrediront.
3. **Lot B1 (socle), sur le nouveau modèle :**
   - exercice et décision exécutoire ;
   - nomenclature à quatre niveaux, en reprenant programme et activité ;
   - structures reliées à l'organigramme ;
   - ligne structure × élément × nature ;
   - journal de mouvements, avec verrou sur le disponible ;
   - une table de seuils unique et datée.
4. **Lot B2 (exécution).** Visa et mise de côté du crédit, bon d'engagement, liquidation et réalisé, avec des circuits portés par le moteur de workflow. Cela suppose d'ajouter au moteur le routage par montant, qui lui manque.
5. **Réallocation après les réponses de la SODEPA** (Q-22 à Q-27). D'ici là, retirer la règle « CA dès 5 % ou entre sections », que rien ne fonde.
6. **Tests d'intégration sur le parcours complet** : soumission, validation, disponible, accès concurrents. Les tests sur simulacres n'ont rien vu des défauts bloquants.
