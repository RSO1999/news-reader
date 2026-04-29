#!/usr/bin/env bash
set -euo pipefail
EMAIL="a@gmail.com"
PASSWORD="test"
ARTICLE_ID="11111111-1111-1111-1111-111111111111"
RESP=$(curl -sS -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"'"$EMAIL"'","password":"'"$PASSWORD"'"}')
TOKEN=$(echo "$RESP" | jq -r .token)
docker exec -i storystream-postgres psql -U postgres -d storystream -c "UPDATE users SET subscription_tier='PREMIUM' WHERE email='${EMAIL}';"
docker exec -i storystream-postgres psql -U postgres -d storystream -c "UPDATE articles SET context_payload = NULL WHERE id = '${ARTICLE_ID}';"
docker exec -i storystream-postgres psql -U postgres -d storystream -c "INSERT INTO articles (id, title, section, snippet, image_url, published_at, source_name, external_url) VALUES ('${ARTICLE_ID}','My Article Title','News','A short snippet with useful keywords', NULL, now(), 'Test', 'https://example.com') ON CONFLICT (id) DO UPDATE SET title='My Article Title', snippet='A short snippet with useful keywords';"
curl -sS -H "Authorization: Bearer ${TOKEN}" "http://localhost:8080/api/articles/${ARTICLE_ID}/context" | jq .
curl -sS -H "Authorization: Bearer ${TOKEN}" "http://localhost:8080/internal/debug/gemini/last-request" | jq .
