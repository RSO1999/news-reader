package com.storystream.api.controller;

import com.storystream.api.dto.ReadingInsightsResponse;
import com.storystream.api.dto.ArticleResponse;
import com.storystream.api.model.DailyReadCount;
import com.storystream.api.model.User;
import com.storystream.api.repository.DailyReadCountRepository;
import com.storystream.api.repository.EngagementEventRepository;
import com.storystream.api.repository.UserRepository;
import com.storystream.api.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class InsightsController {

    private final UserRepository userRepository;
    private final DailyReadCountRepository dailyReadCountRepository;
    private final EngagementEventRepository engagementEventRepository;
    private final JwtService jwtService;

    public InsightsController(UserRepository userRepository,
                              DailyReadCountRepository dailyReadCountRepository,
                              EngagementEventRepository engagementEventRepository,
                              JwtService jwtService) {
        this.userRepository = userRepository;
        this.dailyReadCountRepository = dailyReadCountRepository;
        this.engagementEventRepository = engagementEventRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/insights")
    public ResponseEntity<ReadingInsightsResponse> insights(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean unlimited = "PREMIUM".equals(user.getSubscriptionTier());

        DailyReadCount cnt = dailyReadCountRepository
                .findByUserIdAndReadDate(user.getId(), LocalDate.now())
                .orElse(new DailyReadCount(user.getId(), LocalDate.now(), 0));

        int limit = unlimited ? -1 : 5;

        List<Object[]> top = engagementEventRepository.topSections(user.getId(), 10);
        int totalViews = 0;
        for (Object[] row : top) {
            totalViews += ((Number) row[1]).intValue();
        }

        List<ReadingInsightsResponse.TopSection> topSections = new ArrayList<>();
        for (Object[] row : top) {
            String section = (String) row[0];
            int views = ((Number) row[1]).intValue();
            double pct = totalViews == 0 ? 0.0 : (views * 1.0) / totalViews;
            topSections.add(new ReadingInsightsResponse.TopSection(section, views, pct));
        }

        List<Object[]> recent = engagementEventRepository.recentViewed(user.getId(), 20);
        List<ReadingInsightsResponse.ArticleSummary> history = new ArrayList<>();
        for (Object[] r : recent) {
            history.add(new ReadingInsightsResponse.ArticleSummary(
                    r[0].toString(),
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    (String) r[4],
                    r[5].toString(),
                    (String) r[6]
            ));
        }

        ReadingInsightsResponse resp = new ReadingInsightsResponse(
                new ReadingInsightsResponse.UserInfo(user.getEmail(), user.getSubscriptionTier()),
                new ReadingInsightsResponse.DailyUsage(LocalDate.now().toString(), cnt.getCount(), limit, unlimited),
                topSections,
                history
        );

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/saved")
    public ResponseEntity<List<ArticleResponse>> saved(Principal principal,
                                                       @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                       @RequestParam(name = "limit", defaultValue = "50") int limit) {
        String email = null;
        if (principal != null) {
            email = principal.getName();
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length()).trim();
            try {
                email = jwtService.extractEmail(token);
            } catch (Exception ignored) {
            }
        }

        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Object[]> rows = engagementEventRepository.recentSaved(user.getId(), limit);
        List<ArticleResponse> results = new ArrayList<>();
        for (Object[] r : rows) {
            results.add(new ArticleResponse(
                    UUID.fromString(r[0].toString()),
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    (String) r[4],
                    (java.time.Instant) r[5],
                    (String) r[6],
                    (String) r[7]
            ));
        }

        return ResponseEntity.ok(results);
    }
}
