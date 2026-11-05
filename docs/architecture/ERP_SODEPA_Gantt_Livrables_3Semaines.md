# ERP SODEPA — Planning Prévisionnel & Gantt des Livrables
## Démarche de Livraison Continue en Sprints de 3 Semaines (11 Sprints · 33 Semaines ≈ 7,6 Mois)

**Projet :** Système d'Information ERP Centralisé de la SODEPA  
**Auteur :** Antigravity (Architecte Logiciel Senior & Direction de Projet SI)  
**Date d'établissement :** 25 octobre 2026  
**Référentiel visuel :** [`ERP_SODEPA_Gantt_Livrables_3Semaines.excalidraw`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables_3Semaines.excalidraw) · Image HD : [`ERP_SODEPA_Gantt_Livrables_3Semaines.png`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables_3Semaines.png)  
**Scène Cloud Excalidraw+ :** Scene ID `2OaDesrQbpc` (Workspace `6dUQktLplDj`, collection `Main`)

---

## 1. Principes Directeurs & Cadence des Sprints de 3 Semaines

Dans le cadre du Pilier 2 du Plan Directeur de Transformation Numérique de la SODEPA, l'organisation en **sprints de 3 semaines** offre un compromis optimal entre vélocité de développement et temps d'appropriation métier par les utilisateurs finaux :
1. **Rythme Tri-hebdomadaire (11 Sprints = 33 Semaines ≈ 7,6 Mois)** : Le projet s'exécute en 33 semaines, garantissant le respect strict de la durée maximale de 8 mois avec une marge de sécurité contractuelle de 1 à 2 semaines.
2. **Livraison Continue & Revue de Sprint** : Chaque sprint (de S1 à S11) se clôture formellement par une revue opérationnelle et la livraison d'un incrément logiciel testé et validé.
3. **Priorisation Absolue RH & Finances (Go-Live S6)** : Le socle vital d'exploitation (**D2** RH/Paie, **D3** Comptabilité SYSCOHADA, **D4** Budget, **D5** Trésorerie) est déployé et opérationnel à la fin du Sprint 6 (Semaine 18, soit à mi-parcours du projet).
4. **Période d'Hypercare & Transfert (S11)** : Le Sprint 11 (Semaines 31 à 33) est intégralement alloué à la garantie, au support renforcé post-démarrage et au transfert de compétences aux équipes support internes (Module D14).

---

## 2. Calendrier Synthétique des 11 Sprints

```text
 Sprints :     S1       S2       S3       S4       S5       S6       S7       S8       S9      S10      S11
 Semaines: [Sem 1-3][Sem 4-6][Sem 7-9][Sem10-12][Sem13-15][Sem16-18][Sem19-21][Sem22-24][Sem25-27][Sem28-30][Sem31-33]
═════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
0. PILOTAGE    ◆ Kick-off                                                                 ◆ MEP Globale
               ◆ Valid. Arch.                                                             [==== Hypercare D14 ====]
  Revues Sprints: ◆        ◆        ◆        ◆        ◆        ◆        ◆        ◆        ◆        ◆        ◆
─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
1. BESOINS     [== AS-IS ==]
               [★ Ateliers RH/Finances ★]
                        [===== Ateliers Socle & Autres =====]
                        [================ Spécifications & Backlog ===============]
2. ARCHI.      [== DAT ==]
                        [====================== Dossier de Paramétrage =====================]
3. DOCS                          [=================== Documentation Technique ===================]
                                          [================ Guides Utilisateurs ==================]
4. MIGRATION   [== Plan ==]
                                 [★ Reprise Données RH & Finances ★]
                                                                   [=========== Reprise Autres Données ==========]
5. FORMATION                     [== Plan ==]
                                          [★ Formations RH & Finances ★]
                                                                   [========== Formations Autres Modules ========]
6. RECETTE                       [==================== Tests Unitaires & Intégration =====================]
                                                                                          [== Recette VABF / VSR ==]
─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
7. MODULES :
  V1 Socle               [=== D1, D13, D14 ===] ▲ (Go-Live S3)
  ★ V2 RH/Fin                    [★ D3 Compta, D4 Budget, D5 Trésorerie, D2 RH ★] ▲ (Go-Live S6 ★)
  V3 Achats/Stocks                        [===== D9 Recettes, D6 Achats, D7 Stocks, D8 Immo =====] ▲ (Go-Live S8)
  V4 GED/BI                                                [===== D10 GED, D11 BI Décisionnel =====] ▲ (Go-Live S9)
  Transverse             [======================== D12 Interopérabilité & API ========================]
═════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
 Jalons Go-Live:                                ▲ S3                    ▲ S6             ▲ S8     ▲ S9    ▲ S10
```

---

## 3. Matrice Détaillée des Groupes et Livrables

### Groupe 0 — Pilotage & Livraison Continue (`#475569`)
* **Jalon S1 :** Lancement officiel du projet (*Kick-off*).
* **Jalon S2 :** Validation formelle du dossier d'architecture technique et logicielle.
* **Activité récurrente S1–S11 :** Revue de sprint toutes les 3 semaines et livraison d'un incrément testé.
* **Jalon S10 :** **Mise en Production Globale** (Go-Live général de tous les modules).
* **S11 :** Période d'Hypercare, assistance rapprochée, levée des réserves et transfert au support (D14).

### Groupe 1 — Recueil des Besoins & Cadrage Fonctionnel (`#4f46e5`)
* **S1–S2 :** Analyse de l'existant & cartographie des processus opérationnels (AS-IS).
* **S1–S2 :** ★ Ateliers prioritaires de recueil des besoins : RH, comptabilité, budget, trésorerie.
* **S2–S4 :** Ateliers de recueil des besoins : socle et autres domaines métiers.
* **S2–S6 :** Spécifications fonctionnelles détaillées & backlog priorisé (affinés à chaque sprint).
* **Jalon S2 :** Validation des besoins et priorités par la Direction Générale.

### Groupe 2 — Conception & Architecture (`#2563eb`)
* **S1–S2 :** Dossier d'Architecture Technique (DAT), applicatif, sécurité (OWASP) et API/ESB.
* **S2–S9 :** Dossier de paramétrage transverse mis à jour à chaque sprint selon les incréments livrés.

### Groupe 3 — Documentation & Manuels d'Exploitation (`#7c3aed`)
* **S3–S10 :** Documentation technique vivante (composants, schémas, interfaces REST, OpenAPI).
* **S4–S10 :** Guides utilisateurs finaux & fiches réflexes pour les *Key Users*.
* **S3–S7 :** Manuel d'administration (gestion des identités Keycloak, rôles, habilitations, audits).
* **S4–S10 :** Manuel d'exploitation (supervision, sauvegardes PostgreSQL/ClickHouse, plans de reprise).

### Groupe 4 — Migration & Continuité d'Activité (`#ea580c`)
* **S1–S2 :** Plan cadre de migration des données (inventaire des sources, nettoyage, formats cibles).
* **S3–S6 :** ★ Reprise des données prioritaires (personnel, plans de comptes, écritures, budgets).
* **S6–S10 :** Reprise des autres données historiques (achats, stocks, patrimoine immobilisé, tiers).
* **S4–S9 :** Plan de Reprise d'Activité (PRA) et Plan de Continuité d'Activité (PCA).

### Groupe 5 — Formation & Conduite du Changement (`#0d9488`)
* **S3–S4 :** Plan de formation & élaboration des supports pédagogiques.
* **S4–S6 :** ★ Formations prioritaires des utilisateurs des modules RH, comptabilité, budget, trésorerie.
* **S6–S10 :** Formations continues : autres modules, administrateurs et utilisateurs finaux.

### Groupe 6 — Qualité & Recette (`#dc2626`)
* **S3–S10 :** Campagnes continues de tests unitaires, d'intégration et fonctionnels (CI/CD).
* **S6–S9 :** Tests de charge, de performance et tests de sécurité applicative (OWASP).
* **S9–S10 :** Recette utilisateurs finale (VABF / VSR) et Rapport de recette final.

---

## 4. Déploiement des 14 Modules Métier (D1 à D14)

### Vague 1 — Socle Prérequis (`#16a34a`)
* **S2–S3 :** **D1** Référentiel ERP (MDM, tiers partagés, workflows).
* **S2–S3 :** **D13** Sécurité, administration & conformité du SI.
* **S2–S3 :** **D14** Administration, exploitation & support.
* **Jalon S3 :** **Go-Live Vague 1** (Socle opérationnel).

### ★ Vague 2 — PRIORITAIRE : RH & Finances (`#0891b2`)
* **S3–S5 :** **★ D3** Gestion financière & comptable (SYSCOHADA révisé).
* **S3–S5 :** **★ D4** Gestion budgétaire (lettres de cadrage, engagements, contrôles).
* **S3–S5 :** **★ D5** Gestion de la trésorerie et financements.
* **S3–S6 :** **★ D2** Ressources humaines (SIRH, paie, portail ESS).
* **Jalon S6 :** **★ Go-Live Vague 2** (Basculement opérationnel RH & Finances).

### Vague 3 — Recettes, Achats, Stocks & Patrimoine (`#ca8a04`)
* **S4–S7 :** **D9** Recettes & facturation.
* **S5–S8 :** **D6** Achats & marchés.
* **S5–S8 :** **D7** Stocks & approvisionnements.
* **S6–S8 :** **D8** Immobilisations & patrimoine.
* **Jalon S8 :** **Go-Live Vague 3** (Chaîne logistique et patrimoniale).

### Vague 4 — GED & Décisionnel (`#be185d`)
* **S5–S8 :** **D10** GED & workflows documentaires.
* **S6–S9 :** **D11** BI, reporting stratégique & aide à la décision.
* **Jalon S9 :** **Go-Live Vague 4** (GED documentaire et tableaux de bord).

### Transverse — Interopérabilité (`#475569`)
* **S2–S9 :** **D12** Intégration & interopérabilité (API REST, banques EBICS, Mobile Money).

---

## 5. Répertoire des Livrables Produits

| Type | Fichier & Emplacement | Usage |
| :--- | :--- | :--- |
| **Schéma Excalidraw Source** | [**`ERP_SODEPA_Gantt_Livrables_3Semaines.excalidraw`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables_3Semaines.excalidraw) | Fichier vectoriel complet (359 éléments) éditable sous [excalidraw.com](https://excalidraw.com). |
| **Rendu Visuel PNG HD** | [**`ERP_SODEPA_Gantt_Livrables_3Semaines.png`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables_3Semaines.png) | Image haute résolution (242 Ko) pour intégration dans les rapports Word/PDF et comités DSI. |
| **Scène Cloud Excalidraw+** | **Scène ID :** `2OaDesrQbpc` | Hébergée en ligne dans votre espace Excalidraw+ (Workspace `6dUQktLplDj`, collection `Main`). |
| **Document de Cadrage (.md)** | [**`ERP_SODEPA_Gantt_Livrables_3Semaines.md`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables_3Semaines.md) | Synthèse contractuelle et opérationnelle de planification. |
