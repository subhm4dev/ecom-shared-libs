package com.ecom.jwt.core;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

/**
 * Core JWT Token Parser
 * 
 * <p>Shared logic for parsing JWT tokens and extracting claims.
 * Used by both blocking and reactive implementations.
 */
public class JwtTokenParser {
    
    private static final Logger log = LoggerFactory.getLogger(JwtTokenParser.class);
    
    /**
     * Parse JWT token string into SignedJWT
     * 
     * @param token JWT token string
     * @return SignedJWT parsed token
     * @throws IllegalArgumentException if token is invalid
     */
    public static SignedJWT parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        
        try {
            return SignedJWT.parse(token);
        } catch (ParseException e) {
            log.error("Failed to parse JWT token", e);
            throw new IllegalArgumentException("Invalid JWT token format", e);
        }
    }
    
    /**
     * Extract token ID (jti) from token
     * 
     * @param token JWT token string
     * @return Token ID (jti), or hash of token as fallback
     */
    public static String extractTokenId(String token) {
        try {
            SignedJWT signedJWT = parseToken(token);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            if (jti == null || jti.isBlank()) {
                // Fallback: use token hash
                return String.valueOf(token.hashCode());
            }
            return jti;
        } catch (Exception e) {
            log.error("Failed to extract token ID", e);
            // Fallback: use token hash
            return String.valueOf(token.hashCode());
        }
    }
    
    /**
     * Extract Key ID (kid) from token header
     * 
     * @param token JWT token string
     * @return Key ID (kid)
     * @throws IllegalArgumentException if kid is missing
     */
    public static String extractKeyId(String token) {
        SignedJWT signedJWT = parseToken(token);
        String kid = signedJWT.getHeader().getKeyID();
        if (kid == null) {
            throw new IllegalArgumentException("JWT token missing Key ID (kid)");
        }
        return kid;
    }
    
    /**
     * Extract user ID from token claims
     * 
     * @param claims JWT claims set
     * @return User ID (from userId claim or subject)
     * @throws IllegalArgumentException if user ID is missing
     */
    public static String extractUserId(JWTClaimsSet claims) {
        // Try userId claim first, fallback to subject
        String userId = claims.getClaim("userId") != null
            ? claims.getClaim("userId").toString()
            : claims.getSubject();
        
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("JWT token missing user ID");
        }
        
        return userId;
    }
    
    /**
     * Extract tenant ID from token claims
     * 
     * @param claims JWT claims set
     * @return Tenant ID
     * @throws IllegalArgumentException if tenant ID is missing
     */
    public static String extractTenantId(JWTClaimsSet claims) {
        Object tenantIdObj = claims.getClaim("tenantId");
        if (tenantIdObj == null) {
            throw new IllegalArgumentException("JWT token missing tenant ID");
        }
        return tenantIdObj.toString();
    }
    
    /**
     * Extract roles from token claims
     * 
     * @param claims JWT claims set
     * @return List of roles (empty list if none)
     */
    @SuppressWarnings("unchecked")
    public static java.util.List<String> extractRoles(JWTClaimsSet claims) {
        Object rolesObj = claims.getClaim("roles");
        if (rolesObj instanceof java.util.List) {
            return (java.util.List<String>) rolesObj;
        }
        return java.util.List.of(); // Return empty list if no roles
    }
    
    /**
     * Validate token expiry
     * 
     * @param claims JWT claims set
     * @throws IllegalArgumentException if token is expired
     */
    public static void validateExpiry(JWTClaimsSet claims) {
        java.util.Date expirationTime = claims.getExpirationTime();
        if (expirationTime != null && expirationTime.before(new java.util.Date())) {
            throw new IllegalArgumentException("JWT token has expired");
        }
    }
    
    /**
     * Validate token issuer (optional check)
     * 
     * @param claims JWT claims set
     * @param expectedIssuer Expected issuer value
     */
    public static void validateIssuer(JWTClaimsSet claims, String expectedIssuer) {
        if (expectedIssuer == null || expectedIssuer.isBlank()) {
            return; // Skip validation if no expected issuer
        }
        
        String issuer = claims.getIssuer();
        if (issuer != null && !issuer.equals(expectedIssuer)) {
            log.warn("JWT token from unexpected issuer: expected={}, actual={}", expectedIssuer, issuer);
        }
    }
}

