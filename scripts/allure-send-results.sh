#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
#  Envoie les résultats de tests au serveur Allure partagé, puis déclenche
#  la génération du rapport.
#
#  Usage :
#      ./scripts/allure-send-results.sh
#      ALLURE_SERVER=http://192.168.1.20:5050 ./scripts/allure-send-results.sh
#
#  Variables d'environnement :
#      ALLURE_SERVER      URL de l'API Allure       (défaut : http://localhost:5050)
#      ALLURE_PROJECT_ID  Identifiant du projet     (défaut : sodepa)
#      ALLURE_USER        Compte administrateur     (défaut : admin)
#      ALLURE_PASS        Mot de passe              (défaut : changez-moi)
#      ALLURE_RESULTS     Dossier de résultats      (défaut : target/allure-results)
# ═══════════════════════════════════════════════════════════════════════════
set -euo pipefail

ALLURE_SERVER="${ALLURE_SERVER:-http://localhost:5050}"
ALLURE_PROJECT_ID="${ALLURE_PROJECT_ID:-sodepa}"
ALLURE_USER="${ALLURE_USER:-admin}"
ALLURE_PASS="${ALLURE_PASS:-changez-moi}"
ALLURE_RESULTS="${ALLURE_RESULTS:-target/allure-results}"

API="${ALLURE_SERVER}/allure-docker-service"
COOKIES="$(mktemp)"
trap 'rm -f "$COOKIES"' EXIT

if [[ ! -d "$ALLURE_RESULTS" ]]; then
  echo "✗ Dossier introuvable : $ALLURE_RESULTS" >&2
  echo "  Lancez d'abord : mvn test" >&2
  exit 1
fi

shopt -s nullglob
files=("$ALLURE_RESULTS"/*)
if [[ ${#files[@]} -eq 0 ]]; then
  echo "✗ Aucun résultat dans $ALLURE_RESULTS" >&2
  exit 1
fi

echo "→ Authentification sur $API"
curl -sS -X POST "$API/login" \
  -H 'Content-Type: application/json' \
  -c "$COOKIES" \
  -d "{\"username\":\"$ALLURE_USER\",\"password\":\"$ALLURE_PASS\"}" >/dev/null

# Crée le projet s'il n'existe pas encore (409 si déjà présent : sans gravité).
echo "→ Vérification du projet « $ALLURE_PROJECT_ID »"
curl -sS -X POST "$API/projects" \
  -H 'Content-Type: application/json' \
  -b "$COOKIES" \
  -d "{\"id\":\"$ALLURE_PROJECT_ID\"}" >/dev/null || true

echo "→ Envoi de ${#files[@]} fichier(s) de résultats"
args=()
for f in "${files[@]}"; do
  [[ -f "$f" ]] && args+=(-F "files[]=@$f")
done
curl -sS -X POST "$API/send-results?project_id=$ALLURE_PROJECT_ID&force_project_creation=true" \
  -b "$COOKIES" "${args[@]}" >/dev/null

BUILD_NAME="${BUILD_NAME:-$(git rev-parse --short HEAD 2>/dev/null || date +%Y%m%d-%H%M%S)}"
echo "→ Génération du rapport (build : $BUILD_NAME)"
curl -sS -X GET "$API/generate-report?project_id=$ALLURE_PROJECT_ID&execution_name=$BUILD_NAME" \
  -b "$COOKIES" >/dev/null

echo
echo "✓ Rapport disponible :"
echo "  ${ALLURE_SERVER%:*}:5252/allure-docker-service-ui/projects/$ALLURE_PROJECT_ID"
echo "  ${API}/projects/$ALLURE_PROJECT_ID/reports/latest/index.html"
