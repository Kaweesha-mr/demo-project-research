package com.synapse.taskmanager.dto;

import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.TaskStatus;
import lombok.Data;

@Data
public class UpdateTaskRequest {
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private String assignedTo;
}
