package com.synapse.taskmanager.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.taskmanager.controller.TaskApiController;
import com.synapse.taskmanager.dto.CreateTaskRequest;
import com.synapse.taskmanager.service.TaskService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskApiController.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    // ── EntityNotFoundException → 404 ─────────────────────────────────────────

    @Nested
    @DisplayName("EntityNotFoundException handling")
    class EntityNotFoundHandling {

        @Test
        @DisplayName("GET unknown task returns 404 with error body")
        void getUnknownTask_returns404WithBody() throws Exception {
            when(taskService.getTaskById(42L))
                    .thenThrow(new EntityNotFoundException("Task not found with id: 42"));

            mockMvc.perform(get("/api/tasks/42"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Task not found with id: 42"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("DELETE unknown task returns 404 with error body")
        void deleteUnknownTask_returns404WithBody() throws Exception {
            doThrow(new EntityNotFoundException("Task not found with id: 77"))
                    .when(taskService).deleteTask(77L);

            mockMvc.perform(delete("/api/tasks/77"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Task not found with id: 77"));
        }

        @Test
        @DisplayName("PATCH status on unknown task returns 404")
        void patchStatusUnknownTask_returns404() throws Exception {
            when(taskService.changeStatus(anyLong(), any()))
                    .thenThrow(new EntityNotFoundException("Task not found with id: 55"));

            mockMvc.perform(patch("/api/tasks/55/status").param("status", "DONE"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Task not found with id: 55"));
        }
    }

    // ── MethodArgumentNotValidException → 400 ────────────────────────────────

    @Nested
    @DisplayName("Validation error handling")
    class ValidationErrorHandling {

        @Test
        @DisplayName("POST with blank title returns 400 with fieldErrors")
        void postBlankTitle_returns400WithFieldErrors() throws Exception {
            CreateTaskRequest req = new CreateTaskRequest();
            req.setTitle("");

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.title").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("POST with null title returns 400 with fieldErrors")
        void postNullTitle_returns400WithFieldErrors() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.title").exists());
        }

        @Test
        @DisplayName("POST with whitespace-only title returns 400")
        void postWhitespaceTitle_returns400() throws Exception {
            CreateTaskRequest req = new CreateTaskRequest();
            req.setTitle("   ");

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.title").exists());
        }

        @Test
        @DisplayName("POST with oversized title returns 400")
        void postOversizedTitle_returns400() throws Exception {
            CreateTaskRequest req = new CreateTaskRequest();
            req.setTitle("A".repeat(300));   // exceeds @Size(max = 255)

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.title").exists());
        }
    }

    // ── Generic Exception → 500 ───────────────────────────────────────────────

    @Nested
    @DisplayName("Generic exception handling")
    class GenericExceptionHandling {

        @Test
        @DisplayName("Unexpected service exception returns 500 with message")
        void unexpectedException_returns500() throws Exception {
            when(taskService.getAllTasks())
                    .thenThrow(new RuntimeException("Database connection refused"));

            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("Database connection refused")));
        }
    }
}
