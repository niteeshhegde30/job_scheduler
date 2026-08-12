package com.niteesh.job_scheduler.controller;

import com.niteesh.job_scheduler.domain.Job;
import com.niteesh.job_scheduler.service.JobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("jobs/")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/{id}")
    public Job findJobById(@PathVariable Long id){
        return jobService.getJob(id);
    }

}
