package com.anner.rag.exception;

import lombok.Getter;

@Getter
public class RagException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;

    public RagException(RagErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

    public RagException(RagErrorCode errorCode, String detailMessage) {
        super(errorCode.getMessage() + ": " + detailMessage);
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage() + ": " + detailMessage;
    }

    public RagException(RagErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage() + ": " + cause.getMessage();
    }
}