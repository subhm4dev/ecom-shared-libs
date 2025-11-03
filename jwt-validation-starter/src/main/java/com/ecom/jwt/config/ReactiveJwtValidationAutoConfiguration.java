package com.ecom.jwt.config;

import com.ecom.jwt.jwks.ReactiveJwksService;
import com.ecom.jwt.reactive.ReactiveJwtValidationService;
import com.ecom.jwt.session.ReactiveSessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-Configuration for Reactive JWT Validation
 * 
 * <p>Configures reactive JWT validation services for Spring WebFlux services (Gateway).
 * Only activates when:
 * - WebFlux is on classpath
 * - ReactiveRedisTemplate is available
 */
@Configuration
@ConditionalOnClass({org.springframework.web.reactive.DispatcherHandler.class, WebClient.class})
@ConditionalOnProperty(prefix = "gateway.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JwtValidationProperties.class)
@EnableScheduling // Required for scheduled JWKS cache refresh
public class ReactiveJwtValidationAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public ReactiveJwksService reactiveJwksService(
            WebClient webClient,
            JwtValidationProperties properties) {
        return new ReactiveJwksService(webClient, properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ReactiveRedisTemplate.class)
    public ReactiveSessionService reactiveSessionService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            JwtValidationProperties properties) {
        return new ReactiveSessionService(redisTemplate, properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ReactiveJwtValidationService reactiveJwtValidationService(
            ReactiveJwksService jwksService,
            ReactiveSessionService sessionService,
            JwtValidationProperties properties) {
        return new ReactiveJwtValidationService(jwksService, sessionService, properties);
    }
}

