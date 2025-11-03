package com.ecom.httpclient.client;

import com.ecom.httpclient.config.HttpClientProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Resilient WebClient Builder
 * 
 * <p>Provides a WebClient with built-in:
 * <ul>
 *   <li>Timeouts (connect, read, write, response)</li>
 *   <li>Circuit breaker (fails fast when service is down)</li>
 *   <li>Retry mechanism (automatic retries on transient failures)</li>
 *   <li>Rate limiting (prevents API abuse)</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * resilientWebClient.create("identity-service", "http://localhost:8081")
 *     .get()
 *     .uri("/.well-known/jwks.json")
 *     .retrieve()
 *     .bodyToMono(String.class);
 * }
 * </pre>
 */
@Component
public class ResilientWebClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientWebClient.class);

    private final HttpClientProperties properties;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public ResilientWebClient(
            HttpClientProperties properties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rateLimiterRegistry) {
        this.properties = properties;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    /**
     * Create a resilient WebClient for a specific service
     * 
     * @param serviceName Service name (used for circuit breaker, retry, rate limiter names)
     * @param baseUrl Base URL for the service
     * @return Configured WebClient with resilience features
     */
    public WebClient create(String serviceName, String baseUrl) {
        // Get service-specific config or use defaults
        HttpClientProperties.ServiceConfig serviceConfig = 
            properties.getServices().getOrDefault(serviceName, null);
        
        Duration timeout = serviceConfig != null && serviceConfig.getTimeout() != null
            ? serviceConfig.getTimeout()
            : properties.getDefaultTimeout();

        // Create HTTP client with timeouts
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(timeout)
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 
                (int) properties.getConnectTimeout().toMillis());

        // Create base WebClient
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(1024 * 1024)); // 1MB buffer

        // Add resilience features as filters
        builder.filter(createCircuitBreakerFilter(serviceName, serviceConfig));
        builder.filter(createRetryFilter(serviceName, serviceConfig));
        builder.filter(createRateLimiterFilter(serviceName, serviceConfig));

        return builder.build();
    }

    /**
     * Create circuit breaker filter
     */
    private ExchangeFilterFunction createCircuitBreakerFilter(
            String serviceName, HttpClientProperties.ServiceConfig serviceConfig) {
        
        HttpClientProperties.CircuitBreakerConfig config = 
            (serviceConfig != null && serviceConfig.getCircuitBreaker() != null)
                ? serviceConfig.getCircuitBreaker()
                : properties.getCircuitBreaker();

        if (!config.isEnabled()) {
            return ExchangeFilterFunction.ofRequestProcessor(request -> Mono.just(request));
        }

        // Create or get circuit breaker
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName, 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(config.getFailureRateThreshold())
                .waitDurationInOpenState(java.time.Duration.ofMillis(
                    config.getWaitDurationInOpenState().toMillis()))
                .slidingWindowSize(config.getRingBufferSizeInClosedState())
                .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(config.getRingBufferSizeInClosedState())
                .permittedNumberOfCallsInHalfOpenState(config.getRingBufferSizeInHalfOpenState())
                .recordExceptions(
                    WebClientRequestException.class,
                    WebClientResponseException.class,
                    ConnectException.class,
                    SocketTimeoutException.class,
                    TimeoutException.class
                )
                .build());

        log.info("Circuit breaker configured for service: {} (failureRateThreshold: {}%)", 
            serviceName, config.getFailureRateThreshold());

        return (request, next) -> 
            next.exchange(request)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }

    /**
     * Create retry filter
     */
    private ExchangeFilterFunction createRetryFilter(
            String serviceName, HttpClientProperties.ServiceConfig serviceConfig) {
        
        HttpClientProperties.RetryConfig config = 
            (serviceConfig != null && serviceConfig.getRetry() != null)
                ? serviceConfig.getRetry()
                : properties.getRetry();

        if (!config.isEnabled()) {
            return ExchangeFilterFunction.ofRequestProcessor(request -> Mono.just(request));
        }

        // Create or get retry
        Retry retry = retryRegistry.retry(serviceName,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(config.getMaxAttempts())
                .waitDuration(java.time.Duration.ofMillis(config.getWaitDuration().toMillis()))
                .retryOnException(throwable -> {
                    // Retry on network errors and 5xx errors
                    if (throwable instanceof WebClientRequestException) {
                        return true; // Network errors
                    }
                    if (throwable instanceof WebClientResponseException ex) {
                        // Retry on server errors (5xx), not client errors (4xx)
                        return ex.getStatusCode().is5xxServerError();
                    }
                    return throwable instanceof ConnectException ||
                           throwable instanceof SocketTimeoutException ||
                           throwable instanceof TimeoutException;
                })
                .build());

        log.info("Retry configured for service: {} (maxAttempts: {}, waitDuration: {}ms)", 
            serviceName, config.getMaxAttempts(), config.getWaitDuration().toMillis());

        return (request, next) -> 
            next.exchange(request)
                .transformDeferred(RetryOperator.of(retry));
    }

    /**
     * Create rate limiter filter
     */
    private ExchangeFilterFunction createRateLimiterFilter(
            String serviceName, HttpClientProperties.ServiceConfig serviceConfig) {
        
        HttpClientProperties.RateLimiterConfig config = 
            (serviceConfig != null && serviceConfig.getRateLimiter() != null)
                ? serviceConfig.getRateLimiter()
                : properties.getRateLimiter();

        if (!config.isEnabled()) {
            return ExchangeFilterFunction.ofRequestProcessor(request -> Mono.just(request));
        }

        // Create or get rate limiter
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(serviceName,
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(config.getLimitForPeriod())
                .limitRefreshPeriod(java.time.Duration.ofMillis(config.getLimitRefreshPeriod().toMillis()))
                .timeoutDuration(java.time.Duration.ofMillis(config.getTimeoutDuration().toMillis()))
                .build());

        log.info("Rate limiter configured for service: {} (limitForPeriod: {}, limitRefreshPeriod: {}ms)", 
            serviceName, config.getLimitForPeriod(), config.getLimitRefreshPeriod().toMillis());

        return (request, next) -> 
            next.exchange(request)
                .transformDeferred(RateLimiterOperator.of(rateLimiter));
    }
}
