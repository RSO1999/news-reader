package com.storystream.api.service;

import com.storystream.api.model.DailyReadCount;
import com.storystream.api.model.User;
import com.storystream.api.repository.DailyReadCountRepository;
import com.storystream.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class GatingService {

    @Autowired
    private DailyReadCountRepository repository;

    @Autowired
    private UserRepository userRepository;

    public boolean canAccessContent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("PREMIUM".equals(user.getSubscriptionTier())) return true;

        DailyReadCount readCount = repository.findByUserIdAndReadDate(user.getId(), LocalDate.now())
                .orElse(new DailyReadCount(user.getId(), LocalDate.now(), 0));

        return readCount.getCount() < 5;
    }

    @Transactional
    public void incrementCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("FREE".equals(user.getSubscriptionTier())) {
            DailyReadCount readCount = repository.findByUserIdAndReadDate(user.getId(), LocalDate.now())
                    .orElse(new DailyReadCount(user.getId(), LocalDate.now(), 0));

            readCount.setCount(readCount.getCount() + 1);
            repository.save(readCount);
        }
    }
}