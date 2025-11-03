# HTTP Client Starter

Standardized HTTP client for all e-commerce microservices with built-in resilience patterns.

## Features

✅ **WebClient** (reactive, non-blocking)  
✅ **Circuit Breaker** (fails fast when service is down)  
✅ **Retry Mechanism** (automatic retries on transient failures)  
✅ **Rate Limiting** (prevents API abuse)  
✅ **Configurable Timeouts** (connect, read, write, response)  
✅ **Service-Specific Configuration** (per-service customization)

## Usage

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.ecom</groupId>
    <artifactId>http-client-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Inject ResilientWebClient

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final ResilientWebClient resilientWebClient;
    
    public void callExternalService() {
        WebClient webClient = resilientWebClient.create("service-name", "http://localhost:8080");
        
        String response = webClient
            .get()
            .uri("/api/endpoint")
            .retrieve()
            .bodyToMono(String.class)
            .block(); // or use reactive chain
    }
}
```

### 3. Configure in application.yml

```yaml
http-client:
  default-timeout: PT5S
  connect-timeout: PT2S
  read-timeout: PT5S
  circuit-breaker:
    enabled: true
    failure-rate-threshold: 50.0
    wait-duration-in-open-state: PT60S
  retry:
    enabled: true
    max-attempts: 3
    wait-duration: PT1S
  rate-limiter:
    enabled: true
    limit-for-period: 100
    limit-refresh-period: PT1M
  # Service-specific configs
  services:
    identity-service:
      timeout: PT10S
      circuit-breaker:
        failure-rate-threshold: 30.0
```

## Configuration Options

### Timeouts
- `default-timeout`: Default timeout for all requests
- `connect-timeout`: Time to establish connection
- `read-timeout`: Time waiting for response
- `write-timeout`: Time waiting to send request
- `response-timeout`: Total time for request

### Circuit Breaker
- `failure-rate-threshold`: Percentage of failures before opening circuit (0-100)
- `wait-duration-in-open-state`: Time to wait before retrying after circuit opens
- `ring-buffer-size-in-closed-state`: Number of calls to evaluate failure rate

### Retry
- `max-attempts`: Maximum number of retry attempts
- `wait-duration`: Wait time between retries
- Automatically retries on: Network errors, 5xx server errors

### Rate Limiter
- `limit-for-period`: Maximum requests allowed in period
- `limit-refresh-period`: Time period for rate limit
- `timeout-duration`: Max wait time for permission

## Service-Specific Configuration

You can override defaults for specific services:

```yaml
http-client:
  services:
    identity-service:
      timeout: PT10S
      circuit-breaker:
        failure-rate-threshold: 30.0  # More sensitive
    payment-service:
      timeout: PT30S  # Longer timeout for payments
      retry:
        max-attempts: 5  # More retries for critical service
```

## Benefits

1. **Standardized** - Same HTTP client across all services
2. **Resilient** - Built-in circuit breaker, retry, rate limiting
3. **Configurable** - Per-service customization
4. **Production-Ready** - Proper timeouts, error handling
5. **No Code Duplication** - One starter for all services

## Example: User Profile Service

```java
@Service
@RequiredArgsConstructor
public class JwksService {
    private final ResilientWebClient resilientWebClient;
    
    private WebClient webClient; // Lazy init
    
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = resilientWebClient.create("identity-service", "http://localhost:8081");
        }
        return webClient;
    }
    
    public void fetchJwks() {
        String jwks = getWebClient()
            .get()
            .uri("/.well-known/jwks.json")
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
}
```

