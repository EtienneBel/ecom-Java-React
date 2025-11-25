#!/bin/bash

# manage.sh - Project management script

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_banner() {
    echo -e "${GREEN}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║     E-Commerce Distributed Cache Solution Manager            ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

show_help() {
    echo "Usage: ./manage.sh [command]"
    echo ""
    echo "Commands:"
    echo "  start          Start all services (Docker + Spring Boot)"
    echo "  stop           Stop all services"
    echo "  restart        Restart all services"
    echo "  build          Build the application"
    echo "  test           Run load tests"
    echo "  logs           Show application logs"
    echo "  status         Check service status"
    echo "  clean          Clean build artifacts and Docker volumes"
    echo "  demo           Run full demo (build + start + test)"
    echo "  metrics        Show current metrics"
    echo "  help           Show this help message"
    echo ""
}

check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"

    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ Java not found. Please install Java 17+${NC}"
        exit 1
    fi

    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}❌ Maven not found. Please install Maven 3.8+${NC}"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker not found. Please install Docker${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ All prerequisites satisfied${NC}"
}

start_services() {
    echo -e "${GREEN}🚀 Starting services...${NC}"

    # Start Docker services
    echo "Starting Docker services..."
    docker-compose up -d

    # Wait for services to be ready
    echo "Waiting for MySQL..."
    until docker-compose exec -T mysql mysqladmin ping -h localhost -u root -prootpassword --silent &> /dev/null; do
        sleep 1
    done
    echo -e "${GREEN}✅ MySQL ready${NC}"

    echo "Waiting for Redis..."
    until docker-compose exec -T redis redis-cli ping &> /dev/null; do
        sleep 1
    done
    echo -e "${GREEN}✅ Redis ready${NC}"

    # Start Spring Boot
    echo "Starting Spring Boot application..."
    mvn spring-boot:run &
    SPRING_PID=$!

    # Wait for Spring Boot to be ready
    echo "Waiting for Spring Boot..."
    until curl -s http://localhost:8080/actuator/health | grep -q "UP" 2>/dev/null; do
        sleep 2
    done
    echo -e "${GREEN}✅ Spring Boot ready${NC}"

    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  🎉 All services started successfully!           ║${NC}"
    echo -e "${GREEN}╠═══════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}║  📊 API:        http://localhost:8080            ║${NC}"
    echo -e "${GREEN}║  📈 Prometheus: http://localhost:9090            ║${NC}"
    echo -e "${GREEN}║  📊 Grafana:    http://localhost:3000            ║${NC}"
    echo -e "${GREEN}║  🔍 Redis UI:   http://localhost:8081            ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════╝${NC}"
}

stop_services() {
    echo -e "${YELLOW}🛑 Stopping services...${NC}"

    # Stop Spring Boot
    pkill -f "spring-boot:run" || true

    # Stop Docker services
    docker-compose down

    echo -e "${GREEN}✅ All services stopped${NC}"
}

build_project() {
    echo -e "${GREEN}🔨 Building project...${NC}"
    mvn clean package -DskipTests
    echo -e "${GREEN}✅ Build complete${NC}"
}

run_tests() {
    echo -e "${GREEN}🧪 Running load tests...${NC}"

    if [ -f "./load-test.sh" ]; then
        chmod +x load-test.sh
        ./load-test.sh
    else
        echo -e "${RED}❌ load-test.sh not found${NC}"
        exit 1
    fi
}

show_logs() {
    echo -e "${GREEN}📋 Application logs:${NC}"
    docker-compose logs -f spring-boot || tail -f nohup.out
}

show_status() {
    echo -e "${GREEN}📊 Service Status:${NC}"
    echo ""

    # Docker services
    echo "Docker Services:"
    docker-compose ps

    echo ""
    echo "Application Health:"
    curl -s http://localhost:8080/actuator/health | jq '.' || echo "Application not responding"
}

clean_all() {
    echo -e "${YELLOW}🧹 Cleaning project...${NC}"

    # Stop services
    stop_services

    # Clean Maven
    mvn clean

    # Remove Docker volumes
    docker-compose down -v

    echo -e "${GREEN}✅ Cleanup complete${NC}"
}

run_demo() {
    echo -e "${GREEN}🎬 Running full demo...${NC}"

    check_prerequisites
    build_project
    start_services

    echo ""
    echo -e "${YELLOW}Waiting 10 seconds for cache warming...${NC}"
    sleep 10

    run_tests

    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  🎉 Demo complete!                               ║${NC}"
    echo -e "${GREEN}║                                                   ║${NC}"
    echo -e "${GREEN}║  Next steps:                                      ║${NC}"
    echo -e "${GREEN}║  1. View Grafana dashboards (localhost:3000)     ║${NC}"
    echo -e "${GREEN}║  2. Check Prometheus metrics (localhost:9090)    ║${NC}"
    echo -e "${GREEN}║  3. Test API endpoints manually                  ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════╝${NC}"
}

show_metrics() {
    echo -e "${GREEN}📊 Current Metrics:${NC}"
    echo ""

    echo "Cache Hit Ratio:"
    curl -s http://localhost:8080/actuator/metrics/cache.hit | jq '.measurements[0].value' || echo "N/A"

    echo ""
    echo "Cache Miss Count:"
    curl -s http://localhost:8080/actuator/metrics/cache.miss | jq '.measurements[0].value' || echo "N/A"

    echo ""
    echo "Database Queries:"
    curl -s http://localhost:8080/actuator/metrics/database.query | jq '.measurements[0].value' || echo "N/A"

    echo ""
    echo "Average Response Time:"
    curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements[] | select(.statistic=="TOTAL_TIME") | .value' || echo "N/A"
}

# Main script logic
print_banner

case "${1:-help}" in
    start)
        check_prerequisites
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        stop_services
        sleep 2
        start_services
        ;;
    build)
        build_project
        ;;
    test)
        run_tests
        ;;
    logs)
        show_logs
        ;;
    status)
        show_status
        ;;
    clean)
        clean_all
        ;;
    demo)
        run_demo
        ;;
    metrics)
        show_metrics
        ;;
    help|*)
        show_help
        ;;
esac