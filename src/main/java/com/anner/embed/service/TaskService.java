package com.anner.embed.service;

import java.util.List;

import com.anner.embed.model.Task;

public interface TaskService {
    Task createTask(String fileName, String modelType);

    void updateTaskProgress(String taskId, double progress);

    void updateTaskProgress(String taskId, int processedCount, int totalCount);

    void completeTask(String taskId);

    void failTask(String taskId, String errorMessage);

    void cancelTask(String taskId);

    List<Task> getAllTasks();

    Task getTask(String taskId);

    boolean isTaskCancelled(String taskId);
}