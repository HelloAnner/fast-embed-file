package com.anner.embed.exception;

import lombok.Getter;

@Getter
public enum RagErrorCode {
    // 文件相关错误 (1000-1999)
    FILE_UPLOAD_FAILED("1001", "文件上传失败"),
    FILE_NOT_FOUND("1002", "文件不存在"),
    FILE_TOO_LARGE("1003", "文件大小超过限制"),
    INVALID_FILE_FORMAT("1004", "无效的文件格式"),
    FILE_EXTRACT_ERROR("1005", "文件解压失败"),
    FILE_DOWNLOAD_ERROR("1008", "文件下载失败"),

    // 目录相关错误 (2000-2999)
    DIRECTORY_CREATE_FAILED("2001", "创建目录失败"),
    DIRECTORY_NOT_FOUND("2002", "目录不存在"),
    DIRECTORY_ACCESS_DENIED("2003", "目录访问被拒绝"),

    // 文件处理相关错误 (3000-3999)
    FILE_PROCESS_FAILED("3001", "文件处理失败"),
    FILE_READ_ERROR("3002", "文件读取错误"),
    FILE_WRITE_ERROR("3003", "文件写入错误"),
    TEXT_PROCESS_ERROR("3004", "文本处理错误"),
    JSON_PARSE_ERROR("3005", "JSON解析错误"),

    // 向量化相关错误 (4000-4999)
    VECTORIZATION_FAILED("4001", "向量化处理失败"),
    MODEL_ERROR("4002", "模型处理错误"),
    API_ERROR("4003", "API调用错误"),
    INVALID_API_KEY("4004", "无效的API密钥"),

    // 系统错误 (5000-5999)
    SYSTEM_ERROR("5001", "系统内部错误"),
    UNEXPECTED_ERROR("5002", "未预期的错误"),

    // 参数验证错误 (6000-6999)
    INVALID_PARAMETER("6001", "无效的参数"),
    MISSING_PARAMETER("6002", "缺少必要参数"),
    PARAMETER_OUT_OF_RANGE("6003", "参数超出范围"),

    // 任务相关错误 (7000-7999)
    TASK_NOT_FOUND("7001", "任务不存在"),
    TASK_CANCELLED("7002", "任务已取消"),
    TASK_ALREADY_COMPLETED("7003", "任务已完成"),
    TASK_ALREADY_CANCELLED("7004", "任务已被取消");

    private final String code;
    private final String message;

    RagErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}