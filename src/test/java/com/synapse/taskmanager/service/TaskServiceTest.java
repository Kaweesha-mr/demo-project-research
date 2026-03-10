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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

    @Test
    @DisplayName("searchTasks returns empty list when no match found")
    void searchTasks_returnsEmptyWhenNoMatch() {
        when(taskRepository.searchByKeyword("xyznotfound")).thenReturn(List.of());

        List<TaskDTO> results = taskService.searchTasks("xyznotfound");

        assertThat(results).isEmpty();
        verify(taskRepository).searchByKeyword("xyznotfound");
    }

    // ── getTasksByStatus ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTasksByStatus")
    class GetTasksByStatus {

        @ParameterizedTest(name = "returns tasks for status {0}")
        @EnumSource(TaskStatus.class)
        @DisplayName("returns tasks for every valid status")
        void returnsTasksForEveryStatus(TaskStatus status) {
            Task task = Task.builder().id(1L).title("T").status(status).priority(Priority.LOW).build();
            when(taskRepository.findByStatus(status)).thenReturn(List.of(task));

            List<TaskDTO> result = taskService.getTasksByStatus(status);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("returns empty list when no tasks match status")
        void returnsEmptyList() {
            when(taskRepository.findByStatus(TaskStatus.CANCELLED)).thenReturn(List.of());

            assertThat(taskService.getTasksByStatus(TaskStatus.CANCELLED)).isEmpty();
        }
    }

    // ── getTasksByPriority ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTasksByPriority")
    class GetTasksByPriority {

        @ParameterizedTest(name = "returns tasks for priority {0}")
        @EnumSource(Priority.class)
        @DisplayName("returns tasks for every valid priority")
        void returnsTasksForEveryPriority(Priority priority) {
            Task task = Task.builder().id(1L).title("T").status(TaskStatus.TODO).priority(priority).build();
            when(taskRepository.findByPriority(priority)).thenReturn(List.of(task));

            List<TaskDTO> result = taskService.getTasksByPriority(priority);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPriority()).isEqualTo(priority);
        }

        @Test
        @DisplayName("returns empty list when no tasks match priority")
        void returnsEmptyList() {
            when(taskRepository.findByPriority(Priority.CRITICAL)).thenReturn(List.of());

            assertThat(taskService.getTasksByPriority(Priority.CRITICAL)).isEmpty();
        }
    }

    // ── updateTask – field-level coverage ─────────────────────────────────────

    @Nested
    @DisplayName("updateTask field coverage")
    class UpdateTaskFieldCoverage {

        @Test
        @DisplayName("updateTask updates title when provided")
        void updatesTitle() {
            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("Renamed task");

            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

            TaskDTO result = taskService.updateTask(1L, req);

            assertThat(result.getTitle()).isEqualTo("Renamed task");
        }

        @Test
        @DisplayName("updateTask does not overwrite title when new title is blank")
        void doesNotOverwriteBlankTitle() {
            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("   ");   // blank – should be ignored

            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

            TaskDTO result = taskService.updateTask(1L, req);

            assertThat(result.getTitle()).isEqualTo("Fix pipeline bug");
        }

        @Test
        @DisplayName("updateTask updates assignedTo when provided")
        void updatesAssignedTo() {
            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setAssignedTo("newowner@synapse.io");

            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

            TaskDTO result = taskService.updateTask(1L, req);

            assertThat(result.getAssignedTo()).isEqualTo("newowner@synapse.io");
        }

        @Test
        @DisplayName("updateTask updates description when provided")
        void updatesDescription() {
            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setDescription("Updated description");

            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

            TaskDTO result = taskService.updateTask(1L, req);

            assertThat(result.getDescription()).isEqualTo("Updated description");
        }
    }

    // ── changeStatus – all transitions ────────────────────────────────────────

    @Nested
    @DisplayName("changeStatus transitions")
    class ChangeStatusTransitions {

        @ParameterizedTest(name = "transitions to {0}")
        @EnumSource(TaskStatus.class)
        @DisplayName("can transition to any status")
        void canTransitionToAnyStatus(TaskStatus target) {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

            TaskDTO result = taskService.changeStatus(1L, target);

            assertThat(result.getStatus()).isEqualTo(target);
        }

        @Test
        @DisplayName("changeStatus throws EntityNotFoundException when task missing")
        void throwsWhenTaskMissing() {
            when(taskRepository.findById(88L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.changeStatus(88L, TaskStatus.DONE))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("88");
        }
    }

    // ── getTaskStatistics – zero counts ──────────────────────────────────────

    @Test
    @DisplayName("getTaskStatistics contains all status keys")
    void getTaskStatistics_containsAllKeys() {
        when(taskRepository.count()).thenReturn(0L);
        for (TaskStatus s : TaskStatus.values()) {
            when(taskRepository.countByStatus(s)).thenReturn(0L);
        }

        Map<String, Long> stats = taskService.getTaskStatistics();

        assertThat(stats).containsKey("total");
        for (TaskStatus s : TaskStatus.values()) {
            assertThat(stats).containsKey(s.name().toLowerCase());
        }
    }

    // ── createTask – status is always TODO ────────────────────────────────────

    @Test
    @DisplayName("createTask always sets initial status to TODO")
    void createTask_statusIsAlwaysTodo() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Status check task");
        req.setPriority(Priority.LOW);

        when(taskRepository.save(argThat(t -> t.getStatus() == TaskStatus.TODO)))
                .thenReturn(sampleTask);

        taskService.createTask(req);

        verify(taskRepository).save(argThat(t -> t.getStatus() == TaskStatus.TODO));
    }

    // ── getAllTasks – DTO field mapping ───────────────────────────────────────

    @Test
    @DisplayName("getAllTasks maps all DTO fields from entity")
    void getAllTasks_mapsDtoFields() {
        LocalDateTime fixedTime = LocalDateTime.of(2026, 3, 10, 9, 0);
        Task detailed = Task.builder()
                .id(7L)
                .title("Detailed task")
                .description("Some desc")
                .status(TaskStatus.IN_PROGRESS)
                .priority(Priority.CRITICAL)
                .assignedTo("ops@synapse.io")
                .createdAt(fixedTime)
                .updatedAt(fixedTime)
                .build();
        when(taskRepository.findAll()).thenReturn(List.of(detailed));

        List<TaskDTO> result = taskService.getAllTasks();

        TaskDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getDescription()).isEqualTo("Some desc");
        assertThat(dto.getAssignedTo()).isEqualTo("ops@synapse.io");
        assertThat(dto.getCreatedAt()).isEqualTo(fixedTime);
        assertThat(dto.getUpdatedAt()).isEqualTo(fixedTime);
    }
}
