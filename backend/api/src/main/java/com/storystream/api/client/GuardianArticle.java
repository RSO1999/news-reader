package com.storystream.api.client;
public record GuardianArticle(
        String webUrl,
        String webTitle,
        String sectionName,
        String webPublicationDate,
        GuardianFields fields
) {}