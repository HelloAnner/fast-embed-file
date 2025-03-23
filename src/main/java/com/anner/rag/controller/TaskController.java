package com.anner.rag.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anner.rag.model.ApiResponse;
import com.anner.rag.model.Task;
import com.anner.rag.service.TaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    public ApiResponse<List<Task>> getAllTasks() {
        return ApiResponse.success(taskService.getAllTasks());
    }

    @GetMapping("/{taskId}")
    public ApiResponse<Task> getTask(@PathVariable String taskId) {
        return ApiResponse.success(taskService.getTask(taskId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<Void> cancelTask(@PathVariable String taskId) {
        taskService.cancelTask(taskId);
        return ApiResponse.success();
    }
}