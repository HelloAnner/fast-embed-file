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
    private TaskStatus status;
    private double progress;
    private double segmentProgress; // 文本分段进度
    private double embeddingProgress; // 向量化进度
    private String currentStage; // 当前处理阶段
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
        this.segmentProgress = 0;
        this.embeddingProgress = 0;
        this.currentStage = "准备处理文件";
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