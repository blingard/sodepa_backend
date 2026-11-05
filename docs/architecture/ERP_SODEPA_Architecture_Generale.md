# Rapport d'Architecture Générale du Système d'Information — ERP SODEPA
**Document d'Architecture Technique d'Entreprise (DSI / Comités de Gouvernance SI / DevOps)**  
*Date d'établissement : 25 octobre 2026*  
*Auteur : Antigravity (Architecte Logiciel Senior & Infrastructure)*  
*Périmètre audité : Backend Spring Boot (`erp`) & Frontend Angular (`sodepa-frontend`)*

---

## 1. Résumé de l'Architecture Observée

L'**ERP SODEPA** (Société de Développement et d'Exploitation des Productions Animales) constitue le socle numérique central unifiant les processus administratifs, comptables, budgétaires et financiers de l'entreprise.

L'architecture réelle identifiée dans le code source est structurée selon une **organisation verticale en 5 couches techniques principales** complétée par une **zone d'architecture cible** :

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  COUCHE 1 : CLIENTS, UTILISATEURS & ACCÈS RÉSEAU                                       │
│  - Direction Générale & Services Centraux (LAN / WAN Yaoundé)                          │
│  - Unités Décentralisées : Ranches, Abattoirs, Magasins Frigorifiques (WAN / 4G VPN)   │
│  - Navigateurs Web Modernes (Chrome, Firefox, Safari, Edge) via HTTPS / TLS 1.3        │
└────────────────────────────────────────┬───────────────────────────────────────────────┘
                                         │ HTTPS / TLS (Port 443 / 8080)
┌────────────────────────────────────────▼───────────────────────────────────────────────┐
│  COUCHE 2 : POINT D'ENTRÉE, SÉCURITÉ PÉRIMÉTRIQUE & SERVEUR WEB                        │
│  - Reverse Proxy Web Nginx 1.27 Alpine (Port 8080)                                     │
│  - Headers de Sécurité HSTS, X-Content-Type-Options: nosniff, X-Frame-Options: DENY   │
│  - Dynamic CSP & Injection API_URL via entrypoint envsubst                             │
└──────────────────┬─────────────────────────────────────┬───────────────────────────────┘
                   │ Assets Statiques                    │ Appels REST / JSON
┌──────────────────▼───────────────────┐ ┌───────────────▼───────────────────────────────┐
│  COUCHE 3A : FRONTEND SPA            │ │  COUCHE 3B : BACKEND CORE ERP (Spring Boot 4) │
│  - Angular 21 (Zoneless Signals)     │ │  - REST Controllers (25 contrôleurs, port 8080)│
│  - Tailwind CSS 4 & Angular Material │ │  - Stateless Security & JwtAuthFilter (RS256) │
│  - Intercepteurs HTTP (Auth, Trace)  │ │  - Rate Limiting Redis (FAIL_CLOSED)          │
│  - Traçabilité X-Correlation-Id      │ │  - Maker-Checker 4-Eyes Engine                │
│  - Modules Mockés : RH, GED          │ │  - VaultEnvironmentPostProcessor (KV Secrets) │
└──────────────────────────────────────┘ └───────┬───────────────────────────────┬───────┘
                                                 │                               │
                                                 │ JDBC / AMQP / RESP            │ OIDC / Secrets / APIs
                                                 │ (Persistance & Cache)         │ (Liaison directe 3B → 5)
                                                 ▼                               │
┌────────────────────────────────────────────────────────────────────────────────┤       │
│  COUCHE 4 : PERSISTANCE, TRAITEMENT ASYNCHRONE & AUDIT TRAIL                   │       │
│  - PostgreSQL 16 (Port 5432) : Données Métier Relationnelles OLTP & SYSCOHADA  │       │
│  - Redis 7 Alpine (Port 6379) : Cache applicatif, Sessions & Rate Limiting     │       │
│  - RabbitMQ 3.13 (Ports 5672 / 15672) : Broker AMQP Événementiel               │       │
│  - ClickHouse 26.7 (Port 8123 / 9000) : Entrepôt OLAP des Pistes d'Audit       │       │
│  - DuckDB In-Process (sodepa_audit.db) : Idempotence & Vues analytiques        │       │
│  [Couche 4 Isolée : Strictement aucun flux réseau vers la Couche 5]            │       │
└────────────────────────────────────────────────────────────────────────────────┘       │
                                                                                         │
┌────────────────────────────────────────────────────────────────────────────────────────▼┐
│  COUCHE 5 : SYSTÈMES TIERS & SERVICES D'INFRASTRUCTURE                                  │
│  - Keycloak 26 (Port 8080) : Identity & Access Management (OIDC / OAuth2, JWKS)         │
│  - HashiCorp Vault 1.15 (Port 8200) : Gestionnaire Centralisé de Secrets & Clés Chiffre │
│  - Collabora CODE (Port 9980) : Édition & Visualisation Office (Configuré Docker)       │
│  - Target / Roadmap : Connecteurs Bancaires EBICS, Passerelle DGI / Douanes             │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technologies Détectées et Confirmées dans le Code Source

| Composant | Technologie & Version | Rôle & Modalités Techniques Observées |
| :--- | :--- | :--- |
| **Frontend Web** | **Angular 21.0.8** (Zoneless) | Composants standalone, Angular Material 21, Tailwind CSS 4, i18n FR/EN. Exécuté sous conteneur **Nginx 1.27-alpine** (port 8080). |
| **Backend Applicatif** | **Spring Boot 4.1.0** (Java 21) | **Monolithe modulaire en Clean Architecture (Hexagonale)**. 25 Contrôleurs Spring MVC, 45+ Use Cases, 32 Repositories Spring Data JPA. Exécuté sur JVM hôte (port 8088). |
| **Gestion des Identités (IAM)** | **Keycloak 26.0.0** (OIDC / OAuth2) | Realm `sodepa`, émission de tokens signés en RS256, validation sans état par clés publiques JWKS (`/protocol/openid-connect/certs`). Base dédiée `keycloak-db`. |
| **Distribution des Secrets** | **HashiCorp Vault 1.15.2** | Composant `VaultEnvironmentPostProcessor` chargeant les secrets KV (`secret/data/erp`) au boot JVM via client HTTP natif. |
| **Persistance Transactionnelle** | **PostgreSQL 15** | Base `sodepa` (port 5433), 33 entités JPA managées par **Liquibase (8 changelogs)**. Volume persistant `postgres_data_sodepa`. |
| **Entrepôt Analytique & Audit** | **ClickHouse 26.7** (OLAP) | Base orientée colonnes (ports 8123 HTTP, 9010 JDBC) recevant les traces d'audit et les transactions répliquées. |
| **Moteur SQL Embarqué** | **DuckDB (sodepa_audit.db)** | Moteur embarqué dans le processus JVM gérant l'idempotence des requêtes et les requêtes SQL fédérées vers ClickHouse. |
| **Broker de Messagerie** | **RabbitMQ 3.13.1** (AMQP) | Exchange direct `audit.exchange` routant vers trois files asynchrones (`activities`, `transactions`, `maker-checker`). |
| **Cache & Limitation de Débit** | **Redis 7-alpine** | Compteurs atomiques pour l'intercepteur `EndpointRateLimitInterceptor` (politique `FAIL_CLOSED`). |
| **Stockage Objets (S3)** | **MinIO (RELEASE.2024-01-18)** | Bucket `avatars` (port 7004) pour les photos de profils et logos bancaires via le SDK MinIO Java. |
| **Workflows Internes** | **Moteur Maker-Checker (share)** | Séparation stricte des tâches (règle des 4 yeux : `makerId != checkerId`) imposée sur les actes sensibles. |

---

## 3. Composants Applicatifs et Leurs Responsabilités

### 3.1 Couche Présentation (Frontend SPA)
* **Application Web** : Single Page Application (SPA) bâtie sous Angular 21, servie par un conteneur Nginx 1.27.
* **Navigation & Accès** : 8 modules fonctionnels chargés à la demande (*Lazy-Loading*) via `loadChildren`.
* **Communication avec le Backend** : `ApiService` centralisé injectant systématiquement le jeton d'accès `Authorization: Bearer <JWT>` et l'en-tête de corrélation `X-Correlation-Id` (UUID v4).
* **Renouvellement automatique** : Capture des codes HTTP 401 par `errorInterceptor` pour déclencher silencieusement `/api/auth/refresh`.

### 3.2 Couche Applicative (Backend Spring Boot)
* **Style Architectural** : Monolithe modulaire en Clean Architecture (Hexagonale). Les contrôleurs Spring MVC délèguent immédiatement aux Use Cases applicatifs sans porter de logique métier.
* **Domaines Métier Opérationnels** :
  1. *Comptabilité Générale* : Plan comptable OHADA révisé (`planComptableOHADA.json`), journaux, écritures équilibrées $D=C$, tiers, banques, déclaration TVA, clôture d'exercice.
  2. *Comptabilité Analytique* : Centres de coûts (axes et sections), clés de répartition de charges, budgets analytiques.
  3. *Gestion Budgétaire* : Plans annuels, budget collaboratif avec lettres de cadrage, cycle complet des engagements (pré-engagement, engagement ferme, liquidation).
  4. *Trésorerie & Financement* : Prévisions glissantes, rapprochement bancaire algorithmique, couverture du risque de change, gestion des emprunts.
  5. *Immobilisations* : Fiches d'actifs, calcul des amortissements linéaires/dégressifs.
  6. *Administration Utilisateurs* : Habilitations, permissions applicatives (`Permissions`), profils.

---

## 4. Flux de Communication Confirmés & Règles de Ségrégation

1. **Utilisateur $\rightarrow$ Reverse Proxy Nginx (Couche 1 $\rightarrow$ Couche 2)** : Protocole `HTTPS / TLS 1.3` sur le port 8080 (serveur web Nginx servant l'artefact compilé de la SPA).
2. **Reverse Proxy Nginx $\rightarrow$ Frontend SPA (Couche 2 $\rightarrow$ Couche 3A)** : Distribution locale des bundles JavaScript/CSS statiques.
3. **Frontend SPA $\rightarrow$ Backend Core ERP (Couche 3A $\rightarrow$ Couche 3B)** : Protocole `REST / JSON` avec en-têtes obligatoires `Authorization: Bearer <JWT>` et `X-Correlation-Id` (UUID v4).
4. **Backend Core ERP $\rightarrow$ Persistance & Audit (Couche 3B $\rightarrow$ Couche 4)** :
   * `PostgreSQL 16` : Protocole `JDBC / SQL` via le pool de connexions HikariCP (port 5432/5433).
   * `Redis 7` : Protocole `RESP` pour le contrôle atomique des quotas de rate-limiting.
   * `RabbitMQ 3.13` : Protocole `AMQP` (publication asynchrone découplée d'événements d'audit après commit transactionnel).
   * `DuckDB` : Moteur in-process pour les contrôles d'idempotence et les requêtes analytiques fédérées.
   * *Au sein de la Couche 4* : Consommation asynchrone des messages RabbitMQ par `AuditRabbitListener` et insertion par lots via `clickhouse-jdbc` dans `ClickHouse 26.7`.
5. **Backend Core ERP $\rightarrow$ Systèmes Tiers & Services Infra (Liaison Directe Couche 3B $\rightarrow$ Couche 5)** :
   * `Keycloak 26` : Protocole `HTTP` pour la validation sans état des clés publiques JWKS (`/protocol/openid-connect/certs`).
   * `HashiCorp Vault 1.15` : Protocole `HTTP GET /v1/secret` au boot JVM via `VaultEnvironmentPostProcessor` pour injecter les secrets applicatifs en mémoire.
   * `Collabora Online CODE` : Protocole `HTTP / WOPI` pour l'édition et la visualisation de documents bureautiques intégrés.
   * `Roadmap / Cible` : Connecteurs bancaires EBICS et passerelles fiscales DGI / Douanes invoqués par le Backend.
6. **Isolation Stricte de la Couche 4 vis-à-vis de la Couche 5** :
   * **Aucun flux direct Couche 4 $\rightarrow$ Couche 5** : Les bases de données, le cache Redis et le broker RabbitMQ sont confinés au stockage et au routage interne. Ils n'émettent aucun appel vers Keycloak, Vault ou les services tiers. Tous les accès passent obligatoirement par le contrôle métier du Backend Core ERP (Couche 3B).

---

## 5. Mécanismes de Sécurité Observés

* **Sécurité Périmétrique** : En-têtes HTTP stricts injectés par Nginx (`Content-Security-Policy`, `X-Frame-Options DENY`, `X-Content-Type-Options nosniff`, `Referrer-Policy`).
* **Sécurité Applicative Sans État (Stateless)** : Spring Security désactive les sessions HTTP et le CSRF (justifié par le mode Bearer token non stocké en cookie).
* **Traçabilité Stricte** : L'intercepteur `JwtAuthenticationFilter` rejette immédiatement (HTTP 400) toute requête privée dépourvue d'en-tête `X-Correlation-Id`.
* **Rate-Limiting (Anti-Déni de Service)** : `EndpointRateLimitInterceptor` applique une politique `FAIL_CLOSED` sur compteurs Redis (ex. 10 requêtes/min sur `/api/auth/login`).
* **Séparation des Pouvoirs (Maker-Checker)** : Mécanisme cryptographique garantissant qu'un utilisateur ne peut approuver ses propres créations (`makerId != checkerId`).

---

## 6. Éléments d'Infrastructure Identifiés

* **Conteneurisation Frontend** : Image Docker multi-étapes (`node:22-alpine` $\rightarrow$ `nginx:1.27-alpine`) avec script d'entrée `docker-entrypoint.sh` substituant les variables d'environnement (`API_URL`, `CSP_CONNECT_SRC`) dans `assets/config.json`.
* **Exécution Backend** : Processus Java 21 autonome (Spring Boot `erp.jar` sur port 8088). Aucun Dockerfile backend n'est présent dans le dépôt.
* **Stack d'Infrastructure Docker Compose** : 9 services managés (`postgressodepa`, `postgres`, `keycloak`, `redis`, `rabbitmq`, `clickhouse`, `minio`, `vault`, `mailhog`) interconnectés via deux réseaux bridge isolés (`sodepa-net-db` et `sodepa-net-app`), avec publication de ports strictement confinée à `127.0.0.1`.

---

## 7. Composants Restant à Confirmer & Inconnus de l'Architecture

1. **Reverse Proxy Frontal Global (TLS 443 / WAF)** : Le serveur Nginx actuel sert uniquement de serveur statique pour le frontend (port 8080) sans rôle de proxy d'API. L'existence d'une passerelle d'accès d'entreprise (Traefik, Nginx Ingress, WAF ModSecurity) en amont n'est pas documentée dans les dépôts.
2. **Dépendances Déclarées Dormantes dans `pom.xml`** :
   * `spring-boot-starter-kafka` & `kafka-streams` : 0 Producer / 0 Listener dans le code Java (RabbitMQ utilisé exclusivement).
   * `pgvector` : extension vectorielle non exploitée.
   * `spring-boot-starter-webflux` : non utilisé (Spring MVC bloquant utilisé partout).
   * `pdfbox` : présent sans générateur de documents actif.
   * Registres métriques Datadog & OTLP : présents mais désactivés par configuration (`enabled: false`).
3. **Services Configurés Inactifs** :
   * `COLLABORA_URL` : configuré dans `application.yml` mais sans composant actif l'invoquant.
   * `MailHog` : présent dans Compose et `spring-mail` présent dans Maven, mais aucun service d'envoi de mail actif dans le code.

---

## 8. Écarts entre l'Existant et les Objectifs de l'ERP (Trajectoire Cible)

1. **Ressources Humaines & SIRH** : Package backend `com.sodepa.erp.rh` vide (0 classe Java). L'écran Personnel du frontend s'exécute sur le simulateur mémoire (`useInMemoryApi`).
2. **Gestion Électronique des Documents (GED)** : Package backend `com.sodepa.erp.ged` vide.
3. **Fiscalité Dédiée** : Package backend `com.sodepa.erp.fiscalite` vide (la TVA est gérée dans le module comptable).
4. **Tableaux de Bord BI** : Package backend `com.sodepa.erp.dashboard` vide.
5. **Achats, Marchés & Stocks** : Modules complètement absents du code source actuel (marqués en cible sur le diagramme).
6. **Facturation & Recettes Ventes** : Absentes du code source (seule la gestion des tiers clients existe).

---

## 9. Recommandations d'Amélioration (Séparées de l'Existant)

1. **Résolution du Verrou DuckDB pour le Multi-Instances** : DuckDB utilisant un fichier local (`sodepa_audit.db`), tout déploiement du backend en plusieurs réplicas provoquera des verrous de base de données. Il est recommandé de déporter la table d'idempotence directement sur Redis.
2. **Systématisation des Contrôles d'Accès Déclaratifs** : Ajouter des annotations `@PreAuthorize("hasAuthority(...)")` sur chaque contrôleur REST Spring pour renforcer la sécurité en profondeur (actuellement déléguée au filtre JWT et aux Use Cases).
3. **Industrialisation du Déploiement Backend** : Écrire un `Dockerfile` multi-étapes pour le backend Spring Boot basé sur une image allégée `eclipse-temurin:21-jre-alpine` et intégrer le service au `docker-compose.yml`.
4. **Nettoyage des Dépendances Mortes** : Élaguer le fichier `pom.xml` des dépendances inutilisées (Kafka, WebFlux, pgvector) pour réduire la surface d'attaque et le temps de démarrage.

---

## 10. Répertoire des Livrables Produits

* **Livrable 1 — Schéma d'Architecture Générale (.excalidraw)** :
  * [`ERP_SODEPA_Architecture_Generale.excalidraw` (dossier docs)](file:///C:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Architecture_Generale.excalidraw)
  * [`ERP_SODEPA_Architecture_Generale.excalidraw` (racine projet)](file:///C:/Users/GLC/Documents/sodepa/ERP_SODEPA_Architecture_Generale.excalidraw)
  * *Caractéristiques* : 96 éléments structurés en 5 couches verticales + zone latérale de trajectoire cible, format paysage 16:9, palette sobre institutionnelle.
* **Livrable 2 — Rapport d'Analyse d'Architecture Générale (.md)** :
  * [`ERP_SODEPA_Architecture_Generale.md` (dossier docs)](file:///C:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Architecture_Generale.md)
  * [`ERP_SODEPA_Architecture_Generale.md` (racine projet)](file:///C:/Users/GLC/Documents/sodepa/ERP_SODEPA_Architecture_Generale.md)
* **Livrable 3 — Export Visuel Haute Résolution (.png)** :
  * [`ERP_SODEPA_Architecture_Generale.png` (dossier docs)](file:///C:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Architecture_Generale.png)
  * [`ERP_SODEPA_Architecture_Generale.png` (racine projet)](file:///C:/Users/GLC/Documents/sodepa/ERP_SODEPA_Architecture_Generale.png)
* **Scène en Ligne Excalidraw+ (Serveur MCP)** :
  * Scène : `ERP_SODEPA_Architecture_Generale` | ID : `2UkFytZIqz8` (Workspace `6dUQktLplDj`, Collection `Main`).
