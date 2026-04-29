package com.storystream.api.dto;

import java.util.List;

public record ReadingInsightsResponse(
        UserInfo user,
        DailyUsage dailyUsage,
        List<TopSection> topSections,
        List<ArticleSummary> recentHistory
) {
    public record UserInfo(String email, String tier) {}

    public record DailyUsage(String date, int reads, int limit, boolean isUnlimited) {}

    public record TopSection(String section, int views, double percent) {}

    public record ArticleSummary(
            String articleId,
            String title,
            String section,
            String snippet,
            String imageUrl,
            String publishedAt,
            String sourceName
    ) {}
}

