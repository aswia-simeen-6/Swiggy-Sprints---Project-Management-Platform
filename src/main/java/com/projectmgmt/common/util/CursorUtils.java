package com.projectmgmt.common.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class CursorUtils {

    private CursorUtils() {}

    public static String encode(Instant timestamp, UUID id) {
        String raw = timestamp.toEpochMilli() + "|" + id.toString();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorData decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid cursor format");
            }
            Instant timestamp = Instant.ofEpochMilli(Long.parseLong(parts[0]));
            UUID id = UUID.fromString(parts[1]);
            return new CursorData(timestamp, id);
        } catch (Exception e) {
            throw new com.projectmgmt.common.exception.ValidationException("Invalid cursor: " + cursor);
        }
    }

    public record CursorData(Instant timestamp, UUID id) {}
}
