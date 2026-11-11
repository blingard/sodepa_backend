# SPÉCIFICATION DÉTAILLÉE DU MOTEUR DE WORKFLOW UNIVERSEL SODEPA
## Société de Développement et d'Exploitation des Productions Animales (SODEPA)

---

### 1. Contexte et Objectifs Métier

L'ERP SODEPA gère des activités réparties entre les services centraux de Yaoundé (Direction Générale, Direction Administrative et Financière, Direction Technique, Audit Interne, Contrôle de Gestion) et des unités opérationnelles décentralisées (Ranches de Jakiri, Dumbo et Faro, Abattoirs industriels de Yaoundé, Douala et Ngaoundéré, Entrepôts frigorifiques).

Le **Moteur de Workflow Universel** fournit un socle transverse, paramétrable sans re-développement (*No-Code / Low-Code*), capable d'orchestrer le cycle de vie de n'importe quel objet métier de l'entreprise :
- **Ressources Humaines** : Recrutement, titularisation, mutation, sanctions, avancements d'échelon.
- **Gestion Budgétaire** : Demandes d'engagement, dépassements de seuils, virements de crédits entre unités, réallocations.
- **Comptabilité Générale & Analytique** : Validation d'écritures sensibles, opérations d'inventaire, clôtures périodiques, lettrages dérogatoires.
- **Finance & Trésorerie** : Ordres de virement bancaire, demandes de décaissement, régies d'avance, double signature bancaire.
- **GED & Contrats** : Cycles de validation documentaire, visa juridique, circuits de signature électronique sécurisée (DG, DAF).
- **Achats & Pastoral** : Expressions de besoin, bons de commande, mouvements de cheptel, abattage.

---

### 2. Principes Fondamentaux d'Ingénierie

1. **Agnosticisme Métier & Pattern Strategy** : Le moteur manipule des concepts génériques (`entityName`, `entityId`, `donneesContexte`). Il délègue les mutations et la finalisation métier à des handlers dédiés implémentant [`WorkflowEntityHandler`](file:///c:/Users/GLC/Documents/sodepa/erp/src/main/java/com/sodepa/erp/workflow/application/handler/WorkflowEntityHandler.java).
2. **Intégration Organigramme Dynamique** : Résolution automatique des approbateurs via l'organigramme (Poste, Manager N+1, N+2, Chef de Ranch, Directeur d'Abattoir) avec prise en charge des **délégations de pouvoir** actives (`DelegationPouvoirEntity`).
3. **Ségrégation Stricte des Tâches (Maker-Checker / 4-Eyes)** : Un utilisateur ne peut en aucun cas valider une étape sur un dossier qu'il a lui-même initié.
4. **Verrous d'Action Granulaires (`ValidationStepActionMode`)** : Contrôle strict imposant ou non l'ajout de pièces justificatives (téléversement MinIO) ou la saisie de champs modifiés pour pouvoir valider une étape.
5. **Gestion Temporelle des SLA & Escalades** : Surveillance des délais limites de traitement avec calcul d'échéances et suspension des tâches concurrentes lors d'un renvoi pour révision (`ASK_REVISION`).
6. **Piste d'Audit Infalsifiable & Multi-Niveaux** : Horodatage, journalisation relationnelle locale (`workflows_historique_audit`) et ingestion analytique asynchrone dans ClickHouse (`clickhouse_workflow_events`).

---

### 3. Modèle Conceptuel de Données (MCD)

```mermaid
erDiagram
    WORKFLOW_METIER ||--o{ WORKFLOW_DEFINITION : "régit"
    WORKFLOW_DEFINITION ||--o{ WORKFLOW_ETAPE_DEFINITION : "compose (1..n)"
    WORKFLOW_ETAPE_DEFINITION ||--o{ WORKFLOW_ETAPE_ACTEUR_DEFINITION : "configure (1..n)"
    WORKFLOW_ETAPE_DEFINITION ||--o{ WORKFLOW_CONDITION_TRANSITION : "déclenche (0..n)"
    WORKFLOW_DEFINITION ||--o{ WORKFLOW_INSTANCE : "instancie (0..n)"
    WORKFLOW_INSTANCE ||--o{ WORKFLOW_TACHE : "génère (1..n)"
    WORKFLOW_INSTANCE ||--o{ WORKFLOW_HISTORIQUE_AUDIT : "trace (1..n)"

    WORKFLOW_METIER {
        uuid id PK
        string code UK "ex: RECRUITMENT, ACHAT_MATERIEL"
        string title
        string domain "RH, BUDGET, COMPTABILITE, PASTORAL..."
        boolean is_transversal
        boolean is_active
        uuid authorized_unit_id FK
    }

    WORKFLOW_DEFINITION {
        uuid id PK
        string code UK "Auto-généré WFD-xxxxxxxx"
        string intitule
        string domaine
        uuid metier_id FK
        uuid unite_organisationnelle_id FK
        string roles_initiateurs
        string postes_initiateurs
        int version
        boolean actif
    }

    WORKFLOW_ETAPE_DEFINITION {
        uuid id PK
        uuid workflow_definition_id FK
        int ordre
        string code_etape "ex: WFD-00000001-E1"
        string intitule
        string type_etape "VALIDATION_HUMAINE, SIGNATURE_ELECTRONIQUE, CONTROLE_AUTO"
        string mode_action_validation "DO_NOTHING, ADD_FILE, SET_ATTRIBUTE, FILE_AND_ATTRIBUTE"
        string politique_validation "ONLY_ONE_CAN_VALIDATE, EVERY_BODY_CAN_VALIDATE, MUST_HAVE_MAJORITY"
        int delai_sla_heures
        string action_depassement_sla "RELANCE_EMAIL, ESCALADE_MANAGER, REJET_AUTO"
        boolean actif
    }

    WORKFLOW_ETAPE_ACTEUR_DEFINITION {
        uuid id PK
        uuid etape_definition_id FK
        string intitule_action
        uuid employe_id FK
        uuid utilisateur_id FK
        uuid poste_id FK
        uuid unite_organisationnelle_id FK
        string strategie_assignation "UTILISATEUR_FIXE, POSTE_SPECIFIQUE, MANAGER_N1, ROLE_KEYCLOAK"
        string valeur_assignation "ex: ROLE_DAF, SOUS_DIRECTEUR_RH"
        string mode_action_validation
        boolean obligatoire
        boolean can_be_delegate
        int delai_sla_heures
        boolean actif
    }

    WORKFLOW_INSTANCE {
        uuid id PK
        uuid workflow_definition_id FK
        string entity_name "ex: EMPLOYE, BUDGET_DEMANDE, FACTURE"
        string entity_id "Clé primaire métier UUID"
        uuid initiateur_id FK
        uuid initiateur_employe_id FK
        uuid unite_organisationnelle_id FK
        string statut "EN_COURS, APPROUVE, REJETE, SUSPENDU"
        uuid etape_courante_id FK
        jsonb donnees_contexte
        timestamp date_initiation
        timestamp date_cloture
    }

    WORKFLOW_TACHE {
        uuid id PK
        uuid workflow_instance_id FK
        uuid etape_definition_id FK
        uuid etape_acteur_id FK
        uuid assigne_employe_id FK
        uuid assigne_utilisateur_id FK
        uuid assigne_poste_id FK
        string statut "PENDING, APPROUVED, REJETED, EXPIRED, CANCELLED"
        string decision "APPROUVED, REJETE, ASK_REVISION, PENDING"
        string mode_action_validation
        jsonb current_data
        text commentaire
        boolean obligatoire
        boolean is_delegated
        timestamp date_creation
        timestamp date_echeance
        timestamp date_decision
    }

    WORKFLOW_HISTORIQUE_AUDIT {
        uuid id PK
        uuid workflow_instance_id FK
        string etape_code
        uuid acteur_id
        string action "INITIATION, APPROBATION, REJET, DEMANDE_REVISION, CLOTURE"
        string decision
        text commentaire
        timestamp horodatage
    }
```

---

### 4. Dictionnaire des Données Détaillé

#### A. Entité `WorkflowDefinitionEntity` (`workflows_definitions`)
| Colonne | Type | Nullable | Contraintes / Description |
| :--- | :--- | :---: | :--- |
| `id` | UUID | NON | Clé primaire |
| `code` | VARCHAR(60) | NON | Code unique généré automatiquement via `CodeGenerator` (ex: `WFD-00000001`) |
| `intitule` | VARCHAR(150) | NON | Libellé explicite du circuit |
| `domaine` | VARCHAR(40) | NON | Énumération `WorkflowDomaine` (`RH`, `BUDGET`, `ACHATS`, `COMPTABILITE`...) |
| `metier_id` | UUID | OUI | FK vers `workflows_metiers` |
| `unite_organisationnelle_id` | UUID | OUI | FK vers `unites_organisationnelles` (cloisonnement) |
| `roles_initiateurs` | VARCHAR | OUI | Rôles Keycloak autorisés à initier ce workflow |
| `postes_initiateurs` | VARCHAR | OUI | Postes de l'organigramme autorisés à initier |
| `version` | INT | NON | Numéro de version incrémenté à chaque modification |
| `actif` | BOOLEAN | NON | Indique si le circuit est activable |
| `date_creation` | TIMESTAMP | NON | Date de création du circuit |

#### B. Entité `WorkflowEtapeDefinitionEntity` (`workflows_etapes_definitions`)
| Colonne | Type | Nullable | Contraintes / Description |
| :--- | :--- | :---: | :--- |
| `id` | UUID | NON | Clé primaire |
| `workflow_definition_id` | UUID | NON | FK vers `workflows_definitions` |
| `ordre` | INT | NON | Numéro d'ordre séquentiel d'exécution (1, 2, 3...) |
| `code_etape` | VARCHAR(60) | NON | Code unique au sein du circuit (ex: `WFD-00000001-E1`) |
| `intitule` | VARCHAR(150) | NON | Libellé affiché sur le Stepper UI |
| `type_etape` | VARCHAR(40) | NON | `VALIDATION_HUMAINE`, `SIGNATURE_ELECTRONIQUE`, `CONTROLE_AUTO` |
| `mode_action_validation` | VARCHAR(40) | NON | `DO_NOTHING`, `ADD_FILE`, `SET_ATTRIBUTE`, `FILE_AND_ATTRIBUTE`, `OPTIONAL_FILE_OR_ATTRIBUTE` |
| `politique_validation` | VARCHAR(40) | NON | `ONLY_ONE_CAN_VALIDATE`, `EVERY_BODY_CAN_VALIDATE`, `MUST_HAVE_MAJORITY` |
| `delai_sla_heures` | INT | OUI | Délai maximal de traitement alloué pour l'étape |
| `action_depassement_sla`| VARCHAR(40) | NON | `RELANCE_EMAIL`, `ESCALADE_MANAGER`, `REJET_AUTOMATIQUE` |
| `actif` | BOOLEAN | NON | Coupe-circuit : étape ignorée si `false` |

#### C. Entité `WorkflowEtapeActeurDefinitionEntity` (`workflows_etapes_acteurs_definitions`)
| Colonne | Type | Nullable | Contraintes / Description |
| :--- | :--- | :---: | :--- |
| `id` | UUID | NON | Clé primaire |
| `etape_definition_id` | UUID | NON | FK vers `workflows_etapes_definitions` |
| `intitule_action` | VARCHAR(150) | NON | Libellé explicite de l'action attendue |
| `employe_id` | UUID | OUI | Désignation nominative d'un employé |
| `utilisateur_id` | UUID | OUI | Liaison compte technique utilisateur |
| `poste_id` | UUID | OUI | Ciblage d'un poste de l'organigramme |
| `unite_organisationnelle_id` | UUID | OUI | Restriction d'affectation par cellule |
| `strategie_assignation`| VARCHAR(40) | NON | `UTILISATEUR_FIXE`, `POSTE_SPECIFIQUE`, `MANAGER_N1`, `ROLE_KEYCLOAK` |
| `valeur_assignation` | VARCHAR(100) | OUI | Consigne dynamique (ex: `"ROLE_DAF"`, `"CHEF_RANCH_FARO"`) |
| `mode_action_validation` | VARCHAR(40) | NON | Mode d'action spécifique exigé de cet acteur |
| `donnees_json` | JSONB | OUI | Dictionnaire de configuration dynamique des champs |
| `obligatoire` | BOOLEAN | NON | `true` = approbation indispensable pour franchir l'étape |
| `can_be_delegate` | BOOLEAN | NON | Indique si cet acteur peut être suppléé par délégation active |
| `delai_sla_heures` | INT | OUI | SLA spécifique à cet acteur |
| `actif` | BOOLEAN | NON | Coupe-circuit acteur |

---

### 5. Algorithme d'Aiguillage et de Contrôle d'Assignation

```mermaid
flowchart TD
    Start([Démarrage d'une Étape]) --> CheckType{Type d'étape ?}
    
    CheckType -->|CONTROLE_AUTO| RunAuto[Exécution tâche système / règle métier]
    RunAuto --> AutoSuccess{Succès ?}
    AutoSuccess -->|Oui| NextStep[Passage à l'étape suivante]
    AutoSuccess -->|Non| Reject[Rejet automatique avec motif]

    CheckType -->|VALIDATION_HUMAINE / SIGNATURE| CheckStrat{Stratégie d'assignation ?}

    CheckStrat -->|POSTE_SPECIFIQUE| ResolvePoste[Résolution titulaire du poste via Organigramme]
    CheckStrat -->|MANAGER_N1| ResolveN1[Résolution supérieur hiérarchique direct N+1]
    CheckStrat -->|ROLE_KEYCLOAK| ResolveRole[Agents disposant du rôle Keycloak dans l'UO]
    CheckStrat -->|UTILISATEUR_FIXE| ResolveUser[Compte utilisateur nominatif]

    ResolvePoste --> CheckDelegation{Délégation de pouvoir active ?}
    ResolveN1 --> CheckDelegation
    ResolveRole --> CheckDelegation
    ResolveUser --> CheckDelegation

    CheckDelegation -->|Oui & canBeDelegate=true| AssignDelegue[Assigner au mandataire délégué]
    CheckDelegation -->|Non| AssignTitulaire[Assigner au titulaire]

    AssignDelegue --> CheckMaker{Est-ce l'initiateur du dossier ?}
    AssignTitulaire --> CheckMaker

    CheckMaker -->|Oui - Maker/Checker strict| EscalateSegregation[Escalade obligatoire au supérieur hiérarchique]
    CheckMaker -->|Non - Conforme 4-yeux| CreateTask[Création WorkflowTaskEntity PENDING]
    EscalateSegregation --> CreateTask
```


---

### 6. Remplacement Définitif de `MakerCheckerEnginePort` et Convergence

L'ancienne interface `MakerCheckerEnginePort` et son implémentation `MakerCheckerEngineAdapter` ont été complètement supprimées au profit du **Moteur de Workflow Universel** (`WorkflowService`).

#### A. Principes de la Convergence
1. **Point d'Entrée Unique** : Tous les domaines métiers de l'ERP (Achats, Budget, Comptabilité, Organigramme, Paie, Pastoral, RH, Trésorerie, Utilisateurs) injectent directement `WorkflowService`.
2. **Double Bénéfice** :
   - Préservation de la signature d'appel `submitChange(entityName, entityPk, payload, operationType)` pour la rétrocompatibilité des 15 adaptateurs métiers.
   - Instanciation automatique et transparente d'un graphe de workflow (`WorkflowInstanceEntity`) rattaché à l'entité, avec assignation d'acteurs réels et évaluation d'étapes.
3. **Observabilité Immédiate** : Publication automatique des événements de soumission et de décision vers RabbitMQ pour ingestion dans la base analytique ClickHouse.

---

### 7. Orchestration Multi-Domaines et Événements Découplés

Le moteur de workflow universel permet à un circuit de validation de faire collaborer plusieurs modules métiers sans couplage fort :
- **Chaîne ordonnée de Handlers (`WorkflowEntityHandlerRegistry`)** : Exécute tous les `WorkflowEntityHandler` enregistrés pour une entité donnée en respectant leur priorité `@Order`.
- **Événements de Domaine Spring** :
  - `WorkflowStepApprovedEvent` : Notifie les modules à chaque franchissement d'étape avec les modifications apportées.
  - `WorkflowInstanceCompletedEvent` : Notifie de l'approbation définitive pour clôture ou activation métier.
  - `WorkflowInstanceRejectedEvent` : Notifie du rejet pour libération des réservations (ex: déblocage de crédits budgétaires).
  - `WorkflowRevisionRequestedEvent` : Notifie de la demande de révision (`ASK_REVISION`) pour correction par l'initiateur.

---

### 8. Sécurité, Verrouillage Optimiste et Traitement des Exceptions

1. **Sécurité RBAC Granulaire** : Contrôle systématique des permissions `WORKFLOW_READ`, `WORKFLOW_CONFIG`, `WORKFLOW_SUPERVISION` et `WORKFLOW_DECISION`.
2. **Identification JWT** : L'identité de l'agent est extraite du contexte de sécurité JWT officiel, empêchant toute usurpation par injection d'identifiant arbitraire.
3. **Anti Auto-Approbation (Règle des 4 Yeux)** : Rejet strict (`AccesRefuseException`) si un utilisateur tente de valider une tâche d'une instance qu'il a lui-même initiée.
4. **Verrouillage Optimiste** : Les entités `WorkflowInstanceEntity` et `WorkflowTaskEntity` portent l'annotation JPA `@Version`, prévenant les écritures concurrentes et les accès concurrents non autorisés.
5. **Gestion d'Erreurs Typées** : Rejet normalisé via les exceptions `RequeteInvalideException` (HTTP 400), `AccesRefuseException` (HTTP 403), `RessourceIntrouvableException` (HTTP 404), et `ConflitMetierException` (HTTP 409).
