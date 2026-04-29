package com.storystream.api.controller;

import com.storystream.api.model.User;
import com.storystream.api.repository.UserRepository;
import com.storystream.api.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public UserController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Phase 6: Instant upgrade.
     * Updates subscription tier in DB and returns a refreshed JWT containing the new tier claim.
     */
    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeToPremium(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setSubscriptionTier("PREMIUM");
        userRepository.save(user);

        String newToken = jwtService.generateToken(user.getEmail(), user.getSubscriptionTier());
        return ResponseEntity.ok(Map.of(
                "token", newToken,
                "tier", user.getSubscriptionTier()
        ));
    }
}

