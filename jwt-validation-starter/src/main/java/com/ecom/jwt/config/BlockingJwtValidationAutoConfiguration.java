package com.ecom.jwt.config;

import com.ecom.httpclient.client.ResilientWebClient;
import com.ecom.jwt.blocking.BlockingJwtValidationService;
import com.ecom.jwt.jwks.BlockingJwksService;
import com.ecom.jwt.session.BlockingSessionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-Configuration for Blocking JWT Validation
 * 
 * <p>Configures blocking JWT validation services for Spring MVC services.
 * Only activates when:
 * - Spring MVC is on classpath (not WebFlux)
 * - RedisTemplate is available
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnMissingBean(com.ecom.jwt.reactive.ReactiveJwtValidationService.class)
@ConditionalOnProperty(prefix = "jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JwtValidationProperties.class)
@EnableScheduling // Required for scheduled JWKS cache refresh
public class BlockingJwtValidationAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public BlockingJwksService blockingJwksService(
            ResilientWebClient resilientWebClient,
            JwtValidationProperties properties) {
        return new BlockingJwksService(resilientWebClient, properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisTemplate.class)
    public BlockingSessionService blockingSessionService(
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            JwtValidationProperties properties) {
        return new BlockingSessionService(redisTemplate, properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public BlockingJwtValidationService blockingJwtValidationService(
            BlockingJwksService jwksService,
            BlockingSessionService sessionService,
            JwtValidationProperties properties) {
        return new BlockingJwtValidationService(jwksService, sessionService, properties);
    }
}

