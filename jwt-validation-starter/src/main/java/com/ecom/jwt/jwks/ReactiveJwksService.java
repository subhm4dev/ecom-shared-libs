package com.ecom.jwt.jwks;

import com.ecom.jwt.config.JwtValidationProperties;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reactive JWKS Service
 * 
 * <p>Fetches and caches JSON Web Key Set (JWKS) from Identity service.
 * Periodically refreshes keys to support key rotation.
 */
public class ReactiveJwksService {
    
    private static final Logger log = LoggerFactory.getLogger(ReactiveJwksService.class);
    
    private final WebClient webClient;
    private final JwtValidationProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    
    private final Map<String, RSAKey> jwkCache = new ConcurrentHashMap<>();
    private volatile long lastFetchTime = 0;
    
    public ReactiveJwksService(
            WebClient webClient,
            JwtValidationProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    }
    
    private String getJwksEndpoint() {
        return properties.getIdentityServiceUrl() + properties.getJwksEndpoint();
    }
    
    /**
     * Get public key by Key ID (kid)
     * 
     * @param kid Key ID from JWT header
     * @return Mono<RSAKey> with the public key
     * @throws IllegalArgumentException if key not found
     */
    public Mono<RSAKey> getPublicKey(String kid) {
        RSAKey key = jwkCache.get(kid);
        if (key != null) {
            return Mono.just(key);
        }
        
        // Cache miss - refresh and try again
        log.warn("JWK key not found in cache: kid={}, refreshing cache...", kid);
        return refreshJwksCache()
            .then(Mono.fromCallable(() -> {
                RSAKey refreshedKey = jwkCache.get(kid);
                if (refreshedKey == null) {
                    throw new IllegalArgumentException("JWK key not found after refresh: " + kid);
                }
                return refreshedKey;
            }));
    }
    
    /**
     * Refresh JWKS cache from Identity service
     * 
     * Note: @Scheduled uses fixedDelay in milliseconds.
     */
    @Scheduled(fixedDelayString = "${gateway.jwt.jwks-cache-refresh-interval-ms:300000}")
    public Mono<Void> refreshJwksCache() {
        log.debug("Refreshing JWKS cache from Identity service...");
        
        return webClient.get()
            .uri(getJwksEndpoint())
            .retrieve()
            .bodyToMono(String.class)
            .doOnNext(response -> {
                try {
                    // Extract JWKS from wrapped response (if wrapped in ApiResponse)
                    String jwksJson = JwksResponseExtractor.extractJwksFromResponse(response);
                    
                    JWKSet jwkSet = JWKSet.parse(jwksJson);
                    Map<String, RSAKey> newCache = new ConcurrentHashMap<>();
                    
                    for (JWK jwk : jwkSet.getKeys()) {
                        if (jwk instanceof RSAKey) {
                            RSAKey rsaKey = (RSAKey) jwk;
                            newCache.put(rsaKey.getKeyID(), rsaKey);
                            log.debug("Cached JWK: kid={}", rsaKey.getKeyID());
                        }
                    }
                    
                    jwkCache.clear();
                    jwkCache.putAll(newCache);
                    lastFetchTime = System.currentTimeMillis();
                    
                    log.info("JWKS cache refreshed: {} keys cached", jwkCache.size());
                    
                } catch (ParseException e) {
                    log.error("Failed to parse JWKS response from Identity service", e);
                } catch (Exception e) {
                    log.error("Failed to process JWKS response from Identity service", e);
                }
            })
            .doOnError(error -> {
                log.error("Failed to fetch JWKS from Identity service: {}", error.getMessage());
                // Don't clear cache on error - use stale keys
            })
            .then();
    }
    
    /**
     * Initialize cache on startup
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void initializeCache() {
        log.info("Initializing JWKS cache...");
        refreshJwksCache().block(Duration.ofSeconds(10));
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "cachedKeys", jwkCache.size(),
            "lastFetchTime", lastFetchTime
        );
    }
}

