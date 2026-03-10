package com.synapse.taskmanager.repository;

import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.Task;
import com.synapse.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TaskRepository Tests")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();

        taskRepository.save(Task.builder().title("Alpha task").description("Fix the bug").status(TaskStatus.TODO).priority(Priority.HIGH).assignedTo("alice@synapse.io").build());
        taskRepository.save(Task.builder().title("Beta task").description("Deploy service").status(TaskStatus.IN_PROGRESS).priority(Priority.MEDIUM).assignedTo("bob@synapse.io").build());
        taskRepository.save(Task.builder().title("Gamma task").description("Write docs").status(TaskStatus.DONE).priority(Priority.LOW).assignedTo("alice@synapse.io").build());
        taskRepository.save(Task.builder().title("Delta task").description("Chaos testing").status(TaskStatus.TODO).priority(Priority.CRITICAL).assignedTo("carol@synapse.io").build());
        taskRepository.save(Task.builder().title("Epsilon task").description("Pipeline monitor").status(TaskStatus.CANCELLED).priority(Priority.LOW).assignedTo("bob@synapse.io").build());
    }

    // ── Basic CRUD ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Basic CRUD")
    class BasicCrud {

        @Test
        @DisplayName("save persists a task and assigns an ID")
        void save_assignsId() {
            Task task = Task.builder().title("New task").status(TaskStatus.TODO).priority(Priority.MEDIUM).build();
            Task saved = taskRepository.save(task);
            assertThat(saved.getId()).isNotNull().isPositive();
        }

        @Test
        @DisplayName("findById returns the correct task")
        void findById_returnsTask() {
            Task saved = taskRepository.save(Task.builder().title("Find me").status(TaskStatus.TODO).priority(Priority.LOW).build());
            Optional<Task> found = taskRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Find me");
        }

        @Test
        @DisplayName("findById returns empty for missing id")
        void findById_returnsEmpty() {
            assertThat(taskRepository.findById(999999L)).isEmpty();
        }

        @Test
        @DisplayName("deleteById removes the task")
        void deleteById_removesTask() {
            Task saved = taskRepository.save(Task.builder().title("Delete me").status(TaskStatus.TODO).priority(Priority.LOW).build());
            Long id = saved.getId();
            taskRepository.deleteById(id);
            assertThat(taskRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("findAll returns all persisted tasks")
        void findAll_returnsAll() {
            List<Task> all = taskRepository.findAll();
            assertThat(all).hasSize(5);
        }

        @Test
        @DisplayName("count returns correct number of tasks")
        void count_correct() {
            assertThat(taskRepository.count()).isEqualTo(5L);
        }

        @Test
        @DisplayName("existsById returns true for existing task")
        void existsById_true() {
            Task saved = taskRepository.save(Task.builder().title("Exists").status(TaskStatus.TODO).priority(Priority.LOW).build());
            assertThat(taskRepository.existsById(saved.getId())).isTrue();
        }

        @Test
        @DisplayName("existsById returns false for missing task")
        void existsById_false() {
            assertThat(taskRepository.existsById(999999L)).isFalse();
        }
    }

    // ── findByStatus ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("returns only TODO tasks")
        void returnsOnlyTodo() {
            List<Task> todo = taskRepository.findByStatus(TaskStatus.TODO);
            assertThat(todo).hasSize(2).allMatch(t -> t.getStatus() == TaskStatus.TODO);
        }

        @Test
        @DisplayName("returns only IN_PROGRESS tasks")
        void returnsOnlyInProgress() {
            List<Task> inProgress = taskRepository.findByStatus(TaskStatus.IN_PROGRESS);
            assertThat(inProgress).hasSize(1).allMatch(t -> t.getStatus() == TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("returns only DONE tasks")
        void returnsOnlyDone() {
            List<Task> done = taskRepository.findByStatus(TaskStatus.DONE);
            assertThat(done).hasSize(1).allMatch(t -> t.getStatus() == TaskStatus.DONE);
        }

        @Test
        @DisplayName("returns only CANCELLED tasks")
        void returnsOnlyCancelled() {
            List<Task> cancelled = taskRepository.findByStatus(TaskStatus.CANCELLED);
            assertThat(cancelled).hasSize(1).allMatch(t -> t.getStatus() == TaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("returns empty list when no tasks match status")
        void returnsEmptyWhenNoMatch() {
            taskRepository.deleteAll();
            assertThat(taskRepository.findByStatus(TaskStatus.TODO)).isEmpty();
        }
    }

    // ── findByPriority ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByPriority")
    class FindByPriority {

        @Test
        @DisplayName("returns only HIGH priority tasks")
        void returnsHighPriority() {
            List<Task> high = taskRepository.findByPriority(Priority.HIGH);
            assertThat(high).hasSize(1).allMatch(t -> t.getPriority() == Priority.HIGH);
        }

        @Test
        @DisplayName("returns only CRITICAL priority tasks")
        void returnsCriticalPriority() {
            List<Task> critical = taskRepository.findByPriority(Priority.CRITICAL);
            assertThat(critical).hasSize(1).allMatch(t -> t.getPriority() == Priority.CRITICAL);
        }

        @Test
        @DisplayName("returns empty list when no tasks match priority")
        void returnsEmptyWhenNoMatch() {
            taskRepository.deleteAll();
            assertThat(taskRepository.findByPriority(Priority.HIGH)).isEmpty();
        }
    }

    // ── findByAssignedTo ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByAssignedTo")
    class FindByAssignedTo {

        @Test
        @DisplayName("returns all tasks assigned to alice")
        void returnsAliceTasks() {
            List<Task> tasks = taskRepository.findByAssignedTo("alice@synapse.io");
            assertThat(tasks).hasSize(2).allMatch(t -> "alice@synapse.io".equals(t.getAssignedTo()));
        }

        @Test
        @DisplayName("returns all tasks assigned to bob")
        void returnsBobTasks() {
            List<Task> tasks = taskRepository.findByAssignedTo("bob@synapse.io");
            assertThat(tasks).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list for unknown assignee")
        void returnsEmptyForUnknown() {
            assertThat(taskRepository.findByAssignedTo("nobody@synapse.io")).isEmpty();
        }
    }

    // ── searchByKeyword ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchByKeyword")
    class SearchByKeyword {

        @Test
        @DisplayName("matches title case-insensitively")
        void matchesTitleCaseInsensitive() {
            List<Task> results = taskRepository.searchByKeyword("ALPHA");
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Alpha task");
        }

        @Test
        @DisplayName("matches description case-insensitively")
        void matchesDescriptionCaseInsensitive() {
            List<Task> results = taskRepository.searchByKeyword("pipeline");
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Epsilon task");
        }

        @Test
        @DisplayName("returns multiple matches when keyword is broad")
        void returnsMultipleMatches() {
            List<Task> results = taskRepository.searchByKeyword("task");
            assertThat(results).hasSize(5);
        }

        @Test
        @DisplayName("returns empty when keyword has no matches")
        void returnsEmptyForNoMatch() {
            assertThat(taskRepository.searchByKeyword("xyznonexistent")).isEmpty();
        }

        @Test
        @DisplayName("partial keyword matches are returned")
        void partialKeywordMatches() {
            List<Task> results = taskRepository.searchByKeyword("bug");
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDescription()).contains("bug");
        }
    }

    // ── countByStatus ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("countByStatus")
    class CountByStatus {

        @Test
        @DisplayName("counts TODO tasks correctly")
        void countsToDoCorrectly() {
            assertThat(taskRepository.countByStatus(TaskStatus.TODO)).isEqualTo(2L);
        }

        @Test
        @DisplayName("counts IN_PROGRESS tasks correctly")
        void countsInProgressCorrectly() {
            assertThat(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).isEqualTo(1L);
        }

        @Test
        @DisplayName("counts DONE tasks correctly")
        void countsDoneCorrectly() {
            assertThat(taskRepository.countByStatus(TaskStatus.DONE)).isEqualTo(1L);
        }

        @Test
        @DisplayName("counts CANCELLED tasks correctly")
        void countsCancelledCorrectly() {
            assertThat(taskRepository.countByStatus(TaskStatus.CANCELLED)).isEqualTo(1L);
        }

        @Test
        @DisplayName("returns 0 when no tasks match the status")
        void returnsZeroWhenNoMatch() {
            taskRepository.deleteAll();
            assertThat(taskRepository.countByStatus(TaskStatus.TODO)).isEqualTo(0L);
        }
    }

    // ── Timestamps ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Timestamps")
    class Timestamps {

        @Test
        @DisplayName("createdAt is set automatically on save")
        void createdAtIsSetOnSave() {
            Task saved = taskRepository.save(Task.builder().title("Timestamp test").status(TaskStatus.TODO).priority(Priority.LOW).build());
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("updatedAt is set automatically on save")
        void updatedAtIsSetOnSave() {
            Task saved = taskRepository.save(Task.builder().title("Timestamp test 2").status(TaskStatus.TODO).priority(Priority.LOW).build());
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }
}
