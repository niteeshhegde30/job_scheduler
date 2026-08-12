package com.niteesh.job_scheduler.service.impl;

import com.niteesh.job_scheduler.domain.Job;
import com.niteesh.job_scheduler.domain.repository.JobRepository;
import com.niteesh.job_scheduler.exception.JobNotFoundException;
import com.niteesh.job_scheduler.service.JobService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }


    @Override
    public Job createJob(Job job) {
        return null;
    }

    @Override
    public Job getJob(Long id) {
        System.out.println("Received id : " + id);
        Optional<Job> optionalJob = jobRepository.findById(id);
        if (optionalJob.isEmpty()){
            throw new JobNotFoundException(id);
        }
        Job job = optionalJob.get();
        System.out.printf("Returning job %s for %s%n", job, id);
        return job;
    }

    @Override
    public Job cancelJob(Long jobId) {
        return null;
    }
}
