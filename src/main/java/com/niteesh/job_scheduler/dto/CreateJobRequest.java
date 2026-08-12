package com.niteesh.job_scheduler.dto;

import java.time.Instant;


public class CreateJobRequest {
    private String taskName;
    private String scheduleType;
    private Instant scheduledAt;
    private String cronExpression;
    private Object parameters;

    // Getters and setters

    public String getTaskName() {
        return taskName;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public Object getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "CreateJobRequest{" +
                "taskName='" + taskName + '\'' +
                ", scheduleType='" + scheduleType + '\'' +
                ", scheduledAt=" + scheduledAt +
                ", cronExpression='" + cronExpression + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
