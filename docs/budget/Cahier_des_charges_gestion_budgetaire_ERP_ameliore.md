# Cahier des charges : Module de gestion budgétaire (ERP)

*Version 1.0 - Document de cadrage fonctionnel et technique*

## 1. Contexte et objectifs

### 1.1 Contexte

Les entreprises pilotent encore souvent leur budget avec des fichiers tableur dispersés, difficiles à consolider, sans traçabilité ni lien direct avec la comptabilité, les achats ou les RH. Ce projet vise à concevoir un module ERP de gestion budgétaire, intégré aux autres modules métier, qui devient la source unique de vérité pour la planification et le suivi financier.

### 1.2 Objectifs

- Centraliser l'élaboration, la validation et le suivi des budgets de l'entreprise.
- Contrôler en temps réel la consommation budgétaire (engagé, réalisé, disponible).
- Automatiser la comparaison budget / réalisé et l'analyse des écarts.
- Fiabiliser les prévisions (reforecast, scénarios) et la trésorerie prévisionnelle.
- Garantir la traçabilité et la conformité (audit, piste d'audit, droits d'accès).
- S'interfacer nativement avec la comptabilité, la finance, les achats, les ventes et les RH.

### 1.3 Indicateurs de succès

| Indicateur | Cible |
| --- | --- |
| Durée du cycle budgétaire annuel | Réduction de 40 % |
| Délai de consolidation | Moins de 24 h après la clôture mensuelle |
| Dépassements budgétaires non détectés | 0 (blocage ou alerte avant engagement) |
| Taux d'adoption par les responsables de centres de coûts | Supérieur à 90 % |
| Fiabilité des prévisions (écart prévision / réel à 3 mois) | Inférieur à 5 % |

## 2. Périmètre

### 2.1 Inclus dans le périmètre

- Structuration budgétaire (axes, périodes, versions).
- Élaboration des budgets (fonctionnement, investissement, masse salariale, trésorerie, ventes).
- Workflow de validation multi-niveaux.
- Contrôle budgétaire à l'engagement et à la réalisation.
- Suivi, analyse des écarts, reforecast, simulations.
- Consolidation multi-entités et multi-devises.
- Reporting, tableaux de bord et exports.
- Intégrations avec les autres modules de l'ERP et systèmes externes.

### 2.2 Hors périmètre (version 1)

- Tenue de la comptabilité générale elle-même (module séparé, interfacé).
- Calcul de la paie (module RH, interfacé).
- Prévisions par intelligence artificielle avancée (prévu en évolution ultérieure).
- Consolidation statutaire et réglementaire des comptes de groupe.

## 3. Acteurs et rôles

| Rôle | Responsabilités principales |
| --- | --- |
| Direction générale | Fixe les orientations, arbitre et valide le budget global |
| Directeur financier (DAF) | Pilote la campagne budgétaire, consolide, valide les enveloppes |
| Contrôleur de gestion | Paramètre les structures, analyse les écarts, produit les reportings |
| Responsable de centre de coûts / de département | Saisit et justifie son budget, suit sa consommation |
| Acheteur / Responsable achats | Engage les dépenses dans le respect des enveloppes |
| Responsable RH | Fournit les hypothèses de masse salariale et d'effectifs |
| Comptable | Contrôle la cohérence entre budget et écritures comptables |
| Trésorier | Exploite le budget pour la trésorerie prévisionnelle |
| Auditeur / commissaire aux comptes | Consulte l'historique et la piste d'audit (lecture seule) |
| Administrateur | Gère les paramètres, droits et référentiels |

## 4. Exigences fonctionnelles

### 4.1 Paramétrage et structure budgétaire

- **RF-01** : Définir des exercices budgétaires et des périodes (mensuel, trimestriel, annuel) avec dates d'ouverture et de clôture.
- **RF-02** : Gérer plusieurs axes d'analyse : entité juridique, département, centre de coûts, centre de profit, projet, produit, activité, zone géographique.
- **RF-03** : Lier chaque ligne budgétaire à un compte du plan comptable (général et analytique).
- **RF-04** : Gérer plusieurs versions : budget initial, budget révisé, reforecast, scénarios (optimiste, central, pessimiste).
- **RF-05** : Configurer des types de budgets : exploitation (charges et produits), investissement (CAPEX), masse salariale, trésorerie, budget par projet.
- **RF-06** : Paramétrer des règles de contrôle par ligne ou par enveloppe : blocage, avertissement, tolérance en pourcentage ou en montant.
- **RF-07** : Gérer une hiérarchie d'enveloppes (enveloppe globale, sous-enveloppes, lignes) avec règles de répartition.

### 4.2 Élaboration du budget

- **RF-10** : Ouvrir et clôturer une campagne budgétaire avec calendrier, rappels automatiques et suivi d'avancement par contributeur.
- **RF-11** : Saisie par grille (type tableur) avec copier-coller, import Excel / CSV et contrôles de cohérence.
- **RF-12** : Méthodes d'initialisation : reprise du réalisé N-1, du budget N-1, application d'un coefficient d'évolution, saisie à zéro (base zéro).
- **RF-13** : Ventilation automatique d'un montant annuel par période (linéaire, saisonnalité, profil personnalisé).
- **RF-14** : Budgets pilotés par les volumes et les prix (quantités x tarif x taux) avec hypothèses nommées et réutilisables.
- **RF-15** : Commentaires et justificatifs rattachés à chaque ligne (pièces jointes, hypothèses).
- **RF-16** : Budget de masse salariale alimenté par les données RH (effectifs, grilles, charges sociales, recrutements prévus, départs, augmentations).
- **RF-17** : Budget d'investissement : fiche projet d'investissement, échéancier de décaissement, amortissements prévisionnels.
- **RF-18** : Budget de ventes et de chiffre d'affaires, alimenté par le CRM, les contrats et les carnets de commandes.
- **RF-19** : Génération automatique des budgets de trésorerie et du compte de résultat prévisionnel à partir des budgets validés.

### 4.3 Validation et workflow

- **RF-20** : Circuit de validation configurable à plusieurs niveaux (responsable, directeur, DAF, direction générale).
- **RF-21** : Statuts : brouillon, soumis, en révision, validé, verrouillé, archivé.
- **RF-22** : Possibilité de rejeter avec motif, de demander des modifications et d'annoter.
- **RF-23** : Notifications (e-mail et in-app) à chaque étape.
- **RF-24** : Délégation de validation en cas d'absence, avec traçabilité.
- **RF-25** : Verrouillage d'une version validée ; toute modification passe par une demande de révision budgétaire formalisée.
- **RF-26** : Arbitrage : comparaison des demandes à l'enveloppe disponible, simulation des coupes et reports.

### 4.4 Exécution et contrôle budgétaire

- **RF-30** : Calcul permanent de trois états : **budgété**, **engagé** (commandes, contrats, demandes d'achat approuvées) et **réalisé** (factures comptabilisées, paie, notes de frais), ainsi que du **disponible** = budget validé - engagements ouverts - réalisé comptabilisé non déjà couvert par un engagement ouvert.
- **RF-31** : Contrôle budgétaire à la création d'une demande d'achat, d'une commande ou d'une note de frais, avec blocage, alerte ou demande de dérogation selon les règles.
- **RF-32** : Gestion des virements budgétaires (transfert entre lignes ou enveloppes) avec workflow de validation.
- **RF-33** : Gestion des budgets complémentaires et des reports de crédits d'une période ou d'un exercice sur l'autre.
- **RF-34** : Libération automatique de l'engagement lors de la réception, de la facturation ou de l'annulation de la commande.
- **RF-35** : Gestion des engagements pluriannuels (contrats, investissements) avec répartition par exercice.
- **RF-36** : Alertes configurables : seuil de consommation (80 %, 100 %), dépassement, écart anormal, ligne non consommée.

### 4.5 Suivi, analyse et prévision

- **RF-40** : Tableau de comparaison budget / réalisé / engagé / N-1 par axe, période et compte, avec écarts en valeur et en pourcentage.
- **RF-41** : Analyse des écarts : décomposition en effet volume, effet prix, effet de périmètre ; commentaire obligatoire au-delà d'un seuil.
- **RF-42** : Forçage ou reforecast périodique (mensuel ou trimestriel) : réalisé à date + prévision du reste de l'année.
- **RF-43** : Simulation de scénarios (what-if) sans impact sur le budget officiel, avec comparaison des scénarios.
- **RF-44** : Rolling forecast (prévision glissante sur 12 ou 18 mois).
- **RF-45** : Analyse de rentabilité par projet, produit, client ou activité.
- **RF-46** : Historisation complète des versions pour comparaison dans le temps.

### 4.6 Consolidation, multi-entités et multi-devises

- **RF-50** : Consolidation des budgets de plusieurs entités juridiques, avec éliminations intra-groupe.
- **RF-51** : Gestion multi-devises : devise de saisie, devise de l'entité, devise de consolidation ; taux budgétaires fixes et taux réels pour le suivi.
- **RF-52** : Gestion multi-sociétés avec cloisonnement des données et des droits.
- **RF-53** : Gestion des refacturations internes et des clés de répartition (frais de siège, services partagés).

### 4.7 Reporting et tableaux de bord

- **RF-60** : Tableaux de bord par profil : direction (vision globale), DAF (consolidé et trésorerie), responsable de centre (son périmètre).
- **RF-61** : Rapports standards : compte de résultat budgétaire, budget par nature et par destination, suivi des engagements, tableau de bord des investissements, suivi de la masse salariale.
- **RF-62** : Générateur de rapports personnalisables (filtres, regroupements, colonnes).
- **RF-63** : Exports Excel, CSV, PDF et planification d'envois automatiques.
- **RF-64** : Drill-down : du chiffre agrégé jusqu'à la pièce comptable ou la commande d'origine.
- **RF-65** : Visualisations : histogrammes, courbes de consommation cumulée, waterfall des écarts, cartes de chaleur.

### 4.8 Clôture et archivage

- **RF-70** : Clôture périodique : verrouillage de la période, contrôle de cohérence avec la comptabilité (rapprochement budget / compta).
- **RF-71** : Clôture annuelle : report des engagements non soldés, génération des soldes d'ouverture budgétaires.
- **RF-72** : Archivage des exercices avec conservation de la piste d'audit selon la durée légale.

### 4.9 Organisation et structure hiérarchique

Le budget s'appuie sur l'organigramme, maintenu par le module RH, pour répartir les enveloppes, désigner les responsables, router les validations et filtrer les droits d'accès.

- **RF-80** : Gérer un référentiel d'unités organisationnelles hiérarchisées (société, direction, département, service, équipe), importé du module RH ou de l'annuaire d'entreprise, avec dates de validité.
- **RF-81** : Rattacher chaque centre de coûts, projet ou ligne budgétaire à une unité organisationnelle et à un responsable budgétaire.
- **RF-82** : Décliner les enveloppes de haut en bas selon la hiérarchie et consolider de bas en haut, à chaque niveau de l'arbre.
- **RF-83** : Générer le circuit de validation à partir de la ligne hiérarchique (n+1, n+2, direction, DAF), avec des seuils de montant par niveau.
- **RF-84** : Limiter la visibilité et la saisie de chaque utilisateur à son unité et à ses sous-unités (droits hérités de l'arbre), avec exceptions explicites.
- **RF-85** : Gérer les délégations de validation et les intérims, avec période, périmètre et traçabilité.
- **RF-86** : Gérer les rattachements matriciels (collaborateur ou poste rattaché à plusieurs unités ou responsables) avec clés de répartition.
- **RF-87** : Historiser les réorganisations (fusion, scission, changement de rattachement) et restituer le budget et le réalisé à périmètre constant ou à la structure en vigueur.
- **RF-88** : Signaler les anomalies : centre de coûts sans responsable, responsable ayant quitté l'entreprise, unité sans enveloppe.

## 5. Dépendances avec les autres métiers

Le budget est un module transversal : il consomme des données de plusieurs métiers (entrées) et leur fournit des cadres et des limites (sorties). Le tableau suivant détaille chaque interaction.

### 5.1 Vue d'ensemble

| Métier / module | Données reçues par le budget (entrées) | Données fournies par le budget (sorties) | Criticité |
| --- | --- | --- | --- |
| Comptabilité générale | Plan comptable, écritures réalisées, soldes, clôtures de période | Contrôles de cohérence, écritures prévisionnelles, budget par compte | Critique |
| Comptabilité analytique | Axes analytiques, clés de répartition, coûts réels par centre | Budgets par centre de coûts, projet et activité | Critique |
| Finance et trésorerie | Soldes bancaires, échéanciers, taux de change, financements | Prévisions d'encaissements et de décaissements, plan de trésorerie | Critique |
| Achats | Demandes d'achat, commandes, contrats, réceptions | Disponible budgétaire, blocage ou autorisation d'engager | Critique |
| Ventes et facturation | Commandes clients, facturation, carnet de commandes, pipeline | Objectifs de ventes, budget de chiffre d'affaires | Élevée |
| Ressources humaines et paie | Effectifs, grilles salariales, charges, recrutements, absences | Enveloppe de masse salariale, autorisation de recrutement | Critique |
| Immobilisations et investissements | Actifs, plans d'amortissement, projets CAPEX | Enveloppes d'investissement, dotations prévisionnelles | Élevée |
| Gestion de projets | Charges, jalons, avancement, temps passés | Budget par projet, alertes de dérive | Élevée |
| Stocks et production | Niveaux de stock, coûts standards, plans de production | Budget d'approvisionnement, budget de production | Moyenne |
| Notes de frais et déplacements | Dépenses des collaborateurs, avances | Contrôle d'enveloppe par service | Moyenne |
| Fiscalité | Taux de TVA, impôts et taxes, calendrier fiscal | Prévision de TVA, impôt sur les sociétés, taxes | Moyenne |
| CRM / commercial | Opportunités, probabilités de gain, contrats récurrents | Objectifs commerciaux par segment | Moyenne |
| Juridique et assurances | Contrats, échéances, primes, litiges provisionnés | Budget de provisions et de frais juridiques | Faible |
| Direction générale / stratégie | Plan stratégique, objectifs pluriannuels | Budget aligné sur le plan, tableaux de bord de pilotage | Élevée |
| Organisation / organigramme (RH, direction) | Unités organisationnelles, liens hiérarchiques, responsables, rattachements, entités juridiques | Budgets par unité, périmètres de droits, circuits et seuils de validation, consolidation par niveau | Critique |

### 5.2 Détail des interactions par métier

#### Comptabilité générale

- **Entrée** : le plan comptable et les écritures validées alimentent le « réalisé » ; la clôture comptable déclenche la clôture budgétaire de la période.
- **Sortie** : le budget est ventilé par compte comptable pour permettre la comparaison directe avec la balance.
- **Règles** : un compte inexistant dans le plan comptable ne peut pas être budgété ; toute écriture sur un compte à contrôle budgétaire déclenche une vérification de l'enveloppe.
- **Points d'attention** : synchronisation des périodes, gestion des écritures de régularisation (charges à payer, produits constatés d'avance), rapprochement obligatoire en fin de mois.

#### Comptabilité analytique et contrôle de gestion

- **Entrée** : structure des centres de coûts, clés de répartition, coûts réels par axe.
- **Sortie** : budgets par axe analytique, écarts par responsable.
- **Règles** : un axe analytique obligatoire (par exemple le centre de coûts) doit être renseigné sur chaque engagement soumis au contrôle.

#### Finance et trésorerie

- **Entrée** : taux de change, conditions de financement, soldes de trésorerie réels.
- **Sortie** : échéancier prévisionnel d'encaissements et de décaissements issu du budget (décalage selon les délais de paiement clients et fournisseurs), besoins de financement.
- **Règles** : le budget de trésorerie dérive des budgets d'exploitation et d'investissement et doit rester cohérent avec eux.

#### Achats

- **Entrée** : demandes d'achat, bons de commande, contrats cadres, réceptions, factures fournisseurs.
- **Sortie** : réponse de contrôle budgétaire en temps réel (disponible, bloqué, dérogation requise).
- **Règles** : l'engagement est créé à l'approbation de la commande, ajusté à la réception, soldé à la facturation. Les écarts entre commande et facture sont régularisés automatiquement.
- **Points d'attention** : commandes pluriannuelles, avoirs, annulations partielles, achats hors circuit (cartes d'achat).

#### Ventes et facturation

- **Entrée** : carnet de commandes, facturation réelle, abonnements et contrats récurrents.
- **Sortie** : objectifs de chiffre d'affaires par commercial, produit ou région.
- **Règles** : le budget de ventes pilote les budgets de coûts variables (achats, commissions, production).

#### Ressources humaines et paie

- **Entrée** : effectif actuel, postes budgétés, grilles et rémunérations, charges patronales, primes, recrutements et départs prévus.
- **Sortie** : enveloppe de masse salariale par département, plafond d'effectifs (nombre de postes), autorisation ou blocage des recrutements.
- **Règles** : une création de poste ou un recrutement hors budget déclenche un workflow de dérogation. Les données de rémunération sont confidentielles et accessibles uniquement aux profils habilités.
- **Points d'attention** : conformité aux règles de protection des données personnelles, calcul des charges sociales selon la législation locale, gestion des primes et provisions pour congés.

#### Organisation et organigramme

- **Entrée** : arbre des unités organisationnelles, responsables, liens hiérarchiques, rattachements des postes et des collaborateurs, dates de validité.
- **Sortie** : budgets et enveloppes par unité, plafonds d'effectifs, périmètres de droits, circuits et seuils de validation.
- **Règles** : le module RH reste maître de l'organigramme et le budget le consomme en lecture. L'organigramme hiérarchique et la structure analytique (centres de coûts, projets) sont deux référentiels distincts, reliés par une table de correspondance.
- **Points d'attention** : réorganisations en cours d'exercice, organisations matricielles, comparaison à périmètre constant, mise à jour des départs et arrivées de responsables.

#### Immobilisations et investissements

- **Entrée** : registre des actifs, plans d'amortissement existants.
- **Sortie** : budget CAPEX par projet, dotations aux amortissements prévisionnelles qui impactent le compte de résultat budgétaire.
- **Règles** : une demande d'acquisition d'immobilisation est contrôlée sur l'enveloppe d'investissement et non sur l'enveloppe d'exploitation.

#### Gestion de projets

- **Entrée** : coûts réels imputés au projet, avancement, charges restantes.
- **Sortie** : budget initial du projet, seuil d'alerte, reste à faire (EAC / ETC).

#### Stocks et production

- **Entrée** : coûts standards, niveaux de stock, nomenclatures.
- **Sortie** : budget d'achats de matières, budget de production, valorisation prévisionnelle des stocks.

#### Fiscalité

- **Entrée** : taux et calendrier fiscaux.
- **Sortie** : prévisions de TVA, de charges fiscales et d'impôt sur les résultats.

### 5.3 Flux de données principaux

| N° | Flux | Source | Destination | Fréquence | Mode |
| --- | --- | --- | --- | --- | --- |
| F1 | Plan comptable et référentiels | Comptabilité | Budget | À la modification | API / événement |
| F2 | Écritures réalisées | Comptabilité | Budget | Quotidienne ou temps réel | API / événement |
| F3 | Contrôle budgétaire à l'engagement | Achats | Budget | Temps réel | API synchrone |
| F4 | Engagements (commandes, contrats) | Achats | Budget | Temps réel | Événement |
| F5 | Données d'effectifs et de rémunération | RH / paie | Budget | Mensuelle et à la campagne | Batch / API |
| F6 | Budget de masse salariale | Budget | RH | À la validation | API |
| F7 | Budget de trésorerie prévisionnel | Budget | Trésorerie | À chaque version validée | API |
| F8 | Soldes et taux de change | Finance | Budget | Quotidienne | Batch |
| F9 | Commandes et facturation clients | Ventes | Budget | Quotidienne | API / batch |
| F10 | Plan d'investissement et amortissements | Immobilisations | Budget | À la campagne | API |
| F11 | Notes de frais | Frais | Budget | Temps réel | Événement |
| F12 | Données de projet | Gestion de projet | Budget | Hebdomadaire | API |
| F13 | Organigramme, responsables et rattachements | RH / référentiel organisation | Budget | À la modification | API / événement |

### 5.4 Règles d'interaction transverses

1. Le budget ne crée jamais d'écriture comptable réelle ; il ne produit que des écritures prévisionnelles ou des contrôles.
2. Les référentiels (plan comptable, centres de coûts, projets, tiers) restent maîtrisés par leur module d'origine ; le budget les consomme en lecture.
3. Un engagement n'est valide que s'il porte les axes obligatoires (entité, compte, centre de coûts, période).
4. Toute dérogation budgétaire est tracée (qui, quand, pourquoi, montant).
5. En cas d'indisponibilité du module budgétaire, une règle de repli est définie (autorisation temporaire avec régularisation différée).

## 6. Modèle de données (entités principales)

| Entité | Description | Attributs clés |
| --- | --- | --- |
| Exercice budgétaire | Année ou période de référence | code, dates, statut |
| Version budgétaire | Initial, révisé, reforecast, scénario | code, type, exercice, statut, auteur |
| Enveloppe | Montant alloué à un périmètre | périmètre, montant, devise, parent |
| Ligne budgétaire | Montant par compte, axe et période | compte, centre de coûts, projet, période, montant |
| Hypothèse | Paramètre réutilisable (taux, volume, prix) | nom, valeur, période, source |
| Engagement | Montant réservé par une commande ou un contrat | origine, pièce, ligne budgétaire, montant, statut |
| Réalisé | Montant comptabilisé | écriture, compte, axe, date, montant |
| Virement budgétaire | Transfert entre lignes | source, cible, montant, motif, statut |
| Règle de contrôle | Paramétrage des blocages et alertes | périmètre, seuil, type d'action |
| Campagne budgétaire | Processus d'élaboration | calendrier, contributeurs, statut |
| Validation | Étape de workflow | valideur, décision, date, commentaire |
| Journal d'audit | Piste d'audit | utilisateur, action, avant / après, horodatage |
| Unité organisationnelle | Nœud de l'organigramme (direction, département, service, équipe) | code, libellé, type, unité parente, responsable, entité juridique, dates de validité |
| Délégation | Transfert temporaire du pouvoir de validation | délégant, délégataire, périmètre, période, motif |

## 7. Exigences non fonctionnelles

### 7.1 Performance

- Affichage d'un tableau de bord standard en moins de 3 secondes.
- Contrôle budgétaire synchrone en moins de 1 seconde.
- Consolidation complète d'un groupe en moins de 5 minutes.
- Prise en charge d'au moins 500 utilisateurs simultanés et de plusieurs millions de lignes budgétaires.

### 7.2 Sécurité et conformité

- Authentification forte (SSO, MFA) et gestion des rôles (RBAC) jusqu'au niveau ligne et axe analytique.
- Chiffrement des données au repos et en transit.
- Journal d'audit infalsifiable de toutes les modifications.
- Séparation des tâches (celui qui saisit ne valide pas).
- Conformité aux règles de protection des données personnelles applicables, notamment pour les données de rémunération.
- Conformité aux référentiels comptables applicables (par exemple SYSCOHADA, OHADA, IFRS ou PCG selon le pays).

### 7.3 Disponibilité et reprise

- Disponibilité cible de 99,5 % ou plus, hors maintenance planifiée.
- Sauvegardes quotidiennes, objectif de reprise (RPO) inférieur à 1 h et délai de reprise (RTO) inférieur à 4 h.

### 7.4 Ergonomie

- Interface web responsive, saisie en grille type tableur, raccourcis clavier.
- Multilingue (français et anglais au minimum) et multi-devises.
- Accessibilité conforme aux bonnes pratiques (WCAG niveau AA).

### 7.5 Évolutivité et maintenabilité

- Architecture modulaire et API ouvertes (REST, webhooks).
- Paramétrage sans développement (champs, règles, workflows).
- Documentation technique et fonctionnelle versionnée, tests automatisés.

## 8. Architecture technique suggérée

- **Couche présentation** : application web (par exemple React ou Vue) avec composants de grille de saisie.
- **Couche métier** : services modulaires (budget, engagements, workflow, consolidation, reporting) exposés par API REST ; moteur de règles et de workflow configurable.
- **Données** : base relationnelle (PostgreSQL) pour les données transactionnelles ; entrepôt ou cube analytique pour le reporting volumineux.
- **Intégration** : bus d'événements (Kafka, RabbitMQ) pour les engagements en temps réel ; connecteurs API et fichiers pour les systèmes externes.
- **Sécurité** : fournisseur d'identité (OIDC / SAML), journalisation centralisée.
- **Déploiement** : conteneurs, CI/CD, environnements de développement, recette et production.

Ces choix sont indicatifs et doivent être confirmés selon les compétences de l'équipe et le contexte de l'ERP existant.

## 9. Planning et feuille de route

| Lot | Contenu | Durée indicative |
| --- | --- | --- |
| Lot 0 | Cadrage, ateliers métiers, spécifications détaillées | 4 à 6 semaines |
| Lot 1 (MVP) | Paramétrage, saisie budgétaire, workflow de validation, comparaison budget / réalisé, intégration comptabilité | 3 à 4 mois |
| Lot 2 | Contrôle à l'engagement (achats), engagements, virements, alertes | 2 à 3 mois |
| Lot 3 | Masse salariale (RH), investissements, trésorerie prévisionnelle | 3 mois |
| Lot 4 | Reforecast, scénarios, consolidation multi-entités et multi-devises | 3 mois |
| Lot 5 | Reporting avancé, tableaux de bord, analyses d'écarts, optimisation | 2 mois |
| Lot 6 | Évolutions (prévision par IA, rolling forecast avancé) | À définir |

## 10. Risques et points de vigilance

| Risque | Impact | Mesure d'atténuation |
| --- | --- | --- |
| Référentiels incohérents entre modules | Élevé | Gouvernance des données, référentiel maître unique |
| Résistance au changement des responsables de budget | Élevé | Accompagnement, formation, interface proche du tableur |
| Performance sur gros volumes | Moyen | Tests de charge, agrégations pré-calculées |
| Complexité des règles de contrôle | Moyen | Paramétrage progressif, jeu de règles par défaut |
| Dépendance forte à la comptabilité | Élevé | Interfaces standardisées, rapprochement automatisé |
| Confidentialité des données RH | Élevé | Droits fins, chiffrement, journalisation des accès |
| Périmètre trop large dès la première version | Élevé | Livraison par lots, MVP resserré |
| Réorganisations fréquentes qui rompent la comparabilité | Élevé | Historisation des structures, restitution à périmètre constant |

## 11. Critères d'acceptation

1. Un budget annuel complet peut être élaboré, validé et verrouillé pour au moins une entité.
2. Le comparatif budget / réalisé est cohérent à l'euro près avec la balance comptable.
3. Une commande dépassant l'enveloppe est bloquée ou envoyée en dérogation, selon la règle paramétrée.
4. Toutes les modifications sont retrouvables dans le journal d'audit.
5. Les rôles et droits sont respectés (test de séparation des tâches).
6. Les exports et tableaux de bord répondent aux exigences de performance.
7. Les interfaces avec comptabilité, achats et RH fonctionnent en conditions de recette.

## 12. Questions ouvertes à clarifier

- Quelle est la taille cible des entreprises clientes (PME, ETI, grands groupes) ?
- Le produit sera-t-il un module d'un ERP existant (Odoo, SAP, Dolibarr, ERPNext...) ou une solution autonome ?
- Quel référentiel comptable et quelle législation de paie faut-il supporter en priorité ?
- Le contrôle budgétaire doit-il être bloquant par défaut ou seulement informatif ?
- Quels niveaux de consolidation sont nécessaires (mono-entité, multi-entités, groupe) ?
- Quel mode de déploiement est visé (SaaS multi-clients, installation chez le client) ?

## 13. Glossaire

| Terme | Définition |
| --- | --- |
| Budgété | Montant prévu et validé pour une période |
| Engagé | Montant réservé par une commande ou un contrat non encore facturé |
| Réalisé | Montant effectivement comptabilisé |
| Disponible | Budget validé moins engagements ouverts et réalisé non déjà couvert par un engagement ouvert, afin d'éviter le double comptage |
| Reforecast | Révision de la prévision en cours d'année (réalisé + reste à faire) |
| CAPEX | Dépenses d'investissement |
| OPEX | Dépenses d'exploitation |
| Centre de coûts | Unité d'analyse à laquelle sont imputées les charges |
| Virement budgétaire | Transfert de crédits d'une ligne vers une autre |
| Rolling forecast | Prévision glissante sur un horizon constant |
|  |  |


---

## 14. Fonctionnalités complémentaires recommandées

Cette section complète le périmètre initial sans remplacer les exigences déjà définies. Les fonctionnalités marquées **P1** sont prioritaires pour un ERP de gestion budgétaire ; **P2** peut être livré après stabilisation du socle ; **P3** relève d'une évolution.

### 14.1 Contrôle budgétaire avancé et prévention des dépassements — P1

- **RF-90** : Contrôler le budget à plusieurs jalons configurables : demande d'achat, validation de la demande, commande, avenant, réception, facture et paiement.
- **RF-91** : Distinguer l'engagement demandé, réservé, confirmé, partiellement consommé, soldé, annulé et en litige.
- **RF-92** : Réserver temporairement une enveloppe pendant la validation d'une demande ; libérer automatiquement la réservation en cas de rejet ou d'expiration.
- **RF-93** : Appliquer une règle de contrôle hiérarchique : ligne budgétaire, sous-enveloppe, enveloppe départementale, enveloppe de direction et enveloppe globale. Une règle de niveau supérieur ne doit pas être contournée par une ligne disposant encore de crédits.
- **RF-94** : Définir des politiques de dépassement par type de dépense : blocage strict, avertissement, tolérance, ou dérogation exceptionnelle avec justification et approbation distincte.
- **RF-95** : Détecter les engagements concurrents afin d'éviter que deux demandes simultanées consomment le même disponible. La réservation et le contrôle doivent être atomiques côté serveur.
- **RF-96** : Prévoir un mode dégradé explicite si le service budgétaire est indisponible : blocage par défaut pour les dépenses critiques, ou autorisation temporaire plafonnée et mise en régularisation, selon une politique approuvée.
- **RF-97** : Gérer les engagements récurrents, les contrats-cadres, les bons de commande ouverts, les avenants et les commandes partiellement réceptionnées.
- **RF-98** : Expliquer le résultat du contrôle à l'utilisateur : montant demandé, disponible avant/après, règle appliquée, niveau dépassé et action attendue.

### 14.2 Workflow de dérogation, maker-checker et délégation — P1

- **RF-100** : Interdire à un utilisateur d'approuver sa propre demande, son propre virement ou sa propre dérogation, sauf exception explicitement autorisée et auditée.
- **RF-101** : Paramétrer les circuits selon le montant, la nature de dépense, l'entité, le centre de coûts, le projet, le niveau de risque et le dépassement.
- **RF-102** : Permettre une approbation séquentielle ou parallèle, avec quorum éventuel et échéance par étape.
- **RF-103** : Relancer automatiquement les approbateurs en retard et escalader vers leur N+1 après un délai paramétré.
- **RF-104** : Exiger un motif, un justificatif et l'identité du demandeur pour toute dérogation, modification après validation ou annulation d'engagement.
- **RF-105** : Conserver la décision, l'horodatage, le commentaire, la version des règles et les délégations actives au moment de la décision.
- **RF-106** : Permettre le remplacement temporaire d'un valideur sans perdre la responsabilité ni l'historique de la délégation.

### 14.3 Référentiel, qualité et gouvernance des données — P1

- **RF-110** : Définir un propriétaire fonctionnel pour chaque référentiel consommé : plan comptable, tiers, entités, projets, centres de coûts, devises et unités organisationnelles.
- **RF-111** : Détecter les doublons, codes invalides, axes manquants, comptes inactifs, centres de coûts sans responsable et rattachements à dates incohérentes.
- **RF-112** : Gérer les dates d'effet des référentiels et conserver l'historique des changements afin qu'une réorganisation ne réécrive pas l'historique budgétaire.
- **RF-113** : Mettre à disposition un écran de suivi des interfaces : dernier échange, volume reçu, erreurs, rejets, possibilité de rejouer un message sans créer de doublon.
- **RF-114** : Fournir des rapports de qualité des données avant l'ouverture d'une campagne et avant les clôtures.

### 14.4 Traçabilité, sécurité et conformité — P1

- **RF-120** : Journaliser les consultations sensibles, exports, changements de droits, validations, dérogations, imports, clôtures et actions d'administration.
- **RF-121** : Enregistrer les valeurs avant/après, l'auteur, la date, le canal, le motif, l'identifiant de corrélation et la source métier de chaque changement significatif.
- **RF-122** : Rendre les journaux d'audit non modifiables par les utilisateurs métier et les administrateurs fonctionnels ; restreindre leur purge à une politique de rétention approuvée.
- **RF-123** : Appliquer les droits au niveau de l'entité, de l'unité organisationnelle, de l'axe analytique, du type de budget et de la sensibilité de la donnée.
- **RF-124** : Masquer les rémunérations individuelles dans les vues agrégées et appliquer des restrictions spécifiques aux exports RH.
- **RF-125** : Ajouter une revue périodique des habilitations et une désactivation des accès en cas de départ ou de changement de fonction.
- **RF-126** : Protéger les imports et exports contre les fichiers malformés, les formules Excel dangereuses et les fuites de données ; tracer les téléchargements.
- **RF-127** : Gérer la rétention, l'archivage et la purge selon la politique de conservation et les obligations applicables, avec preuve de purge lorsque celle-ci est autorisée.

### 14.5 Imports, exports et expérience utilisateur — P1

- **RF-130** : Fournir un assistant d'import avec prévisualisation, validation ligne par ligne, rapport d'erreurs téléchargeable et mode simulation avant intégration.
- **RF-131** : Permettre les imports partiels uniquement si la politique le permet ; sinon rejeter le lot entier pour garantir l'intégrité.
- **RF-132** : Gérer les modèles Excel versionnés, avec identifiant de campagne, version budgétaire, devise, période et axes obligatoires.
- **RF-133** : Proposer filtres enregistrés, favoris, vues personnelles, recherche globale et navigation clavier dans les grilles.
- **RF-134** : Afficher clairement les montants en cours de validation, validés, verrouillés et simulés, avec des codes visuels accessibles.
- **RF-135** : Permettre le commentaire collectif sur une ligne et la résolution des observations, avec auteur et statut.
- **RF-136** : Prévenir les pertes de saisie, gérer les conflits de modification concurrente et afficher les changements effectués depuis l'ouverture de la ligne.

### 14.6 Prévisions et pilotage — P2

- **RF-140** : Calculer les prévisions à terminaison (EAC), le reste à engager, le reste à payer et le reste à faire par projet, contrat, centre de coûts et exercice.
- **RF-141** : Ajouter des indicateurs de dérive : tendance de consommation, vitesse mensuelle, saisonnalité, atterrissage annuel et délai moyen de consommation.
- **RF-142** : Comparer plusieurs versions avec une analyse des changements : lignes ajoutées, supprimées, augmentées, réduites et motifs.
- **RF-143** : Autoriser la création d'un scénario à partir d'une version existante, avec hypothèses partagées ou isolées, sans modifier le budget officiel.
- **RF-144** : Ajouter une simulation de sensibilité sur les prix, volumes, taux de change, masse salariale, dates de paiement et retards d'encaissement.
- **RF-145** : Permettre aux responsables de documenter les causes d'écart à l'aide d'une taxonomie commune : prix, volume, calendrier, périmètre, change, erreur de saisie ou événement exceptionnel.
- **RF-146** : Afficher un indicateur de confiance des prévisions basé sur la qualité et la fraîcheur des données, sans présenter une estimation comme une certitude.
- **RF-147** : Préparer une évolution vers des recommandations assistées par IA, avec explication, validation humaine et possibilité de désactivation ; aucune écriture budgétaire ne doit être appliquée automatiquement.

### 14.7 Trésorerie et cycle fournisseur — P2

- **RF-150** : Distinguer date de commande, date de livraison, date de facture, échéance fournisseur et date probable de paiement.
- **RF-151** : Générer plusieurs vues de trésorerie : prévisionnelle, engagée, probable et réalisée.
- **RF-152** : Simuler des retards de paiement client, des délais fournisseurs et des variations de change.
- **RF-153** : Identifier les dépenses engagées mais non facturées, les factures sans commande et les écarts entre commande, réception et facture.
- **RF-154** : Permettre un rapprochement explicite entre la prévision budgétaire et les flux de trésorerie réels, avec justification des écarts.

### 14.8 API et intégrations robustes — P1

- **RF-160** : Exposer des API versionnées et documentées pour consulter le disponible, contrôler un engagement, créer/libérer une réservation et récupérer les agrégats budgétaires.
- **RF-161** : Rendre les opérations sensibles idempotentes à l'aide d'une clé d'idempotence et d'un identifiant métier externe.
- **RF-162** : Définir des contrats d'événements versionnés pour les commandes, réceptions, factures, écritures comptables, changements d'organigramme et clôtures.
- **RF-163** : Garantir la reprise des événements avec détection des doublons, stratégie de retry, file d'échec (DLQ) et procédure de réconciliation.
- **RF-164** : Assurer la traçabilité de bout en bout par identifiant de corrélation entre le module budgétaire et les modules sources.
- **RF-165** : Publier des webhooks sécurisés avec signature, politique de retry et mécanisme de rejeu.
- **RF-166** : Définir des seuils de limitation de débit, des délais d'attente et des politiques de circuit breaker pour les intégrations externes.

### 14.9 Exploitation, supervision et reprise — P1

- **RF-170** : Fournir des indicateurs techniques : disponibilité des API, latence, taux d'erreur, retard des événements, échecs d'import et durée de consolidation.
- **RF-171** : Déclencher des alertes opérationnelles en cas d'échec d'intégration, d'écart de rapprochement, de saturation ou de retard de traitement.
- **RF-172** : Tester périodiquement la restauration des sauvegardes et documenter les procédures de reprise.
- **RF-173** : Prévoir des migrations de schéma réversibles lorsque cela est réaliste, et une stratégie de retour arrière des versions applicatives.
- **RF-174** : Séparer les environnements et les secrets ; ne jamais exposer les identifiants ou données financières réelles dans les journaux techniques.
- **RF-175** : Fournir un tableau de bord de santé fonctionnelle : campagnes en retard, validations bloquées, interfaces en erreur et rapprochements non terminés.

### 14.10 Administration et paramétrage sans développement — P2

- **RF-180** : Gérer les règles de contrôle par date d'effet, priorité, périmètre et version ; détecter les règles contradictoires avant activation.
- **RF-181** : Simuler une règle sur un échantillon d'opérations avant son activation en production.
- **RF-182** : Fournir un historique des changements de paramétrage et permettre une comparaison entre deux configurations.
- **RF-183** : Exporter et importer les configurations de référence entre environnements, avec validation et journalisation.
- **RF-184** : Prévoir un bac à sable de simulation qui n'émet aucun engagement réel et ne modifie aucune version officielle.

---

## 15. Règles métier à préciser et sécuriser

Les règles suivantes doivent être décidées en atelier métier avant développement. Les propositions ci-dessous constituent une base recommandée, à valider par le DAF et les responsables métiers.

1. **Pas de double comptage** : une facture liée à une commande doit diminuer l'engagement ouvert à hauteur de la part facturée et augmenter le réalisé ; le même montant ne doit pas réduire deux fois le disponible.
2. **Réservation atomique** : deux demandes concurrentes ne peuvent pas réserver le même crédit disponible.
3. **Dérogation explicite** : toute autorisation de dépasser le budget exige un motif, un valideur habilité, un montant maximal et une durée de validité.
4. **Version officielle unique** : une seule version est désignée comme budget de référence pour un périmètre et un exercice donnés ; les scénarios restent isolés.
5. **Pas de modification silencieuse** : une version validée est immuable. Toute correction produit une nouvelle version ou une demande de révision historisée.
6. **Clôture contrôlée** : aucune clôture ne doit masquer les engagements ouverts, les factures en attente ou les événements d'intégration en erreur.
7. **Événements rejouables** : rejouer un événement technique ne doit pas créer un doublon d'engagement ou de réalisé.
8. **Périodes et dates d'effet** : chaque mouvement utilise une date métier, une date comptable et une date de traitement lorsque nécessaire.
9. **Arrondis et devises** : la précision, les règles d'arrondi, la devise de référence et la source des taux de change doivent être explicites.
10. **Périmètre des droits** : un utilisateur ne voit que les données autorisées, y compris dans les exports, les recherches, les agrégats et les API.
11. **Désynchronisation des référentiels** : un axe devenu inactif ne peut pas être utilisé pour une nouvelle opération, mais reste consultable sur les historiques.
12. **Règle de repli** : toute autorisation en mode dégradé doit avoir un plafond, une date d'expiration, un responsable et une régularisation obligatoire.

---

## 16. Modèle de données à compléter

Les entités suivantes sont recommandées pour couvrir le cycle de vie opérationnel et les exigences d'audit.

| Entité complémentaire | Rôle | Attributs clés indicatifs |
| --- | --- | --- |
| Réservation budgétaire | Crédit temporairement immobilisé pendant une demande | référence, source, montant, devise, statut, expiration |
| Mouvement budgétaire | Historique immuable des impacts sur les enveloppes | type, montant, sens, date métier, source, référence externe |
| Dérogation budgétaire | Autorisation exceptionnelle | motif, montant maximal, demandeur, valideur, échéance, statut |
| Rapprochement | Compare les mouvements budgétaires aux sources | source, période, totaux, écarts, statut, date |
| Import budgétaire | Suivi des lots d'import | fichier, empreinte, auteur, date, statut, erreurs |
| Événement d'intégration | Suivi du traitement d'un événement externe | identifiant, type, version, statut, tentatives, corrélation |
| Règle de workflow | Décrit le circuit de validation | périmètre, conditions, étapes, seuils, version |
| Justificatif | Pièce associée à une ligne, une dérogation ou une validation | nom, type, empreinte, stockage, auteur |
| Clôture budgétaire | Résultat du contrôle de fin de période | période, contrôles, écarts, responsable, statut |
| Prévision périodique | Stocke les hypothèses et montants d'une prévision | version, période, axe, montant, hypothèse, source |
| Historique d'habilitation | Trace les changements d'accès | utilisateur, rôle, périmètre, auteur, date d'effet |
| Notification | Suit l'envoi et la lecture d'une alerte | destinataire, canal, modèle, statut, date, tentatives |

Les montants ne doivent pas être modifiés uniquement par écrasement d'une valeur courante : les mouvements significatifs doivent pouvoir être reconstitués à partir d'un journal métier traçable.

---

## 17. API métier minimales recommandées

Les noms ci-dessous sont indicatifs et devront être alignés sur les conventions de l'ERP.

| Opération | Exemple de route | Attendu |
| --- | --- | --- |
| Consulter le disponible | `GET /api/v1/budgets/availability` | Disponible par période, axe et enveloppe |
| Contrôler une dépense | `POST /api/v1/budget-controls/check` | Décision, règle appliquée, montant disponible |
| Réserver un budget | `POST /api/v1/budget-reservations` | Réservation atomique avec référence externe |
| Libérer une réservation | `POST /api/v1/budget-reservations/{id}/release` | Libération idempotente et historisée |
| Soumettre une version | `POST /api/v1/budget-versions/{id}/submit` | Vérifications préalables et démarrage du workflow |
| Demander une révision | `POST /api/v1/budget-versions/{id}/revision-requests` | Motif, périmètre et circuit d'approbation |
| Créer un virement | `POST /api/v1/budget-transfers` | Contrôle du solde source et workflow |
| Lancer un rapprochement | `POST /api/v1/reconciliations` | Contrôle entre source et données budgétaires |
| Consulter les écarts | `GET /api/v1/budget-variance` | Écarts filtrables par période et axe |
| Consulter la santé des interfaces | `GET /api/v1/integrations/health` | Retard, erreurs et dernier traitement |

Les API d'écriture devront vérifier l'autorisation, la période ouverte, la version budgétaire, les axes obligatoires, la clé d'idempotence et les règles de contrôle. Les réponses de refus devront être explicites et exploitables par les modules appelants.

---

## 18. Exigences non fonctionnelles complémentaires

- **RNF-01 — Cohérence transactionnelle** : garantir l'absence de double réservation et de double consommation, y compris sous charge concurrente.
- **RNF-02 — Idempotence** : toute commande technique rejouable doit produire un seul effet métier.
- **RNF-03 — Observabilité** : logs structurés, métriques, traces distribuées et identifiants de corrélation.
- **RNF-04 — Sécurité API** : OAuth 2.0 / OpenID Connect, autorisation côté serveur, validation stricte des entrées et limitation de débit.
- **RNF-05 — Pagination** : les listes volumineuses doivent être paginées et filtrées côté serveur.
- **RNF-06 — Performance mesurable** : les objectifs de performance doivent être vérifiés sur un jeu de données représentatif, avec concurrence et volumes documentés.
- **RNF-07 — Continuité** : définir les sauvegardes, tests de restauration, RPO/RTO et procédures de reprise par environnement.
- **RNF-08 — Accessibilité** : navigation clavier, libellés accessibles, contraste suffisant et restitution non fondée uniquement sur la couleur.
- **RNF-09 — Compatibilité** : définir les versions de navigateurs et les formats d'import supportés.
- **RNF-10 — Multi-tenant éventuel** : si le produit est commercialisé en SaaS, garantir l'isolation stricte des clients dans les données, caches, exports, tâches asynchrones et journaux.

---

## 19. Nouveaux critères d'acceptation

8. Deux demandes concurrentes portant sur le même disponible ne peuvent pas toutes deux dépasser l'enveloppe.
9. La facturation partielle d'une commande libère la part d'engagement correspondante sans double comptage avec le réalisé.
10. Une demande de dérogation ne peut pas être approuvée par son demandeur.
11. Un événement reçu deux fois ne crée ni doublon d'engagement, ni doublon de réalisé.
12. Un import invalide produit un rapport d'erreurs compréhensible et ne laisse pas de données partiellement intégrées lorsque le mode transactionnel complet est configuré.
13. Une clôture est refusée ou mise en attente si les rapprochements critiques sont incomplets, selon la politique paramétrée.
14. Les journaux d'audit permettent de reconstituer l'auteur, l'action, la date, le motif et les valeurs avant/après.
15. Les exports et API respectent les mêmes périmètres d'accès que les écrans.
16. Une restauration de sauvegarde est testée et son résultat est documenté.
17. Une règle de contrôle peut être simulée avant activation sans produire d'effet financier.
18. Les alertes opérationnelles signalent les interfaces en erreur et les événements bloqués.

---

## 20. Feuille de route affinée

| Priorité | Contenu | Justification |
| --- | --- | --- |
| **P0 — Socle** | Référentiels, exercices, versions, lignes budgétaires, imports contrôlés, workflow, droits, audit | Fiabiliser les données et les validations |
| **P1 — Contrôle des dépenses** | Réservations atomiques, contrôle avant engagement, cycle commande-réception-facture, dérogations, virements | Prévenir les dépassements et sécuriser l'exécution |
| **P1 — Rapprochement** | Comptabilité, engagements, gestion des erreurs d'intégration, idempotence, clôtures | Garantir la cohérence entre budget et sources |
| **P1 — Pilotage de base** | Budget/réalisé/engagé/disponible, écarts, alertes, exports et drill-down | Donner une vision opérationnelle exploitable |
| **P2 — Budgets métiers** | Masse salariale, CAPEX, trésorerie, projets, ventes | Étendre la couverture fonctionnelle |
| **P2 — Prévision et consolidation** | Reforecast, scénarios, rolling forecast, multi-entités et devises | Améliorer l'anticipation et les arbitrages |
| **P2 — Exploitation avancée** | Supervision fonctionnelle, simulation de règles, contrôle qualité des données | Réduire les incidents de production |
| **P3 — Aide à la décision** | Recommandations assistées par IA, détection d'anomalies et prévisions avancées | À lancer seulement après fiabilisation de l'historique |

---

## 21. Décisions à prendre avant le développement

Pour éviter les ambiguïtés et les reprises coûteuses, les ateliers de cadrage doivent trancher explicitement les points suivants :

1. Le contrôle est-il bloquant par défaut, et quelles dépenses peuvent utiliser une dérogation ?
2. À quel événement l'engagement est-il créé, ajusté, libéré et soldé ?
3. Comment traiter les factures sans commande, les avances, les avoirs, les annulations partielles et les écarts de réception ?
4. Quelle règle de disponible s'applique aux engagements pluriannuels et aux budgets complémentaires ?
5. Quelle est la politique en cas d'indisponibilité du service de contrôle budgétaire ?
6. Qui est propriétaire de chaque référentiel et quelle est la fréquence de synchronisation ?
7. Quels sont les seuils de validation, les règles de délégation et les cas de séparation des tâches ?
8. Quelles règles d'arrondi, de change et de conversion sont retenues ?
9. Quelle durée de conservation est appliquée aux budgets, justificatifs, journaux et données personnelles ?
10. Le produit doit-il supporter plusieurs clients isolés dans une même instance SaaS ?
11. Quelles intégrations doivent être synchrones et lesquelles peuvent être événementielles ?
12. Quels volumes réels serviront aux tests de charge et aux critères de performance ?
13. Quel référentiel comptable, quelles règles fiscales et quelles obligations locales doivent être supportés en priorité ?
14. Quels rapports sont obligatoires pour la direction, le contrôle de gestion, les achats et l'audit ?

## 22. Synthèse des apports

Les ajouts renforcent principalement :
- **La fiabilité financière** : réservation atomique, prévention du double comptage, rapprochement et cycle complet commande-facture.
- **La gouvernance** : séparation des tâches, dérogations justifiées, délégations et audit enrichi.
- **La robustesse des intégrations** : idempotence, rejeu contrôlé, gestion des événements en échec et corrélation de bout en bout.
- **La qualité des données** : contrôles des référentiels, dates d'effet et suivi des interfaces.
- **Le pilotage** : prévisions à terminaison, analyses de dérive, scénarios et explication des écarts.
- **L'exploitabilité** : supervision, restauration testée et critères d'acceptation vérifiables.
