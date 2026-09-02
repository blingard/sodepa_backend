# Tests & Allure — SODEPA ERP

Ce document est le point d'entrée de l'équipe pour écrire, exécuter et
consulter les cas de test du projet.

---

## 1. Ce qui a été mis en place

| Brique | Rôle | Coût |
|---|---|---|
| **Allure Report** (`allure-jupiter`, `allure-maven`) | Instrumente les tests JUnit 5 et produit un rapport HTML riche : arbre métier, étapes, pièces jointes, sévérités | Gratuit, open source |
| **Serveur Allure partagé** (`docker-compose.allure.yml`) | Une URL unique pour toute l'équipe, avec historique et tendances | Gratuit, auto-hébergé |
| **CI GitHub Actions** (`.github/workflows/tests-allure.yml`) | Exécute les tests à chaque push et publie le rapport sur GitHub Pages | Gratuit |
| **Câblage Allure TestOps** (`.github/workflows/allure-testops.yml`) | Remontée vers TestOps, inerte tant qu'il n'est pas activé | Nécessite une licence Qameta |

### Une précision sur « Allure TestOps »

Deux produits différents portent le nom *Allure* :

- **Allure Report** — le générateur de rapports, **open source et gratuit**.
  C'est ce qui est installé ici, et c'est ce que 95 % des équipes utilisent.
- **Allure TestOps** — la plateforme serveur de Qameta (gestion des cas de
  test, exécutions manuelles, plans de test, intégration Jira). Elle est
  **commerciale** : à ce jour il n'existe **pas de version gratuite ni
  d'édition communautaire**. L'offre cloud est facturée par utilisateur et par
  mois, l'installation auto-hébergée exige une clé de licence et un accès au
  registre Docker de Qameta. Seul un essai est proposé.

Le montage retenu couvre donc gratuitement le besoin « tout le monde accède
aux cas de test et aux résultats », et le jour où une licence TestOps est
achetée, il suffit de renseigner trois variables dans GitHub — aucune ligne de
test n'est à réécrire.

---

## 2. Lancer les tests en local

```bash
# Tests unitaires uniquement (aucune infrastructure nécessaire)
./mvnw test -DexcludedGroups=integration

# Toute la suite, y compris les tests d'intégration
docker compose up -d          # PostgreSQL, Kafka, Redis, Keycloak, MinIO…
./mvnw test

# Ouvrir le rapport dans le navigateur
./mvnw allure:serve

# Ou seulement l'écrire sur disque : target/site/allure-maven-plugin/index.html
./mvnw allure:report
```

Sous Windows, remplacer `./mvnw` par `mvnw.cmd`.

---

## 3. Le serveur Allure partagé

À installer une fois, sur un poste ou un serveur accessible à l'équipe.

```bash
# Adapter d'abord les identifiants et l'URL publique
export ALLURE_ADMIN_PASS='un-mot-de-passe-solide'
export ALLURE_PUBLIC_API_URL='http://192.168.1.20:5050'   # IP ou DNS réel, pas localhost

docker compose -f docker-compose.allure.yml up -d
```

| Adresse | Usage |
|---|---|
| `http://<serveur>:5252` | Interface web — consultation par l'équipe |
| `http://<serveur>:5050/allure-docker-service` | API — envoi des résultats |

Envoyer les résultats après une exécution :

```bash
./mvnw test -DexcludedGroups=integration
ALLURE_SERVER=http://192.168.1.20:5050 \
ALLURE_PASS='un-mot-de-passe-solide' \
  ./scripts/allure-send-results.sh
```

Deux comptes existent : un administrateur (envoi de résultats) et un compte
lecture seule (`sodepa` / `sodepa` par défaut) à distribuer à l'équipe.
**Changez ces mots de passe avant toute exposition sur le réseau.**

---

## 4. Le rapport publié automatiquement

À chaque push sur `main`, `master` ou `develop`, la CI exécute les tests
unitaires et publie le rapport à l'adresse :

```
https://blingard.github.io/sodepa_backend/
```

Prérequis, à faire une seule fois par un administrateur du dépôt :
**Settings → Pages → Source : « Deploy from a branch » → branche `gh-pages`, dossier `/(root)`.**
La branche `gh-pages` est créée automatiquement à la première exécution du
workflow.

Les pull requests exécutent les tests sans publier, pour ne pas écraser le
rapport de la branche principale.

---

## 5. Écrire un cas de test

Le fichier `src/test/java/com/sodepa/erp/ModeleCasDeTest.java` est un modèle à
dupliquer. L'exemple complet et fonctionnel est
`src/test/java/com/sodepa/erp/comptabilite/generale/EcritureEquilibreTest.java`.

### Squelette

```java
@Tag("unit")
@Epic("Comptabilité générale")            // domaine métier
@Feature("Écritures comptables")          // fonctionnalité
@Owner("equipe-comptabilite")             // responsable
class MonSujetTest {

    @Test
    @Story("Le comportement attendu, en langage métier")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("SODEPA-12")                 // référence du cas de test
    @Issue("345")                         // ticket GitHub lié (optionnel)
    @DisplayName("Ce que le test vérifie, en une phrase")
    void nomExplicite() {
        // Étant donné / Quand / Alors, découpés en @Step
    }

    @Step("Créer la pièce {numeroPiece}")
    private EcritureEntity nouvelleEcriture(String numeroPiece) { ... }
}
```

### Les annotations, et ce qu'elles produisent

| Annotation | Effet dans le rapport |
|---|---|
| `@Epic` / `@Feature` / `@Story` | Construisent l'arbre « Behaviors » — la vue métier du rapport |
| `@DisplayName` | Intitulé lisible, en français, à la place du nom de méthode |
| `@Severity` | `BLOCKER`, `CRITICAL`, `NORMAL`, `MINOR`, `TRIVIAL` — sert à trier les échecs |
| `@Owner` | Qui maintient ce cas de test |
| `@Step` | Chaque appel devient une ligne dépliable ; les paramètres `{nom}` sont interpolés |
| `@Attachment` | Attache le retour de la méthode (texte, JSON, capture…) au rapport |
| `@TmsLink` / `@Issue` | Liens cliquables, configurés dans `src/test/resources/allure.properties` |
| `@Description` | Texte long affiché dans l'onglet du cas de test |
| `Allure.parameter(...)` | Trace une valeur d'entrée depuis le corps du test |

### Conventions retenues

1. **Un tag obligatoire sur chaque classe** :
   - `@Tag("unit")` — aucune infrastructure, tourne en CI ;
   - `@Tag("integration")` — base de données, Kafka, Keycloak… exclu de la CI.
2. **`@DisplayName` en français**, phrase complète : le rapport est lu par des
   non-développeurs.
3. **`@Epic` = module** (`Comptabilité générale`, `Comptabilité analytique`,
   `Budget`, `Trésorerie`, `Utilisateurs`, `Audit`, `Plateforme`).
4. **`@Feature` = agrégat ou cas d'usage**, `@Story` = règle métier vérifiée.
5. **Un cas de test identifié mais pas encore automatisé** s'écrit avec
   `@Disabled("motif")` : il apparaît en « skipped » dans le rapport, l'équipe
   voit donc le reste à faire sans casser la CI.
6. **Les étapes passent par `@Step`**, pas par des commentaires : ce sont elles
   qui rendent un échec compréhensible sans lire le code.

### Classification des échecs

`src/test/resources/allure/categories.json` range automatiquement les échecs
par catégorie (« Règle métier violée », « Infrastructure indisponible »,
« Cas de test à écrire »…). Ajoutez-y vos propres motifs au fil de l'eau.

---

## 6. Activer Allure TestOps plus tard

1. Ouvrir un essai ou souscrire sur [qameta.io](https://qameta.io/pricing/), ou
   installer le serveur auto-hébergé (licence + accès registre Docker requis —
   voir la [documentation d'installation](https://docs.qameta.io/allure-testops/install/docker-compose/)).
2. Créer le projet dans TestOps et générer un jeton API depuis votre profil.
3. Dans GitHub → **Settings → Secrets and variables → Actions** :
   - variables : `ALLURE_TESTOPS_ENABLED=true`, `ALLURE_ENDPOINT`, `ALLURE_PROJECT_ID` ;
   - secret : `ALLURE_TOKEN`.
4. Mettre à jour `allure.link.tms.pattern` dans
   `src/test/resources/allure.properties` pour que les `@TmsLink` pointent vers
   les fiches TestOps.

Le workflow `allure-testops.yml` s'active alors tout seul ; les tests déjà
écrits remontent sans modification.

---

## 7. Dépannage

| Symptôme | Cause | Solution |
|---|---|---|
| Les `@Step` n'apparaissent pas | Agent AspectJ non chargé | Vérifier que le `argLine` de `maven-surefire-plugin` n'est pas écrasé en ligne de commande |
| `target/allure-results` vide | Tests non exécutés, ou dépendance `allure-jupiter` absente | Relancer `./mvnw test` |
| `mvn allure:serve` ne trouve rien | Mauvais dossier de résultats | Vérifier `allure.results.directory` dans `allure.properties` |
| L'interface du serveur partagé reste vide | `ALLURE_DOCKER_PUBLIC_API_URL` pointe sur `localhost` | Mettre l'IP ou le DNS réel du serveur |
| `contextLoads` échoue en CI | Test d'intégration non exclu | Lancer avec `-DexcludedGroups=integration` |
