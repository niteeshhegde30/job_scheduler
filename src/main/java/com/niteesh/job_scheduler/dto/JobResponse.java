package com.niteesh.job_scheduler.dto;

import com.niteesh.job_scheduler.domain.Job;
import com.niteesh.job_scheduler.domain.Task;

import java.time.Instant;

public class JobResponse {
    private Long id;
    private String taskName;
    private String scheduleType;
    private Instant scheduledAt;
    private String cronExpression;
    private Object parameters;
    private String status;
    private Instant createdAt;

    private JobResponse(Long id, String taskName, String scheduleType, Instant scheduledAt, String cronExpression,
                       Object parameters, String status, Instant createdAt) {
        this.id = id;
        this.taskName = taskName;
        this.scheduleType = scheduleType;
        this.scheduledAt = scheduledAt;
        this.cronExpression = cronExpression;
        this.parameters = parameters;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static JobResponse createFromJobAndTask(Job job, Task task){
        return new JobResponse(job.getId(), task.getName(), job.getScheduleType(), job.getScheduledAt(), job.getCronExpression(),
                job.getParameters(), job.getStatus(), job.getCreatedAt());
    }

    // Getters and setters


    public Long getId() {
        return id;
    }

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

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "JobResponse{" +
                "id=" + id +
                ", taskName='" + taskName + '\'' +
                ", scheduleType='" + scheduleType + '\'' +
                ", scheduledAt=" + scheduledAt +
                ", cronExpression='" + cronExpression + '\'' +
                ", parameters=" + parameters +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
