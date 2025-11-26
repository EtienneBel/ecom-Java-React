#!/bin/bash

#############################################################################
# BASELINE BENCHMARK SCRIPT (NO CACHE)
#
# This script runs on the nocache-baseline branch to measure
# true database-only performance (no Caffeine, no Redis).
#
# Usage:
#   ./benchmark-baseline.sh
#
# Prerequisites:
#   - MySQL running on localhost:3306
#   - Apache Bench (ab) installed
#############################################################################

set -e

export JAVA_HOME="${JAVA_HOME:-/Users/macbookpro/Library/Java/JavaVirtualMachines/temurin-21.0.8/Contents/Home}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'
BOLD='\033[1m'

# Config
BASE_URL="http://localhost:8080"
RESULTS_DIR="./benchmark-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Test parameters
REQUESTS=1000
CONCURRENCY=50
HIGH_LOAD_REQUESTS=5000
HIGH_LOAD_CONCURRENCY=100

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}${CYAN}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

print_section() {
    echo -e "${YELLOW}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_metric() {
    echo -e "  ${CYAN}$1:${NC} ${BOLD}$2${NC}"
}

stop_app() {
    local pid=$(lsof -ti:8080 2>/dev/null || true)
    if [ -n "$pid" ]; then
        kill -9 $pid 2>/dev/null || true
        sleep 2
    fi
}

start_app() {
    print_header "STARTING APPLICATION (NO CACHE)"

    stop_app

    print_section "Building JAR..."
    mvn clean package -DskipTests -q

    print_section "Starting Spring Boot (database-only, no caching)..."
    nohup $JAVA_HOME/bin/java -jar target/*.jar > "$RESULTS_DIR/app_baseline.log" 2>&1 &

    local app_pid=$!

    print_section "Waiting for application to start (PID: $app_pid)..."
    local max_wait=60
    local waited=0
    while ! curl -s "$BASE_URL/actuator/health" 2>/dev/null | grep -q "UP"; do
        sleep 2
        waited=$((waited + 2))
        if [ $waited -ge $max_wait ]; then
            echo -e "${RED}Application failed to start${NC}"
            exit 1
        fi
        echo -n "."
    done
    echo ""

    print_success "Application started (took ${waited}s)"
}

run_baseline_test() {
    print_header "BASELINE TEST (NO CACHE - Every request hits DB)"

    mkdir -p "$RESULTS_DIR"

    print_section "Running $REQUESTS requests (concurrency: $CONCURRENCY)..."

    ab -n $REQUESTS -c $CONCURRENCY -q "$BASE_URL/api/products/1" > "$RESULTS_DIR/ab_baseline.txt" 2>&1

    local avg_response=$(grep "Time per request:" "$RESULTS_DIR/ab_baseline.txt" | head -1 | awk '{print $4}' | cut -d'.' -f1)
    local rps=$(grep "Requests per second:" "$RESULTS_DIR/ab_baseline.txt" | awk '{print $4}')
    local p50=$(grep "50%" "$RESULTS_DIR/ab_baseline.txt" | awk '{print $2}')
    local p95=$(grep "95%" "$RESULTS_DIR/ab_baseline.txt" | awk '{print $2}')
    local p99=$(grep "99%" "$RESULTS_DIR/ab_baseline.txt" | awk '{print $2}')

    print_section "Baseline Results"
    print_metric "Total Requests" "$REQUESTS"
    print_metric "Concurrency" "$CONCURRENCY"
    print_metric "Average Response Time" "${avg_response}ms"
    print_metric "Requests/Second" "$rps"
    print_metric "P50 Latency" "${p50}ms"
    print_metric "P95 Latency" "${p95}ms"
    print_metric "P99 Latency" "${p99}ms"
    print_metric "Database Queries" "$REQUESTS (100% - no cache)"

    echo "$avg_response" > "$RESULTS_DIR/baseline_avg.txt"
    echo "$rps" > "$RESULTS_DIR/baseline_rps.txt"

    BASELINE_AVG=$avg_response
    BASELINE_RPS=$rps
}

run_high_load_test() {
    print_header "HIGH LOAD TEST ($HIGH_LOAD_REQUESTS requests @ $HIGH_LOAD_CONCURRENCY concurrent)"

    print_section "Running high load test..."

    ab -n $HIGH_LOAD_REQUESTS -c $HIGH_LOAD_CONCURRENCY -q "$BASE_URL/api/products/1" > "$RESULTS_DIR/ab_baseline_highload.txt" 2>&1

    local avg_response=$(grep "Time per request:" "$RESULTS_DIR/ab_baseline_highload.txt" | head -1 | awk '{print $4}')
    local rps=$(grep "Requests per second:" "$RESULTS_DIR/ab_baseline_highload.txt" | awk '{print $4}')
    local failed=$(grep "Failed requests:" "$RESULTS_DIR/ab_baseline_highload.txt" | awk '{print $3}')
    local p95=$(grep "95%" "$RESULTS_DIR/ab_baseline_highload.txt" | awk '{print $2}')

    print_section "High Load Results"
    print_metric "Total Requests" "$HIGH_LOAD_REQUESTS"
    print_metric "Concurrency" "$HIGH_LOAD_CONCURRENCY"
    print_metric "Average Response Time" "${avg_response}ms"
    print_metric "Requests/Second" "$rps"
    print_metric "P95 Latency" "${p95}ms"
    print_metric "Failed Requests" "$failed"

    echo "$rps" > "$RESULTS_DIR/baseline_highload_rps.txt"
}

generate_report() {
    print_header "BASELINE BENCHMARK REPORT"

    local avg=$(cat "$RESULTS_DIR/baseline_avg.txt" 2>/dev/null || echo "N/A")
    local rps=$(cat "$RESULTS_DIR/baseline_rps.txt" 2>/dev/null || echo "N/A")
    local highload_rps=$(cat "$RESULTS_DIR/baseline_highload_rps.txt" 2>/dev/null || echo "N/A")

    echo ""
    echo -e "${BOLD}${RED}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}${RED}║           BASELINE BENCHMARK (NO CACHE)                       ║${NC}"
    echo -e "${BOLD}${RED}╠═══════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${BOLD}${RED}║${NC}                                                               ${BOLD}${RED}║${NC}"
    echo -e "${BOLD}${RED}║${NC}  Average Response Time:  ${BOLD}${avg}ms${NC}                              ${BOLD}${RED}║${NC}"
    echo -e "${BOLD}${RED}║${NC}  Throughput:             ${BOLD}${rps} req/s${NC}                       ${BOLD}${RED}║${NC}"
    echo -e "${BOLD}${RED}║${NC}  High Load Throughput:   ${BOLD}${highload_rps} req/s${NC}                       ${BOLD}${RED}║${NC}"
    echo -e "${BOLD}${RED}║${NC}  Database Queries:       ${BOLD}100%${NC} (every request)               ${BOLD}${RED}║${NC}"
    echo -e "${BOLD}${RED}║${NC}                                                               ${BOLD}${RED}║${NC}"
    echo -e "${BOLD}${RED}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    cat > "$RESULTS_DIR/BASELINE_REPORT_${TIMESTAMP}.md" << EOF
# Baseline Benchmark Report (NO CACHE)

**Date:** $(date '+%Y-%m-%d %H:%M:%S')
**Branch:** nocache-baseline
**Environment:** Spring Boot 3.3.0, Java 21, MySQL 8.0

---

## Summary

| Metric | Value |
|--------|-------|
| Average Response Time | **${avg}ms** |
| Throughput | **${rps} req/s** |
| High Load Throughput | **${highload_rps} req/s** |
| Database Queries | **100%** (no caching) |

---

## Test Configuration

- **Requests:** $REQUESTS
- **Concurrency:** $CONCURRENCY
- **High Load Requests:** $HIGH_LOAD_REQUESTS
- **High Load Concurrency:** $HIGH_LOAD_CONCURRENCY

---

## Notes

This is the **baseline** measurement with:
- NO Caffeine (L1 cache)
- NO Redis (L2 cache)
- Every request goes directly to MySQL

Compare these results with the \`master\` branch (with caching) to measure improvement.

---

*Generated on $(date)*
EOF

    print_success "Report saved to: $RESULTS_DIR/BASELINE_REPORT_${TIMESTAMP}.md"
}

cleanup() {
    print_section "Cleaning up..."
    stop_app
}

trap cleanup EXIT

main() {
    echo ""
    echo -e "${BOLD}${RED}"
    echo "  ╔═══════════════════════════════════════════════════════════╗"
    echo "  ║     BASELINE BENCHMARK (NO CACHE)                         ║"
    echo "  ║     Every request goes directly to MySQL                  ║"
    echo "  ╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"

    mkdir -p "$RESULTS_DIR"

    start_app
    run_baseline_test
    run_high_load_test
    generate_report

    print_header "BENCHMARK COMPLETE"
    echo -e "${GREEN}Results saved to: ${BOLD}$RESULTS_DIR/${NC}"
}

main "$@"
