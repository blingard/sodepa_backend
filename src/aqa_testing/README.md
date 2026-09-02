# Sodepa — AQA Playwright (API + UI)

Socle d'automatisation des tests de l'ERP Sodepa : tests REST contre le backend
Spring Boot (`sodepa_backend`, port 8082) et tests d'interface Playwright.

**Couverture actuelle : 142 routes REST, 475 tests API + 4 tests UI.**

## Prérequis

- Node.js 18+ (testé avec Node 26)
- Le backend démarré (`mvnw spring-boot:run` à la racine de `sodepa_backend`)
- Les dépendances du backend actives : PostgreSQL (5433), Redis, Keycloak (8070, realm `sodepa`)
  — voir `docker-compose.yml`

## Installation

```bash
npm install
npm run install:browsers   # télécharge Chromium
cp .env.example .env       # puis renseigner les comptes de test
```

## Lancer les tests

| Commande | Effet |
| --- | --- |
| `npm test` | tous les projets |
| `npm run test:api` | tests REST uniquement (aucun navigateur) |
| `npm run test:ui` | tests d'interface (dépend du projet `ui-setup`) |
| `npm run test:headed` | tests UI avec navigateur visible |
| `npm run test:debug` | inspecteur Playwright |
| `npm run report` | ouvre le rapport HTML |
| `npm run typecheck` | vérification TypeScript sans exécution |

Filtrer un fichier ou un titre :

```bash
npx playwright test tests/api/financement.spec.ts
npx playwright test -g "pagination"
```

## Couverture par domaine

| Fichier | Domaine backend | Tests |
| --- | --- | --- |
| `security.spec.ts` | chaîne de sécurité, toutes routes protégées | 144 |
| `referentiel.spec.ts` | banques, comptes, tiers, journaux | 44 |
| `tresorerie.spec.ts` | trésorerie, change, arbitrage, pilotage | 35 |
| `financement.spec.ts` | financements, simulation, hors-bilan, KPI | 34 |
| `budget-plan.spec.ts` | plans budgétaires et engagements | 30 |
| `analytique.spec.ts` | axes, sections, ventilations, budgets, clés, reporting | 30 |
| `budget-collaboratif.spec.ts` | demandes, cadrage, consolidation, workflow | 26 |
| `immobilisation.spec.ts` | immobilisations et amortissements | 25 |
| `rapprochement.spec.ts` | relevés bancaires et clôture d'exercice | 22 |
| `users.spec.ts` | utilisateurs et permissions | 21 |
| `ecriture.spec.ts` | écritures, TVA, workflow de validation | 19 |
| `reporting.spec.ts` | états OHADA (bilan, TFT, TVA, FEC…) | 18 |
| `auth.spec.ts` | login, refresh, logout, sessions | 14 |
| `audit.spec.ts` | audit ClickHouse et piste d'audit métier | 11 |
| `surface.spec.ts` | garde-fou de couverture (voir plus bas) | 2 |

Types de cas couverts pour chaque endpoint : chemin nominal, pagination et tri,
filtres, validation de chaque champ contraint (`@NotBlank`, `@NotNull`,
`@Positive`, `@Email`, énumérations), ressource inexistante, identifiant
malformé, paramètre obligatoire absent, et refus d'accès sans jeton.

## Structure

```
.
├── playwright.config.ts        # 3 projets : api, ui-setup, ui-chromium
├── .env / .env.example         # URLs cibles, comptes, garde-fou destructif
├── src/
│   ├── config/env.ts           # lecture typée de l'environnement
│   ├── api/
│   │   ├── endpoints.ts        # registre des 142 routes du backend
│   │   ├── http.ts             # appels bruts + familles de statuts attendus
│   │   ├── clients/            # un client par contrôleur REST
│   │   └── models/             # types des payloads (PageRecord, outputs)
│   ├── ui/pages/               # Page Objects (BasePage, LoginPage, DashboardPage)
│   ├── fixtures/               # injection : contextes HTTP authentifiés, clients, Page Objects
│   ├── data/                   # comptes et générateurs de données valides
│   └── utils/                  # assertions réutilisables, log
└── tests/
    ├── api/                    # 15 fichiers, un par domaine fonctionnel
    ├── ui/                     # login, dashboard
    └── setup/auth.setup.ts     # session UI sérialisée dans .auth/user.json
```

## Comment ça s'authentifie

`POST /api/auth/login` est public (cf. `SecurityConfig.PUBLIC_ENDPOINTS`) et renvoie
la réponse Keycloak (`access_token`, `refresh_token`, …). La fixture `session`
l'appelle **une fois par worker** ; `apiContext` en dérive un `APIRequestContext`
qui porte l'en-tête `Authorization: Bearer …` sur tous les appels. La fixture
`anonContext` reste sans jeton pour tester les refus d'accès.

## Le garde-fou de couverture

`surface.spec.ts` lit les sources Java de `../main/java`, extrait toutes les
routes déclarées par les `@*Mapping` et vérifie dans les deux sens qu'elles
correspondent au registre `src/api/endpoints.ts`. Une route ajoutée au backend
sans test fait échouer ce fichier ; une entrée du registre pointant vers une
route supprimée aussi. Ce test ne sollicite pas le backend et tourne en < 1 s.

**Ajouter une route au backend implique donc :** l'ajouter à `endpoints.ts`,
étendre le client du domaine, puis écrire ses cas dans le `.spec.ts` du domaine.

## Tests destructifs

Certains endpoints sont irréversibles ou polluants : clôture d'exercice,
réévaluation de devises, suppression de compte, bascule d'activation d'un
journal, changement de mot de passe, rapprochement automatique. Ces tests
existent mais sont en `skip` tant que `RUN_DESTRUCTIVE=true` n'est pas positionné
dans le `.env`. **À n'activer que sur un environnement jetable.**

## Conventions

- Les tests sont indépendants ; ceux qui ont besoin de données existantes se
  mettent en `skip` explicite avec un motif lisible plutôt que d'échouer.
- Les assertions de statut passent par `expectStatusIn`, qui remonte le corps de
  la réponse dans le message d'échec — indispensable pour distinguer un 500
  fonctionnel d'un 400 de validation.
- Les familles de statuts acceptés (`BAD_REQUEST_STATUSES`, `NOT_FOUND_STATUSES`)
  sont larges à dessein : `GestionnaireErreursApi` ne normalise pas encore tous
  les cas, et les tests documentent le comportement réel sans le figer trop tôt.
- Les assertions de pagination passent par `expectValidPage` pour couvrir en une
  fois `content`, `numberOfElements`, `first`/`last`, `totalPages`.
- Les traces, captures et vidéos ne sont conservées qu'en cas d'échec.

## État des tests UI

Aucun front n'est présent dans le dépôt : les specs de `tests/ui/` sont écrites
mais marquées `describe.skip`, et `auth.setup.ts` se met en `skip` si
`UI_BASE_URL` est injoignable. Pour les activer : renseigner `UI_BASE_URL`,
retirer les `.skip`, puis aligner les locators des Page Objects sur le vrai DOM
(de préférence via des `data-testid`).
