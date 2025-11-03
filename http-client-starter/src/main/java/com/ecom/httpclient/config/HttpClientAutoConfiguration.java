package com.ecom.httpclient.config;

import com.ecom.httpclient.client.ResilientWebClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for HTTP Client Starter
 * 
 * <p>Automatically configures ResilientWebClient with circuit breaker,
 * retry, and rate limiting when this starter is included as a dependency.
 * 
 * <p>Configuration via application.yml:
 * <pre>
 * http-client:
 *   default-timeout: PT5S
 *   circuit-breaker:
 *     enabled: true
 *     failure-rate-threshold: 50
 *   retry:
 *     enabled: true
 *     max-attempts: 3
 *   rate-limiter:
 *     enabled: true
 *     limit-for-period: 100
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientAutoConfiguration {

    @Bean
    public ResilientWebClient resilientWebClient(
            HttpClientProperties properties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rateLimiterRegistry) {
        return new ResilientWebClient(
            properties,
            circuitBreakerRegistry,
            retryRegistry,
            rateLimiterRegistry
        );
    }
}

