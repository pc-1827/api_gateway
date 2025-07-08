#!/bin/bash
echo "Generating traffic to API Gateway..."
for i in {1..30}; do
  echo "Request $i"
  curl -s http://localhost:8080/httpbin/get > /dev/null
  curl -s http://localhost:8080/test > /dev/null
  sleep 1
done
echo "Traffic generation complete"
