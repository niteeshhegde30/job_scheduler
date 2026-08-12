package com.niteesh.job_scheduler.service;

import com.niteesh.job_scheduler.domain.Task;
import com.niteesh.job_scheduler.domain.repository.TaskRepository;
import com.niteesh.job_scheduler.exception.TaskNotFoundException;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    public Task getTaskByName(String taskName){
        Optional<Task> optionalTask = taskRepository.findByName(taskName);
        if (optionalTask.isEmpty()){
            throw new TaskNotFoundException(taskName);
        } else {
            return optionalTask.get();
        }
    }
}
