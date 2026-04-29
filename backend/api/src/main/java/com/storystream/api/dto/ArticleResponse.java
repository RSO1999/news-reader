package com.storystream.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String title,
        String section,
        String snippet,
        String imageUrl,
        Instant publishedAt,
        String sourceName,
        String externalUrl
) {}