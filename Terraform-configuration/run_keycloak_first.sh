#!/usr/bin/env bash
set -euxo pipefail
DOMAIN=${DOMAIN:?missing}
KC_IMAGE=${KC_IMAGE:?missing}
KC_ADMIN=${KC_ADMIN:?missing}
KC_PASS=${KC_PASS:?missing}

# Stop any previous container
(docker rm -f keycloak || true)

# First run WITHOUT --optimized so Keycloak can initialize
# Bind only to localhost; NGINX handles public traffic

docker run -d --name keycloak \
  --restart unless-stopped \
  -p 127.0.0.1:8080:8080 \
  -e KEYCLOAK_ADMIN="${KC_ADMIN}" \
  -e KEYCLOAK_ADMIN_PASSWORD="${KC_PASS}" \
  -e KC_PROXY=edge \
  -e KC_PROXY_HEADERS=xforwarded \
  -e KC_HTTP_ENABLED=true \
  -e KC_HOSTNAME="${DOMAIN}" \
  -e KC_HOSTNAME_STRICT=true \
  -e KC_HOSTNAME_STRICT_HTTPS=true \
  "${KC_IMAGE}" start

