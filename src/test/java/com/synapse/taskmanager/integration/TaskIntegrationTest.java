package com.synapse.taskmanager.integration;

import com.synapse.taskmanager.dto.CreateTaskRequest;
import com.synapse.taskmanager.dto.TaskDTO;
import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.TaskStatus;
import com.synapse.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Task Manager Integration Tests")
class TaskIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @AfterEach
    void cleanup() {
        taskRepository.deleteAll();
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/api/tasks";
    }

    // ── Full CRUD flow ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full CRUD lifecycle: create → get → update status → delete")
    void fullCrudLifecycle() {
        // 1. Create
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Integration test task");
        req.setDescription("Tests the complete REST lifecycle");
        req.setPriority(Priority.HIGH);
        req.setAssignedTo("tester@synapse.io");

        ResponseEntity<TaskDTO> createResp = restTemplate.postForEntity(baseUrl(), req, TaskDTO.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getBody()).isNotNull();
        Long taskId = createResp.getBody().getId();
        assertThat(taskId).isPositive();
        assertThat(createResp.getBody().getTitle()).isEqualTo("Integration test task");
        assertThat(createResp.getBody().getStatus()).isEqualTo(TaskStatus.TODO);

        // 2. Get by ID
        ResponseEntity<TaskDTO> getResp = restTemplate.getForEntity(baseUrl() + "/" + taskId, TaskDTO.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody().getId()).isEqualTo(taskId);

        // 3. Change status → IN_PROGRESS
        ResponseEntity<TaskDTO> patchResp = restTemplate.exchange(
                baseUrl() + "/" + taskId + "/status?status=IN_PROGRESS",
                HttpMethod.PATCH, null, TaskDTO.class);
        assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResp.getBody().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        // 4. Change status → DONE
        restTemplate.exchange(baseUrl() + "/" + taskId + "/status?status=DONE",
                HttpMethod.PATCH, null, TaskDTO.class);

        // 5. Get all and check
        ResponseEntity<List<TaskDTO>> allResp = restTemplate.exchange(
                baseUrl(), HttpMethod.GET, null,
                new ParameterizedTypeReference<List<TaskDTO>>() {});
        assertThat(allResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allResp.getBody()).anyMatch(t -> t.getId().equals(taskId));

        // 6. Delete
        restTemplate.delete(baseUrl() + "/" + taskId);

        ResponseEntity<TaskDTO> notFound = restTemplate.getForEntity(
                baseUrl() + "/" + taskId, TaskDTO.class);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Filter by status ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter tasks by status returns only matching tasks")
    void filterByStatus() {
        // Create two tasks with different statuses
        CreateTaskRequest r1 = new CreateTaskRequest();
        r1.setTitle("Todo task"); r1.setPriority(Priority.LOW);
        restTemplate.postForEntity(baseUrl(), r1, TaskDTO.class);

        CreateTaskRequest r2 = new CreateTaskRequest();
        r2.setTitle("Another task"); r2.setPriority(Priority.MEDIUM);
        ResponseEntity<TaskDTO> r2Resp = restTemplate.postForEntity(baseUrl(), r2, TaskDTO.class);
        Long r2Id = r2Resp.getBody().getId();

        // Move r2 to IN_PROGRESS
        restTemplate.exchange(baseUrl() + "/" + r2Id + "/status?status=IN_PROGRESS",
                HttpMethod.PATCH, null, TaskDTO.class);

        // Filter
        ResponseEntity<List<TaskDTO>> resp = restTemplate.exchange(
                baseUrl() + "?status=IN_PROGRESS", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<TaskDTO>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).allMatch(t -> t.getStatus() == TaskStatus.IN_PROGRESS);
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Statistics endpoint returns correct counts")
    void statistics_correctCounts() {
        CreateTaskRequest r = new CreateTaskRequest();
        r.setTitle("Stat task"); r.setPriority(Priority.MEDIUM);
        restTemplate.postForEntity(baseUrl(), r, TaskDTO.class);

        ResponseEntity<Map<String, Long>> resp = restTemplate.exchange(
                baseUrl() + "/stats", HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Long>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("total")).isGreaterThanOrEqualTo(1L);
        assertThat(resp.getBody()).containsKey("todo");
        assertThat(resp.getBody()).containsKey("done");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Create task with blank title returns 400")
    void createTask_blankTitle_returns400() {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateTaskRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<Map> resp = restTemplate.exchange(baseUrl(), HttpMethod.POST, entity, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Search by keyword returns matching tasks")
    void searchByKeyword() {
        CreateTaskRequest r = new CreateTaskRequest();
        r.setTitle("Synapse pipeline monitor"); r.setPriority(Priority.MEDIUM);
        restTemplate.postForEntity(baseUrl(), r, TaskDTO.class);

        ResponseEntity<List<TaskDTO>> resp = restTemplate.exchange(
                baseUrl() + "?search=pipeline", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<TaskDTO>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).anyMatch(t -> t.getTitle().contains("pipeline"));
    }

    // ── Health check ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Actuator health endpoint returns UP")
    void actuatorHealth_returnsUp() {
        ResponseEntity<Map> resp = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("status")).isEqualTo("UP");
    }
}
