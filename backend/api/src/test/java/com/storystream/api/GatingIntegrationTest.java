package com.storystream.api;

import com.storystream.api.model.Article;
import com.storystream.api.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatingIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ArticleRepository articleRepository;

    @Test
    void articleDetail_requiresJwt_and_returns403OnlyAfterLimit() throws Exception {
        // Create an article we can fetch.
        Article article = new Article();
        article.setTitle("Test");
        article.setSection("Test");
        article.setSnippet("Test");
        article.setSourceName("Test");
        article.setPublishedAt(Instant.parse("2026-03-01T00:00:00Z"));
        article.setExternalUrl("https://example.com/test-" + System.nanoTime());
        article = articleRepository.save(article);

        // 1) Without JWT -> 401
        mockMvc.perform(get("/api/articles/{id}", article.getId()))
                .andExpect(status().isUnauthorized());

        // 2) Register to get JWT token
        String token = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"free@test.dev\",\"password\":\"pw\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Quick extract without adding dependencies: read between "token":" and next quote.
        String extracted = token.replaceAll(".*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

        // 3) First 5 reads OK
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/articles/{id}", article.getId())
                            .header("Authorization", "Bearer " + extracted))
                    .andExpect(status().isOk());
        }

        // 4) 6th read -> 403 LIMIT_REACHED
        mockMvc.perform(get("/api/articles/{id}", article.getId())
                        .header("Authorization", "Bearer " + extracted))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIMIT_REACHED"));
    }
}
