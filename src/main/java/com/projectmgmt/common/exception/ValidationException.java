package com.projectmgmt.common.exception;

public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }

    public ValidationException(String message, Object details) {
        super("VALIDATION_ERROR", message, details);
    }
}
