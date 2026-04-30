
package com.storystream.api.service;

import com.storystream.api.client.GuardianArticle;
import com.storystream.api.client.GuardianResponse;
import com.storystream.api.model.Article;
import com.storystream.api.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
public class ArticleSyncService {

    private static final Logger log = LoggerFactory.getLogger(ArticleSyncService.class);
    private final ArticleRepository articleRepository;
    private final RestClient restClient;

    public ArticleSyncService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
        this.restClient = RestClient.create("https://content.guardianapis.com");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void fetchAndSaveArticles() {
        log.info("Fetching latest articles from The Guardian...");

        try {
            GuardianResponse response = restClient.get()
                    .uri("/search?api-key=test&show-fields=headline,trailText,thumbnail&page-size=20")
                    .retrieve()
                    .body(GuardianResponse.class);

            if (response != null && response.response() != null) {
                int savedCount = 0;

                for (GuardianArticle gArticle : response.response().results()) {
                    Article article = new Article();
                    article.setExternalUrl(gArticle.webUrl());
                    article.setTitle(gArticle.webTitle());
                    article.setSection(gArticle.sectionName());
                    article.setPublishedAt(Instant.parse(gArticle.webPublicationDate()));
                    article.setSourceName("The Guardian");

                    if (gArticle.fields() != null) {
                        article.setSnippet(gArticle.fields().trailText() != null ? gArticle.fields().trailText() : gArticle.fields().headline());
                        article.setImageUrl(gArticle.fields().thumbnail());
                    }

                    try {
                        articleRepository.save(article);
                        savedCount++;
                    } catch (DataIntegrityViolationException e) {
                        log.debug("Article already exists, skipping: {}", article.getTitle());
                    }
                }
                log.info("Successfully synced {} new articles to the database.", savedCount);
            }
        } catch (Exception e) {
            log.error("Failed to sync articles: ", e);
        }
    }
}