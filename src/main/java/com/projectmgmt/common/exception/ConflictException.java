package com.projectmgmt.common.exception;

import lombok.Getter;

@Getter
public class ConflictException extends BaseException {

    private final int currentVersion;

    public ConflictException(String resourceType, Object identifier, int currentVersion) {
        super("OPTIMISTIC_LOCK_CONFLICT",
                "%s '%s' was modified by another request. Current version: %d. Refresh and retry."
                        .formatted(resourceType, identifier, currentVersion),
                java.util.Map.of(
                        "resourceType", resourceType,
                        "resourceId", identifier.toString(),
                        "currentVersion", currentVersion,
                        "suggestion", "Re-fetch the resource and resubmit with the current version"
                ));
        this.currentVersion = currentVersion;
    }
}
