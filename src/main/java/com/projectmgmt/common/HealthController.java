package com.projectmgmt.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("timestamp", Instant.now());

        // Check DB
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("SELECT 1");
            result.put("database", "UP");
        } catch (Exception e) {
            result.put("database", "DOWN");
            result.put("status", "DEGRADED");
        }

        // Check Redis
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            result.put("redis", "UP");
        } catch (Exception e) {
            result.put("redis", "DOWN");
            result.put("status", "DEGRADED");
        }

        int statusCode = "UP".equals(result.get("status")) ? 200 : 503;
        return ResponseEntity.status(statusCode).body(result);
    }
}
