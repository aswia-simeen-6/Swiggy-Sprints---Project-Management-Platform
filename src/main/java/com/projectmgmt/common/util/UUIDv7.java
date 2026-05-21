package com.projectmgmt.common.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * UUID v7 generator — time-sortable UUIDs per RFC 9562.
 * First 48 bits = Unix timestamp in milliseconds.
 * Remaining bits = random.
 * 
 * Benefits over UUID v4:
 * - Time-sortable: natural ordering matches creation order
 * - No B-tree page splits: sequential inserts are adjacent
 * - Globally unique: safe across distributed systems
 */
public final class UUIDv7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UUIDv7() {}

    public static UUID generate() {
        return generate(Instant.now());
    }

    public static UUID generate(Instant timestamp) {
        long millis = timestamp.toEpochMilli();

        // Random bytes for the lower 80 bits
        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        // Build the 128-bit UUID
        long msb = (millis << 16) | (0x7000L) | ((randomBytes[0] & 0xFF) << 4) | ((randomBytes[1] & 0x0F));
        long lsb = 0x8000000000000000L; // variant bits
        lsb |= ((long)(randomBytes[2] & 0x3F) << 56);
        lsb |= ((long)(randomBytes[3] & 0xFF) << 48);
        lsb |= ((long)(randomBytes[4] & 0xFF) << 40);
        lsb |= ((long)(randomBytes[5] & 0xFF) << 32);
        lsb |= ((long)(randomBytes[6] & 0xFF) << 24);
        lsb |= ((long)(randomBytes[7] & 0xFF) << 16);
        lsb |= ((long)(randomBytes[8] & 0xFF) << 8);
        lsb |= ((long)(randomBytes[9] & 0xFF));

        return new UUID(msb, lsb);
    }
}
