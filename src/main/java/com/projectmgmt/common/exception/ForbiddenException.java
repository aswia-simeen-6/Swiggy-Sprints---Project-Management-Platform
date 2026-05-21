package com.projectmgmt.common.exception;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
