package com.gateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document("apiMetrics")
public class ApiMetric {
    @Id
    private String id;
    private String fromService; // Source of the request (client or service)
    private String toService; // Target service
    private LocalDateTime timestamp; // When the request was made
    private long duration; // Response time in milliseconds
    private String routeId; // Route identifier
    private String method; // HTTP method (GET, POST, etc.)
    private String path; // Request path
    private String interactionType; // "USER_TO_APP" or "APP_TO_APP"
    private int statusCode; // HTTP status code
    private String queryParameters; // Request query parameters
    private String requestPayload; // Request body (for POST/PUT)
    private boolean success; // Whether the request was successful
    private String errorMessage; // Error message if failed
    private String userAgent; // User agent for user requests
    private String clientIp; // Client IP address
}