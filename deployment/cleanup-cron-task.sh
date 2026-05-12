#!/bin/sh
set -e

KEYCLOAK_URL="${KEYCLOAK_URL:-http://keycloak:8180}"
SHORTENER_URL="${SHORTENER_URL:-http://shortener:8080}"

TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/shortener/protocol/openid-connect/token" \
  -d "client_id=${CRON_CLIENT_ID:?}" \
  -d "client_secret=${CRON_CLIENT_SECRET:?}" \
  -d "grant_type=client_credentials" | jq -r '.access_token')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
  echo "Failed to obtain token" >&2
  exit 1
fi

echo "Start time: $(date '+%Y-%m-%d %H:%M:%S')" && \
curl -s -X POST "$SHORTENER_URL/api/v1/admin/cleanup" \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nCleanup completed. HTTP Status: %{http_code}. Duration: %{time_total}s\n"
