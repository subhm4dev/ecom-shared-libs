package com.ecom.jwt.jwks;

import com.ecom.httpclient.client.ResilientWebClient;
import com.ecom.jwt.config.JwtValidationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocking JWKS Service
 * 
 * <p>Fetches and caches JSON Web Key Set (JWKS) from Identity service.
 * Periodically refreshes keys to support key rotation.
 * 
 * <p>Uses ResilientWebClient with circuit breaker, retry, and rate limiting.
 */
public class BlockingJwksService {
    
    private static final Logger log = LoggerFactory.getLogger(BlockingJwksService.class);
    
    private final ResilientWebClient resilientWebClient;
    private final JwtValidationProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private WebClient webClient; // Lazy initialization
    
    private final Map<String, RSAKey> jwkCache = new ConcurrentHashMap<>();
    private volatile long lastFetchTime = 0;
    
    public BlockingJwksService(
            ResilientWebClient resilientWebClient,
            JwtValidationProperties properties) {
        this.resilientWebClient = resilientWebClient;
        this.properties = properties;
    }
    
    /**
     * Get WebClient instance (lazy initialization)
     */
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = resilientWebClient.create("identity-service", properties.getIdentityServiceUrl());
        }
        return webClient;
    }
    
    /**
     * Get public key by Key ID (kid)
     * 
     * @param kid Key ID from JWT header
     * @return RSAKey with the public key
     * @throws IllegalArgumentException if key not found
     */
    public RSAKey getPublicKey(String kid) {
        RSAKey key = jwkCache.get(kid);
        if (key != null) {
            return key;
        }
        
        // Cache miss - refresh and try again
        log.warn("JWK key not found in cache: kid={}, refreshing cache...", kid);
        refreshJwksCache();
        
        RSAKey refreshedKey = jwkCache.get(kid);
        if (refreshedKey == null) {
            throw new IllegalArgumentException("JWK key not found after refresh: " + kid);
        }
        return refreshedKey;
    }
    
    /**
     * Refresh JWKS cache from Identity service
     * 
     * Note: @Scheduled uses fixedDelay in milliseconds.
     */
    @Scheduled(fixedDelayString = "${jwt.jwks-cache-refresh-interval-ms:300000}")
    public void refreshJwksCache() {
        log.debug("Refreshing JWKS cache from Identity service...");
        
        try {
            String jwksEndpoint = properties.getIdentityServiceUrl() + properties.getJwksEndpoint();
            
            // Use ResilientWebClient with circuit breaker, retry, and rate limiting
            String responseBody = getWebClient()
                .get()
                .uri(jwksEndpoint)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Failed to fetch JWKS: status={}, message={}", ex.getStatusCode(), ex.getMessage());
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Failed to fetch JWKS from Identity service", ex);
                    return Mono.empty();
                })
                .block(); // Blocking call (we're in a scheduled method)
            
            if (responseBody == null || responseBody.isBlank()) {
                log.error("Empty JWKS response from Identity service");
                return;
            }
            
            // Extract JWKS from wrapped response (if wrapped in ApiResponse)
            String jwksJson = JwksResponseExtractor.extractJwksFromResponse(responseBody);
            
            // Parse JWKS
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
            log.error("Failed to fetch JWKS from Identity service", e);
        }
    }
    
    /**
     * Get cached keys count (for monitoring)
     */
    public int getCachedKeysCount() {
        return jwkCache.size();
    }
}

