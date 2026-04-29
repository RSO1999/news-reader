package com.storystream.api.repository;

import com.storystream.api.model.EngagementEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EngagementEventRepository extends JpaRepository<EngagementEvent, UUID> {

    @Query(value = """
            SELECT a.section, COUNT(*) AS views
            FROM engagement_events e
            JOIN articles a ON a.id = e.article_id
            WHERE e.user_id = :userId
              AND e.event_type = 'VIEW'
            GROUP BY a.section
            ORDER BY views DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topSections(@Param("userId") UUID userId, @Param("limit") int limit);

    @Query(value = """
            SELECT a.id, a.title, a.section, a.snippet, a.image_url, a.published_at, a.source_name
            FROM engagement_events e
            JOIN articles a ON a.id = e.article_id
            WHERE e.user_id = :userId
              AND e.event_type = 'VIEW'
            ORDER BY e.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> recentViewed(@Param("userId") UUID userId, @Param("limit") int limit);

    @Query(value = """
            SELECT a.id, a.title, a.section, a.snippet, a.image_url, a.published_at, a.source_name, a.external_url, COUNT(*) AS views
            FROM engagement_events e
            JOIN articles a ON a.id = e.article_id
            WHERE e.event_type = 'VIEW'
              AND e.created_at >= (NOW() - INTERVAL '24 hours')
            GROUP BY a.id, a.title, a.section, a.snippet, a.image_url, a.published_at, a.source_name, a.external_url
            ORDER BY views DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> trendingArticles(@Param("limit") int limit);

    @Query(value = """
            SELECT a.id, a.title, a.section, a.snippet, a.image_url, a.published_at, a.source_name, a.external_url
            FROM engagement_events e
            JOIN articles a ON a.id = e.article_id
            WHERE e.user_id = :userId
              AND e.event_type = 'SAVE'
            ORDER BY e.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> recentSaved(@Param("userId") UUID userId, @Param("limit") int limit);

    boolean existsByUserIdAndArticleIdAndEventType(UUID userId, UUID articleId, String eventType);
}
