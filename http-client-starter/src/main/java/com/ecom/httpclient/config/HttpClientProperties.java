package com.ecom.httpclient.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP Client Configuration Properties
 * 
 * <p>Configuration for ResilientWebClient with timeouts, circuit breaker,
 * retry, and rate limiting settings.
 * 
 * <p>Configure in application.yml:
 * <pre>
 * http-client:
 *   default-timeout: PT5S
 *   circuit-breaker:
 *     failure-rate-threshold: 50
 *     wait-duration: PT60S
 *   retry:
 *     max-attempts: 3
 *     wait-duration: PT1S
 *   rate-limiter:
 *     limit-for-period: 100
 *     limit-refresh-period: PT1M
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "http-client")
public class HttpClientProperties {
    
    /**
     * Default timeout for HTTP requests (ISO-8601 duration format)
     * Example: PT5S = 5 seconds, PT1M = 1 minute
     */
    private Duration defaultTimeout = Duration.ofSeconds(5);
    
    /**
     * Connect timeout (time to establish connection)
     */
    private Duration connectTimeout = Duration.ofSeconds(2);
    
    /**
     * Read timeout (time waiting for response)
     */
    private Duration readTimeout = Duration.ofSeconds(5);
    
    /**
     * Write timeout (time waiting to send request)
     */
    private Duration writeTimeout = Duration.ofSeconds(5);
    
    /**
     * Response timeout (total time for request)
     */
    private Duration responseTimeout = Duration.ofSeconds(10);
    
    /**
     * Circuit breaker configuration
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    
    /**
     * Retry configuration
     */
    private RetryConfig retry = new RetryConfig();
    
    /**
     * Rate limiter configuration
     */
    private RateLimiterConfig rateLimiter = new RateLimiterConfig();
    
    /**
     * Service-specific configurations (key = service name, value = specific config)
     */
    private Map<String, ServiceConfig> services = new HashMap<>();
    
    @Data
    public static class CircuitBreakerConfig {
        /**
         * Failure rate threshold percentage (0-100)
         * Circuit opens when failure rate exceeds this
         */
        private float failureRateThreshold = 50f;
        
        /**
         * Wait duration before allowing next attempt after circuit opens
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);
        
        /**
         * Number of calls to evaluate failure rate
         */
        private int ringBufferSizeInClosedState = 100;
        
        /**
         * Number of calls in half-open state before closing
         */
        private int ringBufferSizeInHalfOpenState = 10;
        
        /**
         * Enable circuit breaker (default: true)
         */
        private boolean enabled = true;
    }
    
    @Data
    public static class RetryConfig {
        /**
         * Maximum number of retry attempts
         */
        private int maxAttempts = 3;
        
        /**
         * Wait duration between retries
         */
        private Duration waitDuration = Duration.ofSeconds(1);
        
        /**
         * Enable retry (default: true)
         */
        private boolean enabled = true;
    }
    
    @Data
    public static class RateLimiterConfig {
        /**
         * Maximum number of requests allowed in period
         */
        private int limitForPeriod = 100;
        
        /**
         * Period duration for rate limiting
         */
        private Duration limitRefreshPeriod = Duration.ofMinutes(1);
        
        /**
         * Timeout for acquiring permission
         */
        private Duration timeoutDuration = Duration.ofSeconds(5);
        
        /**
         * Enable rate limiter (default: true)
         */
        private boolean enabled = true;
    }
    
    @Data
    public static class ServiceConfig {
        private Duration timeout;
        private CircuitBreakerConfig circuitBreaker;
        private RetryConfig retry;
        private RateLimiterConfig rateLimiter;
    }
}

