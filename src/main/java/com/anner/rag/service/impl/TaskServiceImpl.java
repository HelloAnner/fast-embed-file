package com.anner.rag.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.anner.rag.model.Task;
import com.anner.rag.model.Task.TaskStatus;
import com.anner.rag.service.TaskService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public Task createTask(String fileName, String modelType) {
        Task task = new Task();
        task.setId(UUID.randomUUID().toString());
        task.setFileName(fileName);
        task.setModelType(modelType);
        task.setProgress(0.0);
        task.setStatus(TaskStatus.RUNNING);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        tasks.put(task.getId(), task);
        log.info("Created new task: {}", task.getId());
        return task;
    }

    @Override
    public void updateTaskProgress(String taskId, double progress) {
        Task task = getTask(taskId);
        task.setProgress(progress);
        task.setUpdateTime(LocalDateTime.now());
    }

    @Override
    public void updateTaskProgress(String taskId, int processedCount, int totalCount) {
        Task task = getTask(taskId);
        double progress = totalCount > 0 ? (double) processedCount / totalCount * 100 : 0;
        task.setProgress(progress);
        task.setUpdateTime(LocalDateTime.now());
    }

    @Override
    public void completeTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setProgress(100.0);
            task.setUpdateTime(LocalDateTime.now());
            log.info("Task completed: {}", taskId);
        }
    }

    @Override
    public void failTask(String taskId, String errorMessage) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            task.setUpdateTime(LocalDateTime.now());
            log.error("Task failed: {} - {}", taskId, errorMessage);
        }
    }

    @Override
    public void cancelTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task != null && task.getStatus() == TaskStatus.RUNNING) {
            task.setStatus(TaskStatus.CANCELLED);
            task.setUpdateTime(LocalDateTime.now());
            log.info("Task cancelled: {}", taskId);
        }
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public Task getTask(String taskId) {
        return tasks.get(taskId);
    }

    @Override
    public boolean isTaskCancelled(String taskId) {
        Task task = tasks.get(taskId);
        return task != null && task.getStatus() == TaskStatus.CANCELLED;
    }
}