package com.projectmgmt.common.exception;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final transient Object details;

    protected BaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    protected BaseException(String errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    protected BaseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }
}
