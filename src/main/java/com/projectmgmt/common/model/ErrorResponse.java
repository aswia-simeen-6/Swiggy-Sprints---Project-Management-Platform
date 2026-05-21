package com.projectmgmt.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        Object details,
        String requestId,
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message, Object details, String requestId) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .requestId(requestId)
                .timestamp(Instant.now())
                .build();
    }
}
