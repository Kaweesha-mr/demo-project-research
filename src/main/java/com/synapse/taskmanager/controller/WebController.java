package com.synapse.taskmanager.controller;

import com.synapse.taskmanager.dto.CreateTaskRequest;
import com.synapse.taskmanager.model.Priority;
import com.synapse.taskmanager.model.TaskStatus;
import com.synapse.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final TaskService taskService;

    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String search) {

        if (search != null && !search.isBlank()) {
            model.addAttribute("tasks", taskService.searchTasks(search));
            model.addAttribute("search", search);
        } else if (status != null && !status.isBlank()) {
            model.addAttribute("tasks", taskService.getTasksByStatus(TaskStatus.valueOf(status)));
            model.addAttribute("filterStatus", status);
        } else {
            model.addAttribute("tasks", taskService.getAllTasks());
        }

        model.addAttribute("stats", taskService.getTaskStatistics());
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("newTask", new CreateTaskRequest());
        return "index";
    }

    @PostMapping("/tasks")
    public String createTask(@Valid @ModelAttribute("newTask") CreateTaskRequest request,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("tasks", taskService.getAllTasks());
            model.addAttribute("stats", taskService.getTaskStatistics());
            model.addAttribute("statuses", TaskStatus.values());
            model.addAttribute("priorities", Priority.values());
            return "index";
        }
        taskService.createTask(request);
        redirectAttributes.addFlashAttribute("successMessage", "Task created successfully!");
        return "redirect:/";
    }

    @GetMapping("/tasks/{id}")
    public String viewTask(@PathVariable Long id, Model model) {
        model.addAttribute("task", taskService.getTaskById(id));
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", Priority.values());
        return "task-detail";
    }

    @PostMapping("/tasks/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam TaskStatus status,
                               RedirectAttributes redirectAttributes) {
        taskService.changeStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Status updated!");
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        taskService.deleteTask(id);
        redirectAttributes.addFlashAttribute("successMessage", "Task deleted.");
        return "redirect:/";
    }
}
