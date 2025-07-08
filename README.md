# API Gateway

A flexible and scalable API Gateway built with Spring Cloud Gateway, supporting dynamic route management, advanced traffic control, and real-time monitoring.


# API Gateway

A flexible and scalable API Gateway built with Spring Cloud Gateway, supporting dynamic route management, advanced traffic control, and real-time monitoring.

## Features

### Implemented Features

- **Dynamic Route Management**: Create, update, delete and query routes via REST API
- **MongoDB Storage**: Route definitions persisted in MongoDB
- **Circuit Breaker**: Prevent cascading failures with configurable circuit breakers
- **Rate Limiting**: Control request rates to protect backend services
- **Timeout Handling**: Configure timeouts for backend service requests
- **Path Rewriting**: Rewrite request paths before forwarding to backend services
- **Metrics Collection**: Track request metrics in MongoDB
- **Prometheus Integration**: Expose metrics for Prometheus scraping
- **Grafana Dashboard**: Visualize API Gateway metrics in real-time
- **Request Logging**: Comprehensive logging of request/response details

### Planned Features

- **Authentication & Authorization**: OAuth2/JWT support
- **API Key Management**: Issue and validate API keys
- **Request Validation**: Validate incoming requests against schemas
- **Response Transformation**: Modify service responses
- **Service Discovery**: Integrate with service registry (Eureka/Consul)
- **Caching**: Response caching for improved performance
- **Custom Filters**: Plugin architecture for custom request/response filters
- **Advanced Analytics**: Detailed traffic analysis and reporting
- **Multi-tenancy**: Support for tenant isolation
- **API Documentation**: Auto-generated API documentation

## Tech Stack

- **Spring Boot/Cloud Gateway**: Core API Gateway functionality
- **Spring WebFlux**: Reactive programming model
- **MongoDB**: Route and metrics storage
- **Redis**: Rate limiting and distributed state
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- MongoDB (or use the Docker Compose setup)

### Running the API Gateway

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/api_gateway.git
   cd api_gateway
   ```

2. Build the project:
   ```bash
   cd api_gateway
   mvn clean package
   ```

3. Start the API Gateway:
   ```bash
   mvn spring-boot:run
   ```

4. The API Gateway will be available at http://localhost:8080

### Running the Dashboard

1. Start the monitoring dashboard:
   ```bash
   cd dashboard
   docker-compose up -d
   ```

2. Access Grafana at http://localhost:3000 (default credentials: admin/admin)

## API Documentation

### Route Management

- **Create a route**:
  ```bash
  curl -X POST http://localhost:8080/api/routes \
    -H "Content-Type: application/json" \
    -d '{
      "routeId": "example-route",
      "path": "/example/**",
      "method": "GET",
      "uri": "http://example-service:8080",
      "enabled": true
    }'
  ```

- **Get all routes**:
  ```bash
  curl -X GET http://localhost:8080/api/routes
  ```

- **Get a specific route**:
  ```bash
  curl -X GET http://localhost:8080/api/routes/{routeId}
  ```

- **Update a route**:
  ```bash
  curl -X PUT http://localhost:8080/api/routes/{routeId} \
    -H "Content-Type: application/json" \
    -d '{
      "path": "/updated-path/**",
      "enabled": true
    }'
  ```

- **Delete a route**:
  ```bash
  curl -X DELETE http://localhost:8080/api/routes/{routeId}
  ```

- **Refresh routes**:
  ```bash
  curl -X POST http://localhost:8080/api/routes/refresh
  ```

### Advanced Route Configuration

- **Circuit Breaker**:
  ```bash
  curl -X POST http://localhost:8080/api/routes \
    -H "Content-Type: application/json" \
    -d '{
      "routeId": "circuit-test",
      "path": "/circuit/**",
      "method": "GET",
      "uri": "http://service:8080",
      "enabled": true,
      "circuitBreaker": {
        "name": "circuitTest",
        "failureRateThreshold": 50,
        "slidingWindowSize": 10,
        "waitDurationInOpenState": "PT5S",
        "permittedCallsInHalfOpenState": 2,
        "automaticTransition": true,
        "fallbackUri": "forward:/local-test"
      }
    }'
  ```

- **Rate Limiter**:
  ```bash
  curl -X POST http://localhost:8080/api/routes \
    -H "Content-Type: application/json" \
    -d '{
      "routeId": "rate-limited",
      "path": "/limited/**",
      "method": "GET",
      "uri": "http://service:8080",
      "enabled": true,
      "rateLimiter": {
        "replenishRate": 1,
        "burstCapacity": 2,
        "requestedTokens": 1
      }
    }'
  ```


## License

This project is licensed under the MIT License - see the LICENSE file for details.