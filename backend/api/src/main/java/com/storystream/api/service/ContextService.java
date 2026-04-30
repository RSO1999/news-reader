package com.storystream.api.service;

import com.storystream.api.dto.ContextEntity;
import com.storystream.api.dto.wikipedia.WikipediaPage;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Duration;
import java.util.Optional;

@Service
public class ContextService {

    private final RestClient restClient;

    public ContextService(RestClient.Builder restClientBuilder) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = restClientBuilder
                .baseUrl("https://en.wikipedia.org/api/rest_v1")
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<ContextEntity> getContext(String entity) {
        if (entity == null || entity.isBlank()) return Optional.empty();

        String wikiTitle = entity.trim().replace(' ', '_');

        try {
            WikipediaPage page = restClient.get()
                    .uri("/page/summary/{title}", wikiTitle)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RestClientException("Wikipedia returned HTTP " + res.getStatusCode());
                    })
                    .body(WikipediaPage.class);

            if (page == null || page.extract() == null || page.extract().isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new ContextEntity(
                    page.title() != null ? page.title() : entity.trim(),
                    page.extract(),
                    "Wikipedia",
                    page.thumbnail() != null ? page.thumbnail().source() : null
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}