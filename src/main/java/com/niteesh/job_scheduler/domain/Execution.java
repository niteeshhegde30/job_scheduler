package com.niteesh.job_scheduler.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "executions")
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private String status;
    private int attempt;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempt() {
        return attempt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Execution{" +
                "id=" + id +
                ", jobId=" + jobId +
                ", scheduledAt=" + scheduledAt +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", status='" + status + '\'' +
                ", attempt=" + attempt +
                ", errorMessage='" + errorMessage + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
