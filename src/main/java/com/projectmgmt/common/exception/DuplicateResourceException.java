package com.projectmgmt.common.exception;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String resourceType, String field, Object value) {
        super("DUPLICATE_RESOURCE",
                "%s with %s '%s' already exists".formatted(resourceType, field, value));
    }
}
