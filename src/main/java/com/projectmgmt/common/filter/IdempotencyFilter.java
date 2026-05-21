package com.projectmgmt.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String redisKey = KEY_PREFIX + idempotencyKey;

        try {
            String cached = redisTemplate.opsForValue().get(redisKey);

            if (cached != null) {
                // Return cached response
                IdempotencyEntry entry = objectMapper.readValue(cached, IdempotencyEntry.class);
                response.setStatus(entry.statusCode());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("X-Idempotent-Replayed", "true");
                response.getWriter().write(entry.body());
                return;
            }

            // Wrap response to capture output
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(request, wrappedResponse);

            // Cache successful responses only
            int status = wrappedResponse.getStatus();
            if (status >= 200 && status < 300) {
                String body = new String(wrappedResponse.getContentAsByteArray());
                IdempotencyEntry entry = new IdempotencyEntry(status, body);
                redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(entry), TTL);
            }

            wrappedResponse.copyBodyToResponse();
        } catch (Exception ex) {
            log.warn("Idempotency check failed, proceeding without: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to POST/PUT/PATCH
        String method = request.getMethod();
        return !(HttpMethod.POST.name().equals(method) ||
                 HttpMethod.PUT.name().equals(method) ||
                 HttpMethod.PATCH.name().equals(method));
    }

    private record IdempotencyEntry(int statusCode, String body) {}
}
