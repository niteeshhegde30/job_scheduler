package com.niteesh.job_scheduler.dto;

import java.time.Instant;

public class ExecutionResponse {
    private Long id;
    private Long jobId;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private String status;
    private int attempt;
    private String errorMessage;
    private Instant createdAt;

    // Getters and setters


    @Override
    public String toString() {
        return "ExecutionResponse{" +
                "id=" + id +
                ", jobId=" + jobId +
                ", scheduledAt=" + scheduledAt +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", status='" + status + '\'' +
                ", attempt=" + attempt +
                ", errorMessage='" + errorMessage + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
