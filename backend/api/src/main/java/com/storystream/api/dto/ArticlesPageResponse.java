package com.storystream.api.dto;

import java.util.List;

/**
 * Paged list response shape expected by the Android app:
 * { "content": [...], "totalPages": n }
 */
public record ArticlesPageResponse(
        List<ArticleResponse> content,
        int totalPages
) {}

