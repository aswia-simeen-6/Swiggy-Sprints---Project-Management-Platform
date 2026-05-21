package com.projectmgmt.common.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ETagUtils {

    private ETagUtils() {}

    public static String generate(Object... components) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object component : components) {
                if (component != null) {
                    digest.update(component.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            String hash = HexFormat.of().formatHex(digest.digest()).substring(0, 16);
            return "\"" + hash + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static <T> ResponseEntity<T> withETag(T body, String etag, String ifNoneMatch) {
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(body);
    }
}
