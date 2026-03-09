package com.synapse.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.taskmanager.dto.CreateTaskRequest;
import com.synapse.taskmanager.dto.TaskDTO;
import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.TaskStatus;
import com.synapse.taskmanager.service.TaskService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
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
}
