package com.synapse.taskmanager.service;

import com.synapse.taskmanager.dto.CreateTaskRequest;
import com.synapse.taskmanager.dto.TaskDTO;
import com.synapse.taskmanager.dto.UpdateTaskRequest;
import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.Task;
import com.synapse.taskmanager.model.TaskStatus;
import com.synapse.taskmanager.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .id(1L)
                .title("Fix pipeline bug")
                .description("Pipeline restarts after Docker build")
                .status(TaskStatus.TODO)
                .priority(Priority.HIGH)
                .assignedTo("dev@synapse.io")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── getAllTasks ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllTasks returns list of TaskDTOs")
    void getAllTasks_returnsList() {
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        List<TaskDTO> result = taskService.getAllTasks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("Fix pipeline bug");
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllTasks returns empty list when no tasks")
    void getAllTasks_returnsEmpty() {
        when(taskRepository.findAll()).thenReturn(List.of());
        assertThat(taskService.getAllTasks()).isEmpty();
    }

    // ── getTaskById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTaskById returns correct task")
    void getTaskById_returnsTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        TaskDTO dto = taskService.getTaskById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Fix pipeline bug");
        assertThat(dto.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(dto.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("getTaskById throws EntityNotFoundException for missing task")
    void getTaskById_throwsNotFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── createTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTask persists and returns new task")
    void createTask_persists() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("New Feature");
        req.setDescription("Add chaos engineering support");
        req.setPriority(Priority.MEDIUM);
        req.setAssignedTo("alice@synapse.io");

        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        TaskDTO result = taskService.createTask(req);

        assertThat(result).isNotNull();
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask uses MEDIUM priority when none specified")
    void createTask_defaultPriority() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Test default priority");
        req.setPriority(null);

        when(taskRepository.save(argThat(t -> t.getPriority() == Priority.MEDIUM)))
                .thenReturn(sampleTask);

        taskService.createTask(req);

        verify(taskRepository).save(argThat(t -> t.getPriority() == Priority.MEDIUM));
    }

    // ── updateTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTask applies partial updates correctly")
    void updateTask_partialUpdate() {
        UpdateTaskRequest req = new UpdateTaskRequest();
        req.setStatus(TaskStatus.IN_PROGRESS);
        req.setPriority(Priority.CRITICAL);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        taskService.updateTask(1L, req);

        verify(taskRepository).save(argThat(t ->
                t.getStatus() == TaskStatus.IN_PROGRESS &&
                t.getPriority() == Priority.CRITICAL));
    }

    @Test
    @DisplayName("updateTask throws when task not found")
    void updateTask_notFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.updateTask(99L, new UpdateTaskRequest()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── changeStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("changeStatus updates status correctly")
    void changeStatus_updatesStatus() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        taskService.changeStatus(1L, TaskStatus.DONE);

        verify(taskRepository).save(argThat(t -> t.getStatus() == TaskStatus.DONE));
    }

    // ── deleteTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteTask successfully deletes existing task")
    void deleteTask_success() {
        when(taskRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(1L);

        assertThatCode(() -> taskService.deleteTask(1L)).doesNotThrowAnyException();
        verify(taskRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTask throws when task not found")
    void deleteTask_notFound() {
        when(taskRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> taskService.deleteTask(999L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(taskRepository, never()).deleteById(any());
    }

    // ── getTaskStatistics ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getTaskStatistics returns correct counts per status")
    void getTaskStatistics_returnsCounts() {
        when(taskRepository.count()).thenReturn(5L);
        when(taskRepository.countByStatus(TaskStatus.TODO)).thenReturn(2L);
        when(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).thenReturn(1L);
        when(taskRepository.countByStatus(TaskStatus.DONE)).thenReturn(2L);
        when(taskRepository.countByStatus(TaskStatus.CANCELLED)).thenReturn(0L);

        Map<String, Long> stats = taskService.getTaskStatistics();

        assertThat(stats.get("total")).isEqualTo(5L);
        assertThat(stats.get("todo")).isEqualTo(2L);
        assertThat(stats.get("in_progress")).isEqualTo(1L);
        assertThat(stats.get("done")).isEqualTo(2L);
    }

    // ── searchTasks ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchTasks delegates to repository and maps results")
    void searchTasks_delegatesToRepository() {
        when(taskRepository.searchByKeyword("synapse")).thenReturn(List.of(sampleTask));

        List<TaskDTO> results = taskService.searchTasks("synapse");

        assertThat(results).hasSize(1);
        verify(taskRepository).searchByKeyword("synapse");
    }
}
