package com.ecom.jwt.session;

import com.ecom.jwt.config.JwtValidationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

/**
 * Reactive Session Service
 * 
 * <p>Checks token blacklist in Redis for logged-out tokens.
 * Uses reactive Redis for WebFlux compatibility.
 */
public class ReactiveSessionService {
    
    private static final Logger log = LoggerFactory.getLogger(ReactiveSessionService.class);
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JwtValidationProperties properties;
    
    public ReactiveSessionService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            JwtValidationProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }
    
    /**
     * Check if a token is blacklisted
     * 
     * @param tokenId JWT ID (jti) from token
     * @return Mono<Boolean> true if blacklisted, false otherwise
     */
    public Mono<Boolean> isTokenBlacklisted(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return Mono.just(false);
        }
        
        String key = properties.getBlacklistPrefix() + tokenId;
        
        return redisTemplate.hasKey(key)
            .doOnNext(blacklisted -> {
                if (blacklisted) {
                    log.debug("Token blacklisted: tokenId={}", tokenId);
                }
            })
            .onErrorResume(error -> {
                log.error("Error checking token blacklist: tokenId={}, error={}", 
                    tokenId, error.getMessage());
                // If Redis is unavailable, allow request (fail open)
                // In production, you might want to fail closed for security
                return Mono.just(false);
            });
    }
}

