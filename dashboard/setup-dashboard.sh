#!/bin/bash

echo "Setting up API Gateway dashboard..."

# Check if API Gateway is running
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
  echo "⚠️ API Gateway doesn't seem to be running. Make sure it's started first."
  echo "  You can start it with: cd ../api_gateway && ./mvnw spring-boot:run"
  read -p "Continue anyway? (y/n) " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
  fi
fi

# Start Docker containers
echo "Starting Prometheus and Grafana containers..."
docker compose down
docker compose up -d

echo
echo "✅ Dashboard setup complete!"
echo "🔍 Prometheus is available at: http://localhost:9090"
echo "📊 Grafana is available at: http://localhost:3000"
echo "  Login with admin/admin"
echo
echo "🔗 The API Gateway dashboard should be automatically available in Grafana"
echo "  If not, check Prometheus targets at: http://localhost:9090/targets"
echo "  to ensure API Gateway metrics are being scraped correctly."
echo