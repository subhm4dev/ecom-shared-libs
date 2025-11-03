package com.ecom.response;

import com.ecom.response.annotation.NoWrapResponse;
import com.ecom.response.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Automatically wraps successful controller responses in ApiResponse.
 * 
 * <p>This advice intercepts all controller responses and wraps them
 * in the standardized ApiResponse format, unless:
 * <ul>
 *   <li>The response is already an ApiResponse (to prevent double-wrapping)</li>
 *   <li>The endpoint is annotated with @NoWrapResponse</li>
 *   <li>The response is an ErrorResponse (errors are handled separately)</li>
 *   <li>The response is a ResponseEntity with error status code</li>
 * </ul>
 */
@ControllerAdvice(annotations = RestController.class)
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, 
                           Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip if method or class is annotated with @NoWrapResponse
        if (returnType.hasMethodAnnotation(NoWrapResponse.class) ||
            returnType.getContainingClass().isAnnotationPresent(NoWrapResponse.class)) {
            return false;
        }
        
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                 MethodParameter returnType,
                                 MediaType selectedContentType,
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request,
                                 ServerHttpResponse response) {
        
        // Don't wrap if already wrapped
        if (body instanceof ApiResponse) {
            return body;
        }
        
        // Don't wrap errors (ErrorResponse is handled by custom-error-starter)
        if (body != null && isErrorResponse(body.getClass())) {
            return body;
        }
        
        // If it's a ResponseEntity, check status code
        if (body instanceof ResponseEntity<?> responseEntity) {
            var status = responseEntity.getStatusCode();
            
            // Only wrap success responses (2xx)
            if (status.is2xxSuccessful()) {
                var bodyData = responseEntity.getBody();
                
                // Don't wrap if already an ApiResponse
                if (bodyData instanceof ApiResponse) {
                    return body;
                }
                
                // Don't wrap ErrorResponse
                if (bodyData != null && isErrorResponse(bodyData.getClass())) {
                    return body;
                }
                
                // Extract message from ResponseEntity body if it's a simple type
                String message = extractMessage(bodyData);
                
                return ResponseEntity
                    .status(status)
                    .headers(responseEntity.getHeaders())
                    .body(ApiResponse.success(bodyData, message));
            }
            
            // For non-2xx responses, return as-is (errors handled elsewhere)
            return body;
        }
        
        // For plain objects, wrap in ApiResponse
        String message = extractMessage(body);
        return ApiResponse.success(body, message);
    }
    
    /**
     * Checks if the given class is an ErrorResponse from the error package.
     * This avoids wrapping error responses which are handled separately.
     */
    private boolean isErrorResponse(Class<?> clazz) {
        String packageName = clazz.getPackageName();
        return packageName.equals("com.ecom.error.dto") && 
               clazz.getSimpleName().equals("ErrorResponse");
    }
    
    /**
     * Attempts to extract a message from the response body.
     * Looks for common patterns like objects with a "message" field.
     */
    private String extractMessage(Object body) {
        if (body == null) {
            return null;
        }
        
        // Try to extract message field via reflection
        try {
            var messageField = body.getClass().getDeclaredField("message");
            messageField.setAccessible(true);
            var messageValue = messageField.get(body);
            if (messageValue instanceof String) {
                return (String) messageValue;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // No message field, that's fine
        }
        
        return null;
    }
}

