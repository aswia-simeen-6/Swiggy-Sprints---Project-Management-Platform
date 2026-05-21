package com.projectmgmt.common.exception;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super("RESOURCE_NOT_FOUND",
                "%s with identifier '%s' not found".formatted(resourceType, identifier));
    }

    public ResourceNotFoundException(String resourceType, String field, Object value) {
        super("RESOURCE_NOT_FOUND",
                "%s with %s '%s' not found".formatted(resourceType, field, value));
    }
}
