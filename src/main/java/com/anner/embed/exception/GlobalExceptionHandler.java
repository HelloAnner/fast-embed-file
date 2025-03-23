package com.anner.embed.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.anner.embed.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RagException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleRagException(RagException e) {
        log.error("业务异常", e);
        return ApiResponse.error(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("文件大小超过限制", e);
        return ApiResponse.error(RagErrorCode.FILE_TOO_LARGE.getCode(), RagErrorCode.FILE_TOO_LARGE.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(RagErrorCode.SYSTEM_ERROR.getCode(), RagErrorCode.SYSTEM_ERROR.getMessage());
    }
}