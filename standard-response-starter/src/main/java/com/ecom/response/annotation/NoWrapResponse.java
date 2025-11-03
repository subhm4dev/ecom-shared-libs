package com.ecom.response.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to exclude an endpoint from automatic response wrapping.
 * 
 * <p>Use this when you need to return a response in a different format
 * (e.g., file downloads, streaming responses, custom JSON structures).
 * 
 * <p>Can be applied to:
 * <ul>
 *   <li>Controller methods - excludes only that method</li>
 *   <li>Controller classes - excludes all methods in that controller</li>
 * </ul>
 * 
 * <p>Example:
 * <pre>{@code
 * @GetMapping("/export")
 * @NoWrapResponse
 * public ResponseEntity<byte[]> exportData() {
 *     // This won't be wrapped in ApiResponse
 * }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoWrapResponse {
}

