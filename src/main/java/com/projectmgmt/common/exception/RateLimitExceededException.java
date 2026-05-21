package com.projectmgmt.common.exception;

public class RateLimitExceededException extends BaseException {

    private final long retryAfterMs;

    public RateLimitExceededException(long retryAfterMs) {
        super("RATE_LIMIT_EXCEEDED",
                "Too many requests. Retry after %d ms".formatted(retryAfterMs),
                java.util.Map.of("retryAfterMs", retryAfterMs));
        this.retryAfterMs = retryAfterMs;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }
}
