# ERP SODEPA — Spécification des Exigences Matérielles & Dimensionnement (Sizing)
## Référentiel Technique d'Infrastructure & Guide de Dimensionnement Serveurs

**Projet :** Système d'Information ERP Centralisé de la SODEPA  
**Auteur :** Antigravity (Architecte Logiciel Senior & Infrastructure SI)  
**Date d'établissement :** 26 octobre 2026  
**Référentiel visuel :** [`ERP_SODEPA_Exigences_Materielles_Sizing.excalidraw`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Exigences_Materielles_Sizing.excalidraw) · Image HD : [`ERP_SODEPA_Exigences_Materielles_Sizing.png`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Exigences_Materielles_Sizing.png)  
**Scène Cloud Excalidraw+ :** Scene ID `1VX8uiTxuwB` (Workspace `6dUQktLplDj`, collection `Main`)

---

## 1. Synthèse Comparative par Environnement

Le dimensionnement serveur a été calculé pour couvrir l'ensemble des 14 domaines fonctionnels de l'ERP SODEPA, avec un accent particulier sur la tenue de la comptabilité SYSCOHADA et l'immuabilité de la piste d'audit ClickHouse :

| Métrique d'Infrastructure | Staging / Pré-production (Recette) | Production Centrale (Cible) | Justification Technique |
| :--- | :--- | :--- | :--- |
| **Processeur (CPU)** | **4 vCPU** (2.4 GHz+) | **8 à 16 vCPU** (3.0 GHz+) | Capacité d'absorption des pics de validation budgétaire et batchs d'écritures. |
| **Mémoire Vive (RAM)** | **16 Go RAM** | **32 à 64 Go RAM ECC** | Caches in-memory (`shared_buffers` Postgres, JVM Heap `-Xmx6g`, ClickHouse, Redis). |
| **Stockage Système & App** | 100 Go SSD SATA | **250 Go SSD NVMe** | Système d'exploitation, conteneurs Docker, logs applicatifs rotationnés. |
| **Stockage Données & Audit** | 200 Go SSD SATA | **750 Go à 1 To SSD NVMe RAID 10** | PostgreSQL (250 Go) + ClickHouse (400 Go) + S3 MinIO (200 Go). |
| **Débit I/O Disque** | ~1 500 IOPS | **> 10 000 IOPS (latence < 1ms)** | Requis pour éviter les goulots d'étranglement lors des clôtures comptables. |
| **Bande Passante Réseau** | 100 Mbps symétrique | **1 Gbps redondé** | Liaison Fibre Optique Siège Yaoundé + VPN IPsec sécurisé vers les 3 ranches. |
| **Architecture Déploiement** | VM Unique Docker Compose | **Serveur Dédié / Cluster HA** | Isolation des réseaux `sodepa-net-db` et `sodepa-net-app`, ports 127.0.0.1. |

---

## 2. Décomposition du Dimensionnement par Composant (7 Tiers)

```
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                       RÉPARTITION DE LA MÉMOIRE RAM (64 Go)                            │
│  ┌──────────────────┬─────────────────┬─────────────────┬────────────────┬───────────────┬──────────┐  │
│  │ PostgreSQL 16    │ ClickHouse 26.7 │ Backend Spring  │ Keycloak/Vault │ MinIO/Collab. │ Redis/OS │  │
│  │ 16 Go            │ 16 Go           │ 12 Go           │ 6 Go           │ 6 Go          │ 8 Go     │  │
│  └──────────────────┴─────────────────┴─────────────────┴────────────────┴───────────────┴──────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Tier 1 : Frontend Web & Reverse Proxy Nginx
* **Composant :** Conteneur `sodepa-frontend` (Nginx 1.27-alpine + bundle SPA Angular 21 Zoneless).
* **CPU :** 0.5 vCPU (Staging) $\rightarrow$ 1 à 2 vCPU (Production).
* **RAM :** 256 Mo $\rightarrow$ 512 Mo à 1 Go.
* **Disque :** 15 Go SSD (fichiers compilés, cache statique, logs d'accès).
* **Ports & Protocoles :** Port interne `8080`, exposé en frontal sur le port `443` HTTPS (TLS 1.3).

### Tier 2 : Backend Core ERP (Spring Boot 4)
* **Composant :** Processus JVM Java 21 LTS (`erp.jar`, Clean Architecture Hexagonale).
* **CPU :** 1 vCPU (Staging) $\rightarrow$ 4 vCPU (Production).
* **RAM :** 2 Go $\rightarrow$ 6 à 8 Go RAM (`-Xms4g -Xmx6g`, Garbage Collector G1).
* **Disque :** 25 Go SSD (logs Logback, fichier temporaire in-process DuckDB `sodepa_audit.db`).
* **Fonctions :** 25 contrôleurs Spring MVC, 45 use cases, moteur Maker-Checker, validation JWKS RS256.

### Tier 3 : Base Transactionnelle OLTP (PostgreSQL 16)
* **Composant :** PostgreSQL 15/16 (Bases `sodepa` et `keycloak`).
* **CPU :** 1 vCPU (Staging) $\rightarrow$ 4 vCPU (Production).
* **RAM :** 2 Go $\rightarrow$ 8 à 16 Go RAM (`shared_buffers = 4GB`, `work_mem = 64MB`, `maintenance_work_mem = 512MB`).
* **Disque :** 100 Go $\rightarrow$ 250 Go SSD NVMe (RAID 10).
* **Volumétrie :** 33 entités JPA, plan comptable OHADA, journaux et écritures, suivi des engagements budgétaires.

### Tier 4 : Entrepôt Analytique & Pistes d'Audit (ClickHouse 26.7)
* **Composant :** ClickHouse Server (Base orientée colonnes OLAP temps réel).
* **CPU :** 1 vCPU (Staging) $\rightarrow$ 4 vCPU (Production).
* **RAM :** 2 Go $\rightarrow$ 8 à 16 Go RAM (mémoire allouée aux agrégations et requêtes analytiques lourdes).
* **Disque :** 100 Go $\rightarrow$ 350 Go SSD NVMe (croissance d'environ 50 Go/an, rétention réglementaire 5 ans).
* **Paramètre noyau requis :** `nofile` (descripteurs de fichiers) fixé à **262 144** minimum.

### Tier 5 : Gestion des Identités & Coffre de Secrets (Keycloak & Vault)
* **Composant :** Keycloak 26.0 (OIDC/OAuth2) & HashiCorp Vault 1.15.2 (Moteur KV v2 `secret/data/erp`).
* **CPU :** 1 vCPU (Staging) $\rightarrow$ 2 vCPU (Production).
* **RAM :** 1.5 Go $\rightarrow$ 4 Go RAM.
* **Disque :** 30 Go SSD (base Keycloak, audit d'accès, snapshots chiffrés de Vault).
* **Sécurité :** Port Vault `8200` strictement confiné à `127.0.0.1` (pas d'exposition externe).

### Tier 6 : Cache & Broker Asynchrone (Redis 7 & RabbitMQ 3.13)
* **Composant :** Redis 7 Alpine & RabbitMQ 3.13.1-management.
* **CPU :** 0.5 vCPU (Staging) $\rightarrow$ 2 vCPU (Production).
* **RAM :** 1 Go $\rightarrow$ 2 à 4 Go RAM (compteurs atomiques rate-limiting + buffers de files AMQP).
* **Disque :** 25 Go SSD (instantanés RDB/AOF Redis, persistance des messages non consommés).
* **Paramètre noyau requis :** `vm.overcommit_memory = 1` sur l'hôte Linux.

### Tier 7 : Stockage Objets & GED Bureautique (MinIO & Collabora)
* **Composant :** MinIO S3 (RELEASE.2024-01-18) & Collabora Online CODE.
* **CPU :** 1 vCPU (Staging) $\rightarrow$ 2 vCPU (Production).
* **RAM :** 1.5 Go $\rightarrow$ 4 Go RAM.
* **Disque :** 50 Go $\rightarrow$ 200 Go SSD (photos de profil, pièces jointes scannées, contrats, documents GED).

---

## 3. Recommandations Critiques DSI & Règles d'Exploitation

1. **Obligation du Stockage NVMe :**
   * L'ingestion par lots des traces comptables dans ClickHouse via `AuditRabbitListener` et les requêtes concurrentes sur PostgreSQL saturent rapidement les disques SSD SATA conventionnels. L'usage de disques **SSD NVMe en RAID 10** garantit une latence d'écriture inférieure à 1 ms et plus de 10 000 IOPS.
2. **Dimensionnement Réseau pour les Ranches Décentralisés :**
   * Pour les ranches de **Dumbo**, **Faro**, **Jakiri** et les abattoirs de **Yaoundé** et **Douala**, une liaison sécurisée VPN IPsec avec débit garanti de **2 Mbps par site** et latence inférieure à 120 ms est requise. La SPA Angular limite la consommation réseau en échangeant exclusivement des flux REST légers au format JSON.
3. **Contrainte d'Architecture DuckDB :**
   * En raison du verrou exclusif posé sur le fichier in-process `sodepa_audit.db`, le Backend Spring Boot doit être déployé sur un nœud unique (ou migrer sa table d'idempotence sur Redis en cas de mise à l'échelle horizontale multi-pods).
4. **Politique de Sauvegarde (PRA / PCA) :**
   * `pg_dump -Fc sodepa` quotidien envoyé sur stockage déporté (NAS / S3 froid).
   * Snapshot hebdomadaire de ClickHouse via `clickhouse-backup`.
   * Export chiffré des secrets HashiCorp Vault après chaque modification.

---

## 4. Répertoire des Livrables Produits

| Type | Fichier & Emplacement | Usage |
| :--- | :--- | :--- |
| **Schéma Excalidraw Source** | [**`ERP_SODEPA_Exigences_Materielles_Sizing.excalidraw`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Exigences_Materielles_Sizing.excalidraw) | Schéma vectoriel modifiable sur [excalidraw.com](https://excalidraw.com) ou extension VS Code. |
| **Rendu Visuel PNG HD** | [**`ERP_SODEPA_Exigences_Materielles_Sizing.png`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Exigences_Materielles_Sizing.png) | Image haute résolution (1620×956 px · 305 Ko) calibrée pour insertion dans les dossiers techniques et comités d'investissement DSI. |
| **Scène Cloud Excalidraw+** | **Scène ID :** `1VX8uiTxuwB` | Accessible directement sur votre compte Excalidraw+ (Workspace `6dUQktLplDj`, collection `Main`). |
| **Spécification Markdown** | [**`ERP_SODEPA_Exigences_Materielles_Sizing.md`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Exigences_Materielles_Sizing.md) | Document d'ingénierie système détaillé. |
