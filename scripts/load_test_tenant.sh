#!/bin/bash
# Load Test Script for Tenant Creation

URL="http://localhost:8182"
USERNAME="superadmin"
PASSWORD="SuperAdmin@123"

echo "1. Authenticating as Super Admin..."

# Login
RESPONSE=$(curl -s -X POST "$URL/api/v1/users/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\"}")

# Extract Token (Basic parsing)
TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "Login failed!"
  echo "Response: $RESPONSE"
  exit 1
fi

echo "Login successful. Token acquired."

# Loop for load testing
COUNT=10
echo "Starting load test: Creating $COUNT tenants..."

for i in $(seq 1 $COUNT); do
  TENANT_NAME="loadtest_tenant_$(date +%s)_$i"
  SUBDOMAIN="lt-$(date +%s)-$i"
  
  echo "Creating tenant $i of $COUNT: $TENANT_NAME (Subdomain: $SUBDOMAIN)"
  
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$URL/api/v1/tenant-onboarding/provision" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"tenantName\": \"$TENANT_NAME\",
      \"displayName\": \"Load Test Tenant $i\",
      \"adminEmail\": \"admin_$i@loadtest.com\",
      \"adminPassword\": \"StrongPass123!\",
      \"organizationType\": \"SCHOOL\",
      \"enableSubdomain\": true,
      \"subdomainPrefix\": \"$SUBDOMAIN\"
    }")
    
  if [ "$HTTP_CODE" -eq 201 ]; then
    echo "  Success (201)"
  else
    echo "  Failed ($HTTP_CODE)"
  fi
done

echo "Load test complete."
