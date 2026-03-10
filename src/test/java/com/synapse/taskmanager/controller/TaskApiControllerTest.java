package com.synapse.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.taskmanager.dto.CreateTaskRequest;
import com.synapse.taskmanager.dto.TaskDTO;
import com.synapse.taskmanager.dto.UpdateTaskRequest;
import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.TaskStatus;
import com.synapse.taskmanager.service.TaskService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskApiController.class)
@DisplayName("TaskApiController Tests")
class TaskApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private TaskDTO sampleDTO;

    @BeforeEach
    void setUp() {
        sampleDTO = TaskDTO.builder()
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

    // ── GET /api/tasks ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/tasks returns 200 with list")
    void getAllTasks_returns200() throws Exception {
        when(taskService.getAllTasks()).thenReturn(List.of(sampleDTO));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Fix pipeline bug"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/tasks?status=TODO filters by status")
    void getAllTasks_filterByStatus() throws Exception {
        when(taskService.getTasksByStatus(TaskStatus.TODO)).thenReturn(List.of(sampleDTO));

        mockMvc.perform(get("/api/tasks").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    @DisplayName("GET /api/tasks?search=pipeline searches tasks")
    void getAllTasks_search() throws Exception {
        when(taskService.searchTasks("pipeline")).thenReturn(List.of(sampleDTO));

        mockMvc.perform(get("/api/tasks").param("search", "pipeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── GET /api/tasks/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/tasks/{id} returns 200 for existing task")
    void getTask_returns200() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(sampleDTO);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fix pipeline bug"));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} returns 404 for missing task")
    void getTask_returns404() throws Exception {
        when(taskService.getTaskById(999L))
                .thenThrow(new EntityNotFoundException("Task not found with id: 999"));

        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: 999"));
    }

    // ── POST /api/tasks ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/tasks returns 201 with created task")
    void createTask_returns201() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("New chaos test");
        req.setPriority(Priority.HIGH);

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(sampleDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/tasks returns 400 when title is blank")
    void createTask_returns400ForBlankTitle() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("");  // blank

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    // ── PATCH /api/tasks/{id}/status ──────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/tasks/{id}/status updates status")
    void changeStatus_returns200() throws Exception {
        TaskDTO updated = TaskDTO.builder().id(1L).title("Fix pipeline bug")
                .status(TaskStatus.DONE).priority(Priority.HIGH)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(taskService.changeStatus(1L, TaskStatus.DONE)).thenReturn(updated);

        mockMvc.perform(patch("/api/tasks/1/status").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    // ── DELETE /api/tasks/{id} ────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/tasks/{id} returns 204")
    void deleteTask_returns204() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} returns 404 for missing task")
    void deleteTask_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Task not found with id: 999"))
                .when(taskService).deleteTask(999L);

        mockMvc.perform(delete("/api/tasks/999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/tasks/stats ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/tasks/stats returns statistics")
    void getStats_returns200() throws Exception {
        when(taskService.getTaskStatistics()).thenReturn(Map.of("total", 5L, "todo", 2L));

        mockMvc.perform(get("/api/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5));
    }

    // ── GET /api/tasks – empty list ───────────────────────────────────────────

    @Test
    @DisplayName("GET /api/tasks returns empty array when no tasks exist")
    void getAllTasks_returnsEmptyArray() throws Exception {
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/tasks?priority=HIGH ──────────────────────────────────────────

    @Test
    @DisplayName("GET /api/tasks?priority=HIGH filters by priority")
    void getAllTasks_filterByPriority() throws Exception {
        when(taskService.getTasksByPriority(Priority.HIGH)).thenReturn(List.of(sampleDTO));

        mockMvc.perform(get("/api/tasks").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priority").value("HIGH"));
    }

    @ParameterizedTest(name = "filter by status {0}")
    @EnumSource(TaskStatus.class)
    @DisplayName("GET /api/tasks filters correctly for each status value")
    void getAllTasks_filterByEachStatus(TaskStatus status) throws Exception {
        TaskDTO dto = TaskDTO.builder().id(1L).title("T").status(status)
                .priority(Priority.LOW).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(taskService.getTasksByStatus(status)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/tasks").param("status", status.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value(status.name()));
    }

    // ── GET /api/tasks – search returns empty ─────────────────────────────────

    @Test
    @DisplayName("GET /api/tasks?search=noresult returns empty list")
    void getAllTasks_searchReturnsEmpty() throws Exception {
        when(taskService.searchTasks("noresult")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks").param("search", "noresult"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/tasks/{id} – response body fields ────────────────────────────

    @Test
    @DisplayName("GET /api/tasks/{id} returns all expected fields")
    void getTask_returnsAllFields() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(sampleDTO);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fix pipeline bug"))
                .andExpect(jsonPath("$.description").value("Pipeline restarts after Docker build"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignedTo").value("dev@synapse.io"));
    }

    // ── POST /api/tasks – full field mapping ─────────────────────────────────

    @Test
    @DisplayName("POST /api/tasks sends all request fields correctly")
    void createTask_sendsAllFields() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Full field task");
        req.setDescription("Complete description");
        req.setPriority(Priority.CRITICAL);
        req.setAssignedTo("qa@synapse.io");

        when(taskService.createTask(any())).thenReturn(sampleDTO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("POST /api/tasks without Content-Type returns non-2xx")
    void createTask_missingContentType_returnsError() throws Exception {
        mockMvc.perform(post("/api/tasks").content("{}"))
                .andExpect(status().is5xxServerError());
    }

    // ── PUT /api/tasks/{id} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/tasks/{id}")
    class UpdateTask {

        @Test
        @DisplayName("returns 200 with updated task body")
        void returns200WithUpdatedTask() throws Exception {
            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("Updated title");
            req.setStatus(TaskStatus.IN_PROGRESS);

            TaskDTO updated = TaskDTO.builder().id(1L).title("Updated title")
                    .status(TaskStatus.IN_PROGRESS).priority(Priority.HIGH)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            when(taskService.updateTask(eq(1L), any(UpdateTaskRequest.class))).thenReturn(updated);

            mockMvc.perform(put("/api/tasks/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated title"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("returns 404 when task does not exist")
        void returns404ForMissingTask() throws Exception {
            when(taskService.updateTask(eq(99L), any(UpdateTaskRequest.class)))
                    .thenThrow(new EntityNotFoundException("Task not found with id: 99"));

            mockMvc.perform(put("/api/tasks/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Task not found with id: 99"));
        }
    }

    // ── PATCH /api/tasks/{id}/status – all status values ─────────────────────

    @ParameterizedTest(name = "PATCH status to {0}")
    @EnumSource(TaskStatus.class)
    @DisplayName("PATCH /api/tasks/{id}/status works for every status value")
    void changeStatus_allStatusValues(TaskStatus status) throws Exception {
        TaskDTO updated = TaskDTO.builder().id(1L).title("T").status(status)
                .priority(Priority.LOW).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(taskService.changeStatus(eq(1L), eq(status))).thenReturn(updated);

        mockMvc.perform(patch("/api/tasks/1/status").param("status", status.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status.name()));
    }

    @Test
    @DisplayName("PATCH /api/tasks/{id}/status with invalid status returns non-2xx")
    void changeStatus_invalidStatus_returnsError() throws Exception {
        mockMvc.perform(patch("/api/tasks/1/status").param("status", "INVALID_STATUS"))
                .andExpect(status().is5xxServerError());
    }

    // ── GET /api/tasks/stats – all keys present ───────────────────────────────

    @Test
    @DisplayName("GET /api/tasks/stats response contains all expected keys")
    void getStats_containsAllKeys() throws Exception {
        Map<String, Long> stats = Map.of(
                "total", 10L,
                "todo", 4L,
                "in_progress", 3L,
                "done", 2L,
                "cancelled", 1L
        );
        when(taskService.getTaskStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.todo").value(4))
                .andExpect(jsonPath("$.in_progress").value(3))
                .andExpect(jsonPath("$.done").value(2))
                .andExpect(jsonPath("$.cancelled").value(1));
    }
}
