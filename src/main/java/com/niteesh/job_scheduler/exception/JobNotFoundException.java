package com.niteesh.job_scheduler.exception;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(Long jobId) {
        super("Job not found with ID: " + jobId);
    }
}
