package com.storystream.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storystream.api.dto.ContextEntity;
import com.storystream.api.dto.ContextResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Calls the Google Gemini REST API to generate structured article context panels.
 */
@Service
public class GeminiContextService {

    private static final Logger log = LoggerFactory.getLogger(GeminiContextService.class);

    private final RestClient geminiClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;

    private final ConcurrentHashMap<String, Boolean> inFlight = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();

    public GeminiContextService(
            @Value("${storystream.gemini.api-key}") String apiKey,
            @Value("${storystream.gemini.model:gemini-2.5-flash-lite}") String model,
            @Value("${storystream.gemini.max-output-tokens:800}") int maxOutputTokens,
            RestClient.Builder restClientBuilder
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.objectMapper = new ObjectMapper();

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(20));

        this.geminiClient = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(factory)
                .build();
    }

    public ContextResponse getContext(
            String articleId,
            String title,
            String snippet,
            String section,
            String imageUrl,
            Instant publishedAt,
            String sourceName,
            String externalUrl
    ) {
        if (title == null) title = "";
        if (snippet == null) snippet = "";

        // Coalesce: skip duplicate concurrent calls for same article
        if (inFlight.putIfAbsent(articleId, true) != null) {
            log.debug("Context fetch already in flight for articleId={}", articleId);
            // Even if coalesced, still return an empty but non-null response to caller to avoid nulls
            return new ContextResponse(List.of());
        }

        try {
            String userPrompt = buildUserPrompt(title, snippet, section, imageUrl, publishedAt, sourceName, externalUrl);
            String requestBody = buildRequestBody(userPrompt);

            // Debug logging of the request body (safe: API key is sent as query param, not in body).
            if (log.isDebugEnabled()) {
                log.debug("Gemini request body for articleId={}: {}", articleId, requestBody);
            }

            // Store the last request body for opt-in debug retrieval
            try {
                lastRequestBody.set(requestBody);
            } catch (Exception ignored) {
            }

            // Info-level summary to allow quick inspection in logs without printing full prompt/body.
            String promptPreview = userPrompt.replaceAll("\\s+", " ");
            if (promptPreview.length() > 160) {
                promptPreview = promptPreview.substring(0, 160) + "...";
            }
            log.info("Sending Gemini request: articleId={} model={} promptPreview='{}' requestBodyLen={}",
                    articleId, model, promptPreview, requestBody != null ? requestBody.length() : 0);

            log.info("Calling Gemini for articleId={} model={}", articleId, model);

            String rawResponse = geminiClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("Gemini raw response length={} for articleId={}{}",
                    rawResponse != null ? rawResponse.length() : 0,
                    articleId,
                    "");
            log.debug("Gemini raw response: {}", rawResponse);

            return parseResponseLenient(rawResponse, title, userPrompt);

        } catch (Exception e) {
            log.error("Gemini call failed for articleId={}: {}", articleId, e.getMessage(), e);
            String fallback = "Gemini call failed: " + e.getMessage();
            return new ContextResponse(List.of(new ContextEntity(title != null ? title : "", fallback, "AI Context (Gemini)", null)));
        } finally {
            inFlight.remove(articleId);
        }
    }

    // ─── Prompt Engineering ────────────────────────────────────────────────────

    private String buildUserPrompt(String title, String snippet, String section, String imageUrl, Instant publishedAt, String sourceName, String externalUrl) {
        String cleanSnippet = snippet != null ? snippet.replaceAll("<[^>]+>", "").trim() : "";
        StringBuilder sb = new StringBuilder();
        sb.append("Given the full article data below, produce a single structured JSON object that provides background context, key facts, and a list of entities.\n\n");
        sb.append("ARTICLE TITLE: ").append(title == null ? "" : title).append("\n");
        sb.append("SNIPPET: ").append(cleanSnippet).append("\n");
        sb.append("SECTION: ").append(section == null ? "" : section).append("\n");
        sb.append("IMAGE_URL: ").append(imageUrl == null ? "" : imageUrl).append("\n");
        sb.append("PUBLISHED_AT: ").append(publishedAt == null ? "" : publishedAt.toString()).append("\n");
        sb.append("SOURCE_NAME: ").append(sourceName == null ? "" : sourceName).append("\n");
        sb.append("EXTERNAL_URL: ").append(externalUrl == null ? "" : externalUrl).append("\n\n");
        sb.append("Return ONLY a single valid JSON object (no surrounding text) with this exact schema:\n");
        sb.append("{\n");
        sb.append("  \"summary\": string,\n");
        sb.append("  \"bullets\": [string],\n");
        sb.append("  \"entities\": [ { \"name\": string, \"type\": string|null, \"short_summary\": string|null, \"relevance\": number|null } ],\n");
        sb.append("  \"confidence\": number|null,\n");
        sb.append("  \"status\": string|null,\n");
        sb.append("  \"raw\": optional string (return the original article text or any additional info)\n");
        sb.append("}\n");
        sb.append("If you cannot find factual info, set confidence low but still return the JSON. Do not refuse or return errors. Output ONLY the JSON object.");
        return sb.toString();
    }

    // ─── Request / Response Helpers ────────────────────────────────────────────

    private String buildRequestBody(String userPrompt) throws Exception {
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text",
                                "You are a factual news context assistant. Output ONLY a single valid JSON object. No markdown, no commentary, no text outside the JSON."))
                ),
                "contents", List.of(
                        Map.of("role", "user",
                                "parts", List.of(Map.of("text", userPrompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.0,
                        "maxOutputTokens", maxOutputTokens,
                        "responseMimeType", "application/json"
                )
        );
        return objectMapper.writeValueAsString(body);
    }

    private ContextResponse parseResponseLenient(String raw, String fallbackTitle, String prompt) {
        try {
            String text = raw == null ? "" : raw;
            if (text.isBlank()) {
                return new ContextResponse(List.of(new ContextEntity(fallbackTitle, "", "AI Context (Gemini)", null)));
            }

            JsonNode root = null;
            try {
                root = objectMapper.readTree(text);
            } catch (Exception e) {
                // If top-level parsing fails, try to extract candidates->content->parts[0].text as raw text
                try {
                    JsonNode tmp = objectMapper.readTree(text);
                    root = tmp;
                } catch (Exception ex) {
                    // Not JSON at top-level; treat raw as the text the model returned.
                    String summary = text.strip();
                    return new ContextResponse(List.of(new ContextEntity(fallbackTitle, summary, "AI Context (Gemini)", null)));
                }
            }

            // If response contains candidates, attempt to pull the first parts text
            JsonNode candidates = root.path("candidates");
            String extracted = null;
            if (candidates.isArray() && !candidates.isEmpty()) {
                extracted = candidates.get(0).path("content").path("parts").get(0).path("text").asText(null);
            }

            String payloadText = extracted != null ? extracted : (root.has("summary") || root.has("bullets") ? root.toString() : text);
            if (payloadText == null) payloadText = text;

            String cleaned = payloadText.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```\\s*$", "").strip();
            }

            JsonNode json;
            try {
                json = objectMapper.readTree(cleaned);
            } catch (Exception e) {
                return new ContextResponse(List.of(new ContextEntity(fallbackTitle, cleaned, "AI Context (Gemini)", null)));
            }

            String summary = json.path("summary").asText("");
            String source = "AI Context (Gemini)";
            List<ContextEntity> entities = new ArrayList<>();

            String rich = buildRichSummary(json);

            entities.add(new ContextEntity(cleanTitle(fallbackTitle), rich == null || rich.isBlank() ? summary : rich, source, null));

            JsonNode entitiesNode = json.path("entities");
            if (entitiesNode.isArray()) {
                for (JsonNode e : entitiesNode) {
                    String name = e.path("name").asText(null);
                    String shortSummary = e.path("short_summary").asText(null);
                    double relevance = e.path("relevance").asDouble(0.0);
                    String type = e.path("type").asText("");

                    if (name == null) continue; // still skip if name missing

                    String title = name + (type.isBlank() ? "" : " · " + type);
                    String sum = shortSummary != null ? shortSummary : "";
                    entities.add(new ContextEntity(title, sum, source, null));
                }
            }

            return new ContextResponse(entities);

        } catch (Exception e) {
            String fallback = raw == null ? "" : raw;
            return new ContextResponse(List.of(new ContextEntity(fallbackTitle, fallback, "AI Context (Gemini)", null)));
        }
    }

    /**
     * Combines the summary and bullets into one rich summary string for the UI card.
     */
    private String buildRichSummary(JsonNode json) {
        StringBuilder sb = new StringBuilder();

        String summary = json.path("summary").asText("");
        if (!summary.isBlank()) {
            sb.append(summary).append("\n\n");
        }

        JsonNode bullets = json.path("bullets");
        if (bullets.isArray() && !bullets.isEmpty()) {
            sb.append("Key facts:\n");
            for (JsonNode b : bullets) {
                String bullet = b.asText("").trim();
                if (!bullet.isBlank()) {
                    sb.append("• ").append(bullet).append("\n");
                }
            }
        }

        return sb.toString().strip();
    }

    private String cleanTitle(String title) {
        // Strip subtitle after colon/dash for a cleaner card header
        return title == null ? "" : title.split("[:\\-–—|]")[0].trim();
    }

    // Getter for debug controller
    public String getLastRequestBody() {
        return lastRequestBody.get();
    }
}
