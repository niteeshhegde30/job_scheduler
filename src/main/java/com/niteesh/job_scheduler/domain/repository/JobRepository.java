package com.niteesh.job_scheduler.domain.repository;

import com.niteesh.job_scheduler.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    // Repository methods
}
