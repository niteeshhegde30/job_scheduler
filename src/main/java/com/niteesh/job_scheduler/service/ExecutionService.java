package com.niteesh.job_scheduler.service;

import com.niteesh.job_scheduler.domain.Execution;
import com.niteesh.job_scheduler.domain.repository.ExecutionRepository;

public interface ExecutionService {
    Execution createExecution(Execution execution);
    Execution getExecution(Long executionId);
    // Other service methods
}
