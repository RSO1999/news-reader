package com.storystream.api.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "engagement_events")
public class EngagementEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private UUID articleId;

    /**
     * VIEW, SAVE, LIKE, etc.
     */
    private String eventType;

    private Instant createdAt;

    public EngagementEvent() {}

    public EngagementEvent(UUID userId, UUID articleId, String eventType, Instant createdAt) {
        this.userId = userId;
        this.articleId = articleId;
        this.eventType = eventType;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getArticleId() { return articleId; }
    public void setArticleId(UUID articleId) { this.articleId = articleId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

