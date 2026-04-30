package com.storystream.api.controller;

import com.storystream.api.model.User;
import com.storystream.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final UserRepository userRepository;

    public SubscriptionController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeToPremium(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setSubscriptionTier("PREMIUM");
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("tier", user.getSubscriptionTier()));
    }
}

