package com.synapse.taskmanager.dto;

import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be 1–255 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Priority priority = Priority.MEDIUM;

    private String assignedTo;
}
