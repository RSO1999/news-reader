package com.storystream.api.dto.wikipedia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WikipediaPage(String title, String extract, Thumbnail thumbnail) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnail(String source) {}
}
