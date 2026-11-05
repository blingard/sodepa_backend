# Rapport d'Analyse d'Architecture Technique Détaillée — ERP SODEPA
**Document d'Architecture Technique (DAT) Exhaustif**  
*Date d'inspection et d'analyse : 25 octobre 2026*  
*Auteur : Antigravity (Architecte Logiciel Senior)*  
*Statut : Document de Référence Technique issu de l'Inspection du Code Source*

---

## 1. Introduction et Périmètre de l'Analyse

Le présent rapport formalise l'architecture technique détaillée de l'**ERP SODEPA** (Société de Développement et d'Exploitation des Productions Animales). Il résulte d'une inspection exhaustive et méthodique des répertoires réels du projet :
* **Backend Spring Boot** : `C:\Users\GLC\Documents\sodepa\erp`
* **Frontend Angular SPA** : `C:\Users\GLC\Documents\sodepa\sodepa-frontend`

Ce document sépare scrupuleusement les **faits observés dans le code**, les **hypothèses techniques déduites** et les **recommandations d'architecture d'entreprise**.

---

## 2. Synthèse des Technologies et Versions Identifiées

### 2.1. Backend Java / Spring Boot
* **Langage & Runtime** : Java `21` (LTS, bytecode 65).
* **Framework Principal** : Spring Boot `4.1.0` (sur Spring Framework 7 / Jakarta EE).
* **Style Architectural Réel** : **Monolithe Modulaire** en **Architecture Hexagonale (Ports et Adaptateurs / Clean Architecture)**.
  * Découpage standardisé par module : `presentation` (contrôleurs REST, DTOs de requête/réponse), `application` (Use Cases métier, modèles métier, ports), `infrastructure` (adaptateurs, entités JPA, repositories Spring Data, listeners AMQP).
* **Couche Web & Sécurité** :
  * `spring-boot-starter-webmvc` (Spring MVC bloquant avec servlets).
  * `spring-boot-starter-security-oauth2-resource-server` (Validation des jetons JWT via clés publiques JWKS Keycloak).
  * `keycloak-admin-client` `26.0.0` (Client d'administration REST pour le provisioning de comptes).
* **Persistance Relationnelle (OLTP)** :
  * `spring-boot-starter-data-jpa` (Hibernate ORM 6/7, JPA 3.1).
  * Pilote JDBC PostgreSQL `org.postgresql.Driver` (`postgresql:42.x`).
  * `spring-boot-starter-liquibase` : Gestionnaire de versioning de schéma avec 8 changelogs chaînés.
  * Pool de connexions : HikariCP (par défaut Spring Boot).
* **Analytique & Audit (OLAP)** :
  * Pilote `clickhouse-jdbc` `0.6.3` (Base orientée colonnes ClickHouse 26.7).
  * Pilote `duckdb_jdbc` `1.0.0` (Moteur SQL embarqué dans le processus JVM sur `sodepa_audit.db`).
* **Messagerie & Cache** :
  * `spring-boot-starter-amqp` (Spring AMQP / RabbitMQ 3.13).
  * `spring-boot-starter-data-redis` (Client Redis via `StringRedisTemplate`).
* **Stockage Fichiers (S3)** :
  * `minio` `8.6.0` (SDK Java officiel MinIO compatible AWS S3).
* **Documentation & Métriques** :
  * `springdoc-openapi-starter-webmvc-ui` `3.0.0` (`/swagger-ui.html`, `/v3/api-docs`).
  * `spring-boot-starter-actuator` (`/actuator/health`, `/actuator/info`).
* **Gestion des Secrets** :
  * `VaultEnvironmentPostProcessor` : Client HTTP natif Java (`java.net.http.HttpClient`) interrogeant HashiCorp Vault avant l'initialisation du contexte Spring.

### 2.2. Frontend Web Angular SPA
* **Framework Principal** : Angular `21.0.8`.
* **Moteur de Détection** : `provideZonelessChangeDetection()` (Architecture moderne sans Zone.js).
* **Bibliothèques UI & Styles** :
  * Angular Material `21.0.6` & Angular CDK `21.0.6`.
  * Tailwind CSS `4.1.18` (intégré via `@tailwindcss/postcss`).
  * Icônes : Iconify (`@iconify-json/solar`, `@iconify-json/tabler`).
* **Routage & Asynchronisme** :
  * Angular Router avec chargement à la demande (`loadChildren` et `loadComponent`).
  * RxJS `7.8.2`, TypeScript `5.9.3`.
* **Internationalisation** : `@ngx-translate/core` `17.0.0` (Français `fr` et Anglais `en`).
* **Runtime de Déploiement** : Image Docker basée sur `nginx:1.27-alpine` avec génération dynamique de configuration au boot (`docker-entrypoint.sh` via `envsubst`).

---

## 3. Structure Technique Détaillée du Backend

### 3.1. Couche d'Exposition (Présentation REST)
Le backend expose **25 contrôleurs REST Spring MVC** totalisant plus de **140 méthodes HTTP** :

| Domaine / Préfixe | Contrôleurs Significatifs | Rôle Technique & Exemples de Routes |
| :--- | :--- | :--- |
| **Authentification & Sessions** (`/api/auth`) | `AuthenticationRestController` | Mire applicative, renouvellement et révocation :<br>• `POST /api/auth/login` (relais vers Keycloak)<br>• `POST /api/auth/refresh`<br>• `GET /api/auth/sessions`, `DELETE /api/auth/sessions/{id}` |
| **Audit & Idempotence** (`/api/auth/audit`, `/api/audit`) | `AuditRestController`, `AuditTrailRestController` | Consultation des traces :<br>• `GET /api/auth/audit/activities` (via DuckDB)<br>• `GET /api/auth/audit/clickhouse/transactions` (SQL fédéré) |
| **Comptabilité Générale** (`/api/comptabilite`, `/api/v1/caccounting`) | `EcritureRestController`, `CompteRestController`, `JournalRestController`, `TiersRestController`, `BanqueRestController`, `ClotureRestController`, `ReportingRestController` | Gestion conforme SYSCOHADA :<br>• `POST /api/comptabilite/ecritures` (Validation équilibre D/C)<br>• `GET /api/v1/caccounting/compte`<br>• `POST /api/comptabilite/cloture/initiate`<br>• `GET /api/comptabilite/reporting/balance` |
| **Comptabilité Analytique** (`/api/comptabilite/analytique`) | `AnalytiqueRestController`, `CleRepartitionRestController`, `BudgetRestController`, `ReportingAnalytiqueRestController` | Ventilation et centres de coûts :<br>• `POST /api/comptabilite/analytique/axes`<br>• `POST /api/comptabilite/analytique/cles/lignes/{id}/appliquer/{cleId}` |
| **Gestion Budgétaire** (`/api/budget`) | `BudgetPlanRestController`, `BudgetCollaboratifRestController`, `EngagementWorkflowRestController` | Élaboration et suivi d'engagements :<br>• `POST /api/budget/plans`<br>• `POST /api/budget/collaboratif/cadrage`<br>• `POST /api/budget/engagements/workflow/valider` |
| **Trésorerie & Financement** (`/api/tresorerie`, `/api/financement`) | `TresorerieRestController`, `FinancementRestController`, `ChangeHedgingRestController`, `RapprochementBancaireRestController` | Liquidités et emprunts :<br>• `POST /api/tresorerie/previsions`<br>• `POST /api/tresorerie/change/couverture`<br>• `POST /api/financement` |
| **Immobilisations** (`/api/v1/immobilisations`) | `ImmobilisationRestController` | Fiches d'actifs et amortissements :<br>• `POST /api/v1/immobilisations/init_create`<br>• `POST /api/v1/immobilisations/init_amortir` |
| **Pilotage Stratégique** (`/api/reporting`) | `PilotageStrategiqueRestController` | États financiers calculés à la demande :<br>• `GET /api/reporting/tft` (Tableau des Flux de Trésorerie OHADA)<br>• `GET /api/reporting/runway` (Burn rate & autonomie) |
| **Administration Utilisateurs** (`/api/v1/users`) | `UserRestController` | Gestion des comptes et habilitations :<br>• `POST /api/v1/users/init_create`<br>• `PUT /api/v1/users/change_photo` (téléversement MinIO) |

### 3.2. Filtres, Intercepteurs et Gestion des Erreurs
1. **Filtre `JwtAuthenticationFilter` (OncePerRequestFilter)** :
   * Exige la présence obligatoire de l'en-tête `X-Correlation-Id` sur toutes les requêtes sécurisées (renvoie `400 Bad Request` en son absence).
   * Valide le jeton JWT avec `JwtDecoder` (clés publiques JWKS de Keycloak).
   * Extrait les rôles depuis le claim `realm_access.roles` (préfixés `ROLE_*`).
   * Récupère les permissions métier en base PostgreSQL via `UserManagementEnginePort`.
   * Enregistre un `CurrentUserAuthenticationToken` dans le `SecurityContextHolder`.
2. **Intercepteur `EndpointRateLimitInterceptor` (HandlerInterceptor)** :
   * Applique les quotas de trafic définis dans `application.yml` via des compteurs atomiques Redis (`EndpointThrottleService`).
   * Comportement configurable en cas de panne de Redis : politique `FAIL_CLOSED` (sécurité prioritaire).
3. **Intercepteur `TrackingUserActionsFilter` & `ContentCachingFilter`** :
   * Enveloppe la requête et la réponse pour lire leur contenu sans consommer le flux d'entrée.
   * Masque les données sensibles (champs `password`, `token`, `secret`).
   * Mesure le temps d'exécution en millisecondes et publie un `RequestTrackingEvent`.
4. **Gestionnaire Global `GestionnaireErreursApi` (`@RestControllerAdvice`)** :
   * Traduit les exceptions en réponses JSON standardisées (`Problem Details`).
   * Gère les erreurs métier : `AccesRefuseException` (403), `RessourceIntrouvableException` (404), `ConflitMetierException` (409), `RequeteInvalideException` (400), `AuthentificationIndisponibleException` (503).

### 3.3. Couche Métier (Clean Architecture & Use Cases)
* **Découplage strict** : Les contrôleurs Spring ne contiennent aucune règle métier. Ils délèguent immédiatement à des **Use Cases unitaires** (annotés `@UseCase` ou `@Service`).
* **Volume identifié** : Plus de **45 Use Cases** dédiés (ex. `CreerBudgetPlanUseCase`, `SoumettrePreEngagementUseCase`, `ValiderEtapeWorkflowUseCase`, `GenererTftOhadaUseCase`, `SimulerAmortissementUseCase`).
* **Gestion transactionnelle** : Gestion fine avec `@Transactional(readOnly = true/false)` sur les méthodes de Use Cases.
* **Moteur Transversal Maker-Checker (`com.sodepa.erp.share`)** :
  * Implémente la règle des 4 yeux : une entité sensible créée passe au statut `SUBMITTED`.
  * La validation (`VALIDATED`) ou le rejet (`REJECTED`) ne peut être effectué que par un agent distinct (`checkerId != makerId`).
  * Entité d'audit dédiée : `MakerCheckerRequestEntity` stockée dans PostgreSQL.

---

## 4. Persistance et Modèle de Données

### 4.1. Modèle Relationnel PostgreSQL (`sodepa`)
La base PostgreSQL principale contient **33 entités JPA réparties selon les changelogs Liquibase** :
* `db-changelog-1.0.yaml` : `comptes` (Plan comptable OHADA révisé).
* `db-changelog-2.0.yaml` : `journaux`, `tiers`, `ecritures`, `lignes_ecriture`, `immobilisations`, `releves_bancaires`, `lignes_releve_bancaire`.
* `db-changelog-3.0.yaml` : `banques`.
* `db-changelog-4.0.yaml` : `axes_analytiques`, `sections_analytiques`, `ventilation_analytique`.
* `db-changelog-5.0.yaml` : `cles_repartition`, `details_cle_repartition`, `budgets`.
* `db-changelog-6.0.yaml` : `budget_plan`, `budget_item`, `budget_item_periode`, `budget_engagement`, `budget_reallocation`, `prevision_tresorerie`, `ligne_decouvert`, `ligne_financement`, `echeance_financement`, `engagement_hors_bilan`, `audit_trail`.
* `db-changelog-7.0.yaml` : `budget_demande`, `contrat_couverture`.
* `db-changelog-8.0.yaml` : `utilisateurs`, `utilisateur_telephones`, `utilisateur_permissions`, `maker_checker_requests`.

### 4.2. Persistance Analytique (ClickHouse) & Moteur SQL Embarqué (DuckDB)
* **ClickHouse 26.7 (OLAP)** :
  * Conçu pour l'ingestion massive et le requêtage haute performance de volumes d'audit.
  * Reçoit les transactions comptables validées, les traces d'activité utilisateur et les décisions Maker-Checker.
* **DuckDB (Moteur embarqué `sodepa_audit.db`)** :
  * Stocke localement les tables `idempotency_keys` pour garantir qu'un rejeu de requête HTTP avec le même identifiant ne duplique pas l'opération.
  * Exécute des vues SQL fédérées exploitant le moteur HTTP de ClickHouse pour agréger des données sans duplication.

### 4.3. Stockage Documentaire Objet (MinIO S3) & Cache (Redis)
* **MinIO S3** : Stockage binaire d'objets (bucket `avatars` créé automatiquement par le conteneur `minio-init`). Stocke les photos de profils et les logos des banques partenaires.
* **Redis 7** : Maintient les compteurs d'accès et les fenêtres de temps glissantes pour l'application des quotas du `EndpointThrottleService`.

---

## 5. Traitements Asynchrones et Intégrations Externes

### 5.1. Pipeline Événementiel AMQP (RabbitMQ 3.13)
Le découplage événementiel interne de l'application est assuré par **Spring AMQP / RabbitMQ** :
* **Exchange** : `audit.exchange` (type direct).
* **Files d'attente (Queues)** :
  1. `audit.activities.queue` (clé de routage `audit.activity`) : Journalise les requêtes HTTP capturées par `TrackingUserActionsFilter`.
  2. `audit.transactions.queue` (clé de routage `audit.transaction`) : Émise sur événement Spring `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` lorsqu'une écriture comptable est validée.
  3. `audit.maker-checker.queue` (clé de routage `audit.maker-checker`) : Événements de soumission ou décision Maker-Checker.
* **Composants d'exécution** :
  * Producteur : `AuditEventPublisher` (utilise `RabbitTemplate`).
  * Consommateur : `AuditRabbitListener` (utilise `@RabbitListener`), qui dépile les messages et invoque `ClickHouseManager` pour l'insertion batch JDBC.

### 5.2. Gestion Centralisée de l'Identité (Keycloak 26.0)
* **Fournisseur OpenID Connect / OAuth2** :
  * Le serveur Keycloak est instancié sur le port 8070 avec sa propre base PostgreSQL (`keycloak-db`).
  * Realm : `sodepa` | Client : `sodepa` (avec client-secret).
  * Le backend agit comme passerelle : l'écran de connexion du frontend envoie `POST /api/auth/login`, et le backend appelle le endpoint token de Keycloak via `AuthenticationAdapter`.
  * La validation des jetons JWT entrants repose sur la vérification des signatures cryptographiques par rapport au endpoint JWKS (`/protocol/openid-connect/certs`).
  * Le backend administre les utilisateurs dans Keycloak via l'API REST `KeycloakAdminClient`.

### 5.3. Distribution des Secrets (HashiCorp Vault 1.15)
* Le composant `VaultEnvironmentPostProcessor` implémente `EnvironmentPostProcessor` de Spring Boot.
* Il s'exécute dès le lancement du jar JVM (avant le démarrage du contexte Spring) et contacte l'API REST de Vault (`http://localhost:8200/v1/secret/data/erp`) avec le token d'accès.
* Les secrets récupérés alimentent dynamiquement les propriétés système (`DB_URL`, `DB_PASSWORD`, `KEYCLOAK_CLIENT_SECRET`, etc.).

---

## 6. Structure Technique du Frontend

### 6.1. Organisation des Composants et Routage
* **Coquilles Applicatives (Layouts)** :
  * `BlankComponent` : Affichage nu pour les pages déconnectées (`/login`) et les pages d'erreur (`/erreur/403`, `/erreur/404`).
  * `FullComponent` : Gabarit connecté complet avec en-tête (`HeaderComponent`), menu latéral dynamique (`SidebarComponent`), fil d'Ariane (`BreadcrumbComponent`) et tiroir de réglages (`CustomizerComponent`).
* **Modules Fonctionnels (Lazy-Loading)** :
  * Chargement dynamique par chunks Webpack : `loadChildren: () => import('@features/comptabilite/comptabilite.routes')`.
  * Découpage en 8 espaces métier : `comptabilite`, `analytique`, `budget`, `tresorerie`, `financement`, `immobilisations`, `audit`, `utilisateurs`.

### 6.2. Chaîne d'Intercepteurs HTTP
Les requêtes HTTP émises par les composants frontend traversent 5 intercepteurs en cascade :
```
[Composant UI] 
      │
      ▼
1. loadingInterceptor   ──► Déclenche l'indicateur visuel de chargement
      │
      ▼
2. errorInterceptor     ──► Capture les erreurs 401 et lance /api/auth/refresh
      │
      ▼
3. authInterceptor      ──► Ajoute 'Authorization: Bearer <token>' et 'X-Correlation-Id' (UUID)
      │
      ▼
4. retryInterceptor     ──► Rejoue automatiquement les requêtes idempotentes échouées
      │
      ▼
5. inMemoryInterceptor  ──► (Si activé par config.json) Simule les données en mémoire
      │
      ▼
[Serveur Backend REST]
```

### 6.3. Sécurité Côté Client vs Côté Serveur
* **Protection Client (Ergonomie)** :
  * Les gardes Angular (`authGuard`, `guestGuard`, `hasAnyRole`, `hasAnyPermission`) et les directives structurelles (`*hasRole`, `*hasPermission`) adaptent la navigation et masquent les boutons non autorisés.
  * **Attention** : Ces protections visuelles ne constituent pas une barrière de sécurité ; la sécurité réelle est exclusivement assurée par le backend lors du décodage du JWT et du contrôle de permissions par `JwtAuthenticationFilter`.

---

## 7. Analyse du Déploiement et de l'Infrastructure

### 7.1. Topologie Détectée (Faits Observés)
* **Dépôt Frontend** :
  * Contient un `Dockerfile` multi-étapes (`node:22-alpine` pour la compilation et `nginx:1.27-alpine` pour le runtime).
  * Contient un `docker-entrypoint.sh` qui substitue dynamiquement au démarrage du conteneur les variables `API_URL` et `CSP_CONNECT_SRC` dans `assets/config.json` et `nginx.conf`.
* **Dépôt Backend** :
  * Ne contient aucun Dockerfile pour l'application Spring Boot elle-même (exécutée directement sur la JVM hôte, port 8088).
  * Contient un fichier `docker-compose.yml` déclarant **9 conteneurs d'infrastructure** :
    1. `postgressodepa` (postgres:15) : Base SODEPA sur port 5433.
    2. `postgres` (postgres:15) : Base dédiée Keycloak.
    3. `keycloak` (keycloak:26.0.0) : Serveur IAM sur port 8070.
    4. `redis` (redis:7-alpine) : Cache et limitation de débit sur port 6379.
    5. `rabbitmq` (rabbitmq:3.13.1-management) : Broker AMQP sur ports 5672 et 15672.
    6. `clickhouse` (clickhouse-server:26.7) : Base OLAP sur ports 8123 et 9010.
    7. `minio` (minio:RELEASE.2024-01-18) : Stockage S3 sur ports 7004 et 9001.
    8. `minio-init` : Initialiseur de bucket S3.
    9. `vault` (hashicorp/vault:1.15.2) : Coffre de secrets sur port 8200.
    10. `mailhog` : Serveur de capture SMTP sur ports 1025 et 8025.
  * Réseaux Docker : `sodepa-net-db` (isolation des bases de données) et `sodepa-net-app` (passerelle applicative). Tous les ports publiés sont explicitement restreints à l'interface locale `127.0.0.1`.

---

## 8. Limites, Risques Techniques & Éléments à Confirmer

### 8.1. Dépendances et Services Déclarés mais Dormants
1. **Kafka & Kafka Streams** : Déclarés dans le `pom.xml` (`spring-boot-starter-kafka`, `kafka-streams`), mais aucune classe Java n'utilise de `KafkaTemplate` ni `@KafkaListener`. RabbitMQ est l'unique broker actif.
2. **Collabora Online & MailHog** : `COLLABORA_URL` est présent dans la configuration, et MailHog tourne dans Compose, mais aucun code Java n'émet d'email (`JavaMailSender` absent) ni n'appelle l'API WOPI de Collabora.
3. **Bibliothèques non reliées** : `pgvector`, `spring-boot-starter-webflux`, `pdfbox` et les registres métriques Datadog/OTLP sont importés mais sans usage identifié dans le code source.

### 8.2. Risques Techniques Identifiables
1. **Verrous exclusifs sur DuckDB en environnement multi-instances** :
   * DuckDB est instancié sur un fichier local unique `sodepa_audit.db`. Si le backend Spring Boot est déployé en plusieurs instances (scaling horizontal), l'accès concurrent au fichier générera des exceptions de verrouillage de base de données (`Database lock exception`).
   * *Recommandation* : Externaliser l'idempotence sur Redis ou migrer DuckDB vers un service d'audit dédié.
2. **Absence d'annotations d'autorisation déclarative (`@PreAuthorize`)** :
   * Bien que `@EnableMethodSecurity` soit actif sur `SecurityConfig`, aucune annotation `@PreAuthorize("hasRole(...)")` n'est posée sur les méthodes de contrôleur. Le contrôle est uniquement assuré par le filtre JWT et au cas par cas dans les Use Cases.
   * *Recommandation* : Systématiser `@PreAuthorize` sur tous les contrôleurs REST pour une sécurité en profondeur.
3. **Absence de Dockerfile Backend & Reverse Proxy Global** :
   * Le backend ne dispose pas de recette de conteneurisation dans son dépôt, et le Nginx du frontend ne relaie pas les requêtes API (les requêtes pointent directement sur l'URL d'API externe).
   * *Recommandation* : Fournir un Dockerfile pour le backend et un reverse proxy d'entreprise unifié (ex. Traefik, Nginx Ingress).

---

## 9. Référentiel des Diagrammes Techniques Excalidraw Générés

L'ensemble des diagrammes techniques a été produit au format natif Excalidraw v2, vérifié sans chevauchement et conforme aux conventions graphiques :

| Nom du Livrable | Emplacement Local | Rôle et Périmètre Représenté |
| :--- | :--- | :--- |
| **Livrable 1 : Architecture Technique Détaillée** | [`ERP_SODEPA_Architecture_Technique_Detaillee.excalidraw`](file:///C:/Users/GLC/Documents/sodepa/ERP_SODEPA_Architecture_Technique_Detaillee.excalidraw) | **Diagramme Principal** en 6 zones (A: Client, B: Sécurité, C: Logique Métier, D: Persistance, E: Intégrations, F: Déploiement). |
| **Vue 2 : Architecture Interne Backend** | [`ERP_SODEPA_Architecture_Backend.excalidraw`](file:///C:/Users/GLC/Documents/sodepa/ERP_SODEPA_Architecture_Backend.excalidraw) | Découpage Hexagonal Clean Architecture (Présentation REST, Application / Use Cases, Infrastructure / Repositories). |
| **Vue 3 : Architecture Sécurité & IAM** | [`ERP_SODEPA_Architecture_Securite.excalidraw`](file:///C:/Users/GLC/Documents/sodepa/ERP_SODEPA_Architecture_Securite.excalidraw) | Parcours d'authentification OIDC Keycloak, filtres JWT, RBAC, Rate-Limiting Redis et contrôle Maker-Checker. |
| **Vue 4 : Flux de Données & Intégrations** | [`ERP_SODEPA_Flux_Donnees_Integrations.excalidraw`](file:///C:/Users/GLC/Documents/sodepa/ERP_SODEPA_Flux_Donnees_Integrations.excalidraw) | Cartographie des producteurs de données, broker AMQP RabbitMQ, base relationnelle PostgreSQL, OLAP ClickHouse et MinIO S3. |
| **Vue 5 : Déploiement & Conteneurs** | [`ERP_SODEPA_Architecture_Deploiement.excalidraw`](file:///C:/Users/GLC/Documents/sodepa/ERP_SODEPA_Architecture_Deploiement.excalidraw) | Topologie physique et réseau réelle : conteneur Nginx 1.27, processus JVM Spring Boot, réseaux bridge Docker et volumes persistants. |

*Note sur les copies* : Tous les diagrammes sont également disponibles dans le sous-dossier de documentation du projet : `C:\Users\GLC\Documents\sodepa\erp\docs\architecture\`.
