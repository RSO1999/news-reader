package com.storystream.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_url", unique = true, nullable = false)
    private String externalUrl;

    private String title;

    private String section;

    @Column(columnDefinition = "TEXT")
    private String snippet;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "source_name")
    private String sourceName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_payload", columnDefinition = "jsonb")
    private Map<String, Object> contextPayload;

    // Getters and Setters (Generate these in your IDE or use Lombok @Data if you added it)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public Map<String, Object> getContextPayload() { return contextPayload; }
    public void setContextPayload(Map<String, Object> contextPayload) { this.contextPayload = contextPayload; }
}