package com.synapse.taskmanager.dto;

import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.Task;
import com.synapse.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TaskDTO Tests")
class TaskDTOTest {

    // ── fromEntity mapping ────────────────────────────────────────────────────
    @Nested
    @DisplayName("fromEntity")
    class FromEntity {

        @Test
        @DisplayName("maps all fields from Task entity correctly")
        void mapsAllFields() {
            LocalDateTime now = LocalDateTime.now();
            Task task = Task.builder()
                    .id(10L)
                    .title("My task")
                    .description("Some description")
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(Priority.HIGH)
                    .assignedTo("dev@synapse.io")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            TaskDTO dto = TaskDTO.fromEntity(task);

            assertThat(dto.getId()).isEqualTo(10L);
            assertThat(dto.getTitle()).isEqualTo("My task");
            assertThat(dto.getDescription()).isEqualTo("Some description");
            assertThat(dto.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(dto.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(dto.getAssignedTo()).isEqualTo("dev@synapse.io");
            assertThat(dto.getCreatedAt()).isEqualTo(now);
            assertThat(dto.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("maps null description without error")
        void mapsNullDescription() {
            Task task = Task.builder()
                    .id(1L)
                    .title("No-desc task")
                    .status(TaskStatus.TODO)
                    .priority(Priority.LOW)
                    .build();

            TaskDTO dto = TaskDTO.fromEntity(task);

            assertThat(dto.getDescription()).isNull();
            assertThat(dto.getAssignedTo()).isNull();
        }

        @Test
        @DisplayName("maps null assignedTo without error")
        void mapsNullAssignedTo() {
            Task task = Task.builder()
                    .id(2L)
                    .title("Unassigned task")
                    .status(TaskStatus.TODO)
                    .priority(Priority.MEDIUM)
                    .assignedTo(null)
                    .build();

            TaskDTO dto = TaskDTO.fromEntity(task);
            assertThat(dto.getAssignedTo()).isNull();
        }

        @Test
        @DisplayName("maps all TaskStatus values correctly")
        void mapsAllStatuses() {
            for (TaskStatus status : TaskStatus.values()) {
                Task task = Task.builder().id(1L).title("t").status(status).priority(Priority.LOW).build();
                assertThat(TaskDTO.fromEntity(task).getStatus()).isEqualTo(status);
            }
        }

        @Test
        @DisplayName("maps all Priority values correctly")
        void mapsAllPriorities() {
            for (Priority priority : Priority.values()) {
                Task task = Task.builder().id(1L).title("t").status(TaskStatus.TODO).priority(priority).build();
                assertThat(TaskDTO.fromEntity(task).getPriority()).isEqualTo(priority);
            }
        }
    }

    // ── Builder ───────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builder creates DTO with correct values")
        void builderCreatesDto() {
            LocalDateTime now = LocalDateTime.now();
            TaskDTO dto = TaskDTO.builder()
                    .id(5L)
                    .title("Builder task")
                    .description("desc")
                    .status(TaskStatus.DONE)
                    .priority(Priority.CRITICAL)
                    .assignedTo("ops@synapse.io")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            assertThat(dto.getId()).isEqualTo(5L);
            assertThat(dto.getTitle()).isEqualTo("Builder task");
            assertThat(dto.getDescription()).isEqualTo("desc");
            assertThat(dto.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(dto.getPriority()).isEqualTo(Priority.CRITICAL);
            assertThat(dto.getAssignedTo()).isEqualTo("ops@synapse.io");
        }

        @Test
        @DisplayName("two DTOs with same field values are equal")
        void equalityHoldsForSameValues() {
            LocalDateTime now = LocalDateTime.of(2026, 3, 10, 12, 0);
            TaskDTO a = TaskDTO.builder().id(1L).title("T").status(TaskStatus.TODO).priority(Priority.LOW).createdAt(now).updatedAt(now).build();
            TaskDTO b = TaskDTO.builder().id(1L).title("T").status(TaskStatus.TODO).priority(Priority.LOW).createdAt(now).updatedAt(now).build();
            assertThat(a).isEqualTo(b);
        }
    }
}
