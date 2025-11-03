# JWT Validation Starter

Shared library for JWT token validation across microservices.

## Features

- **JWT Token Validation**: Validate JWT tokens (signature, expiry, issuer)
- **JWKS Support**: Fetch and cache JSON Web Key Set from Identity service
- **Token Blacklisting**: Check Redis blacklist for revoked tokens
- **Dual APIs**: Provides both blocking (MVC) and reactive (WebFlux) implementations

## Usage

### For Blocking Services (MVC - e.g., user-profile)

```java
@Configuration
@Import(JwtValidationAutoConfiguration.class)
public class AppConfig {
    // Auto-configured beans available:
    // - BlockingJwtValidationService
    // - BlockingJwksService  
    // - BlockingSessionService
}
```

### For Reactive Services (WebFlux - e.g., Gateway)

```java
@Configuration
@Import(JwtValidationAutoConfiguration.class)
public class GatewayConfig {
    // Auto-configured beans available:
    // - ReactiveJwtValidationService
    // - ReactiveJwksService
    // - ReactiveSessionService
}
```

## Configuration

```yaml
jwt:
  identity-service-url: http://localhost:8081
  jwks-endpoint: /.well-known/jwks.json
  jwks-cache-refresh-interval-ms: 300000  # 5 minutes
  issuer: ecom-identity

redis:
  host: localhost
  port: 6379
```

## Architecture

- **Core Logic**: Shared validation logic (signature verification, expiry checks)
- **Blocking API**: For Spring MVC services (`BlockingJwtValidationService`)
- **Reactive API**: For Spring WebFlux services (`ReactiveJwtValidationService`)
- **Auto-Configuration**: Automatically configures appropriate beans based on classpath

