package com.ecom.response.dto;

import java.time.Instant;

/**
 * Standardized API response wrapper for all successful responses.
 * 
 * <p>This ensures all microservices return a consistent JSON structure:
 * <pre>{@code
 * {
 *   "success": true,
 *   "data": { ... },
 *   "message": "Optional success message",
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 * }</pre>
 * 
 * @param <T> The type of data being returned
 */
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    Instant timestamp
) {
    /**
     * Creates a successful response with data and optional message.
     * 
     * @param data The response data
     * @param message Optional success message (can be null)
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now());
    }
    
    /**
     * Creates a successful response with data only.
     * 
     * @param data The response data
     * @return ApiResponse with success=true and null message
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, null);
    }
}

