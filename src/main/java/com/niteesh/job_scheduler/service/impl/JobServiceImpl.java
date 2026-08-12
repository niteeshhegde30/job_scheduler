package com.niteesh.job_scheduler.service.impl;

import com.niteesh.job_scheduler.domain.Job;
import com.niteesh.job_scheduler.domain.Task;
import com.niteesh.job_scheduler.domain.enums.JobStatus;
import com.niteesh.job_scheduler.domain.repository.JobRepository;
import com.niteesh.job_scheduler.dto.CreateJobRequest;
import com.niteesh.job_scheduler.exception.JobNotFoundException;
import com.niteesh.job_scheduler.service.JobService;
import com.niteesh.job_scheduler.service.TaskService;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    private final TaskService taskService;

    public JobServiceImpl(JobRepository jobRepository, TaskService taskService) {
        this.jobRepository = jobRepository;
        this.taskService = taskService;
    }

    @Override
    public Job createJob(CreateJobRequest createJobRequest) {
        System.out.println("CreateJobRequest : " + createJobRequest);
        if (createJobRequest == null){
            throw new IllegalArgumentException("createJobRequest is null");
        }
        if (StringUtils.isBlank(createJobRequest.getTaskName())){
            throw new IllegalArgumentException("createJobRequest doesn't contain the task name");
        }
        Task task = taskService.getTaskByName(createJobRequest.getTaskName());
        Job job = new Job(task.getId(), createJobRequest.getScheduleType(), createJobRequest.getScheduledAt(),
                createJobRequest.getCronExpression(), createJobRequest.getParameters(),
                JobStatus.SCHEDULED.toString(), Instant.now(), Instant.now());
        Job savedJob = jobRepository.save(job);
        System.out.println("Saved the job : " + savedJob);
        return savedJob;
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
