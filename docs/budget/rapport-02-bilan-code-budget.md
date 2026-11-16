# Module budgétaire — bilan du code existant (rapport n° 2)

Rédigé le 9 novembre 2026. Analyse du code du module budgétaire au regard de la procédure de la SODEPA ([rapport n° 1](rapport-01-procedure-budgetaire.md), règles RG, besoins EX et EL) et du [cahier des charges budget](Cahier_des_charges_gestion_budgetaire_ERP_ameliore.md) (exigences RF et RNF).

**Périmètre.** Dépôt `sodepa_backend` au commit `9d03fc9` du 6 novembre 2026 : paquet `budget/` (152 fichiers, environ 7 000 lignes), ses points de contact avec les achats, la comptabilité, la paie, le moteur de workflow et le maker-checker ; dépôt `sodepa-frontend`, fonctionnalité `budget`.

**Méthode.** J'ai lu intégralement le cœur budgétaire (plans, lignes, engagements, visas, réallocations, budget collaboratif, maker-checker). Trois revues ont été menées en parallèle sur la trésorerie, les intégrations et le front ; j'en ai revérifié les affirmations les plus lourdes dans le source. Le soir même, les constats ont été vérifiés par des appels réels à l'API démarrée (§ 10) : tous ceux qui ont pu être essayés sont confirmés, et les essais ont révélé trois défauts que la lecture n'avait pas vus (B7, B8, B9).

**Gravité.** *Bloquant* : empêche un usage réel ou fausse les montants. *Majeur* : contourne une règle de gestion ou de sécurité, ou rend un résultat faux dans un cas courant. *Mineur* : qualité, maintenabilité, cas rare.

---

## 1. Verdict

**Le module ne permet pas aujourd'hui d'exécuter un budget de bout en bout, ni de le contrôler.** Huit raisons, toutes confirmées par des appels réels (§ 10) :

1. **Depuis le 6 novembre, toute création soumise au maker-checker échoue en 500** (plan, engagement, réallocation) : la soumission démarre un circuit de workflow qui n'existe pas, et l'échec, bien qu'intercepté, condamne la transaction de l'appelant. **Ce défaut touche tous les modules du dépôt** : la base de développement ne contient ni plan, ni journal, ni écriture (B0). Sur cette base, la table des demandes refuse en outre 22 des 33 types de demandes, dont les trois du budget (B7).
2. **Chaque ligne budgétaire ajoutée est enregistrée deux fois** (B8). Le pré-engagement, seul chemin d'engagement sans maker-checker, échoue donc lui aussi en 500 sur toute ligne créée par l'API, et le réalisé y est imputé deux fois.
3. **Même ces obstacles levés, la validation d'un engagement ou d'une réallocation échoue deux fois** : à la lecture de la demande, que l'application construit avec un champ qu'elle refuse ensuite (B9), puis à l'enregistrement, que Hibernate 7 refuse (B1).
4. **Le maker-checker budgétaire est inutilisable** : l'auto-validation échoue toujours, et le valideur n'a aucun moyen de retrouver la demande (B2).
5. **Le disponible n'est pas fiable** : sans verrou, dix pré-engagements simultanés de 600 000 sur un crédit de 1 000 000 en font passer quatre, et la ligne n'en retient qu'un. La liquidation libère le crédit sans enregistrer le réalisé, un avoir augmente la dépense (B3, B4).
6. **Presque rien n'est protégé** : un employé sans aucune habilitation approuve un plan, franchit seul les trois visas, modifie les seuils d'achat. L'identité et le rôle de l'approbateur sont lus dans la requête. À l'inverse, les comptes `admin_maker` et `admin_checker` n'ont aucune habilitation budget (B5).
7. **Le budget est coupé du reste de l'ERP** : ni les achats, ni la paie, ni les régies, ni la formation ne vérifient un disponible ou ne créent un engagement ; la seule passerelle est l'écriture comptable saisie à la main (§ 5).
8. **Le modèle ne correspond pas au budget de la SODEPA** : ni exercice, ni programme, ni structure, ni distinction recettes/dépenses, ni bon d'engagement documenté (B6).

**Ce qui vaut d'être gardé** est mince : l'idée d'une table de seuils paramétrable, l'écoute des écritures validées pour alimenter le réalisé (à réécrire), la piste d'audit, la pagination des listes, les validations Bean Validation des requêtes. **Recommandation** : reconstruire le cœur budgétaire sur le modèle du rapport n° 1 (§ 9 de ce rapport) plutôt que de corriger l'existant ligne à ligne. Si le module doit servir avant, une poignée de correctifs le rendent au moins cohérent (§ 9.1).

---

## 2. Le code en chiffres

| Élément | Valeur |
|---|---|
| Fichiers Java du paquet `budget/` | 152 (≈ 6 950 lignes), dont 4 adaptateurs « budget » (≈ 1 570 lignes) et 4 adaptateurs « trésorerie, financement, pilotage » (≈ 1 030 lignes) |
| Routes REST du paquet | 51, sous `/api/budget`, `/api/tresorerie`, `/api/financement`, `/api/reporting`, `/api/audit`, `/api/achats/seuils` |
| Routes avec contrôle d'habilitation | 6 (création et validation de plan, réallocation, engagement) |
| Tables budget | `budget_plan`, `budget_item`, `budget_item_periode`, `budget_engagement`, `budget_reallocation`, `budget_demande`, `audit_trail`, `achat_seuils_config` (Liquibase 6.0, 7.0, 12.0) |
| Contraintes d'intégrité métier en base | une seule : unicité de `numero_engagement`. Aucune unicité (plan, compte, section) ni (année, version), aucun contrôle de signe des montants |
| Tests | aucun dans le dépôt ; la suite `sodepa-aqa` n'est pas présente sur ce poste |
| Historique | 4 commits : initialisation (17 août), lectures paginées (27 août), généralisation du maker-checker (5 novembre), moteur de workflow (6 novembre) |

---

## 3. Ce que fait réellement le code

### 3.1 Cycle du plan

Création (maker-checker, en échec depuis le 6 novembre, B0) → ajout de lignes (compte SYSCOHADA et section analytique facultative, ventilées en 12 mensualités égales, chacune enregistrée deux fois, B8) → soumission → approbation (`PUBLISHED`) ou rejet. Un plan publié n'est plus modifiable que par réallocation.

### 3.2 Les trois chemins d'engagement

| Chemin | Route | Contrôles | Aboutit ? |
|---|---|---|---|
| Engagement direct | `POST /api/budget/engagements` | permission `CREATE_BUDGET_ENGAGEMENT`, maker-checker, disponible | **Non.** La soumission échoue (B0, B7) ; sans elle, l'auto-validation est refusée (B2) et la validation par un second utilisateur échoue à la lecture de la demande (B9), puis dans Hibernate (B1) |
| Pré-engagement puis visas | `POST /api/budget/engagements/workflow/pre-engager`, `/valider`, `/rejeter` | disponible seulement ; aucune permission ; rôle lu dans la requête | **Non** sur une ligne créée par l'API : elle existe en double (B8) et la recherche de la ligne échoue en 500. Sur une ligne unique, oui. Impossible sur une ligne sans section (`PreEngagementRequest.sectionId` est `@NotNull`) |
| Engagement par les achats | `POST /api/achats/dossiers/{id}/engager-budget` | — | Ne touche pas les lignes budgétaires (voir § 5) |

Conséquence : **aucun chemin d'engagement n'aboutit aujourd'hui**, et une ligne sans section analytique ne pourrait être engagée par aucun, même une fois B0, B1, B7, B8 et B9 corrigés.

### 3.3 Disponible, engagé, réalisé

`disponible = prévu − engagé − réalisé` sur la ligne, calculé à trois endroits (`BudgetPlanAdapter.java:516`, `:660`, `RapprochementAdapter.java:210`) et une quatrième fois dans le front. L'engagé monte à l'engagement ou au pré-engagement, descend à la liquidation, à l'annulation ou au rejet. Le réalisé monte à la validation d'une écriture comptable de classe 6 ou 7.

### 3.4 Flux réel « besoin → paiement »

Ce que le code permet aujourd'hui, comparé à la chaîne de la procédure (rapport n° 1, § 5.3) :

```text
BESOIN        expression de besoin ─maker-checker─► reste BROUILLON
              budgetDisponible = true en dur · imputation = texte libre · aucun visa budgétaire
                │
COMMANDE      procédure choisie sur le TTC : < 500 000 achat direct · < 15 M bon de commande
              · < 50 M lettre commande · sinon marché  (pas de palier 100 000)
                │
ENGAGEMENT    (facultatif) « engager-budget » ─► statut du dossier + code texte, auto-validé
              ✗ aucune ligne budgétaire lue · ✗ aucun engagement créé · ✗ aucun disponible contrôlé
                │  (réception et liquidation possibles sans cette étape)
RÉCEPTION     procès-verbal ─► « réceptionné », même non conforme
LIQUIDATION   drapeau « liquidé » ─► ✗ aucune écriture · ✗ aucun appel au budget
                ┊ rupture : aucune passerelle achats → comptabilité → budget
RÉALISÉ       écriture saisie à la main ─validation─► réalisé += |débit ou crédit|
              (classes 6 et 7, compte exact, toutes versions du plan)
PAIEMENT      aucun objet · règlement = écriture manuelle, ignorée par le budget

Chaînes parallèles, sans lien avec la précédente :
  Budget      engagement (maker-checker, en échec) ou pré-engagement ─► engagé += X
              liquidation ─► engagé −= X, réalisé inchangé
  Analytique  table « budgets » et réalisé recalculé sur les ventilations
  Paie        validation ─► message dans le journal, rien d'autre
  Régies      dépense ─► solde de régie −= X, imputation texte
  Workflow    circuits budgétaires semés, jamais démarrés
```

---

## 4. Défauts du cœur budgétaire

### 4.1 Bloquants

**B0 — Toute soumission au maker-checker échoue (défaut transverse, introduit le 6 novembre).** Depuis le commit `9d03fc9`, `MakerCheckerEngineAdapter.submitChange` démarre un circuit nommé `"WF-METIER-" + entité` (`share/MakerCheckerEngineAdapter.java:66-86`). Aucune définition `WF-METIER-*` n'existe dans le dépôt (ni seeder, ni changelog). `WorkflowService.demarrerWorkflow` est `@Transactional` et lève `IllegalArgumentException` (« Définition introuvable ») en participant à la transaction de l'appelant (`workflow/application/WorkflowService.java:402-410`). Spring marque alors cette transaction « à annuler » ; le `catch` de l'adaptateur avale l'exception mais n'y change rien. À la fin de la méthode appelante (`creerBudgetPlan`, `effectuerReallocation`, `enregistrerEngagement`, toutes `@Transactional`), la validation de la transaction lève `UnexpectedRollbackException` : réponse 500, rien n'est enregistré, pas même la demande. Le même mécanisme vaut pour `directSubmitChange` (`:118-121`) et pour **tout appelant transactionnel du dépôt** : 44 appels dans 13 fichiers (achats, comptabilité générale, organigramme, RH, paie, pastoral, régies, utilisateurs). Seules les immobilisations y échappent, parce qu'elles appellent `submitChange` hors transaction. *Confirmé (§ 10) : 500 sur le plan comme sur le journal comptable, aucune demande enregistrée, `UnexpectedRollbackException` au journal du serveur ; depuis le 6 novembre, seules des demandes d'immobilisation ont été enregistrées.*

**B1 — Engagement et réallocation validés : échec à l'insertion.** `BudgetEngagementEntity` et `BudgetReallocationEntity` gardent `@GeneratedValue(strategy = AUTO)` et n'ont pas de `@Version`, alors que le commit `e73647d` (5 novembre) leur fixe l'identifiant avant `save()` (`BudgetPlanAdapter.java:669-682`, `:543-552`). `save()` appelle alors `merge()`. J'ai désassemblé Hibernate 7.4.1 (version gérée par Spring Boot 4.1.0) : `DefaultMergeEventListener.entityIsDetached` lève `StaleObjectStateException` quand l'entité est jugée détachée (identifiant généré, renseigné, sans version) et absente de la base. La transaction entière est annulée, y compris la mise à jour de l'engagé, et le client reçoit un 500 opaque. `BudgetPlanEntity` y échappe grâce à son `@Version` nul, qui la fait juger transitoire. Les entités sous maker-checker de la comptabilité n'ont pas `@GeneratedValue` : c'est le bon schéma. Aucun autre module du même commit n'a ce défaut (balayage fait). *Confirmé (§ 10) : `ObjectOptimisticLockingFailureException` (« Row was updated or deleted by another transaction »), engagé et crédits inchangés, demande restée en attente. On ne l'atteint qu'après avoir levé B9.*

**B7 — La table des demandes refuse 22 types sur 33 (défaut transverse, constaté sur la base de développement).** `maker_checker_requests` n'a pas de changelog Liquibase (dette connue) : Hibernate l'a créée par `ddl-auto` avec une contrainte `CHECK` qui fige les valeurs de `MakerCheckerEntityName` du moment, et `ddl-auto: update` ne la met jamais à jour. Sur la base de ce poste, elle n'accepte que les 11 valeurs d'origine (`BANQUE`, `COMPTE`, `ECRITURE`, `FISCALYEARCLOSING`, `IMMOBILISATION`, `JOURNAL`, `LIGNEECRITURE`, `LIGNERELEVEBANCAIRE`, `RELEVEBANCAIRE`, `TIERS`, `USER`). Les 22 autres sont refusées par la base : les trois du budget, et `EMPLOYE`, `POSTE`, `AFFECTATION_POSTE`, `UNITE_ORGANISATIONNELLE`, `DELEGATION_SIGNATURE`, `DEROGATION_PERMISSION`, `EXPRESSION_BESOIN`, `FOURNISSEUR`, `MARCHE_DOSSIER`, `MARCHE_RECEPTION_PV`, `LIQUIDATION_FACTURE`, `PAIE_PERIODE`, `PAIE_VARIABLE`, `REGIE_APPROVISIONNEMENT`, `REGIE_DEPENSE` et les quatre du cheptel. Même B0 corrigé, ces soumissions échoueraient à l'insertion. Toute base créée avant l'ajout de ces valeurs a le même défaut ; une base neuve reçoit la liste du jour et le retrouvera au prochain ajout.

**B8 — Chaque ligne budgétaire est enregistrée deux fois.** `ajouterItemAuPlan` (`BudgetPlanAdapter.java:387-391`) rattache la ligne au plan (`plan.addItem`, cascade `ALL`), enregistre le plan, ce qui insère la ligne par cascade, puis enregistre la ligne elle-même : seconde insertion, avec ses douze périodes. Constaté : cinq lignes ajoutées par l'API donnent dix lignes et cent vingt périodes en base ; le total du plan (18 000 000) ne vaut que la moitié de la somme de ses lignes. Conséquences : toute recherche de la ligne par plan, compte et section (pré-engagement, engagement) échoue en 500 (« 2 results were returned ») ; le réalisé s'impute sur les deux copies, donc deux fois au niveau du plan.

**B9 — La validation d'un engagement ou d'une réallocation échoue à la lecture de la demande.** La soumission ajoute l'identifiant au contenu de la demande (`payload.put("id", …)`, `BudgetPlanAdapter.java:608` et `:469`) ; la validation le relit avec `objectMapper.convertValue(payload, EngagementInput.class)` (`:652`, `:502`). L'`ObjectMapper` injecté est celui de `utils/GeneralConfig.java`, un Jackson 2 construit à la main, qui refuse par défaut les champs inconnus : `IllegalArgumentException`, réponse 400 « Unrecognized field "id" ». Le plan y échappe, parce que sa création lit le contenu clé par clé. Constaté (§ 10).

**B2 — Maker-checker budgétaire inutilisable.**
- L'auto-validation (`BudgetPlanAdapter.java:246-257`, `:487-498`, `:625-636`) appelle la validation avec l'utilisateur qui vient de soumettre ; `submitChange` enregistre ce même utilisateur comme auteur (`MakerCheckerEngineAdapter.java:52`), donc la règle « un valideur ne vote pas sur sa propre demande » (`:318-319`) la rejette toujours. L'exception est avalée par un `log.warn`. Si elle passait, elle violerait la séparation des tâches exigée (RF-100).
- Les créations rendent 200 et un objet, pas 202 et `SubmissionOutput {requestId, entityId}` comme l'exige le dépôt. Il n'existe aucune route listant les demandes `BUDGET_*` en attente. Le valideur ne peut donc pas obtenir l'identifiant à passer à `validate_or_reject`.
- `POST /plans` renvoie un plan `DRAFT` qui n'existe pas en base (`:259-273`) ; `POST /engagements` renvoie un statut `PENDING_VALIDATION` inventé (`:638-648`).

**B3 — Disponible non protégé contre les accès concurrents.** `BudgetItemEntity` n'a ni `@Version` ni verrou, aucun dépôt n'utilise `@Lock`. Le contrôle « disponible ≥ montant » puis l'ajout à l'engagé (`BudgetPlanAdapter.java:660-680`, `RapprochementAdapter.java:210-231`) se font en lecture puis écriture séparées : deux engagements simultanés passent tous deux le contrôle et dépassent le crédit. Contraire à RF-95, RNF-01 et au critère d'acceptation 8 du cahier. *Confirmé, et plus grave que prévu (§ 10) : dix pré-engagements simultanés de 600 000 sur une ligne de 1 000 000 en acceptent quatre (2 400 000 engagés), et la ligne n'affiche que 600 000 engagés, les écritures concurrentes s'écrasant l'une l'autre. Le disponible affiché (400 000) est faux de 2 000 000.*

**B4 — Réalisé faux.**
- La liquidation diminue l'engagé sans augmenter le réalisé (`BudgetPlanAdapter.java:744-746`) : entre la liquidation et la validation de l'écriture comptable, le crédit paraît libre alors qu'il est dépensé.
- L'imputation du réalisé (`RapprochementAdapter.java:159-196`) prend le débit, ou à défaut le crédit, **toujours en positif** : un avoir, une extourne ou une annulation augmentent la dépense au lieu de la réduire.
- Elle cherche le compte **exact** : une écriture sur `601100` n'atteint jamais une ligne budgétée sur `601`.
- Elle ajoute le montant à **toutes** les lignes de même compte et section de l'année, quel que soit l'état du plan : brouillons, plans rejetés et versions successives reçoivent chacun le réalisé.
- Si l'écriture est validée avant la liquidation de l'engagement, le montant compte à la fois en engagé et en réalisé. C'est précisément le double comptage que le cahier interdit (règle 1 du § 15).

**B5 — Habilitations et identité.**
- Sans aucun contrôle d'habilitation : ajout de ligne, soumission, approbation et rejet de plan, liquidation, annulation, pré-engagement, visas, rejet d'engagement, tout le budget collaboratif, la piste d'audit et la modification des seuils d'achat. Aucune séparation des tâches : le même utilisateur soumet et approuve un plan.
- L'utilisateur est lu dans la requête (`userId`, `utilisateurId`, `responsableId`), jamais dans le jeton : la piste d'audit enregistre qui l'appelant prétend être.
- Le circuit des visas compare le rôle **déclaré dans la requête** au rôle attendu (`RapprochementAdapter.java:256`) : un seul utilisateur franchit les trois visas en changeant la chaîne `roleApprobateur`.
- Les permissions budget ne sont attribuées à aucun rôle métier par le seeder de l'organigramme. Les comptes techniques `admin_maker` et `admin_checker` ne les ont pas non plus : `UserSeeder` ne pose `Permissions.values()` qu'à la création du compte et ne complète jamais ensuite. Sur la base de développement (comptes créés le 26 octobre), ils ont 28 habilitations sur plus d'une centaine, aucune du budget : même eux reçoivent un 403 sur les six routes protégées. `CLAUDE.md` affirme le contraire.

**B6 — Modèle sans rapport avec le budget de la SODEPA.** Une ligne est un compte comptable et une section analytique. Il manque l'exercice (un entier `annee`), la nomenclature programme → action → activité → tâche, la structure (Direction Générale, unité opérationnelle), la distinction recettes/dépenses (un compte de classe 7 est contrôlé et bloqué comme une charge), la réservation distincte de l'engagement, le bon d'engagement (numéro saisi librement, ni tiers, ni pièces, ni édition), la liquidation comme objet, le paiement, le budget rectificatif, la clôture et le report. Voir le rapport n° 1, § 2 et § 9.

### 4.2 Majeurs

**M1 — Un même crédit libéré deux fois.** La liquidation ne regarde pas l'état du circuit de visas, et le rejet n'exclut que les engagements visés ou annulés (`RapprochementAdapter.java:298-312`). Séquence : pré-engager (engagé + M) → liquider (engagé − M, état liquidé) → rejeter (engagé − M encore). L'engagé devient négatif et le disponible est gonflé de M. Le rejet annule aussi un engagement direct déjà validé, sans habilitation.

**M2 — Liquidation d'un engagement non visé.** Un pré-engagement sans aucun visa peut être liquidé (`BudgetPlanAdapter.java:740`) : le circuit des visas ne sert à rien.

**M3 — Versions du plan incohérentes.** Le numéro de version est le nombre de plans de l'année plus un, calculé à la soumission (`BudgetPlanAdapter.java:222-223`, deux soumissions simultanées obtiennent le même) ; rien n'empêche plusieurs plans publiés pour une même année ; une version est un plan vide sans lien avec la précédente ; le réalisé s'impute sur chacune (B4). Contraire à la règle 4 du § 15 du cahier (version officielle unique).

**M4 — Budget collaboratif défectueux.**
- La génération depuis l'historique range les montants sans section sous l'UUID `00000000-…` au lieu de `null` (`BudgetCollaboratifAdapter.java:206`) : une fois consolidées, ces lignes ne recevront jamais de réalisé.
- Elle charge **toutes** les écritures de la base (`:129`) puis filtre en mémoire.
- La consolidation ajoute chaque demande approuvée comme ligne du plan, en avalant les erreurs (`:189`). Deux départements qui demandent le même compte et la même section se heurtent au contrôle d'unicité : la seconde demande est rejetée, et comme l'exception traverse le proxy transactionnel de l'adaptateur, la transaction est marquée « à annuler » ; la consolidation entière échoue à la fin (`UnexpectedRollbackException`). *Confirmé (§ 10) : 500, aucune ligne ajoutée au plan.* Les demandes ne sont jamais marquées consolidées, et les montants de départements différents ne sont pas additionnés.
- Une demande s'approuve ou se rejette quel que soit son état (`:68-71`).
- Le « département » est un UUID sans lien avec l'organigramme.

**M5 — Erreurs mal traduites.** `IllegalArgumentException` donne un 400 « requête invalide », y compris pour « crédit insuffisant », qui relève du 409. `IllegalStateException` (plan non publié, engagement déjà liquidé) et les `RuntimeException` du maker-checker donnent un 500 « erreur interne ». CLAUDE.md impose des sous-classes d'`ApiException`.

**M6 — Le budget peut bloquer la comptabilité.** L'imputation du réalisé est un `@EventListener` synchrone (`ExecuterRapprochementBudgetaireUseCase`) exécuté dans la transaction de validation de l'écriture : toute exception côté budget annule la validation comptable.

**M7 — Seuils faux et codés en dur.** Les visas de pré-engagement suivent 500 000 et 5 000 000 FCFA codés en dur (`RapprochementAdapter.java:264`, `:272`), avec des rôles (chef de service, directeur financier) absents des procédures. La table `achat_seuils_config` semée diffère de la procédure : « achat direct caisse » de 0 à 499 999 FCFA avec une pro forma, alors que la procédure fixe moins de 100 000 FCFA sans commande et trois pro forma de 100 000 à 499 999. Sa modification se fait dans le contrôleur (`AchatSeuilsRestController.java:29-45`), sans habilitation, sans maker-checker, sans date d'effet ni historique, et expose l'entité JPA.

**M8 — Trois mécanismes d'approbation parallèles.** Maker-checker, visas codés en dur, circuits déclaratifs du moteur de workflow (WF-DAF-06, WF-DG-01, WF-DG-04, WF-DAF-01) : aucun ne connaît les autres. Le paquet budget ne démarre jamais ces circuits ; seul le maker-checker tente, à chaque soumission, un circuit `WF-METIER-*` qui n'existe pas (B0).

**M9 — Deux budgets concurrents.** La comptabilité analytique a sa propre table `budgets` et ses routes, sans lien avec `budget_plan` (détail § 5).

### 4.3 Mineurs

- Ventilation imposée en douze mensualités égales, arrondies à 4 décimales alors que le FCFA n'en a pas (`BudgetPlanAdapter.java:370`) ; la réallocation retire un douzième par mois sans vérifier qu'un mois de la ligne source ne devient pas négatif (`:527-538`) ; la liste des périodes n'a pas d'`@OrderBy`, et `get(i)` suppose l'ordre.
- États d'engagement et de demande en chaînes libres, pas en énumérations.
- Numéro d'engagement saisi par l'utilisateur ; un doublon heurte la contrainte unique et sort en 500.
- Code de compte jamais vérifié contre le plan comptable.
- Réallocation d'une ligne vers elle-même acceptée.
- La journalisation ClickHouse enregistre le même acteur comme auteur et valideur (`:91-92`).
- `TestDataSeeder` tourne dans tous les profils (`@Profile("test")` commenté). Sa création de plans échoue (confirmé : « Échec création budget » pour 2017 à 2026, B0), et il recrée à chaque démarrage, sans contrôle d'existence, prévisions de trésorerie, emprunts et engagements hors bilan.
- Toute route inconnue répond 500 « erreur interne » au lieu de 404 : `GestionnaireErreursApi` ne traite pas `NoResourceFoundException`.

---

## 5. Intégrations avec les autres modules

**Aucun module ne vérifie un disponible ni ne crée d'engagement budgétaire.** La seule passerelle vers le budget est l'écriture comptable validée, saisie à la main.

| Module | Constat | Preuve | Gravité |
|---|---|---|---|
| Achats | `budgetDisponible` forcé à `true` à la création d'une expression de besoin ; l'imputation budgétaire est un texte de 10 à 100 caractères (une justification, pas un code : `605200` est refusé) | `achats/application/AchatsService.java:347-348` ; `CreerExpressionBesoinRequest.java:31-32` (vérifié) | Bloquant |
| Achats | Aucun visa budgétaire : les états `VISEE_BUDGET`, `VALIDEE_HIERARCHIE`, `APPROUVEE_DG` ne sont posés nulle part (le front appelle des routes qui n'existent pas, § 7) | `StatutExpressionBesoin.java` ; `AchatsService.java:349` | Bloquant |
| Achats | `engager-budget` ne lit aucune ligne et ne crée aucun engagement ; il est auto-validé par son auteur, qui s'inscrit lui-même comme valideur (`update(ACCEPTED, …)` avec son propre identifiant) | `AchatsService.java:739-770` (vérifié) | Bloquant |
| Achats | La réception et la liquidation n'exigent pas l'engagement : on peut recevoir et liquider sans avoir engagé, contrairement à RG-04. La liquidation est un drapeau et un statut, sans contrôle « liquidé ≤ engagé », sans écriture ni appel au budget ; ni facture fournisseur ni paiement comme objets | `AchatsService.java:875-922`, `:1074-1098` (vérifié) | Bloquant |
| Achats | Un procès-verbal « non conforme, rejeté » fait quand même passer le dossier à « réceptionné », puis liquidable | `AchatsService.java:975`, `:984` | Majeur |
| Achats | Seuils lus dans les paramètres (500 000, 15 M, 50 M) sans le palier de 100 000 ; la table `achat_seuils_config` est injectée mais jamais lue, et ses codes diffèrent de l'énumération | `AchatsService.java:62`, `:1104-1118` | Majeur |
| Comptabilité | Aucun module ne génère d'écriture (achats, paie, régies) : le réalisé n'existe que si quelqu'un saisit l'écriture | recherche dans le dépôt | Bloquant |
| Comptabilité | Aucun lien entre écriture et engagement (ni numéro, ni tiers) : double comptage ou trou selon l'ordre de la liquidation et de la validation (B4) | `RapprochementAdapter.java:182-198` | Bloquant |
| Comptabilité | La ventilation analytique se fait après la validation de l'écriture et ne réimpute pas le budget ; une ventilation sur deux axes impute 200 % (à confirmer selon le paramétrage) | `comptabilite/analytique/application/usecase/AnalytiqueUseCase.java:194-250` | Majeur |
| Analytique | Second budget (table `budgets`) et second « réalisé » recalculé autrement, sans engagé, sans habilitation ni maker-checker | `comptabilite/analytique/application/usecase/BudgetUseCase.java:66-92`, `ReportingAnalytiqueUseCase.java:469-531` | Majeur |
| Paie | La validation de la paie écrit « prête pour engagement budgétaire » dans le journal et rien d'autre : aucun engagement mensuel, aucune écriture, alors que les totaux existent | `paie/application/PaieService.java:639-666` (vérifié) | Bloquant |
| Missions, formation | Aucun module de missions ; la formation additionne des coûts estimatifs sans ligne budgétaire, sans TDR ni engagement | `formation/application/FormationService.java:72`, `:111-115` | Bloquant |
| Régies | Une dépense de régie diminue le solde sans engagement ; imputation en texte libre | `tresorerie/regies/application/RegiesTresorerieService.java:353-369` | Majeur |
| Recrutement | Les étapes « visa budgétaire » et « impact budget » des circuits RH sont de simples approbations sans donnée | `WorkflowSeeder.java:287`, `:496` | Majeur |
| Workflow | Circuits budgétaires semés mais jamais démarrés ; étape de contrôle automatique non implémentée (elle crée une tâche humaine) ; assignation par sous-chaîne qui, faute de poste correspondant, retombe sur l'initiateur ; le rejet d'un circuit ne publie aucun événement | `workflow/application/WorkflowService.java:509-530`, `:617-726` | Bloquant |
| Habilitations | Deux familles incohérentes : « Achats – Budget » est distribuée (PCA, DAF, DPD, chef du budget) mais seule `ENGAGE_BUDGET` est contrôlée ; « Budget » est contrôlée mais détenue seulement par le DG et les comptes techniques. `ENGAGE_BUDGET` est donnée aux directeurs d'unité : auto-engagement sans visa du Service du Budget | `utils/Permissions.java:93-98`, `:167-174` ; `SodepaScrapedDataSeeder.java:958-962` | Majeur |

À réutiliser pour rattacher le budget : les unités de l'organigramme et leur chemin matérialisé, l'unité déjà portée par l'expression de besoin, le filtrage par périmètre des achats (`AchatsSecurityService.java:93-153`), la hiérarchie du plan comptable OHADA (`niveau`, `parentCode`), les totaux de paie.

---

## 6. Trésorerie, financement, pilotage

Ces fonctions vivent dans le paquet `budget/` mais n'ont **aucun lien avec le budget** : la trésorerie prévisionnelle est saisie à la main (`TresorerieAdapter.java:41-49` est le seul endroit qui écrit dans `prevision_tresorerie`), sans solde d'ouverture, sans lignes budgétaires ni engagements. RF-19 et le flux F7 du cahier ne sont pas couverts.

| Gravité | Défaut | Preuve |
|---|---|---|
| Bloquant | Aucune habilitation ni maker-checker sur les 21 routes de trésorerie, financement, change, rapprochement et pilotage ; l'auteur est lu dans la requête. Un utilisateur connecté crée un emprunt ou paie une échéance (avec écriture comptable) sans autorisation. Le paquet `tresorerie/regies`, lui, applique le maker-checker | `FinancementAdapter.java:119`, `:203` ; `CreerFinancementRequest` (`utilisateurId`) |
| Bloquant | Rapprochement bancaire inopérant : il lit `releve_bancaire`, table que rien n'alimente, alors que le front saisit les relevés côté comptabilité (`releves_bancaires`) puis envoie cet identifiant. Réponse 400 « relevé introuvable » à chaque appel | `RapprochementAdapter.java:42-43` ; front `tresorerie/rapprochement.service.ts:57-75`, `:117-124` |
| Bloquant | Résultat net des KPI faux à chaque calcul : `solde(7) − solde(6)` en débit moins crédit puis valeur absolue, soit produits **plus** charges ; valeurs inventées (100 000 000, 250 000 000, 1) quand un solde est nul | `ReportingFinancierAdapter.java:29-54` (vérifié) |
| Bloquant | Paiement d'échéance sans transaction ni verrou : deux appels simultanés passent chacun le contrôle « à payer » et produisent deux écritures | `FinancementAdapter.java:203-212`, aucun `@Transactional` (vérifié) |
| Majeur | Comptabilisation des emprunts : journal mal choisi, comptes 521000/162000/661100 en dur, devise XOF au lieu de XAF, échec avalé par `catch (Exception ignored)` | `FinancementAdapter.java:229-236`, `:262-283`, `:318-326` |
| Majeur | Capital restant dû lu sur le solde **après** la première échéance impayée : un prêt neuf affiche capital moins un amortissement | `FinancementAdapter.java:400-402`, `EcheanceFinancementEntity.java:55-59` (vérifié) |
| Majeur | Échéancier : division par zéro possible (2 mois en trimestriel), durée tronquée, `double` dans le calcul, algorithme recopié deux fois, périodicité inconnue changée en mensuelle | `FinancementAdapter.java:57-181` |
| Majeur | Indicateurs qui ne mesurent pas ce qu'ils annoncent : « BFR » = encaissements clients moins décaissements fournisseurs à 30 jours ; what-if sur les comptes de personnel du plan français (63/64, en SYSCOHADA c'est 66), BFR à 15 % en dur ; runway avec un burn rate de repli à 1 000 000 ; TFT qui classe les amortissements en investissement, ignore subventions (13/14) et cessions, et reconstruit son solde d'ouverture | `TresorerieAdapter.java:168`, `:234-289` ; `PilotageAdapter.java:30-124` |
| Majeur | Écritures chargées entièrement en mémoire, plusieurs fois par appel (jusqu'à 9 `findAll()` pour les KPI) | `ReportingFinancierAdapter.java:113`, `TresorerieAdapter.java:277`, `PilotageAdapter.java:31`, `:89` |

**Écart avec la procédure financière** : rien sur les comptes bancaires et leurs co-signataires, l'autorisation des décaissements par le DG, les seuils de 100 000 et 500 000 FCFA, l'état mensuel d'endettement fournisseurs, l'échéancier fiscal. La programmation des paiements (« arbitrage ») part des prévisions saisies, pas des factures, et rompt le FIFO en sautant une facture ancienne trop lourde.

**À garder** : l'ossature financement et échéancier, ses requêtes paginées. **À jeter** : les tables `releve_bancaire` et `ligne_releve_bancaire` du paquet budget (doublon mort du rapprochement comptable) et les calculs de KPI, what-if, BFR et runway, à réécrire sur la balance SYSCOHADA. Cette partie relève du domaine trésorerie du cahier des charges ERP ; je recommande de la sortir du paquet `budget/`.

---

## 7. Front (`sodepa-frontend`)

Cinq écrans sous `/budget` (tableau de bord, plans, détail d'un plan, engagements, demandes), plus `/analytique/budgets` et les actions budgétaires de l'écran des achats. Le mode démonstration est désactivé (`assets/config.json:7`) : les écrans parlent au vrai backend. Le code du dossier `budget` est propre (aucun `any`, composants de taille raisonnable) ; les défauts sont fonctionnels.

| Gravité | Défaut | Preuve |
|---|---|---|
| Bloquant | Le circuit ne démarre pas sur le vrai backend : aucune route `validate_or_reject` n'est appelée, aucune file de demandes en attente, et les messages annoncent des succès (« Plan budgétaire créé », « Engagement routé dans le workflow », « Réallocation effectuée ») pour des demandes seulement soumises. Le mode démonstration crée tout immédiatement et masque le problème | `plan-dialog.component.ts:86`, `assets/i18n/fr.json:1225`, `:1281`, `:1287` ; `core/http/in-memory/budget.store.ts:231-254` |
| Bloquant | Le formulaire d'engagement a un contrôle `sectionId` mais aucun champ : il envoie toujours `null`. Un poste ventilé sur une section ne peut pas être engagé depuis l'écran | `features/budget/engagements/engagements.component.ts:177`, `:203` (vérifié : aucun champ dans le gabarit) |
| Bloquant | L'écran des achats appelle six routes qui n'existent pas côté backend : `viser-budget`, `valider-hierarchie`, `carence-stock`, `approuver-dg`, `rejeter` (expressions de besoin), `signer-dg` (dossiers). Le « Visa Budget » ne peut donc jamais aboutir | `features/achats/achats.service.ts:50-128` contre `achats/presentation/rest/AchatsRestController.java` (vérifié) |
| Bloquant | Données fictives présentées comme réelles : l'audit affiche toujours des événements de démonstration (la route `clickhouse/workflow-events` n'existe pas), la boîte de réception du workflow affiche des tâches fictives (dont un engagement de 8,5 M FCFA) et annonce réussie une décision en erreur | `features/audit/audit.service.ts:12`, `:134-141` ; `features/workflow/workflow.service.ts:789-794`, `:809-813` (vérifié) |
| Majeur | Aucune garde de route ni masquage d'action ; les onze habilitations budget du backend manquent dans `APP_PERMISSIONS` (l'écran d'attribution ne peut pas les accorder) ; le même utilisateur crée, soumet et approuve un plan | `app.routes.ts:206-212` ; `core/auth/auth.model.ts:33-62` |
| Majeur | Identité envoyée dans les requêtes (`utilisateurId`, `userId`, `responsableId`), que le backend croit sur parole | `features/budget/budget.service.ts:82-84`, `:104-126`, `:268-270` |
| Majeur | Disponible affiché faux après liquidation (le backend ne crédite pas le réalisé) ; tableau de bord calculé sur les 50 premiers plans, versions et statuts additionnés ; taux de consommation plafonné à 100 % (un dépassement ne se voit pas) | `budget.model.ts:281-292` ; `tableau-bord.component.ts:59-71` |
| Majeur | Un GET en échec casse le rendu de quatre écrans (`resource.value()` lu sans tester `hasValue()`, qui lève une erreur dans Angular 21) ; plusieurs actions avalent leurs erreurs | `tableau-bord.component.ts:45-57`, `detail-plan.component.ts:111-113`, `engagements.component.ts:122-124`, `demandes.component.ts:97` |
| Majeur | Seuils d'achat codés en dur dans le gabarit (sans le palier de 100 000 FCFA), alors que la table paramétrable `GET /api/achats/seuils` n'est jamais lue ; imputation budgétaire en texte libre, pré-remplie (`6051-PHARMA-VET`) | `gestion-achats.component.html:120-123`, `gestion-achats.component.ts:228`, `:367` |
| Mineur | `api.json` ne déclare ni les lectures budget ni les `validate_or_reject` ; commentaires « absent du contrat » périmés ; huit méthodes de service sans écran (pré-engagement, visas, cadrage, génération, consolidation) ; exercice N+1 absent des filtres | `api.json:1475-2004` ; `budget.service.ts:222-255` ; `budget-collaboratif.service.ts:71-138` |

Absents de l'interface : visa et montant réservé, bon d'engagement, validation maker-checker, réallocation par niveaux, nomenclature programmatique, rapports et exports, saisie en grille et import (RF-130 à RF-136).

---

## 8. Couverture des exigences

« Couvert » : présent et correct. « Partiel » : une partie existe. « Faux » : présent mais donne un résultat erroné ou contourne la règle. « Absent » : rien dans le code.

### 8.1 Procédure de la SODEPA (rapport n° 1)

| Besoin | État | Constat |
|---|---|---|
| EX-01 Visa budgétaire = réservation avec contrôle bloquant | Faux | Le pré-engagement réserve, mais sans verrou (B3), avec des visas codés en dur étrangers à la procédure (M7) et franchissables par un seul utilisateur (B5) |
| EX-02 Bon d'engagement (tiers, objet, nature, pièces) | Absent | Un engagement = numéro libre, description, montant |
| EX-03 Circuit du BE selon la structure et le seuil | Absent | Ni structure, ni circuit SB → SDFBM → DAF → DG ou CSAFC → DUO |
| EX-04 Pièces requises par type de dépense | Absent | — |
| EX-05 Édition du BE et du bon de commande | Absent | — |
| EX-06 Situation d'exécution mise à jour à chaque mouvement | Faux | Agrégats sur la ligne, mais réalisé faux (B4) et crédit libérable deux fois (M1) |
| EX-07 Liquidation sur pièces, transmission au paiement | Faux | Simple changement d'état ; libère le crédit sans enregistrer le réalisé |
| EX-08 Engagements venant de la paie, des missions, des achats | Absent | L'engagement des achats n'impute aucune ligne (§ 5) |
| EX-09 Recettes imputées au budget des produits | Absent | Classe 7 traitée comme une charge |
| EX-10 Engagement d'une unité sur ses crédits | Absent | Pas de structure |
| EL-01 à EL-03 Campagne, nomenclature programmatique, CDMT | Absent | — |
| EL-04, EL-05 Avant-projets par unité, consolidation | Faux | « Demandes » par département libre ; consolidation défectueuse (M4) |
| EL-06 Versions successives | Faux | Versions sans lien, plusieurs plans publiés possibles (M3) |
| EL-07 Pièces de la campagne | Absent | — |
| EL-08 Ouverture des crédits à la décision exécutoire | Absent | La publication vaut ouverture, sans date ni acte |
| EL-09 Annexes (trésorerie, financement) | Absent | Trésorerie saisie à part (§ 6) |
| EL-10 Import Excel du budget approuvé | Absent | — |
| Réallocation à trois niveaux (§ 6.3 du rapport n° 1) | Faux | Transfert simple, un seul niveau de validation, inopérant (B1) |
| Rapports quotidien, mensuels (5 et 10), trimestriel | Absent | — |
| Seuils 100 000 / 500 000 / 15 M / 50 M | Faux | Table semée avec d'autres valeurs, front codé en dur (M7, § 7) |

### 8.2 Cahier des charges budget

| Exigences | État | Constat |
|---|---|---|
| § 4.1 Paramétrage (RF-01 à RF-07) | Partiel | Année, compte, section ; ni périodes paramétrables, ni types de budget, ni règles de contrôle, ni enveloppes |
| § 4.2 Élaboration (RF-10 à RF-19) | Partiel | Demandes, cadrage, génération depuis l'historique ; ventilation linéaire imposée ; ni grille, ni import, ni masse salariale, ni investissement, ni trésorerie dérivée |
| § 4.3 Validation (RF-20 à RF-26) | Partiel | Quatre états ; ni circuit à plusieurs niveaux, ni notification, ni délégation, ni révision formalisée |
| § 4.4 Exécution (RF-30 à RF-36) | Faux | Trois états calculés mais faux ; aucun contrôle branché aux achats ; ni report, ni pluriannuel, ni alerte |
| § 4.5 Suivi et prévision (RF-40 à RF-46) | Absent | — |
| § 4.7 Reporting (RF-60 à RF-65) | Absent | Tableau de bord du front calculé de travers (§ 7) |
| § 4.8 Clôture (RF-70 à RF-72) | Absent | — |
| § 4.9 Organisation (RF-80 à RF-88) | Absent | Aucun lien avec l'organigramme |
| § 14.1 Contrôle avancé (RF-90 à RF-98) | Faux | Pas d'atomicité (RF-95), pas de libération par réception ou facture (RF-34) |
| § 14.2 Maker-checker, dérogation (RF-100 à RF-106) | Faux | L'auto-validation viole RF-100 (et contourne la règle dans les achats) ; circuit budget inutilisable |
| § 14.3 Référentiels (RF-110 à RF-114) | Absent | — |
| § 14.4 Traçabilité (RF-120 à RF-127) | Partiel | `audit_trail` sans valeurs avant/après, auteur déclaré par le client |
| § 14.5 Imports et ergonomie (RF-130 à RF-136) | Absent | — |
| § 14.7 Trésorerie (RF-150 à RF-154) | Absent | § 6 |
| § 14.8 API (RF-160 à RF-166) | Absent | Ni route de consultation du disponible, ni idempotence |
| § 15 Règles 1 (pas de double comptage), 2 (réservation atomique), 4 (version unique), 5 (pas de modification silencieuse), 10 (droits) | Faux | B4, B3, M3, B5 |
| § 18 RNF-01, RNF-02 (cohérence, idempotence) | Faux | B3 ; aucune clé d'idempotence |

---

## 9. Recommandation

### 9.1 Si le module doit servir avant sa refonte

Correctifs localisés qui le rendent cohérent, sans le rendre conforme à la procédure :

Les trois premiers points valent pour tout le dépôt et sont à traiter avant tout le reste.

1. Ne démarrer le circuit `WF-METIER-*` que si sa définition existe, ou isoler ce démarrage dans sa propre transaction (`REQUIRES_NEW`) (B0).
2. Créer le changelog Liquibase de `maker_checker_requests` ; y supprimer la contrainte `CHECK` sur `entity_name` (l'énumération Java suffit) ou la remplacer par la liste complète, à tenir à jour à chaque ajout (B7).
3. Compléter les habilitations des comptes techniques à chaque démarrage dans `UserSeeder`, pas seulement à leur création (B5).
4. N'enregistrer la ligne budgétaire qu'une fois (garder la cascade depuis le plan et retirer le second `save`), dédoublonner les données existantes, ajouter l'unicité (plan, compte, section) en base (B8).
5. Faire ignorer les champs inconnus par l'`ObjectMapper` de `GeneralConfig`, ou ne plus mettre l'identifiant dans le contenu de la demande (B9).
6. Retirer `@GeneratedValue` de `BudgetEngagementEntity` et `BudgetReallocationEntity` (B1).
7. Supprimer l'auto-validation, dans le budget comme dans les achats ; rendre 202 et `SubmissionOutput` ; ajouter les routes `/pending` (`findAllToValideByOther`) et l'écran de validation (B2).
8. Verrouiller la ligne budgétaire (`@Lock(PESSIMISTIC_WRITE)`) à l'engagement, au pré-engagement et à la réallocation (B3).
9. Choisir une seule source du réalisé : la liquidation fait passer le montant d'engagé à réalisé, et l'écoute des écritures est suspendue (B4, M6).
10. Interdire le rejet d'un engagement liquidé et la liquidation d'un engagement non visé (M1, M2).
11. Contrôler une habilitation dans chaque méthode d'adaptateur, lire l'utilisateur dans le jeton, remplacer `roleApprobateur` par les rôles réels de l'appelant (B5) ; reporter les permissions dans `APP_PERMISSIONS`.
12. Remplacer `IllegalStateException` et `RuntimeException` par les sous-classes d'`ApiException` ; traiter `NoResourceFoundException` en 404 (M5).
13. Côté front : champ section dans le formulaire d'engagement, retrait des données fictives (audit, workflow), messages conformes au maker-checker.

### 9.2 Reconstruction recommandée

Le cœur est à reconstruire sur le modèle du rapport n° 1 (§ 9 et lots B1 à B5), en tirant de ce bilan les principes suivants :

- **Journal de mouvements immuable** (ouverture, réservation, engagement, liquidation, paiement, réallocation, libération) ; les montants de la ligne en sont des agrégats recalculables, mis à jour sous verrou.
- **États distincts et typés** : réservé (visa), engagé (BE), liquidé, payé ; des énumérations, pas des chaînes.
- **Un seul mécanisme par usage** : maker-checker pour les référentiels (nomenclature, seuils, structures) ; moteur de workflow pour les circuits à plusieurs étapes (visa, BE, réallocation), après avoir rendu exécutable son étape de contrôle automatique. Les visas codés en dur disparaissent.
- **Structure et périmètre** : chaque ligne appartient à une structure de l'organigramme ; les habilitations s'appliquent au périmètre de l'utilisateur.
- **Réalisé alimenté par la liquidation** ; les écritures comptables servent au rapprochement de contrôle, hors de la transaction comptable.
- **Recettes séparées des dépenses**, avec leur propre suivi.
- **Seuils en paramètres datés** (la table existante, corrigée et historisée).
- **Un seul budget** : le budget analytique devient un axe d'analyse du budget, pas une seconde table de crédits.
- **Trésorerie et financement sortis du paquet `budget/`**, rattachés au domaine trésorerie.
- **Tests des invariants financiers** (deux engagements concurrents, facture partielle, avoir, rejet après liquidation) : aujourd'hui, aucun test ne garde ces règles.

À reprendre de l'existant : la structure de la table de seuils, l'écoute des écritures validées (comme contrôle), la piste d'audit (enrichie), la pagination, les validations des requêtes, l'ossature financement et échéancier.

### 9.3 Essais

Les essais prévus ici ont été menés le 9 novembre ; résultats au § 10.

---

## 10. Vérification par appels réels

Menée le 9 novembre au soir sur l'API démarrée (code au commit `9d03fc9`, inchangé depuis), avec `admin_maker`, `admin_checker` et deux employés sans aucune habilitation. Les constats viennent des réponses HTTP, du journal du serveur et de la base.

**Méthode.** Données d'essai sur l'exercice 2031, libellés et numéros préfixés « ESSAI ». Ce que B0 empêche de créer a été reproduit autrement : plans insérés en base, demandes maker-checker insérées en base puis validées par l'API. Pour atteindre B1, deux aménagements temporaires, annulés en fin d'essai et vérifiés : quatre habilitations budget accordées aux comptes admin, et deux valeurs ajoutées à la contrainte de B7. Les lignes dédoublées par B8 ont été ramenées à une seule copie pour essayer le pré-engagement, et B1 a été essayé avec un contenu sans `id` pour passer B9.

| Point | Résultat | Constat |
|---|---|---|
| B0 | Confirmé | `POST /api/budget/plans` et `POST /api/comptabilite/journaux/init_create` : 500, aucune demande enregistrée ; au journal, « Définition introuvable avec le code: WF-METIER-… » puis `UnexpectedRollbackException`. Au démarrage du 8 novembre, `TestDataSeeder` échoue de même sur les dix plans et sur les journaux : la base ne contient ni plan, ni journal, ni écriture |
| B7 (nouveau) | Constaté | Contrainte `CHECK` de `maker_checker_requests` : 11 valeurs acceptées sur 33 ; insertion d'une demande `BUDGET_PLAN` refusée par la base |
| B8 (nouveau) | Constaté | 5 lignes ajoutées → 10 lignes et 120 périodes en base ; pré-engagement sur ces lignes : 500 « 2 results were returned » ; réalisé du compte 605 imputé sur les deux copies (140 000 au niveau du plan pour 70 000 sur la ligne) |
| B9 (nouveau) | Constaté | Validation d'un engagement et d'une réallocation par `admin_checker` : 400 « Unrecognized field "id" » |
| B1 | Confirmé | Contenu sans `id` : 500, `ObjectOptimisticLockingFailureException`, engagé et crédits inchangés, demande restée en attente |
| B2, auto-vote | Confirmé | `admin_maker` validant sa propre demande : 500 (`RuntimeException` « A checker cannot vote on their own request ») |
| B3 | Confirmé, plus grave | 10 pré-engagements simultanés de 600 000 sur 1 000 000 : 4 acceptés (2 400 000), ligne à 600 000 engagés |
| B4, liquidation | Confirmé | Engagé −100 000, réalisé inchangé |
| B4, avoir | Confirmé | Facture de 50 000 puis avoir de 20 000 sur le compte 605 : réalisé 70 000 au lieu de 30 000 |
| B5 | Confirmé | Un employé sans habilitation ajoute les lignes, soumet et approuve le plan, pré-engage, franchit seul les trois visas d'un engagement de 6 000 000, modifie un seuil d'achat, crée un emprunt. La piste d'audit enregistre l'identifiant inventé passé en paramètre. Les comptes admin reçoivent 403 sur les routes budget protégées |
| B6, recette | Confirmé | Pré-engagement accepté sur le compte 701 |
| Ligne sans section | Confirmé | Pré-engagement refusé (400, `sectionId` obligatoire) |
| M1 | Confirmé | Pré-engager, liquider, rejeter : engagé −300 000, disponible 1 300 000 pour 1 000 000 prévus |
| M2 | Confirmé | Liquidation acceptée sans aucun visa (état `CONVERTED_TO_REAL / PRE_ENGAGEMENT`) |
| M4 | Confirmé | Consolidation de deux demandes sur le même compte et la même section : 500, `UnexpectedRollbackException`, aucune ligne ajoutée |
| M5 | Confirmé | Crédit insuffisant : 400 ; deuxième liquidation : 500 ; auto-vote : 500 |
| M7 | Confirmé | Seuil d'achat modifié par un employé sans habilitation (200) |
| Ventilation | Confirmé | 1 000 000 → 83 333,3333 par mois, 83 333,3337 en décembre |
| § 6, KPI | Confirmé | Avec 30 000 de charges nettes et aucun produit : résultat net +30 000 (une perte affichée en bénéfice), capitaux propres 100 000 000 et ROA 100 inventés |
| § 6, capital restant dû | Confirmé | Prêt de 100 000 000 sans aucune échéance payée : 98 585 051,85 affichés |
| § 6, paiement d'échéance | Confirmé | Six paiements simultanés de la même échéance : six acceptés, six tracés |
| § 6, rapprochement | Confirmé | Relevé saisi côté comptabilité : 400 « Relevé bancaire introuvable » |
| § 7, route « viser-budget » | Confirmé | Absente ; répond 500 et non 404, comme toute route inconnue |

**Non essayé.** L'auto-validation (B2), inatteignable tant que B0, B7, B9 et les habilitations manquantes la précèdent ; l'imputation au compte exact et sur plusieurs plans d'une même année, le double comptage entre engagement et écriture (B4) ; M3, M6 ; la génération depuis l'historique (M4) ; les achats, que B0 et B7 bloquent dès l'expression de besoin ; la paie ; le front.

**Données d'essai laissées en base** : deux plans 2031 « ESSAI P1 / P2 » et leurs lignes, les engagements `ESSAI-*`, deux demandes collaboratives 2031, un journal HA « ESSAI journal », deux écritures `ESSAI-FAC` et `ESSAI-AVOIR`, un emprunt « ESSAI prêt » dont la première échéance est marquée payée, les traces d'audit correspondantes. Le journal HA occupe ce code : au prochain démarrage, `TestDataSeeder` le réutiliserait au lieu de créer le sien.
