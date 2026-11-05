# ERP SODEPA — Spécification d'Architecture : Fonctionnement en Mode Hors-Ligne (Offline First)
## Traitement en Zones Blanches, Synchronisation Différée & Topologies d'Infrastructure

**Projet :** Système d'Information ERP Centralisé de la SODEPA  
**Auteur :** Antigravity (Architecte Logiciel Senior & Systèmes Distribués)  
**Date d'établissement :** 27 octobre 2026  
**Livrables visuels associés :**
* **Scénario 1 (Serveurs Internet) :** [`ERP_SODEPA_Offline_Internet_Server.excalidraw`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Internet_Server.excalidraw) · Image HD : [`ERP_SODEPA_Offline_Internet_Server.png`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Internet_Server.png) · Scène Cloud `1RIwHJm58U6`
* **Scénario 2 (Serveurs Local LAN) :** [`ERP_SODEPA_Offline_Local_Server_LAN.excalidraw`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Local_Server_LAN.excalidraw) · Image HD : [`ERP_SODEPA_Offline_Local_Server_LAN.png`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Local_Server_LAN.png) · Scène Cloud `1Y6JpAidfwm`
* **Scénario 3 (Architecture Edge) :** [`ERP_SODEPA_Offline_Edge_Relay.excalidraw`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Edge_Relay.excalidraw) · Image HD : [`ERP_SODEPA_Offline_Edge_Relay.png`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Edge_Relay.png) · Scène Cloud `1yMTHGTSN8k`

---

## 1. Contexte Métier & Enjeux Opérationnels SODEPA

Les activités d'élevage, de suivi zootechnique, de pesée, de vaccination, de transport et d'abattage de la Société de Développement et d'Exploitation des Productions Animales s'exercent fréquemment dans des zones rurales et enclavées :
* **Ranches pastoraux isolés :** Dumbo (Nord-Ouest), Faro (Adamaoua), Jakiri (Nord-Ouest), souvent situés hors de portée des réseaux 3G/4G stables.
* **Abattoirs et parcs à bétail :** Lieux où le blindage métallique ou la configuration des enclos peut bloquer la couverture réseau.
* **Opérations mobiles :** Tournées de transhumance, ramassage et convoyage de bêtes.

Dans ce contexte, bloquer l'application en l'absence de réseau est inacceptable. Le système doit garantir une **saisie fluide en mode déconnecté (Offline First)** sans perte de données, avec réconciliation automatique dès rétablissement d'une liaison.

---

## 2. Étude Détaillée des Trois Scénarios d'Architecture

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                        LES TROIS TOPOLOGIES DE SYNCHRONISATION HORS-LIGNE SODEPA                       │
├───────────────────────────────┬────────────────────────────────┬───────────────────────────────────────┤
│  SCÉNARIO 1 : SERVEURS SUR    │  SCÉNARIO 2 : SERVEURS UNIQUEMT│  SCÉNARIO 3 : ARCHITECTURE EDGE       │
│  INTERNET                     │  EN LOCAL (INTRANET STRICT)    │  (SERVEUR RELAIS LOCAL DE RANCH)      │
│                               │                                │                                       │
│  Mobile (Terrain Déconnecté)  │  Mobile (Terrain Déconnecté)   │  Mobile (Terrain Déconnecté)          │
│        │                      │        │                       │        │                              │
│        ▼ (Retour 4G/Wi-Fi)    │        ▼ (Vecteur Local Requis)│        ▼ (Wi-Fi Local Ranch)          │
│  Internet Public (TLS 1.3)    │  Wi-Fi Site / VPN / Borne USB  │  Micro-Serveur Edge Local (Ranch)     │
│        │                      │        │                       │        │ (Fonctionnement autonome)    │
│        ▼                      │        ▼                       │        ▼ (Satellite/4G/Navette Batch) │
│  Serveur Central Cloud / DC   │  Salle Serveur Intranet Privée │  Serveur Central Siège Yaoundé        │
└───────────────────────────────┴────────────────────────────────┴───────────────────────────────────────┘
```

---

### Scénario 1 : Mode Hors-Ligne avec Serveurs Accessibles sur Internet
*Référentiel :* [`ERP_SODEPA_Offline_Internet_Server.excalidraw`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Internet_Server.excalidraw)

#### Description Fonctionnelle & Technique :
1. **Saisie sur le Terrain (Zone Blanche) :**
   * L'agent utilise son mobile (PWA Angular 21 ou App durcie Android).
   * L'application s'exécute localement grâce au **Service Worker** qui sert les composants et styles pré-mis en cache.
   * Les données de référence (nomenclatures bétail, barèmes de pesée, clients) sont lues depuis la base locale embarquée **IndexedDB / SQLite** chiffrée en AES-GCM.
   * Chaque formulaire validé (ex: enregistrement d'une pesée ou abattage) génère une transaction dotée d'un **UUID v4 d'idempotence unique**, d'un horodatage UTC et de la signature de l'agent.
   * La transaction est insérée dans la **File d'Attente Sortante locale (Outbox Queue)** avec le statut `PENDING_SYNC`.
2. **Reconnexion & Synchronisation Internet :**
   * Dès que l'agent rejoint une zone couverte par la 4G/3G ou un Wi-Fi connecté à Internet, le navigateur émet l'événement `navigator.onLine`.
   * Le service de synchronisation d'arrière-plan contacte l'URL publique `https://api.sodepa.cm/api/v1/sync/push`.
   * Les transactions de l'Outbox sont envoyées par micro-lots (*Batching* de 20 items) sous le canal chiffré **TLS 1.3**.
3. **Traitement Serveur & Accusé de Réception :**
   * Le Backend Spring Boot reçoit le lot, contrôle la validité du jeton JWT et vérifie via son moteur d'idempotence (DuckDB/Redis) que l'UUID n'a pas déjà été traité.
   * Les données sont injectées dans PostgreSQL 16 (mise à jour des cheptels, écritures de vente).
   * Une trace d'audit détaillée avec mention explicite `ORIGIN: OFFLINE_SYNC` est publiée dans RabbitMQ vers ClickHouse.
   * Le serveur répond avec un accusé de réception (`ACK`), déclenchant la purge immédiate des transactions synchronisées sur le mobile.

---

### Scénario 2 : Mode Hors-Ligne avec Serveurs Strictement en Local (Intranet Privé)
*Référentiel :* [`ERP_SODEPA_Offline_Local_Server_LAN.excalidraw`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Local_Server_LAN.excalidraw)

#### Description Fonctionnelle & Technique :
Dans cette architecture, **les serveurs ERP de la SODEPA ne sont pas visibles sur Internet** (politique de souveraineté stricte, air-gap partiel, hébergement exclusif en salle serveur sur réseau privé 10.x.x.x sans IP publique ni port ouvert sur le WAN).

1. **Le Problème Posé :**
   * Lorsque l'agent quitte la zone blanche et capte la 4G commerciale sur la route, **il ne peut pas contacter le serveur directement**, car l'URL `erp.local` ou `10.0.1.10` n'est pas routable sur l'Internet public.
2. **Les 3 Vecteurs de Reconnexion Autorisés :**
   * **Vecteur A — Wi-Fi Local du Site / Ranch :** L'agent rentre au bureau administratif ou à la station centrale du ranch. Le mobile se connecte au Wi-Fi sécurisé d'entreprise (WPA3 Enterprise / 802.1X). Le mobile reçoit une IP locale du sous-réseau SODEPA et déverse immédiatement son Outbox vers le serveur local.
   * **Vecteur B — Tunnel VPN d'Entreprise :** Si l'agent dispose d'une connexion 4G externe mais doit joindre le serveur privé, son mobile active un tunnel **VPN IPsec / WireGuard d'entreprise**. Le concentrateur VPN du siège authentifie le terminal par certificat X.509 et lui attribue une IP virtuelle interne autorisant la synchronisation vers l'API.
   * **Vecteur C — Station d'Accueil Kiosque / Câble USB LAN :** Pour les terminaux durcis des balances de pesée dépourvus de carte SIM, le terminal est posé sur un dock relié en Ethernet/USB au réseau local. Un utilitaire local récupère les paquets chiffrés et les injecte sur le serveur.
3. **Avantages Sécurité :**
   * Surface d'attaque externe nulle (aucun port exposé aux scanners Internet).
   * Intégrité garantie au sein du périmètre physique et logique de l'entreprise.

---

### Scénario 3 : Architecture Hybride Edge Computing (Micro-Serveurs Relais de Ranch / Abattoir)
*Référentiel :* [`ERP_SODEPA_Offline_Edge_Relay.excalidraw`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Edge_Relay.excalidraw)

#### Description Fonctionnelle & Technique :
C'est la solution d'ingénierie la plus robuste pour les sites géographiquement dispersés et enclavés :

```text
┌─────────────────────────┐         Wi-Fi Local         ┌─────────────────────────────────┐
│ Mobiles Agents Terrain  ├────────────────────────────►│ Micro-Serveur Edge Local Ranch  │
│ (Vachers, Peseurs)      │◄────────────────────────────┤ (Mini-PC durci, Docker, Nginx,  │
│ Saisie 100% Hors-Ligne  │       Vidage Outbox         │  PostgreSQL Edge, Autonomie 30j)│
└─────────────────────────┘                             └────────────────┬────────────────┘
                                                                         │
                                                Liaison Asynchrone       │ Satellite (Starlink/VSAT)
                                                (Dès Disponibilité)      │ 4G Nocturne programmée
                                                                         │ Navette Logistique USB
                                                                         ▼
                                                        ┌─────────────────────────────────┐
                                                        │ Serveur Central ERP (Siège Ydé) │
                                                        │ Consolidation SYSCOHADA globale,│
                                                        │ Bilan national cheptel, Audit   │
                                                        └─────────────────────────────────┘
```

1. **Niveau 1 — Mobiles Opérationnels de Site :**
   * Les agents saisissent les données au cœur des pâturages et des abattoirs.
   * De retour aux baraquements du ranch, les mobiles se synchronisent instantanément en Wi-Fi local avec le micro-serveur du site sans aucun besoin d'Internet.
2. **Niveau 2 — Micro-Serveur Edge de Site (Ranch Node) :**
   * Un boîtier industriel durci (IP65, basse consommation, onduleur de protection) est installé dans chaque ranch (Dumbo, Faro, Jakiri) et abattoir.
   * Il héberge une version allégée conteneurisée de l'ERP (PostgreSQL Edge local, Nginx, service de synchronisation d'agence).
   * **Autonomie complète du site :** Les chefs de ranch émettent des bons de pesée, enregistrent les mouvements de troupeaux et contrôlent les stocks d'aliments même si la coupure avec Yaoundé dure plusieurs semaines.
3. **Niveau 3 — Passerelle de Réplication Asynchrone :**
   * Le micro-serveur Edge regroupe les données et attend l'ouverture d'un canal de transmission :
     * *Canal A :* Connexion satellite périodique (Starlink ou VSAT).
     * *Canal B :* Fenêtre 4G temporaire nocturne programmée.
     * *Canal C :* Export chiffré sur support amovible (clé USB sécurisée transportée lors de la navette hebdomadaire de ravitaillement vers la Direction Générale).
4. **Niveau 4 — Consolidation Centrale à la Direction Générale (Yaoundé) :**
   * Le serveur central intègre les paquets de réplication, valide les écritures globales SYSCOHADA, impute la comptabilité analytique sur le ranch concerné et archive les pistes d'audit dans ClickHouse.

---

## 3. Algorithmes Critiques du Mode Hors-Ligne

### 1. Garantie d'Idempotence (Zéro Doublon)
* Tout enregistrement créé hors-ligne reçoit un identifiant immuable :
  $$\text{Idempotency-Key} = \text{UUIDv4} \quad (\text{ex: } \texttt{550e8400-e29b-41d4-a716-446655440000})$$
* Lors de la reconnexion, si le réseau coupe au moment où le serveur renvoie l'accusé de réception, le mobile re-transmettra le même payload.
* Le composant backend vérifie la clé d'idempotence dans DuckDB/Redis : si la clé a déjà été consommée, le serveur renvoie immédiatement le succès sans ré-exécuter l'écriture en base.

### 2. Résolution des Conflits Métier (Conflict Resolution)
* **Conflits d'enregistrements simples (Fiches descriptives, pesées) :** Règle du *Last-Write-Wins (LWW)* basée sur l'horodatage UTC de capture terrain certifié.
* **Conflits d'inventaire critique (ex: un animal déclaré vendu sur le ranch A et vacciné sur le ranch B) :**
  * Le système ne rejette pas la transaction.
  * Il applique l'écriture et génère automatiquement une tâche de régularisation dans le **Moteur Maker-Checker (Principe des 4 yeux)** soumise à l'arbitrage du Directeur des Productions Animales.

### 3. Cycle de Vie d'une Transaction Hors-Ligne

```text
 [Saisie Formulaire Mobile]
            │
            ▼
 [Génération UUID v4 + Timestamp UTC]
            │
            ▼
 [Écriture dans IndexedDB Locale] ──► Statut: PENDING_SYNC
            │
     (Attente Réseau)
            │
            ▼
 [Détection Réseau (Internet/LAN)] ──► Statut: SYNCING
            │
            ▼
 [Appel POST /api/v1/sync/push]
            │
    ┌───────┴───────┐
    ▼               ▼
(Succès HTTP)  (Échec / Coupure)
    │               │
    │               ▼
    │          [Retry Exponential Backoff] ──► Reste en PENDING_SYNC
    ▼
[Réception ACK Serveur] ──► Statut: COMMITTED ──► [Purge de l'Outbox Locale]
```

---

## 4. Matrice Comparative pour l'Aide à la Décision DSI

| Critère d'Évaluation | Scénario 1 : Serveurs sur Internet | Scénario 2 : Serveurs Uniquement en Local | Scénario 3 : Hybride Edge Computing |
| :--- | :--- | :--- | :--- |
| **Autonomie en Zone Blanche** | Élevée (mobile autonome). | Élevée (mobile autonome). | **Maximale** (site entier autonome). |
| **Délai de Synchronisation** | Immédiat dès captage 4G/Internet. | Nécessite le retour au site ou VPN. | Selon fréquence du relais (sat/navette). |
| **Complexité d'Infrastructure** | Faible (Reverse proxy Nginx Internet). | Moyenne (VPN ou bornes LAN). | Élevée (matériel durci dans chaque ranch). |
| **Coût Matériel** | Faible (smartphones existants). | Faible à modéré. | Modéré (mini-serveurs de site à acquérir). |
| **Niveau de Sécurité Périmétrique** | Standard (TLS 1.3, CSP, WAF). | **Très élevé** (aucun port public). | **Élevé** (chiffrement de bout en bout). |
| **Résilience aux Coupures Siège** | Dépendance de la liaison centrale. | Dépendance du réseau local. | **Indépendance totale des ranches**. |

---

## 5. Répertoire des Fichiers et Livrables Produits

| Schéma / Fichier | Emplacement Physique & Lien Direct | Rendu Visuel HD | Scène Cloud Excalidraw+ |
| :--- | :--- | :--- | :--- |
| **Scénario 1 : Serveurs Internet** | [**`ERP_SODEPA_Offline_Internet_Server.excalidraw`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Internet_Server.excalidraw) | [**Visualiser PNG**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Internet_Server.png) | Scène `1RIwHJm58U6` |
| **Scénario 2 : Serveurs Local LAN** | [**`ERP_SODEPA_Offline_Local_Server_LAN.excalidraw`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Local_Server_LAN.excalidraw) | [**Visualiser PNG**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Local_Server_LAN.png) | Scène `1Y6JpAidfwm` |
| **Scénario 3 : Hybride Edge** | [**`ERP_SODEPA_Offline_Edge_Relay.excalidraw`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Edge_Relay.excalidraw) | [**Visualiser PNG**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Offline_Edge_Relay.png) | Scène `1yMTHGTSN8k` |
| **Dossier d'Architecture (.md)** | [**`ERP_SODEPA_Architecture_Mode_Hors_Ligne.md`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Architecture_Mode_Hors_Ligne.md) | Synthèse technique et algorithmique complète. | — |

*(Des copies miroirs de tous les fichiers sont également disponibles à la racine : [`c:\Users\GLC\Documents\sodepa\`](file:///c:/Users/GLC/Documents/sodepa/)).*
