# Synapse Task Manager

A production-ready **Spring Boot 3 + PostgreSQL** sample project designed as a demo target for the **Synapse-CI** pipeline.

## What is this?

This project exercises every major Synapse-CI pipeline feature:

| Synapse-CI Feature | How this project covers it |
|---|---|
| Language detection | Java 17, Maven |
| Framework detection | Spring Boot 3.2 |
| Database detection | PostgreSQL 15 via JPA/Hibernate |
| Port detection | `SERVER_PORT=8080`, `EXPOSE 8080` |
| Health check | `/actuator/health` (Spring Boot Actuator) |
| Docker build | Multi-stage Dockerfile (JDK build → JRE runtime) |
| Docker Compose | `db` (PostgreSQL) + `app` with `depends_on` + healthcheck |
| Test discovery | 32 JUnit 5 test cases across 3 test classes |
| UI | Thymeleaf server-side rendered pages |

## Project Structure

```
src/
├── main/java/com/synapse/taskmanager/
│   ├── TaskManagerApplication.java          # Spring Boot main
│   ├── controller/
│   │   ├── TaskApiController.java           # REST API  (/api/tasks)
│   │   └── WebController.java              # Thymeleaf UI  (/)
│   ├── dto/
│   │   ├── CreateTaskRequest.java
│   │   ├── UpdateTaskRequest.java
│   │   └── TaskDTO.java
│   ├── exception/GlobalExceptionHandler.java
│   ├── model/
│   │   ├── Task.java                        # JPA entity
│   │   ├── TaskStatus.java                  # TODO / IN_PROGRESS / DONE / CANCELLED
│   │   └── Priority.java                    # LOW / MEDIUM / HIGH / CRITICAL
│   ├── repository/TaskRepository.java       # Spring Data JPA
│   └── service/TaskService.java            # Business logic
├── main/resources/
│   ├── application.yml                      # PostgreSQL + Actuator config
│   └── templates/
│       ├── index.html                       # Dashboard UI
│       └── task-detail.html                 # Single task view
└── test/
    ├── java/.../service/TaskServiceTest.java        # 14 unit tests (Mockito)
    ├── java/.../controller/TaskApiControllerTest.java # 12 MockMvc tests
    └── java/.../integration/TaskIntegrationTest.java  # 6 full integration tests
```

## Quick Start

### 1. Run with Docker Compose (recommended)

```bash
docker compose up --build
```

- **Web UI:** http://localhost:8080
- **REST API:** http://localhost:8080/api/tasks
- **Health:** http://localhost:8080/actuator/health

### 2. Run locally (needs PostgreSQL running)

```bash
export DB_HOST=localhost
export DB_NAME=taskdb
export DB_USERNAME=taskuser
export DB_PASSWORD=taskpassword

./mvnw spring-boot:run
```

### 3. Run tests

```bash
./mvnw test
```

Tests use H2 in-memory DB automatically — no PostgreSQL needed.

## REST API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | List all tasks |
| GET | `/api/tasks?status=TODO` | Filter by status |
| GET | `/api/tasks?search=keyword` | Search by keyword |
| GET | `/api/tasks/{id}` | Get task by ID |
| POST | `/api/tasks` | Create new task |
| PUT | `/api/tasks/{id}` | Update task |
| PATCH | `/api/tasks/{id}/status?status=DONE` | Change status |
| DELETE | `/api/tasks/{id}` | Delete task |
| GET | `/api/tasks/stats` | Get statistics |
| GET | `/actuator/health` | Health check |

### Create Task (Example)

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Deploy to production","priority":"HIGH","assignedTo":"devops@team.io"}'
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `taskdb` | Database name |
| `DB_USERNAME` | `taskuser` | DB username |
| `DB_PASSWORD` | `taskpassword` | DB password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Schema strategy |
| `SERVER_PORT` | `8080` | App port |

## Tech Stack

- **Java 17** + **Spring Boot 3.2**
- **Spring Data JPA** with **PostgreSQL 15**
- **Thymeleaf 3** (server-side UI)
- **Spring Boot Actuator** (health checks)
- **Lombok** (boilerplate reduction)
- **JUnit 5** + **Mockito** + **MockMvc** (testing)
- **H2** (in-memory DB for tests)
- **Docker** multi-stage build with **Alpine Linux** base

> Generated for Synapse-CI pipeline testing
