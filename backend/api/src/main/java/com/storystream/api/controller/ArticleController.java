package com.storystream.api.controller;

import com.storystream.api.dto.ArticleResponse;
import com.storystream.api.dto.ArticlesPageResponse;
import com.storystream.api.model.Article;
import com.storystream.api.model.EngagementEvent;
import com.storystream.api.model.User;
import com.storystream.api.repository.ArticleRepository;
import com.storystream.api.repository.EngagementEventRepository;
import com.storystream.api.repository.UserRepository;
import com.storystream.api.service.GeminiContextService;
import com.storystream.api.service.GatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired private ArticleRepository articleRepository;
    @Autowired private GatingService gatingService;
    @Autowired private GeminiContextService geminiContextService;
    @Autowired private UserRepository userRepository;
    @Autowired private EngagementEventRepository engagementEventRepository;


    @GetMapping
    public ArticlesPageResponse getArticles(
            @PageableDefault(page = 0, size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "personalized", defaultValue = "false") boolean personalized,
            Principal principal
    ) {
        if (!personalized || principal == null) {
            Page<Article> page = articleRepository.findAll(pageable);
            return new ArticlesPageResponse(
                    page.getContent().stream().map(this::toDto).toList(),
                    page.getTotalPages()
            );
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Object[]> top = engagementEventRepository.topSections(user.getId(), 5);
        Map<String, Integer> weights = new HashMap<>();
        int w = 5;
        for (Object[] row : top) {
            String section = (String) row[0];
            weights.put(section, w--);
        }

        Pageable candidatePageable = PageRequest.of(pageable.getPageNumber(), Math.max(pageable.getPageSize() * 3, 60),
                Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Article> candidatePage = articleRepository.findAll(candidatePageable);

        List<Article> sorted = new ArrayList<>(candidatePage.getContent());
        sorted.sort((a, b) -> {
            int wa = weights.getOrDefault(a.getSection(), 0);
            int wb = weights.getOrDefault(b.getSection(), 0);
            if (wa != wb) return Integer.compare(wb, wa);
            return b.getPublishedAt().compareTo(a.getPublishedAt());
        });

        List<ArticleResponse> pageItems = sorted.stream()
                .limit(pageable.getPageSize())
                .map(this::toDto)
                .toList();

        return new ArticlesPageResponse(pageItems, candidatePage.getTotalPages());
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getArticle(@PathVariable UUID id, Principal principal) {
        if (!gatingService.canAccessContent(principal.getName())) {
            return ResponseEntity.status(403).body(Map.of("code", "LIMIT_REACHED"));
        }

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        gatingService.incrementCount(principal.getName());

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        engagementEventRepository.save(new EngagementEvent(user.getId(), article.getId(), "VIEW", Instant.now()));

        return ResponseEntity.ok(toDto(article));
    }


    @GetMapping("/{id}/context")
    public ResponseEntity<?> getArticleContext(@PathVariable UUID id, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"PREMIUM".equals(user.getSubscriptionTier())) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "code", "PREMIUM_FEATURE_LOCKED",
                            "message", "Access to this feature requires a Premium subscription."
                    )
            );
        }

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        Map<String, Object> cached = article.getContextPayload();
        if (cached != null && !cached.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                com.storystream.api.dto.ContextResponse cachedResponse =
                        om.convertValue(cached, com.storystream.api.dto.ContextResponse.class);
                if (cachedResponse != null && !cachedResponse.entities().isEmpty()) {
                    return ResponseEntity.ok(cachedResponse);
                }
            } catch (Exception ignored) {
            }
        }

        com.storystream.api.dto.ContextResponse response =
                geminiContextService.getContext(
                        article.getId().toString(),
                        article.getTitle(),
                        article.getSnippet(),
                        article.getSection(),
                        article.getImageUrl(),
                        article.getPublishedAt(),
                        article.getSourceName(),
                        article.getExternalUrl()
                );

        if (!response.entities().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = om.convertValue(response, Map.class);
                article.setContextPayload(payload);
                articleRepository.save(article);
            } catch (Exception e) {
            }
        }

        return ResponseEntity.ok(response);
    }


    @GetMapping("/trending")
    public List<ArticleResponse> getTrending(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        List<Object[]> rows = engagementEventRepository.trendingArticles(limit);
        List<ArticleResponse> results = new ArrayList<>();
        for (Object[] r : rows) {
            results.add(new ArticleResponse(
                    UUID.fromString(r[0].toString()),
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    (String) r[4],
                    (java.time.Instant) r[5],
                    (String) r[6],
                    (String) r[7]
            ));
        }
        return results;
    }


    @PostMapping("/{id}/save")
    public ResponseEntity<?> saveArticle(@PathVariable UUID id, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!engagementEventRepository.existsByUserIdAndArticleIdAndEventType(user.getId(), id, "SAVE")) {
            engagementEventRepository.save(new EngagementEvent(user.getId(), id, "SAVE", Instant.now()));
        }
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    private ArticleResponse toDto(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getSection(),
                article.getSnippet(),
                article.getImageUrl(),
                article.getPublishedAt(),
                article.getSourceName(),
                article.getExternalUrl()
        );
    }
}
