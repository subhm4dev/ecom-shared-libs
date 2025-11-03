package com.ecom.jwt.session;

import com.ecom.jwt.config.JwtValidationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Blocking Session Service
 * 
 * <p>Handles token blacklisting using Redis.
 * This is a blocking (MVC) version, not reactive.
 */
public class BlockingSessionService {
    
    private static final Logger log = LoggerFactory.getLogger(BlockingSessionService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtValidationProperties properties;
    
    public BlockingSessionService(
            RedisTemplate<String, String> redisTemplate,
            JwtValidationProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }
    
    /**
     * Check if token is blacklisted
     * 
     * @param tokenId Token ID (jti) from JWT
     * @return true if token is blacklisted
     */
    public boolean isTokenBlacklisted(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        
        String key = properties.getBlacklistPrefix() + tokenId;
        // Use hasKey() for efficiency
        Boolean exists = redisTemplate.hasKey(key);
        boolean blacklisted = Boolean.TRUE.equals(exists);
        
        if (blacklisted) {
            log.debug("Token is blacklisted: tokenId={}", tokenId);
        }
        
        return blacklisted;
    }
    
    /**
     * Blacklist a token (used during logout)
     * 
     * @param tokenId Token ID (jti)
     * @param expirySeconds Expiry time in seconds (from JWT expiry)
     */
    public void blacklistToken(String tokenId, long expirySeconds) {
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        
        String key = properties.getBlacklistPrefix() + tokenId;
        redisTemplate.opsForValue().set(key, "blacklisted", expirySeconds, TimeUnit.SECONDS);
        log.info("Token blacklisted: tokenId={}, expirySeconds={}", tokenId, expirySeconds);
    }
}

