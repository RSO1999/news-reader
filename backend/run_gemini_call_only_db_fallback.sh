#!/usr/bin/env bash
set -euo pipefail
API_KEY="${STORYSTREAM_GEMINI_API_KEY:-${GEMINI_API_KEY:-}}"
if [ -z "$API_KEY" ]; then
  echo "Set STORYSTREAM_GEMINI_API_KEY (preferred) or GEMINI_API_KEY before running this script."
  exit 1
fi
EMAIL="a@gmail.com"
PASSWORD="test"
ARTICLE_ID="11111111-1111-1111-1111-111111111111"
ART_TITLE="My Article Title"
ART_SNIPPET="A short snippet with useful keywords"
ART_SECTION="News"
ART_SOURCE="Test"
ART_URL="https://example.com"
LOGIN_RESP=$(curl -sS -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"'"${EMAIL}"'","password":"'"${PASSWORD}"'"}')
TOKEN=$(echo "$LOGIN_RESP" | jq -r .token 2>/dev/null || echo "")
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "Login failed or token not returned:"
  echo "$LOGIN_RESP"
  exit 1
fi
DB_CMD_TYPE=""
if command -v psql >/dev/null 2>&1; then
  DB_CMD_TYPE="local"
elif docker ps -q -f name=storystream-postgres >/dev/null 2>&1; then
  DB_CMD_TYPE="docker"
else
  echo "Neither local psql nor Docker container 'storystream-postgres' available. Install psql or run the Postgres container."
  exit 1
fi
if [ "$DB_CMD_TYPE" = "local" ]; then
  PGPASSWORD=password psql -h localhost -U postgres -d storystream -c "UPDATE users SET subscription_tier='PREMIUM' WHERE email='${EMAIL}';"
  PGPASSWORD=password psql -h localhost -U postgres -d storystream -c "UPDATE articles SET context_payload = NULL WHERE id = '${ARTICLE_ID}';"
  PGPASSWORD=password psql -h localhost -U postgres -d storystream -c "INSERT INTO articles (id, title, section, snippet, image_url, published_at, source_name, external_url) VALUES ('${ARTICLE_ID}','${ART_TITLE}','${ART_SECTION}','${ART_SNIPPET}', NULL, now(), '${ART_SOURCE}', '${ART_URL}') ON CONFLICT (id) DO UPDATE SET title='${ART_TITLE}', snippet='${ART_SNIPPET}';"
else
  docker exec -i storystream-postgres psql -U postgres -d storystream -c "UPDATE users SET subscription_tier='PREMIUM' WHERE email='${EMAIL}';"
  docker exec -i storystream-postgres psql -U postgres -d storystream -c "UPDATE articles SET context_payload = NULL WHERE id = '${ARTICLE_ID}';"
  docker exec -i storystream-postgres psql -U postgres -d storystream -c "INSERT INTO articles (id, title, section, snippet, image_url, published_at, source_name, external_url) VALUES ('${ARTICLE_ID}','${ART_TITLE}','${ART_SECTION}','${ART_SNIPPET}', NULL, now(), '${ART_SOURCE}', '${ART_URL}') ON CONFLICT (id) DO UPDATE SET title='${ART_TITLE}', snippet='${ART_SNIPPET}';"
fi
curl -sS -H "Authorization: Bearer ${TOKEN}" "http://localhost:8080/api/articles/${ARTICLE_ID}/context" | jq .
