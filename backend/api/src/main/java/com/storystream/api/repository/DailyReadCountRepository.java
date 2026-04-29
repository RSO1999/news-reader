package com.storystream.api.repository;

import com.storystream.api.model.DailyReadCount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyReadCountRepository extends JpaRepository<DailyReadCount, UUID> {
    Optional<DailyReadCount> findByUserIdAndReadDate(UUID userId, LocalDate readDate);
}