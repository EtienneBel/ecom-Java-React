#!/bin/bash

# load-test.sh
# Load testing script to demonstrate cache performance improvements
# Uses Apache Bench (ab) or curl

echo "🔥 E-Commerce Cache Solution - Load Testing"
echo "=============================================="
echo ""

BASE_URL="http://localhost:8080/api/products"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Apache Bench is installed
if ! command -v ab &> /dev/null; then
    echo -e "${YELLOW}⚠️  Apache Bench (ab) not found. Installing...${NC}"
    echo "Run: sudo apt-get install apache2-utils (Ubuntu/Debian)"
    echo "Or: brew install apache-bench (macOS)"
    exit 1
fi

# Function to run load test
run_load_test() {
    local endpoint=$1
    local description=$2
    local requests=$3
    local concurrency=$4

    echo ""
    echo -e "${GREEN}📊 Testing: $description${NC}"
    echo "Endpoint: $endpoint"
    echo "Requests: $requests | Concurrency: $concurrency"
    echo "---"

    ab -n $requests -c $concurrency -g /tmp/ab-results.tsv "$endpoint" 2>&1 | \
    grep -E "(Requests per second|Time per request|Failed requests|Percentage of the requests)" || \
    ab -n $requests -c $concurrency "$endpoint"

    echo ""
}

# Wait for application to be ready
echo "⏳ Waiting for application to be ready..."
until curl -s http://localhost:8080/actuator/health | grep -q "UP"; do
    echo "  Waiting for application..."
    sleep 2
done
echo -e "${GREEN}✅ Application is ready!${NC}"
echo ""

# Scenario 1: Cold Cache (First run - will hit database)
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  SCENARIO 1: COLD CACHE (Database Queries)${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"

run_load_test "$BASE_URL/1" "Get Product by ID (Cold Cache)" 100 10
run_load_test "$BASE_URL" "Get All Products (Cold Cache)" 50 5
run_load_test "$BASE_URL/category/Electronics" "Get by Category (Cold Cache)" 50 5

echo ""
echo -e "${GREEN}⏳ Waiting 3 seconds for cache to warm up...${NC}"
sleep 3

# Scenario 2: Warm Cache (Cache hits)
echo ""
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  SCENARIO 2: WARM CACHE (Cache Hits)${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"

run_load_test "$BASE_URL/1" "Get Product by ID (Warm Cache)" 1000 50
run_load_test "$BASE_URL" "Get All Products (Warm Cache)" 500 25
run_load_test "$BASE_URL/category/Electronics" "Get by Category (Warm Cache)" 500 25

# Scenario 3: High Concurrency Test
echo ""
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  SCENARIO 3: HIGH CONCURRENCY TEST${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"

run_load_test "$BASE_URL/1" "Extreme Load (Cached)" 5000 100

# Scenario 4: Mixed Load (Simulate Real Traffic)
echo ""
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  SCENARIO 4: MIXED LOAD PATTERN${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"

echo "Simulating real e-commerce traffic pattern..."
for i in {1..10}; do
    curl -s "$BASE_URL/$i" > /dev/null &
    curl -s "$BASE_URL/category/Electronics" > /dev/null &
    curl -s "$BASE_URL/search?keyword=phone" > /dev/null &
done
wait

echo -e "${GREEN}✅ Mixed load completed${NC}"

# Display Metrics
echo ""
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  📊 METRICS SUMMARY${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"
echo ""
echo "📈 View detailed metrics at:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana:    http://localhost:3000 (admin/admin)"
echo "  - Actuator:   http://localhost:8080/actuator/prometheus"
echo ""
echo "🔍 Key Metrics to Check:"
echo "  - cache_hit_total          (should be high)"
echo "  - cache_miss_total         (should be low)"
echo "  - database_query_total     (should decrease over time)"
echo "  - http_server_requests     (response times)"
echo ""

# Fetch and display some metrics
echo -e "${GREEN}Current Cache Statistics:${NC}"
curl -s http://localhost:8080/actuator/metrics/cache.hit | \
    grep -E "(name|value)" | head -10 || echo "  (Run load tests first)"

echo ""
echo -e "${GREEN}✅ Load testing complete!${NC}"
echo ""
echo -e "${YELLOW}💡 TIP: Run this script multiple times to see cache warming effect${NC}"
echo -e "${YELLOW}💡 TIP: Check Grafana dashboards for visual comparison${NC}"
echo ""