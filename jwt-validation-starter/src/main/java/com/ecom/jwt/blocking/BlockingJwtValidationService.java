package com.ecom.jwt.blocking;

import com.ecom.jwt.config.JwtValidationProperties;
import com.ecom.jwt.core.JwtSignatureVerifier;
import com.ecom.jwt.core.JwtTokenParser;
import com.ecom.jwt.jwks.BlockingJwksService;
import com.ecom.jwt.session.BlockingSessionService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blocking JWT Validation Service
 * 
 * <p>Validates JWT tokens for Spring MVC (blocking) services.
 * Checks signature, expiry, blacklist, and extracts claims.
 */
public class BlockingJwtValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(BlockingJwtValidationService.class);
    
    private final BlockingJwksService jwksService;
    private final BlockingSessionService sessionService;
    private final JwtValidationProperties properties;
    
    public BlockingJwtValidationService(
            BlockingJwksService jwksService,
            BlockingSessionService sessionService,
            JwtValidationProperties properties) {
        this.jwksService = jwksService;
        this.sessionService = sessionService;
        this.properties = properties;
    }
    
    /**
     * Validate JWT token and extract claims
     * 
     * @param token JWT token string
     * @return JWTClaimsSet with validated claims
     * @throws IllegalArgumentException if token is invalid, expired, or blacklisted
     */
    public JWTClaimsSet validateToken(String token) {
        // Parse token
        SignedJWT signedJWT = JwtTokenParser.parseToken(token);
        
        // Extract token ID for blacklist check (BEFORE signature verification for fast fail)
        String tokenId = JwtTokenParser.extractTokenId(token);
        if (sessionService.isTokenBlacklisted(tokenId)) {
            log.warn("Token is blacklisted (revoked): tokenId={}", tokenId);
            throw new IllegalArgumentException("JWT token has been revoked");
        }
        
        // Extract Key ID from header
        String kid = JwtTokenParser.extractKeyId(token);
        
        // Get public key from JWKS cache
        com.nimbusds.jose.jwk.RSAKey publicKey = jwksService.getPublicKey(kid);
        
        // Verify signature
        JwtSignatureVerifier.verifySignature(signedJWT, publicKey);
        
        // Get claims
        JWTClaimsSet claimsSet;
        try {
            claimsSet = signedJWT.getJWTClaimsSet();
        } catch (java.text.ParseException e) {
            log.error("Failed to parse JWT claims", e);
            throw new IllegalArgumentException("Invalid JWT claims format", e);
        }
        
        // Validate expiry
        JwtTokenParser.validateExpiry(claimsSet);
        
        // Validate issuer (optional)
        JwtTokenParser.validateIssuer(claimsSet, properties.getIssuer());
        
        return claimsSet;
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

