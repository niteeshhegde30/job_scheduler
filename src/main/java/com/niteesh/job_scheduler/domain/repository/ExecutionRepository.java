package com.niteesh.job_scheduler.domain.repository;

import com.niteesh.job_scheduler.domain.Execution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {
    // Repository methods
}
