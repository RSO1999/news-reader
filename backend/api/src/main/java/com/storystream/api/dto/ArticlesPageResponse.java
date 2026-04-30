package com.storystream.api.dto;

import java.util.List;


public record ArticlesPageResponse(
        List<ArticleResponse> content,
        int totalPages
) {}

