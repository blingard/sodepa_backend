# ERP SODEPA — Planning Prévisionnel & Gantt des Livrables
## Démarche de Livraison Continue en Sprints de 2 Semaines (16 Sprints · 8 Mois)

**Projet :** Système d'Information ERP Centralisé de la SODEPA  
**Auteur :** Antigravity (Architecte Logiciel Senior & Direction de Projet SI)  
**Date d'établissement :** 25 octobre 2026  
**Référentiel visuel :** [`ERP_SODEPA_Gantt_Livrables.excalidraw`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables.excalidraw) · Image HD : [`ERP_SODEPA_Gantt_Livrables.png`](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables.png)  
**Scène Cloud Excalidraw+ :** Scene ID `5ZZmPccnyiX` (Workspace `6dUQktLplDj`, collection `Main`)

---

## 1. Principes Directeurs & Stratégie de Déploiement

Le plan directeur de transformation numérique de la SODEPA (Pilier 2) impose un déploiement pragmatique et maîtrisé fondé sur une **méthode agile à livraison continue** :
1. **Rythme Bi-hebdomadaire Stricte** : Chaque sprint dure exactement **2 semaines** (S1 à S16).
2. **Incrément Livrable à Chaque Sprint** : Chaque sprint se clôture par une démonstration formelle et la mise à disposition d'un incrément logiciel testé et déployé sur l'environnement de recette.
3. **Priorisation Stratégique Absolue (RH & Finances)** : Les fonctions vitales de gestion du personnel et de tenue comptable/budgétaire (SYSCOHADA) sont déployées en priorité pour un **Go-Live opérationnel à la fin du Sprint 8 (Mois 4)**.
4. **Marge de Sécurité Intégrée (Hypercare)** : Les sprints S15 et S16 sont sanctuarisés pour l'accompagnement rapproché des utilisateurs (*Hypercare*), la levée des réserves et le transfert complet au support interne (Module D14).

---

## 2. Synthèse Temporelle des Vagues de Déploiement (16 Sprints)

```
 Sprint :  S1  S2  S3  S4  S5  S6  S7  S8  S9 S10 S11 S12 S13 S14 S15 S16
 Mois   : [  Mois 1  ] [  Mois 2  ] [  Mois 3  ] [  Mois 4  ] [  Mois 5  ] [  Mois 6  ] [  Mois 7  ] [  Mois 8  ]
------------------------------------------------------------------------------------------------------------------
 Socle V1: [=== D1/D13/D14 ===] ▲(Go-Live S4)
 RH/Fin★ :     [======= D2 / D3 / D4 / D5 =======] ▲(Go-Live S8 ★)
 Vague 3 :                     [====== D6/D7/D8/D9 ======] ▲(Go-Live S11)
 Vague 4 :                             [==== D10 / D11 ====] ▲(Go-Live S12)
 Transv. :         [================= D12 API / ESB =================]
 VABF/VSR:                                                     [=== Recette ===] ▲(MEP Globale S14)
 Garantie:                                                                     [== Hypercare ==]
```

---

## 3. Matrice Détaillée des Groupes et Livrables

### Groupe 0 — Pilotage & Livraison Continue (`#475569`)
* **Jalon S1 :** Lancement officiel du projet (*Kick-off meeting*).
* **Jalon S2 :** Validation du dossier d'architecture technique et logicielle.
* **Activité récurrente S1–S16 :** Revue de sprint bi-hebdomadaire et livraison d'un incrément testé.
* **Jalon S14 :** Validation de l'Aptitude au Bon Fonctionnement (VABF) et **Mise en Production Globale**.
* **S15–S16 :** Période de garantie, assistance renforcée (*Hypercare*) et transfert au support (D14).

### Groupe 1 — Recueil des Besoins & Cadrage Fonctionnel (`#4f46e5`)
* **S1–S2 :** Analyse de l'existant & cartographie des processus opérationnels (AS-IS).
* **S1–S2 :** ★ Ateliers prioritaires de recueil des besoins : RH, comptabilité, budget, trésorerie.
* **S2–S6 :** Ateliers de recueil des besoins : socle et autres domaines métiers (au fil de l'eau).
* **S2–S8 :** Spécifications fonctionnelles détaillées & backlog priorisé (affinés par sprint).
* **Jalon S2 :** Validation formelle des besoins et priorités par la Direction Générale.

### Groupe 2 — Conception & Architecture (`#2563eb`)
* **S1–S2 :** Dossier d'Architecture Technique (DAT), applicatif, sécurité (OWASP) et API/ESB.
* **S2–S13 :** Dossier de paramétrage transverse mis à jour à chaque sprint selon les incréments.

### Groupe 3 — Documentation & Manuels d'Exploitation (`#7c3aed`)
* **S3–S14 :** Documentation technique vivante (composants, schémas, interfaces REST, Swagger).
* **S5–S14 :** Guides utilisateurs finaux & fiches réflexes pour les *Key Users*.
* **S3–S10 :** Manuel d'administration (gestion des identités Keycloak, rôles, habilitations, audits).
* **S5–S14 :** Manuel d'exploitation (supervision, sauvegardes PostgreSQL/ClickHouse, plans de reprise).

### Groupe 4 — Migration & Continuité d'Activité (`#ea580c`)
* **S1–S3 :** Plan cadre de migration des données (inventaire des sources, nettoyage, formats cibles).
* **S3–S8 :** ★ Reprise et injection des données prioritaires (personnel, plans de comptes, écritures, budgets).
* **S8–S14 :** Reprise des autres données historiques (achats, stocks, patrimoine immobilisé, tiers).
* **S6–S13 :** Plan de Reprise d'Activité (PRA) et Plan de Continuité d'Activité (PCA).

### Groupe 5 — Formation & Conduite du Changement (`#0d9488`)
* **S3–S5 :** Plan de formation & élaboration des supports pédagogiques.
* **S5–S9 :** ★ Formations prioritaires des utilisateurs des modules RH, comptabilité, budget, trésorerie.
* **S9–S15 :** Formations continues : utilisateurs achats/stocks, administrateurs et directions de ranchs.

### Groupe 6 — Qualité & Recette (`#dc2626`)
* **S3–S14 :** Campagnes continues de tests unitaires, d'intégration et fonctionnels (CI/CD).
* **S9–S13 :** Tests de charge, de performance et tests d'intrusion / sécurité applicative (OWASP).
* **S13–S15 :** Recette opérationnelle avec les utilisateurs (VABF / VSR) et PV de recette final.

---

## 4. Déploiement des 14 Domaines Métier (D1 à D14)

### Vague 1 — Socle Prérequis (`#16a34a`)
* **S2–S4 :** **D1** Référentiel ERP central (MDM, tiers partagés, plan comptable OHADA, workflows).
* **S2–S4 :** **D13** Sécurité, administration, habilitations & conformité réglementaire.
* **S3–S4 :** **D14** Administration technique, supervision, exploitation & support.
* **Jalon S4 :** **Go-Live Vague 1** (Socle opérationnel).

### ★ Vague 2 — PRIORITAIRE : RH & Finances (`#0891b2`)
* **S3–S7 :** **★ D3** Gestion financière et comptable selon le référentiel SYSCOHADA révisé.
* **S4–S7 :** **★ D4** Gestion budgétaire (lettres de cadrage, engagements, contrôles de dépassement).
* **S4–S7 :** **★ D5** Gestion de la trésorerie et financements (rapprochements, prévisions glissantes).
* **S3–S8 :** **★ D2** Gestion des Ressources Humaines & SIRH (carrières, congés, pré-paie).
* **Jalon S8 :** **★ Go-Live Vague 2** (Basculement opérationnel RH & Finances).

### Vague 3 — Recettes, Achats, Stocks & Patrimoine (`#ca8a04`)
* **S6–S10 :** **D9** Facturation, recettes & suivi des encaissements.
* **S7–S11 :** **D6** Achats, marchés & contrats.
* **S7–S11 :** **D7** Gestion des stocks, approvisionnements & magasins des ranches.
* **S8–S11 :** **D8** Gestion des immobilisations & inventaire du patrimoine.
* **Jalon S11 :** **Go-Live Vague 3** (Intégration de la chaîne logistique et patrimoniale).

### Vague 4 — GED & Décisionnel (`#be185d`)
* **S7–S11 :** **D10** Gestion Électronique des Documents (GED) & circuits de validation.
* **S8–S12 :** **D11** Business Intelligence, reporting stratégique & tableaux de bord décisionnels.
* **Jalon S12 :** **Go-Live Vague 4** (GED documentaire et pilotage DG).

### Transverse — Interopérabilité (`#475569`)
* **S3–S12 :** **D12** Intégration & interopérabilité (API REST, passerelles bancaires EBICS, Mobile Money).

---

## 5. Répertoire des Fichiers Disponibles

| Type | Fichier & Emplacement | Usage |
| :--- | :--- | :--- |
| **Schéma Excalidraw Source** | [**`ERP_SODEPA_Gantt_Livrables.excalidraw`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables.excalidraw) | Fichier vectoriel complet éditable sur [excalidraw.com](https://excalidraw.com). |
| **Rendu Visuel PNG HD** | [**`ERP_SODEPA_Gantt_Livrables.png`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables.png) | Export visuel haute résolution (964×1079 px) prêt pour impression et présentation. |
| **Scène Cloud Excalidraw+** | **Scène ID :** `5ZZmPccnyiX` | Hébergée en ligne sur le compte Excalidraw+ (Workspace `6dUQktLplDj`, collection `Main`). |
| **Documentation Markdown** | [**`ERP_SODEPA_Gantt_Livrables.md`**](file:///c:/Users/GLC/Documents/sodepa/erp/docs/architecture/ERP_SODEPA_Gantt_Livrables.md) | Synthèse contractuelle et opérationnelle de planification. |
