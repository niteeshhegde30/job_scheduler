package com.niteesh.job_scheduler;

import org.springframework.boot.SpringApplication;

public class TestJobSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.from(JobSchedulerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
