#!/bin/sh

# Wait for API Gateway to start
echo "Waiting for API Gateway to be ready..."
sleep 15

# Create directory for logs
mkdir -p /tmp/logs

# Function to generate traffic
generate_traffic() {
  while true; do
    echo "Generating traffic to API Gateway..."
    
    # Test routes
    curl -s http://host.docker.internal:8080/local-test >> /tmp/logs/traffic.log 2>&1
    curl -s http://host.docker.internal:8080/httpbin/get >> /tmp/logs/traffic.log 2>&1
    curl -s http://host.docker.internal:8080/test >> /tmp/logs/traffic.log 2>&1
    
    # API endpoints that generate metrics
    curl -s http://host.docker.internal:8080/api/routes >> /tmp/logs/traffic.log 2>&1
    curl -s http://host.docker.internal:8080/api/metrics/summary >> /tmp/logs/traffic.log 2>&1
    
    # Sleep for 5 seconds
    sleep 5
  done
}

# Create initial routes
echo "Creating initial routes..."

# Create a route to httpbin.org
curl -X POST http://host.docker.internal:8080/api/routes \
  -H "Content-Type: application/json" \
  -d '{
    "routeId": "httpbin-route",
    "path": "/httpbin/**",
    "method": "GET",
    "uri": "http://httpbin.org",
    "enabled": true,
    "filters": ["StripPrefix=1"]
  }' >> /tmp/logs/setup.log 2>&1

# Create a test route
curl -X POST http://host.docker.internal:8080/api/routes \
  -H "Content-Type: application/json" \
  -d '{
    "routeId": "test-route",
    "path": "/test/**",
    "method": "GET",
    "uri": "http://host.docker.internal:8080/local-test",
    "enabled": true
  }' >> /tmp/logs/setup.log 2>&1

# Refresh routes
curl -X POST http://host.docker.internal:8080/api/routes/refresh >> /tmp/logs/setup.log 2>&1

# Start generating traffic
generate_traffic