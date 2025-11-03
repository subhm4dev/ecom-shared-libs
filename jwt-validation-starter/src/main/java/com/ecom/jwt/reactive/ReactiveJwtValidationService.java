package com.ecom.jwt.reactive;

import com.ecom.jwt.config.JwtValidationProperties;
import com.ecom.jwt.core.JwtSignatureVerifier;
import com.ecom.jwt.core.JwtTokenParser;
import com.ecom.jwt.jwks.ReactiveJwksService;
import com.ecom.jwt.session.ReactiveSessionService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Reactive JWT Validation Service
 * 
 * <p>Validates JWT tokens for Spring WebFlux (reactive) services.
 * Checks signature, expiry, blacklist, and extracts claims.
 */
public class ReactiveJwtValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(ReactiveJwtValidationService.class);
    
    private final ReactiveJwksService jwksService;
    private final ReactiveSessionService sessionService;
    private final JwtValidationProperties properties;
    
    public ReactiveJwtValidationService(
            ReactiveJwksService jwksService,
            ReactiveSessionService sessionService,
            JwtValidationProperties properties) {
        this.jwksService = jwksService;
        this.sessionService = sessionService;
        this.properties = properties;
    }
    
    /**
     * Validate JWT token and extract claims
     * 
     * @param token JWT token string
     * @return Mono<JWTClaimsSet> with validated claims
     * @throws IllegalArgumentException if token is invalid, expired, or blacklisted
     */
    public Mono<JWTClaimsSet> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Mono.error(new IllegalArgumentException("Token is required"));
        }
        
        try {
            // Parse token
            SignedJWT signedJWT = JwtTokenParser.parseToken(token);
            
            // Extract Key ID from header
            String kid = JwtTokenParser.extractKeyId(token);
            
            // Extract token ID for blacklist check
            String tokenId = JwtTokenParser.extractTokenId(token);
            
            // Check blacklist first (fast fail)
            return sessionService.isTokenBlacklisted(tokenId)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        log.warn("Token is blacklisted (revoked): tokenId={}", tokenId);
                        return Mono.error(new IllegalArgumentException("JWT token has been revoked"));
                    }
                    
                    // Get public key from JWKS cache (reactive)
                    return jwksService.getPublicKey(kid)
                        .flatMap(publicKey -> {
                            try {
                                // Verify signature
                                JwtSignatureVerifier.verifySignature(signedJWT, publicKey);
                                
                                // Get claims
                                JWTClaimsSet claimsSet;
                                try {
                                    claimsSet = signedJWT.getJWTClaimsSet();
                                } catch (java.text.ParseException e) {
                                    log.error("Failed to parse JWT claims", e);
                                    return Mono.error(new IllegalArgumentException("Invalid JWT claims format", e));
                                }
                                
                                // Validate expiry
                                JwtTokenParser.validateExpiry(claimsSet);
                                
                                // Validate issuer (optional)
                                JwtTokenParser.validateIssuer(claimsSet, properties.getIssuer());
                                
                                return Mono.just(claimsSet);
                            } catch (IllegalArgumentException e) {
                                return Mono.error(e);
                            }
                        });
                });
                
        } catch (IllegalArgumentException e) {
            return Mono.error(e);
        } catch (Exception e) {
            log.error("Unexpected error during token validation", e);
            return Mono.error(new IllegalArgumentException("Token validation failed", e));
        }
    }
    
    /**
     * Extract token ID (jti) from token
     */
    public String extractTokenId(String token) {
        return JwtTokenParser.extractTokenId(token);
    }
    
    /**
     * Extract user ID from token claims
     */
    public String extractUserId(JWTClaimsSet claims) {
        return JwtTokenParser.extractUserId(claims);
    }
    
    /**
     * Extract tenant ID from token claims
     */
    public String extractTenantId(JWTClaimsSet claims) {
        return JwtTokenParser.extractTenantId(claims);
    }
    
    /**
     * Extract roles from token claims
     */
    public java.util.List<String> extractRoles(JWTClaimsSet claims) {
        return JwtTokenParser.extractRoles(claims);
    }
}

