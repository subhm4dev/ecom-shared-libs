package com.ecom.httpclient.config;

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
    
    // Getters and Setters
    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(Duration defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(Duration writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public Duration getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(Duration responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }

    public RateLimiterConfig getRateLimiter() {
        return rateLimiter;
    }

    public void setRateLimiter(RateLimiterConfig rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public Map<String, ServiceConfig> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceConfig> services) {
        this.services = services;
    }

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

        // Getters and Setters
        public float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(float failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public Duration getWaitDurationInOpenState() {
            return waitDurationInOpenState;
        }

        public void setWaitDurationInOpenState(Duration waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public int getRingBufferSizeInClosedState() {
            return ringBufferSizeInClosedState;
        }

        public void setRingBufferSizeInClosedState(int ringBufferSizeInClosedState) {
            this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
        }

        public int getRingBufferSizeInHalfOpenState() {
            return ringBufferSizeInHalfOpenState;
        }

        public void setRingBufferSizeInHalfOpenState(int ringBufferSizeInHalfOpenState) {
            this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
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

        // Getters and Setters
        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getWaitDuration() {
            return waitDuration;
        }

        public void setWaitDuration(Duration waitDuration) {
            this.waitDuration = waitDuration;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
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

        // Getters and Setters
        public int getLimitForPeriod() {
            return limitForPeriod;
        }

        public void setLimitForPeriod(int limitForPeriod) {
            this.limitForPeriod = limitForPeriod;
        }

        public Duration getLimitRefreshPeriod() {
            return limitRefreshPeriod;
        }

        public void setLimitRefreshPeriod(Duration limitRefreshPeriod) {
            this.limitRefreshPeriod = limitRefreshPeriod;
        }

        public Duration getTimeoutDuration() {
            return timeoutDuration;
        }

        public void setTimeoutDuration(Duration timeoutDuration) {
            this.timeoutDuration = timeoutDuration;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    public static class ServiceConfig {
        private Duration timeout;
        private CircuitBreakerConfig circuitBreaker;
        private RetryConfig retry;
        private RateLimiterConfig rateLimiter;

        // Getters and Setters
        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public CircuitBreakerConfig getCircuitBreaker() {
            return circuitBreaker;
        }

        public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }

        public RetryConfig getRetry() {
            return retry;
        }

        public void setRetry(RetryConfig retry) {
            this.retry = retry;
        }

        public RateLimiterConfig getRateLimiter() {
            return rateLimiter;
        }

        public void setRateLimiter(RateLimiterConfig rateLimiter) {
            this.rateLimiter = rateLimiter;
        }
    }
}
