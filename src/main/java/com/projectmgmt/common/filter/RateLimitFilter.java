package com.projectmgmt.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmgmt.common.exception.RateLimitExceededException;
import com.projectmgmt.common.model.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rate-limit.requests-per-second:100}")
    private int requestsPerSecond;

    @Value("${rate-limit.burst-capacity:150}")
    private int burstCapacity;

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = 1
            
            local fill_time = capacity / rate
            local ttl = math.floor(fill_time * 2)
            
            local last_tokens = tonumber(redis.call("hget", key, "tokens"))
            if last_tokens == nil then
                last_tokens = capacity
            end
            
            local last_refreshed = tonumber(redis.call("hget", key, "timestamp"))
            if last_refreshed == nil then
                last_refreshed = 0
            end
            
            local delta = math.max(0, now - last_refreshed)
            local filled_tokens = math.min(capacity, last_tokens + (delta * rate))
            local allowed = filled_tokens >= requested
            local new_tokens = filled_tokens
            
            if allowed then
                new_tokens = filled_tokens - requested
            end
            
            redis.call("hset", key, "tokens", new_tokens)
            redis.call("hset", key, "timestamp", now)
            redis.call("expire", key, ttl)
            
            return { allowed and 1 or 0, new_tokens }
            """;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String clientKey = resolveClientKey(request);
        String redisKey = "rate_limit:" + clientKey;

        try {
            DefaultRedisScript<List> script = new DefaultRedisScript<>(LUA_SCRIPT, List.class);
            long now = System.currentTimeMillis() / 1000;
            @SuppressWarnings("unchecked")
            List<Long> result = redisTemplate.execute(script,
                    List.of(redisKey),
                    String.valueOf(burstCapacity),
                    String.valueOf(requestsPerSecond),
                    String.valueOf(now));

            if (result != null && result.getFirst() == 0L) {
                long retryAfterMs = (long) ((1.0 / requestsPerSecond) * 1000);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", String.valueOf(retryAfterMs / 1000));
                ErrorResponse error = ErrorResponse.of(
                        "RATE_LIMIT_EXCEEDED",
                        "Too many requests. Please slow down.",
                        null, MDC.get("requestId"));
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }
        } catch (Exception ex) {
            // If Redis is down, allow the request through (fail-open)
            log.warn("Rate limit check failed, allowing request through: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        // Prefer workspace-level rate limiting via auth, fall back to IP
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Rate limit per authenticated user — more granular in service layer if needed
            return "auth:" + authHeader.substring(7).hashCode();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return "ip:" + (forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.startsWith("/swagger-ui") || path.startsWith("/api-docs");
    }
}
