#!/usr/bin/env bash
set -euo pipefail
API_KEY="${STORYSTREAM_GEMINI_API_KEY:-${GEMINI_API_KEY:-}}"
if [ -z "$API_KEY" ]; then
  echo "Set STORYSTREAM_GEMINI_API_KEY (preferred) or GEMINI_API_KEY before running this script."
  exit 1
fi
RESP=$(curl -sS -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"a@gmail.com","password":"test"}')
TOKEN=$(echo "$RESP" | jq -r .token 2>/dev/null || echo "$RESP" | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
docker exec -e PGPASSWORD=password storystream-postgres psql -h localhost -U postgres -d storystream -c "UPDATE users SET subscription_tier='PREMIUM' WHERE email='a@gmail.com';"
ARTICLE_ID=11111111-1111-1111-1111-111111111111
docker exec -e PGPASSWORD=password storystream-postgres psql -h localhost -U postgres -d storystream -c "UPDATE articles SET context_payload = NULL WHERE id = '${ARTICLE_ID}';"
docker exec -e PGPASSWORD=password storystream-postgres psql -h localhost -U postgres -d storystream -c "INSERT INTO articles (id, title, section, snippet, image_url, published_at, source_name, external_url) VALUES ('${ARTICLE_ID}','My Article Title','News','A short snippet with useful keywords', NULL, now(), 'Test', 'https://example.com') ON CONFLICT (id) DO UPDATE SET title='My Article Title', snippet='A short snippet with useful keywords';"
curl -sS -H "Authorization: Bearer ${TOKEN}" "http://localhost:8080/api/articles/${ARTICLE_ID}/context" | jq .
