# BACKLOG DÉTAILLÉ — MODULE ORGANIGRAMME & STRUCTURE ORGANISATIONNELLE
## Société de Développement et d'Exploitation des Productions Animales (SODEPA)

Ce backlog définit l'ensemble des tickets d'ingénierie logicielle pour l'implémentation du module **Organigramme & Structure Organisationnelle** au sein de l'ERP SODEPA (Backend Spring Boot 4 & Frontend Angular 21).

---

### Milestone cible
- **Titre** : `Module Organigramme & Structure Organisationnelle`
- **Description** : `Modélisation hiérarchique de la SODEPA (Siège, Ranches, Abattoirs, Magasins), gestion des postes, affectations, résolutions managériales N+1 et cloisonnement des accès.`

---

### Issue #23 : [EPIC-P0] [ORGANIGRAMME] Modélisation hiérarchique des unités organisationnelles (Siège, Ranches, Abattoirs, Magasins)

**Milestone :** `Module Organigramme & Structure Organisationnelle`  
**Labels :** `epic:organigramme`, `priority:P0`, `type:feature`  
**Composants impactés :** `com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity`, `com.sodepa.erp.organigramme.application.usecase`

#### 1. Description & Contexte Métier
La SODEPA possède une structure organisationnelle distribuée comprenant le Siège social à Yaoundé (Direction Générale, Directions, Départements, Services) et des unités décentralisées sur le territoire camerounais :
- **Ranches** : Jakiri, Dumbo, Faro, etc.
- **Abattoirs industriels** : Yaoundé, Douala, etc.
- **Magasins et Entrepôts frigorifiques** : distribution et stockage de la viande et produits dérivés.

Il est nécessaire de modéliser une structure arborescente flexible et performante, capable de gérer des niveaux hiérarchiques illimités tout en garantissant des requêtes d'ancêtres et de descendants ultra-rapides grâce à un champ de chemin matérialisé (`materialized_path`, ex: `/1/4/12`).

#### 2. Spécifications Techniques
- Entité `UniteOrganisationnelleEntity` :
  - `id` (UUID), `code` (unique, ex: `DIR-TECH`, `RNCH-JAKIRI`, `ABAT-DLA`), `intitule` (String 150)
  - `typeUnite` (Enum : `DIRECTION_GENERALE`, `DIRECTION`, `DEPARTEMENT`, `SERVICE`, `RANCH`, `ABATTOIR`, `MAGASIN_FRIGORIFIQUE`, `POSTE_AVANCE`)
  - `parent` (`@ManyToOne` auto-référence vers `UniteOrganisationnelleEntity`)
  - `children` (`@OneToMany` liste des sous-unités)
  - `materializedPath` (String indexé, ex: `/001/004/015/`)
  - `localisation` (Ville / Région : Adamaoua, Nord-Ouest, Centre, Littoral, etc.)
  - `statut` (Enum : `ACTIF`, `EN_RESTRUCTURATION`, `INACTIF`)
  - `dateCreation`, `dateFermeture`
- Méthode de calcul automatique du `materialized_path` à la création ou lors du déplacement d'une unité dans l'arbre.
- Détection et blocage strict des dépendances cycliques (une unité ne peut pas être son propre ancêtre).

#### 3. Critères d'Acceptation (Gherkin)
```gherkin
Fonctionnalité: Gestion de l'arborescence des unités organisationnelles SODEPA

  Scénario: Création réussie d'une unité décentralisée (Ranch de Jakiri)
    Étant donné un utilisateur authentifié avec le rôle "ADMIN_RH"
    Et que la Direction des Exploitations d'identifiant "DIR-EXP" existe
    Quand il crée l'unité de code "RNCH-JAK", type "RANCH", rattachée à "DIR-EXP"
    Alors l'unité est persistée avec le statut "ACTIF"
    Et le champ "materializedPath" contient le chemin hiérarchique complet depuis la racine
    Et un événement d'audit "UNITE_ORGANISATIONNELLE_CREEE" est publié

  Scénario: Détection et rejet d'un rattachement cyclique
    Étant donné une unité "DEPT-ELEVAGE" rattachée à "DIR-EXP"
    Quand un administrateur tente de déplacer "DIR-EXP" comme sous-unité de "DEPT-ELEVAGE"
    Alors le système rejette l'opération avec une exception HTTP 400 "CyclicDependencyException"
    Et la structure arborescente d'origine est intacte
```

---

### Issue #24 : [EPIC-P0] [ORGANIGRAMME] Gestion du référentiel des postes, fonctions et effectifs autorisés (ETP)

**Milestone :** `Module Organigramme & Structure Organisationnelle`  
**Labels :** `epic:organigramme`, `priority:P0`, `type:feature`  
**Composants impactés :** `com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity`, `com.sodepa.erp.organigramme.application.usecase`

#### 1. Description & Contexte Métier
Chaque unité organisationnelle (Direction, Ranch, Abattoir) comporte une grille de postes budgétés et autorisés (fiches de poste). Pour assurer un contrôle rigoureux des effectifs (ETP - Équivalent Temps Plein) et maîtriser la masse salariale, le système doit définir les postes, leur lien hiérarchique N+1 direct, ainsi que le nombre de postes ouverts et pourvus.

#### 2. Spécifications Techniques
- Entité `PosteEntity` :
  - `id` (UUID), `codePoste` (ex: `PST-DIR-FIN`, `PST-CHEF-RNCH-JAK`), `intitule` (String 150)
  - `uniteOrganisationnelle` (`@ManyToOne` vers `UniteOrganisationnelleEntity`)
  - `posteSuperieur` (`@ManyToOne` auto-référence vers le poste du manager direct N+1)
  - `effectifAutorise` (Integer, nombre de postes budgétés autorisés, ex: 1 pour Chef de Ranch, 15 pour Ouvrier d'abattage)
  - `effectifActuel` (Integer calculé dynamiquement basé sur les affectations actives)
  - `categorieConvention` (ex: Cadre Dirigeant, Agent de Maîtrise, Ouvrier Spécialisé)
  - `statut` (Enum : `OUVERT`, `GELÉ`, `SUPPRIMÉ`)
  - `descriptionMission` (Text)
- Règles de gestion :
  - Un poste d'encadrement supérieur (ex: Directeur Général) n'a pas de N+1 (racine de la hiérarchie managériale).
  - Contrôle lors d'une nouvelle affectation : alerte ou blocage si `effectifActuel >= effectifAutorise` sans dérogation RH préalable.

#### 3. Critères d'Acceptation (Gherkin)
```gherkin
Fonctionnalité: Gestion des postes et des effectifs autorisés

  Scénario: Création d'un poste avec lien hiérarchique N+1
    Étant donné l'existence de l'unité "Abattoir de Yaoundé"
    Et l'existence du poste supérieur "PST-DIR-ABAT-YDE" (Directeur de l'abattoir)
    Quand l'administrateur RH crée le poste "PST-RESP-HYGIENE" rattaché à cette unité avec N+1="PST-DIR-ABAT-YDE"
    Alors le poste est enregistré avec un effectif autorisé de 1
    Et son responsable hiérarchique direct est correctement lié

  Scénario: Contrôle de dépassement de l'effectif budgété
    Étant donné un poste "PST-CHEF-EQUIPE-PATURAGE" avec un effectif autorisé de 2 et déjà 2 employés affectés
    Quand un gestionnaire RH tente d'affecter un 3ème collaborateur sans validation de dépassement
    Alors le système signale une violation de plafond budgété "BudgetedHeadcountExceededException"
```

---

### Issue #25 : [EPIC-P0] [ORGANIGRAMME] Affectation des collaborateurs aux postes, gestion des intérims et historique de carrière

**Milestone :** `Module Organigramme & Structure Organisationnelle`  
**Labels :** `epic:organigramme`, `priority:P0`, `type:feature`  
**Composants impactés :** `com.sodepa.erp.organigramme.infrastructure.entities.AffectationPosteEntity`, `com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity`

#### 1. Description & Contexte Métier
Un agent SODEPA (ou utilisateur du système) est affecté à un poste au sein de l'organigramme. Les affectations peuvent être à titre titulaire, intérimaire ou cumulatif (adjoint, intérim d'un chef de ranch en congé ou mission). Le système doit conserver l'historique exhaustif des nominations et des fins de fonctions pour la traçabilité administrative et la piste d'audit.

#### 2. Spécifications Techniques
- Entité `AffectationPosteEntity` :
  - `id` (UUID), `utilisateur` (`@ManyToOne` vers `UtilisateurEntity`)
  - `poste` (`@ManyToOne` vers `PosteEntity`)
  - `typeAffectation` (Enum : `TITULAIRE`, `INTERIMAIRE`, `ADJOINT`, `DETACHE`)
  - `dateDebut` (LocalDate), `dateFin` (LocalDate, nullable)
  - `referenceDecision` (String, ex: `DECISION-DG/2026/042 du 15 Janvier 2026`)
  - `motifFin` (Enum/String : `MUTATION`, `PROMOTION`, `DEPART`, `FIN_INTERIM`)
  - `statut` (Enum : `ACTIF`, `CLOTURE`, `EN_ATTENTE_PRISE_FONCTION`)
- Contraintes d'intégrité :
  - Un collaborateur ne peut avoir qu'une seule affectation `TITULAIRE` active à une date donnée.
  - La clôture d'une affectation met à jour automatiquement la date de fin et décrémente `effectifActuel` du poste.

#### 3. Critères d'Acceptation (Gherkin)
```gherkin
Fonctionnalité: Affectation d'un collaborateur à un poste

  Scénario: Affectation titulaire réussie avec acte de nomination
    Étant donné un utilisateur actif "jean.dupont@sodepa.cm" sans affectation en cours
    Et un poste disponible "PST-CHEF-RNCH-FARO"
    Quand le gestionnaire RH crée une affectation de type "TITULAIRE" avec la décision "DG-NOM-2026-08"
    Alors l'affectation devient "ACTIF" à partir de la date de prise de service
    Et l'effectif actuel du poste est incrémenté de 1

  Scénario: Fin d'intérim automatique et reprise du titulaire
    Étant donné une affectation intérimaire prenant fin au 31/03/2026
    Quand la date système dépasse le 31/03/2026
    Alors le statut de l'affectation intérimaire bascule automatiquement en "CLOTURE"
    Et les droits d'approbation associés à l'intérim expirent
```

---

### Issue #26 : [EPIC-P1] [ORGANIGRAMME] Versioning de l'organigramme, gestion des restructurations et dates d'effet

**Milestone :** `Module Organigramme & Structure Organisationnelle`  
**Labels :** `epic:organigramme`, `priority:P1`, `type:feature`  
**Composants impactés :** `com.sodepa.erp.organigramme.infrastructure.entities.OrganigrammeVersionEntity`

#### 1. Description & Contexte Métier
Les réorganisations administratives de la SODEPA (création d'un nouveau département d'insémination artificielle, scission d'un ranch, fusion de centres de profit) sont votées par le Conseil d'Administration et prennent effet à une date précise. Le système doit permettre de préparer une version future de l'organigramme en mode brouillon/simulation sans perturber le fonctionnement opérationnel courant, puis d'appliquer la bascule à la date d'effet.

#### 2. Spécifications Techniques
- Entité `OrganigrammeVersionEntity` :
  - `id` (UUID), `numeroVersion` (String, ex: `ORG-2026-V1`)
  - `libelle` (String), `description`
  - `dateEffet` (LocalDate)
  - `statut` (Enum : `BROUILLON`, `SOUMIS_VALIDATION`, `EN_VIGUEUR`, `ARCHIVE`)
  - `approuvePar` (UUID), `dateApprobation` (Instant)
- Mécanisme d'instanciation de scénario : duplication de l'arbre existant vers un espace de travail virtuel.
- Commutateur temporel : requête de l'organigramme "tel qu'il était au JJ/MM/AAAA" (reconstitution historique pour les audits financiers et RH).

#### 3. Critères d'Acceptation (Gherkin)
```gherkin
Fonctionnalité: Versioning et simulation de restructuration

  Scénario: Préparation d'un projet de réorganisation sans impact sur la production
    Étant donné la version en vigueur "ORG-2025-V2"
    Quand le directeur général crée un projet de restructuration "ORG-2026-CIBLE"
    Et qu'il crée une nouvelle division "Division Agropastorale" dans cette version brouillon
    Alors cette division n'apparaît pas dans l'organigramme opérationnel en vigueur
    Et elle est consultable uniquement par les utilisateurs habilités sur la version brouillon

  Scénario: Activation à date d'effet d'une nouvelle version validée
    Étant donné une version validée "ORG-2026-V1" programmée pour prise d'effet au 01/04/2026
    Quand la date d'effet est atteinte
    Alors "ORG-2026-V1" passe au statut "EN_VIGUEUR"
    Et la précédente version "ORG-2025-V2" passe automatiquement au statut "ARCHIVE"
```

---

### Issue #27 : [EPIC-P1] [ORGANIGRAMME] Moteur de résolution managériale (N+1, N+2) et gestion des délégations temporaires

**Milestone :** `Module Organigramme & Structure Organisationnelle`  
**Labels :** `epic:organigramme`, `priority:P1`, `type:security`  
**Composants impactés :** `com.sodepa.erp.organigramme.application.ports.HierarchieResolutionPort`, `com.sodepa.erp.organigramme.application.usecase.ResoudreHierarchieUseCase`

#### 1. Description & Contexte Métier
Tous les workflows de validation de l'ERP (approbation des commandes d'achat, validation des feuilles de temps RH, dérogations budgétaires Maker-Checker, ordres de mission) s'appuient sur la chaîne managériale N+1, N+2. En cas d'absence d'un responsable (mission sur le terrain dans un ranch, congés), une délégation de pouvoir temporaire doit être définie avec date de fin stricte et périmètre délimité.

#### 2. Spécifications Techniques
- Interface `HierarchieResolutionPort` :
  - `Optional<UtilisateurEntity> getManagerDirect(UUID utilisateurId);`
  - `List<UtilisateurEntity> getChaineHierarchique(UUID utilisateurId, int maxNiveaux);`
  - `Optional<UtilisateurEntity> getResponsableUnite(UUID uniteId);`
  - `boolean isSuperieurHierarchique(UUID managerId, UUID collaborateurId);`
- Entité `DelegationPouvoirEntity` :
  - `delegant` (Utilisateur titulaire), `delegataire` (Utilisateur suppléant)
  - `typeDelegation` (Enum : `TOTALE`, `BUDGETAIRE_SEULE`, `RH_CONGES`, `ACHATS`)
  - `plafondMontant` (BigDecimal, plafond financier max de signature)
  - `dateDebut`, `dateFin` (Instants stricts)
  - `statut` (Enum : `ACTIVE`, `REVOQUEE`, `EXPIREE`)
- Prise en compte transparente dans les règles Maker-Checker : un délégué ne peut pas approuver ses propres demandes même s'il dispose de la délégation de son N+1.

#### 3. Critères d'Acceptation (Gherkin)
```gherkin
Fonctionnalité: Résolution de la chaîne hiérarchique et délégation

  Scénario: Résolution automatique du N+1 pour validation de dépense
    Étant donné un vétérinaire affecté au poste "PST-VET-FARO"
    Et que le poste supérieur direct de "PST-VET-FARO" est "PST-CHEF-RNCH-FARO"
    Quand le système interroge le supérieur hiérarchique pour une demande d'achat
    Alors le système retourne l'employé actif actuellement titulaire du poste "PST-CHEF-RNCH-FARO"

  Scénario: Routage vers le délégataire en période d'absence
    Étant donné le Directeur d'Abattoir ayant accordé une délégation de signature au Chef de Quai du 10 au 20 Mars
    Quand une demande d'engagement budgétaire arrive le 15 Mars
    Alors la tâche de validation est notifiée et assignée au Chef de Quai
    Et l'action porte la mention "Par délégation de [Directeur d'Abattoir]"
```

---

### Issue #28 : [EPIC-P1] [ORGANIGRAMME] Cloisonnement des données et contrôle d'accès basé sur la structure (ABAC)

**Milestone :** `Module Organigramme & Structure Organisationnelle`  
**Labels :** `epic:organigramme`, `priority:P1`, `type:security`  
**Composants impactés :** `com.sodepa.erp.authorisation`, `com.sodepa.erp.configuration.security`

#### 1. Description & Contexte Métier
Pour des impératifs de confidentialité et de conformité réglementaire, la visibilité des données dans l'ERP SODEPA doit être restreinte selon la position de l'utilisateur dans l'organigramme :
- Le Chef de Ranch de Jakiri ne doit voir que les stocks d'animaux, le personnel et le budget de consommation du Ranch de Jakiri.
- Le Chef d'Abattoir de Douala ne doit accéder qu'aux données opérationnelles de Douala.
- Les Directeurs Centraux et la Direction Générale disposent d'une vue consolidée sur toutes les unités rattachées.

#### 2. Spécifications Techniques
- Intercepteur / Filtre de sécurité JPA / Hibernate `@Filter` et Spring Security SpEL :
  - Évaluation de l'attribut d'accès : `hasOrgAccess(entity.uniteId)`
  - Utilisation du `materialized_path` : un utilisateur affecté à l'unité de chemin `/1/4/` a automatiquement accès aux sous-unités `/1/4/12/`, `/1/4/15/`, mais pas à `/1/2/`.
- Paramétrage de dérogations pour les profils transverses (Audit Interne, Contrôle de Gestion, Trésorier Central).

#### 3. Critères d'Acceptation (Gherkin)
```gherkin
Fonctionnalité: Cloisonnement des données selon l'unité organisationnelle

  Scénario: Accès restreint pour un responsable de ranch
    Étant donné un utilisateur authentifié affecté au "Ranch de Dumbo"
    Quand il consulte la liste des dépenses et stocks
    Alors seules les données dont l'unité d'imputation est "Ranch de Dumbo" ou l'une de ses sous-unités sont retournées
    Et aucune information relative aux abattoirs ou autres ranches n'est visible

  Scénario: Accès consolidé pour la Direction Générale
    Étant donné un utilisateur membre de la "Direction Générale" à la racine de l'arbre
    Quand il consulte les tableaux de bord budgétaires ou opérationnels
    Alors il a visibilité sur toutes les données de l'ensemble des ranches, abattoirs et magasins
```

---

### Issue #29 : [EPIC-P1] [ORGANIGRAMME] API REST Arborescente et export de la structure organisationnelle

**Milestone :** `Module Organigramme & Structure Organisationnelle`  
**Labels :** `epic:organigramme`, `priority:P1`, `type:integration`  
**Composants impactés :** `com.sodepa.erp.organigramme.presentation.rest.OrganigrammeRestController`

#### 1. Description & Contexte Métier
Le backend doit exposer une API RESTful complète permettant au frontend Angular et aux modules satellites de consommer la structure de l'organigramme sous différentes projections : arbre complet, sous-arbre d'une direction, liste à plat paginée, et exports de gouvernance (Excel, CSV, PDF).

#### 2. Spécifications Techniques
- Endpoints REST :
  - `GET /api/v1/organigramme/arborescence` : Arbre hiérarchique complet au format JSON hiérarchique (nodes/children) avec indicateurs d'effectifs (autorisés, pourvus).
  - `GET /api/v1/organigramme/unites/{id}/sous-arbre` : Sous-arbre restreint à une unité décentralisée (ex: ranch ou abattoir).
  - `GET /api/v1/organigramme/postes` : Liste filtrable des postes (par unité, par statut, par responsable).
  - `GET /api/v1/organigramme/collaborateurs/{id}/hierarchie` : Chaîne managériale ascendante et subordonnés directs.
  - `GET /api/v1/organigramme/export?format=xlsx` : Export de la structure organisationnelle au format Excel structuré.
- Cache Spring `@Cacheable(value = "organigramme-tree")` avec éviction automatique (`@CacheEvict`) lors d'une modification de structure ou d'affectation.

#### 3. Critères d'Acceptation (Gherkin)
```gherkin
Fonctionnalité: Exposition API REST de l'organigramme

  Scénario: Récupération de l'arborescence complète en JSON
    Étant donné un client API authentifié avec un token JWT valide
    Quand il appelle "GET /api/v1/organigramme/arborescence"
    Alors la réponse HTTP est 200 OK
    Et le payload contient le nœud racine "Direction Générale" avec ses enfants récursifs
    Et le temps de réponse est inférieur à 200ms grâce à l'optimisation par cache

  Scénario: Export Excel de l'organigramme
    Étant donné un contrôleur de gestion demandant l'inventaire des unités et effectifs
    Quand il appelle "GET /api/v1/organigramme/export?format=xlsx"
    Alors le fichier binaire Excel est généré avec les colonnes Code, Intitulé, N+1, ETP Autorisé, ETP Actuel
```

---

### Issue #30 : [EPIC-P2] [ORGANIGRAMME] Visualisation interactive de l'arbre et fiches d'unités (SPA Frontend Angular)

**Milestone :** `Module Organigramme & Structure Organisationnelle`  
**Labels :** `epic:organigramme`, `priority:P2`, `type:feature`  
**Composants impactés :** `sodepa-frontend/src/app/features/organigramme`

#### 1. Description & Contexte Métier
Les utilisateurs de la SODEPA (Direction, RH, managers de ranchs et abattoirs) ont besoin d'une interface graphique intuitive et fluide pour visualiser l'organigramme de l'entreprise, naviguer d'une direction à une unité locale, zoomer, replier des branches et consulter les fiches détaillées des postes et de leurs titulaires.

#### 2. Spécifications Techniques
- Module Angular 21 (Zoneless Signals) dans `sodepa-frontend/src/app/features/organigramme` :
  - Composant `OrganigrammeViewerComponent` : rendu d'arbre dynamique (D3.js / SVG natif ou Angular CDK Tree).
  - Cartes de poste : affichage de la photo, du nom du titulaire, de l'intitulé du poste, badge d'intérim éventuel, et statut de vacance du poste.
  - Barre d'outils : recherche d'un collaborateur ou d'un ranch avec centrage automatique sur le nœud correspondant, zoom +/- et réinitialisation.
  - Panneau latéral (Drawer) : fiche détaillée de l'unité sélectionnée (effectifs, liste des sous-services, historique des responsables).
  - Export visuel : bouton d'impression et d'export en image PNG ou document PDF vectoriel.

#### 3. Critères d'Acceptation (Gherkin)
```gherkin
Fonctionnalité: Visualisation interactive de l'organigramme

  Scénario: Recherche et focus sur un ranch décentralisé
    Étant donné l'utilisateur sur la page de l'organigramme interactif
    Quand il saisit "Jakiri" dans la barre de recherche rapide
    Alors l'arbre se déplie automatiquement jusqu'au nœud "Ranch de Jakiri"
    Et la carte de l'unité est mise en surbrillance avec centrage de la vue

  Scénario: Consultation de la fiche de poste et du titulaire
    Étant donné l'arborescence affichée à l'écran
    Quand l'utilisateur clique sur le poste "Directeur d'Abattoir de Douala"
    Alors un panneau latéral s'ouvre affichant la photo du directeur, sa date de nomination, ses subordonnés directs et l'effectif actuel
```
