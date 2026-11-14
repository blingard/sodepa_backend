#!/usr/bin/env bash
# Lance le backend en local contre les conteneurs du docker-compose.yml.
# Usage : ./run-local.sh   (prérequis : docker compose up -d)
# Le secret du client Keycloak "sodepa" est lu via l'API admin de Keycloak
# si KEYCLOAK_CLIENT_SECRET n'est pas déjà défini.
set -e

if [ -z "$KEYCLOAK_CLIENT_SECRET" ]; then
  kc=http://localhost:8070
  tok=$(curl -s -d "client_id=admin-cli&username=admin&password=${KEYCLOAK_ADMIN_PASSWORD:-admin123}&grant_type=password" \
    "$kc/realms/master/protocol/openid-connect/token" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
  id=$(curl -s -H "Authorization: Bearer $tok" "$kc/admin/realms/sodepa/clients?clientId=sodepa" \
    | sed -n 's/^\[{"id":"\([^"]*\)".*/\1/p')
  KEYCLOAK_CLIENT_SECRET=$(curl -s -H "Authorization: Bearer $tok" "$kc/admin/realms/sodepa/clients/$id/client-secret" \
    | sed -n 's/.*"value":"\([^"]*\)".*/\1/p')
  export KEYCLOAK_CLIENT_SECRET
fi
: "${KEYCLOAK_CLIENT_SECRET:?Impossible de récupérer le secret Keycloak (Keycloak démarré ?)}"

export PROFILE=dev
export SERVER_PORT=8087
export BACK_END_BASE_URL=http://localhost:8087

export DB_URL=jdbc:postgresql://localhost:5433/sodepa
export DB_USERNAME=toto DB_PASSWORD=12345
export DB_DDL=update DB_SHOW_SQL=false DB_SHOW_SQL_FORMAT=false

export REDIS_IP=localhost REDIS_PORT=6379 REDIS_PASSWORD=redispassword
export RABBITMQ_IP=localhost RABBITMQ_PORT=5672 RABBITMQ_USERNAME=guest RABBITMQ_PASSWORD=guest
export CLICKHOUSE_HOST=localhost CLICKHOUSE_PORT=8123 CLICKHOUSE_DB=default CLICKHOUSE_USERNAME=default CLICKHOUSE_PASSWORD=

export KEYCLOAK_SERVER_URL=http://localhost:8070 KEYCLOAK_REALM=sodepa KEYCLOAK_CLIENT_ID=sodepa
export KEYCLOAK_DEFAULT_PASSWORD='DefaultPassword123!'
export JWT_URI=http://localhost:8070/realms/sodepa
export JWK_URI=http://localhost:8070/realms/sodepa/protocol/openid-connect/certs

export MINIO_ENDPOINT=http://localhost:7004 MINIO_FILEURL=http://localhost:7004
export MINIO_ACCESS_KEY=minioadmin MINIO_SECRET_KEY=minioadmin123
export COLLABORA_URL=http://localhost:9980

# Ne pas effacer la base ni les utilisateurs Keycloak au démarrage.
export SODEPA_PURGE_ON_STARTUP="${SODEPA_PURGE_ON_STARTUP:-false}"

cd "$(dirname "$0")"
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xms512m -Xmx2g"
