package com.projectmgmt.common.exception;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
