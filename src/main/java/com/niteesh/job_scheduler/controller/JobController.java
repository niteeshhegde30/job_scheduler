package com.niteesh.job_scheduler.controller;

import com.niteesh.job_scheduler.domain.Job;
import com.niteesh.job_scheduler.dto.CreateJobRequest;
import com.niteesh.job_scheduler.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/{id}")
    public Job findJobById(@PathVariable Long id){
        return jobService.getJob(id);
    }

    @PostMapping("/job")
    @ResponseStatus(HttpStatus.CREATED)
    public Job createJob(@RequestBody CreateJobRequest createJobRequest){
        return jobService.createJob(createJobRequest);
    }

}
