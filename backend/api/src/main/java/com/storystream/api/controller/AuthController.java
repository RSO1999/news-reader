package com.storystream.api.controller;

import com.storystream.api.model.User;
import com.storystream.api.repository.UserRepository;
import com.storystream.api.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.storystream.api.repository.DailyReadCountRepository;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final DailyReadCountRepository dailyReadCountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          DailyReadCountRepository dailyReadCountRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.dailyReadCountRepository = dailyReadCountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User created = new User();
            created.setEmail(email);
            created.setPassword(passwordEncoder.encode(password));
            return userRepository.save(created);
        });

        dailyReadCountRepository.findByUserIdAndReadDate(user.getId(), LocalDate.now())
                .ifPresent(dailyReadCountRepository::delete);

        String token = jwtService.generateToken(user.getEmail(), user.getSubscriptionTier());
        return Map.of("token", token);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> request) {
        User user = userRepository.findByEmail(request.get("email"))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (passwordEncoder.matches(request.get("password"), user.getPassword())) {
            String token = jwtService.generateToken(user.getEmail(), user.getSubscriptionTier());
            return Map.of("token", token);
        } else {
            throw new RuntimeException("Invalid password");
        }
    }
}