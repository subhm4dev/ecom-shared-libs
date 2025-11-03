package com.ecom.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT Validation Configuration Properties
 * 
 * <p>Configuration for JWT validation, JWKS endpoint, and Redis blacklist.
 * Loaded from application.yml under 'jwt' prefix.
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtValidationProperties {
    
    /**
     * Identity service URL for fetching JWKS
     */
    private String identityServiceUrl = "http://localhost:8081";
    
    /**
     * JWKS endpoint path
     */
    private String jwksEndpoint = "/.well-known/jwks.json";
    
    /**
     * JWKS cache refresh interval in milliseconds
     */
    private long jwksCacheRefreshIntervalMs = 300000; // 5 minutes
    
    /**
     * Expected JWT issuer (for validation)
     */
    private String issuer = "ecom-identity";
    
    /**
     * Redis key prefix for token blacklist
     */
    private String blacklistPrefix = "jwt:blacklist:";

    // Getters and setters
    public String getIdentityServiceUrl() {
        return identityServiceUrl;
    }

    public void setIdentityServiceUrl(String identityServiceUrl) {
        this.identityServiceUrl = identityServiceUrl;
    }

    public String getJwksEndpoint() {
        return jwksEndpoint;
    }

    public void setJwksEndpoint(String jwksEndpoint) {
        this.jwksEndpoint = jwksEndpoint;
    }

    public long getJwksCacheRefreshIntervalMs() {
        return jwksCacheRefreshIntervalMs;
    }

    public void setJwksCacheRefreshIntervalMs(long jwksCacheRefreshIntervalMs) {
        this.jwksCacheRefreshIntervalMs = jwksCacheRefreshIntervalMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getBlacklistPrefix() {
        return blacklistPrefix;
    }

    public void setBlacklistPrefix(String blacklistPrefix) {
        this.blacklistPrefix = blacklistPrefix;
    }
}

