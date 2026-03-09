package com.synapse.taskmanager.service;

import com.synapse.taskmanager.dto.CreateTaskRequest;
import com.synapse.taskmanager.dto.TaskDTO;
import com.synapse.taskmanager.dto.UpdateTaskRequest;
import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.Task;
import com.synapse.taskmanager.model.TaskStatus;
import com.synapse.taskmanager.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public List<TaskDTO> getAllTasks() {
        log.debug("Fetching all tasks");
        return taskRepository.findAll()
                .stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public TaskDTO getTaskById(Long id) {
        log.debug("Fetching task with id={}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
        return TaskDTO.fromEntity(task);
    }

    @Transactional
    public TaskDTO createTask(CreateTaskRequest request) {
        log.info("Creating task: title={}", request.getTitle());
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .assignedTo(request.getAssignedTo())
                .status(TaskStatus.TODO)
                .build();
        Task saved = taskRepository.save(task);
        log.info("Task created with id={}", saved.getId());
        return TaskDTO.fromEntity(saved);
    }

    @Transactional
    public TaskDTO updateTask(Long id, UpdateTaskRequest request) {
        log.info("Updating task id={}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getAssignedTo() != null) {
            task.setAssignedTo(request.getAssignedTo());
        }

        Task updated = taskRepository.save(task);
        return TaskDTO.fromEntity(updated);
    }

    @Transactional
    public TaskDTO changeStatus(Long id, TaskStatus newStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
        task.setStatus(newStatus);
        return TaskDTO.fromEntity(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long id) {
        log.warn("Deleting task id={}", id);
        if (!taskRepository.existsById(id)) {
            throw new EntityNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    public List<TaskDTO> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status)
                .stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByPriority(Priority priority) {
        return taskRepository.findByPriority(priority)
                .stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> searchTasks(String keyword) {
        return taskRepository.searchByKeyword(keyword)
                .stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getTaskStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", taskRepository.count());
        for (TaskStatus status : TaskStatus.values()) {
            stats.put(status.name().toLowerCase(), taskRepository.countByStatus(status));
        }
        return stats;
    }
}
