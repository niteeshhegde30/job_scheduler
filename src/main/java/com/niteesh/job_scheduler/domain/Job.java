package com.niteesh.job_scheduler.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;
    private String scheduleType;
    private Instant scheduledAt;
    private String cronExpression;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Object parameters;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public Job() {
    }

    public Job(Long taskId, String scheduleType, Instant scheduledAt, String cronExpression,
               Object parameters, String status, Instant createdAt, Instant updatedAt) {
        this.taskId = taskId;
        this.scheduleType = scheduleType;
        this.scheduledAt = scheduledAt;
        this.cronExpression = cronExpression;
        this.parameters = parameters;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", scheduleType='" + scheduleType + '\'' +
                ", scheduledAt=" + scheduledAt +
                ", cronExpression='" + cronExpression + '\'' +
                ", parameters=" + parameters +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
