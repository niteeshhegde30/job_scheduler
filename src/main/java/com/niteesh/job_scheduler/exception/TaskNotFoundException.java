package com.niteesh.job_scheduler.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long taskId) {
        super("Task not found with ID: " + taskId);
    }

    public TaskNotFoundException(String taskName) {
        super("Task not found with name: " + taskName);
    }
}
