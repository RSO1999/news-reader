package com.storystream.api.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_read_counts")
public class DailyReadCount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private LocalDate readDate;
    private int count;

    // IMPORTANT: Add a constructor for the Service to use
    public DailyReadCount() {}

    public DailyReadCount(UUID userId, LocalDate readDate, int count) {
        this.userId = userId;
        this.readDate = readDate;
        this.count = count;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public LocalDate getReadDate() { return readDate; }
    public void setReadDate(LocalDate readDate) { this.readDate = readDate; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}