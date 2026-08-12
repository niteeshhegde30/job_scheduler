package com.niteesh.job_scheduler.service;

import com.niteesh.job_scheduler.domain.Job;
import com.niteesh.job_scheduler.domain.repository.JobRepository;
import com.niteesh.job_scheduler.dto.CreateJobRequest;

public interface JobService {
    Job createJob(CreateJobRequest job);
    Job getJob(Long jobId);
    Job cancelJob(Long jobId);
    // Other service methods
}
