#!/bin/sh
set -e

apk add --no-cache curl jq

cat > /etc/crontabs/root << 'CRON'
SHELL=/bin/sh
KEYCLOAK_URL=http://keycloak:8180
SHORTENER_URL=http://shortener:8080
* * * * * sh /cleanup-cron-task.sh
CRON

crond -f -l 2