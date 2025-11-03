# JWT Validation Starter

A Spring Boot starter library that provides JWT token validation for microservices. This library handles JWT signature verification using JWKS (JSON Web Key Set) and token blacklist checking via Redis.

## Features

- ✅ Automatic JWT signature verification using JWKS
- ✅ Token blacklist checking via Redis
- ✅ Support for both blocking (Spring MVC) and reactive (WebFlux) services
- ✅ Automatic JWKS caching and refresh
- ✅ Integrated with `http-client-starter` for resilient JWKS fetching
- ✅ Auto-configuration - works out of the box

## Table of Contents

- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Usage Examples](#usage-examples)
- [Blocking Service (Spring MVC)](#blocking-service-spring-mvc)
- [Reactive Service (WebFlux)](#reactive-service-webflux)
- [Public Paths](#public-paths)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

### 1. Add Dependency

Add `jwt-validation-starter` to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.ecom</groupId>
    <artifactId>jwt-validation-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

**Also ensure you have:**
- `http-client-starter` (for JWKS fetching)
- `spring-boot-starter-data-redis` (for blocking services)
- `spring-boot-starter-data-redis-reactive` (for reactive services)

### 2. Configure JWT Validation

Add configuration to your `application.yml`:

```yaml
# JWT Validation Configuration
jwt:
  enabled: true  # Enable JWT validation (default: true)
  identity-service-url: http://localhost:8081  # Identity service URL
  jwks-endpoint: /.well-known/jwks.json  # JWKS endpoint path
  jwks-cache-refresh-interval-ms: 300000  # 5 minutes (default)
  issuer: ecom-iam  # JWT issuer (optional)
```

### 3. Configure Redis

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. Create Security Filter

Create a JWT authentication filter to validate tokens and extract user context.

**For Spring MVC (Blocking Services):**

```java
package com.ecom.yourservice.security;

import com.ecom.jwt.blocking.BlockingJwtValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final BlockingJwtValidationService jwtValidationService;
    private final PublicPathMatcher publicPathMatcher;
    
    public JwtAuthenticationFilter(
            BlockingJwtValidationService jwtValidationService,
            List<String> publicPaths) {
        this.jwtValidationService = jwtValidationService;
        this.publicPathMatcher = new PublicPathMatcher(publicPaths);
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Skip validation for public paths
        if (publicPathMatcher.isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract token from Authorization header
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid Authorization header\"}");
            return;
        }
        
        String token = authorization.substring(7); // Remove "Bearer " prefix
        
        try {
            // Validate token (signature, expiry, blacklist)
            var claims = jwtValidationService.validateToken(token);
            
            // Extract user context
            String userId = jwtValidationService.extractUserId(claims);
            String tenantId = jwtValidationService.extractTenantId(claims);
            List<String> roles = jwtValidationService.extractRoles(claims);
            
            // Set authentication in SecurityContext
            Authentication authentication = new JwtAuthenticationToken(userId, tenantId, roles);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } catch (IllegalArgumentException e) {
            // JWT validation failed
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                String.format("{\"error\":\"UNAUTHORIZED\",\"message\":\"%s\"}", 
                    e.getMessage().replace("\"", "\\\""))
            );
        }
    }
}
```

### 5. Configure Security

```java
package com.ecom.yourservice.config;

import com.ecom.jwt.blocking.BlockingJwtValidationService;
import com.ecom.yourservice.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BlockingJwtValidationService jwtValidationService) throws Exception {
        
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/public/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtValidationService, getPublicPaths()),
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    private List<String> getPublicPaths() {
        return List.of(
            "/api/v1/public/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
        );
    }
}
```

---

## Architecture

### How It Works

1. **JWT Token Validation Flow:**
   ```
   Request → JwtAuthenticationFilter → BlockingJwtValidationService
                                        ↓
                          1. Parse JWT token
                          2. Fetch JWKS from Identity Service (cached)
                          3. Verify signature
                          4. Check expiry
                          5. Check blacklist in Redis
                          6. Extract claims
   ```

2. **JWKS Caching:**
   - JWKS is fetched from Identity service on first request
   - Cached in memory with configurable refresh interval
   - Automatically refreshed in background

3. **Token Blacklist:**
   - Logged-out tokens are stored in Redis
   - Format: `jwt:blacklist:{tokenHash}`
   - TTL matches token expiry

### Auto-Configuration

The library automatically configures:

- **Blocking Services (Spring MVC):**
  - `BlockingJwtValidationService`
  - `BlockingJwksService`
  - `BlockingSessionService`

- **Reactive Services (WebFlux):**
  - `ReactiveJwtValidationService`
  - `ReactiveJwksService`
  - `ReactiveSessionService`

**Activation Conditions:**
- Blocking: MVC on classpath + `jwt.enabled=true` + `gateway.jwt.enabled!=true`
- Reactive: WebFlux on classpath + `gateway.jwt.enabled=true`

---

## Configuration

### Required Properties

```yaml
jwt:
  enabled: true
  identity-service-url: http://localhost:8081
  jwks-endpoint: /.well-known/jwks.json
```

### Optional Properties

```yaml
jwt:
  jwks-cache-refresh-interval-ms: 300000  # 5 minutes (default)
  issuer: ecom-iam  # JWT issuer validation
```

### Redis Configuration

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: # optional
      database: 0
```

### Public Paths

Configure public (unprotected) paths in your security configuration:

```java
private List<String> getPublicPaths() {
    return List.of(
        "/api/v1/auth/**",      # Authentication endpoints
        "/api/v1/public/**",    # Public API endpoints
        "/actuator/**",         # Spring Boot Actuator
        "/swagger-ui/**",       # Swagger UI
        "/v3/api-docs/**"       # OpenAPI docs
    );
}
```

---

## Usage Examples

### Extract User Context in Controller

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<Product>> getProducts() {
        // Get user ID from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName(); // userId is stored as principal name
        
        // Or use custom JwtAuthenticationToken
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String userId = jwtAuth.getUserId();
            String tenantId = jwtAuth.getTenantId();
            List<String> roles = jwtAuth.getRoles();
        }
        
        // Your business logic
        return ResponseEntity.ok(productService.getProducts(userId));
    }
}
```

### Custom Authentication Token

```java
package com.ecom.yourservice.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    
    private final String userId;
    private final String tenantId;
    private final List<String> roles;
    
    public JwtAuthenticationToken(String userId, String tenantId, List<String> roles) {
        super(roles.stream()
            .map(role -> "ROLE_" + role)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList()));
        this.userId = userId;
        this.tenantId = tenantId;
        this.roles = roles;
        setAuthenticated(true);
    }
    
    @Override
    public Object getCredentials() {
        return null; // JWT token is not stored
    }
    
    @Override
    public Object getPrincipal() {
        return userId; // User ID is the principal
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public List<String> roles() {
        return roles;
    }
}
```

### Helper Method to Extract User Context

```java
@RestController
public abstract class BaseController {
    
    protected String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getUserId();
        }
        return auth.getName();
    }
    
    protected String getTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getTenantId();
        }
        return null;
    }
    
    protected List<String> getRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.roles();
        }
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(authority -> authority.replace("ROLE_", ""))
            .collect(Collectors.toList());
    }
}
```

---

## Blocking Service (Spring MVC)

### Complete Example

**1. Dependencies (`pom.xml`):**
```xml
<dependency>
    <groupId>com.ecom</groupId>
    <artifactId>jwt-validation-starter</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.ecom</groupId>
    <artifactId>http-client-starter</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**2. Configuration (`application.yml`):**
```yaml
jwt:
  enabled: true
  identity-service-url: ${IDENTITY_SERVICE_URL:http://localhost:8081}
  jwks-endpoint: /.well-known/jwks.json
  jwks-cache-refresh-interval-ms: 300000

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

**3. Security Filter** (see example above)

**4. Security Configuration** (see example above)

---

## Reactive Service (WebFlux)

For reactive services (like Gateway), use `ReactiveJwtValidationService`:

```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    private final ReactiveJwtValidationService jwtValidationService;
    private final PublicPathMatcher publicPathMatcher;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        if (publicPathMatcher.isPublicPath(path)) {
            return chain.filter(exchange);
        }
        
        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return handleUnauthorized(exchange, "Missing or invalid Authorization header");
        }
        
        String token = authorization.substring(7);
        
        return jwtValidationService.validateToken(token)
            .flatMap(claims -> {
                String userId = jwtValidationService.extractUserId(claims);
                String tenantId = jwtValidationService.extractTenantId(claims);
                List<String> roles = jwtValidationService.extractRoles(claims);
                
                // Add headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Tenant-Id", tenantId)
                    .header("X-Roles", String.join(",", roles))
                    .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            })
            .onErrorResume(IllegalArgumentException.class, error -> 
                handleUnauthorized(exchange, error.getMessage()));
    }
    
    @Override
    public int getOrder() {
        return -100; // High precedence
    }
}
```

---

## Public Paths

### Path Matching Utility

```java
package com.ecom.yourservice.security;

import java.util.List;
import java.util.regex.Pattern;

public class PublicPathMatcher {
    
    private final List<Pattern> patterns;
    
    public PublicPathMatcher(List<String> publicPaths) {
        this.patterns = publicPaths.stream()
            .map(path -> path.replace("**", ".*").replace("*", "[^/]*"))
            .map(Pattern::compile)
            .collect(Collectors.toList());
    }
    
    public boolean isPublicPath(String path) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(path).matches());
    }
}
```

---

## Troubleshooting

### Issue: `BlockingJwtValidationService` not found

**Solution:** Ensure:
- `jwt.enabled=true` in `application.yml`
- `gateway.jwt.enabled` is NOT set to `true` (for blocking services)
- `http-client-starter` dependency is included
- `spring-boot-starter-data-redis` is included

### Issue: `RedisTemplate` bean not found

**Solution:** 
- Ensure `spring-boot-starter-data-redis` is on classpath
- Configure Redis connection in `application.yml`
- For blocking services, ensure `RedisTemplate` bean exists (auto-configured by Spring Boot)

### Issue: JWKS fetch fails

**Solution:**
- Verify Identity service is running
- Check `jwt.identity-service-url` is correct
- Check network connectivity
- Verify JWKS endpoint is accessible: `{identity-service-url}/.well-known/jwks.json`

### Issue: Token validation fails

**Solution:**
- Verify token is not expired
- Check token signature (JWKS must be accessible)
- Verify token is not blacklisted (check Redis)
- Ensure token issuer matches configuration (if configured)

### Issue: Public paths still require authentication

**Solution:**
- Verify path patterns match exactly (use `**` for wildcard)
- Check SecurityFilterChain configuration
- Ensure public paths are added before `anyRequest().authenticated()`

---

## Best Practices

1. **Always validate tokens server-side** - Don't trust client-provided user context
2. **Use @PreAuthorize for method-level security**:
   ```java
   @PreAuthorize("hasRole('ADMIN')")
   @PreAuthorize("hasAnyRole('CUSTOMER', 'SELLER')")
   ```
3. **Extract user context from SecurityContext** - Don't pass user ID as request parameter
4. **Use tenant isolation** - Always filter data by `tenantId`
5. **Log authentication failures** - Helps with debugging and security monitoring
6. **Monitor JWKS cache refresh** - Ensure JWKS is being refreshed periodically

---

## API Reference

### BlockingJwtValidationService

```java
public interface BlockingJwtValidationService {
    // Validate token (signature, expiry, blacklist)
    JWTClaimsSet validateToken(String token) throws IllegalArgumentException;
    
    // Extract user context
    String extractUserId(JWTClaimsSet claims);
    String extractTenantId(JWTClaimsSet claims);
    List<String> extractRoles(JWTClaimsSet claims);
}
```

### ReactiveJwtValidationService

```java
public interface ReactiveJwtValidationService {
    // Validate token (reactive)
    Mono<JWTClaimsSet> validateToken(String token);
    
    // Extract user context
    String extractUserId(JWTClaimsSet claims);
    String extractTenantId(JWTClaimsSet claims);
    List<String> extractRoles(JWTClaimsSet claims);
}
```

---

## Example Project Structure

```
ecom-your-service/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/ecom/yourservice/
│       │       ├── config/
│       │       │   └── SecurityConfig.java
│       │       ├── security/
│       │       │   ├── JwtAuthenticationFilter.java
│       │       │   ├── JwtAuthenticationToken.java
│       │       │   └── PublicPathMatcher.java
│       │       └── controller/
│       │           └── YourController.java
│       └── resources/
│           └── application.yml
└── pom.xml
```

---

## Support

For issues or questions:
- Check the troubleshooting section above
- Review the example implementations
- Check Identity service logs for JWKS issues
- Verify Redis connectivity for blacklist checks

---

**Version:** 1.0.0  
**Last Updated:** 2025-11-03
