package com.niteesh.job_scheduler.domain.repository;

import com.niteesh.job_scheduler.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Repository methods
    Optional<Task> findByName(String name);
}
