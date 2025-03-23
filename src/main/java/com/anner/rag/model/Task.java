package com.anner.rag.model;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Task {
    private String id;
    private String fileName;
    private String modelType;
    private double progress;
    private TaskStatus status;
    private String errorMessage;
    private String vectorFilePath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Task(String id, String fileName, String modelType) {
        this.id = id;
        this.fileName = fileName;
        this.modelType = modelType;
        this.status = TaskStatus.RUNNING;
        this.progress = 0;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    public enum TaskStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}