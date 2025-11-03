package com.ecom.response;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for standardized API responses.
 * 
 * <p>Automatically registers ResponseWrapperAdvice when this starter
 * is included as a dependency. No additional configuration needed.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class StandardResponseAutoConfiguration {
    
    @Bean
    public ResponseWrapperAdvice responseWrapperAdvice() {
        return new ResponseWrapperAdvice();
    }
}

