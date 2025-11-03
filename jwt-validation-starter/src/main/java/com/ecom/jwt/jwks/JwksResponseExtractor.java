package com.ecom.jwt.jwks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWKS Response Extractor
 * 
 * <p>Extracts JWKS JSON from Identity service's wrapped ApiResponse format.
 * Handles both wrapped (ApiResponse) and unwrapped (raw JWKS) responses.
 */
public class JwksResponseExtractor {
    
    private static final Logger log = LoggerFactory.getLogger(JwksResponseExtractor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Extract JWKS JSON from wrapped ApiResponse
     * 
     * <p>Identity service wraps JWKS in standard ApiResponse format:
     * {"success": true, "data": {"keys": [...]}}
     * 
     * <p>This method extracts just the "data" part which contains the actual JWKS.
     * 
     * @param responseBody Raw response body from Identity service
     * @return JWKS JSON string (either extracted from wrapper or as-is)
     */
    public static String extractJwksFromResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalArgumentException("Empty JWKS response");
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // Check if wrapped in ApiResponse format
            if (rootNode.has("data") && rootNode.get("data").has("keys")) {
                // Return just the data part (JWKS object)
                return objectMapper.writeValueAsString(rootNode.get("data"));
            }
            
            // If not wrapped, return as-is (assume raw JWKS format)
            return responseBody;
        } catch (Exception e) {
            log.warn("Failed to parse response as JSON, assuming raw JWKS format: {}", e.getMessage());
            // If parsing fails, assume it's already in raw JWKS format
            return responseBody;
        }
    }
}

