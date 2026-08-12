package com.niteesh.job_scheduler.service;

import com.niteesh.job_scheduler.domain.Job;
import com.niteesh.job_scheduler.domain.repository.JobRepository;

public interface JobService {
    Job createJob(Job job);
    Job getJob(Long jobId);
    Job cancelJob(Long jobId);
    // Other service methods
}
