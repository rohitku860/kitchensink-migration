#!/bin/bash

# Start Services Script for Kitchensink Application
# This script starts all required services in the correct order:
# 1. MongoDB
# 2. JBoss EAP/WildFly
# 3. Spring Boot Application
# 4. React Application

set -e  # Exit on error

echo "=========================================="
echo "Starting Kitchensink Services"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to check if a service is running
check_port() {
    local port=$1
    local service=$2
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo -e "${YELLOW}Warning: Port $port is already in use. $service may already be running.${NC}"
        return 1
    fi
    return 0
}

# Step 1: Start MongoDB
echo "Step 1: Starting MongoDB..."
if systemctl is-active --quiet mongod 2>/dev/null; then
    echo -e "${GREEN}✓ MongoDB is already running${NC}"
elif pgrep -f mongod > /dev/null; then
    echo -e "${GREEN}✓ MongoDB process is already running${NC}"
else
    if systemctl start mongod 2>/dev/null; then
        echo -e "${GREEN}✓ MongoDB started successfully${NC}"
    else
        echo -e "${YELLOW}⚠ Could not start MongoDB via systemctl. Trying direct start...${NC}"
        if command -v mongod >/dev/null 2>&1; then
            mongod --fork --logpath /var/log/mongodb/mongod.log 2>/dev/null || echo -e "${RED}✗ Failed to start MongoDB${NC}"
        else
            echo -e "${RED}✗ MongoDB not found. Please install MongoDB first.${NC}"
            exit 1
        fi
    fi
    sleep 2
fi

# Verify MongoDB is running
if mongosh --eval "db.adminCommand('ping')" --quiet >/dev/null 2>&1; then
    echo -e "${GREEN}✓ MongoDB is responding${NC}"
else
    echo -e "${RED}✗ MongoDB is not responding. Please check MongoDB status.${NC}"
    exit 1
fi
echo ""

# Step 2: Start JBoss EAP/WildFly
echo "Step 2: Starting JBoss EAP/WildFly..."
WILDFLY_HOME="$HOME/wildfly"
JBOSS_HOME="$HOME/jboss-eap"

if check_port 8080 "JBoss/WildFly"; then
    if [ -d "$WILDFLY_HOME" ] && [ -f "$WILDFLY_HOME/bin/standalone.sh" ]; then
        echo "Starting WildFly from $WILDFLY_HOME..."
        cd "$WILDFLY_HOME"
        JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./bin/standalone.sh > /tmp/wildfly.log 2>&1 &
        WILDFLY_PID=$!
        echo "WildFly starting in background (PID: $WILDFLY_PID)"
        echo "Logs: /tmp/wildfly.log"
        sleep 5
        echo -e "${GREEN}✓ WildFly started${NC}"
    elif [ -d "$JBOSS_HOME" ] && [ -f "$JBOSS_HOME/bin/standalone.sh" ]; then
        echo "Starting JBoss EAP from $JBOSS_HOME..."
        cd "$JBOSS_HOME"
        JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./bin/standalone.sh > /tmp/jboss.log 2>&1 &
        JBOSS_PID=$!
        echo "JBoss EAP starting in background (PID: $JBOSS_PID)"
        echo "Logs: /tmp/jboss.log"
        sleep 5
        echo -e "${GREEN}✓ JBoss EAP started${NC}"
    else
        echo -e "${YELLOW}⚠ JBoss/WildFly not found at $WILDFLY_HOME or $JBOSS_HOME. Skipping...${NC}"
    fi
fi
echo ""

# Step 3: Start Spring Boot Application
echo "Step 3: Starting Spring Boot Application..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SPRING_BOOT_DIR="$SCRIPT_DIR/kitchensink-springboot"

if check_port 8081 "Spring Boot"; then
    if [ -d "$SPRING_BOOT_DIR" ]; then
        echo "Starting Spring Boot from $SPRING_BOOT_DIR..."
        cd "$SPRING_BOOT_DIR"
        JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn spring-boot:run > /tmp/springboot.log 2>&1 &
        SPRING_BOOT_PID=$!
        echo "Spring Boot starting in background (PID: $SPRING_BOOT_PID)"
        echo "Logs: /tmp/springboot.log"
        sleep 10
        echo -e "${GREEN}✓ Spring Boot started${NC}"
    else
        echo -e "${RED}✗ Spring Boot directory not found at $SPRING_BOOT_DIR${NC}"
        exit 1
    fi
fi
echo ""

# Step 4: Start React Application
echo "Step 4: Starting React Application..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REACT_DIR="$SCRIPT_DIR/kitchensink-react"

if check_port 3000 "React App"; then
    if [ -d "$REACT_DIR" ]; then
        echo "Starting React App from $REACT_DIR..."
        cd "$REACT_DIR"
        
        # Check if node_modules exists, if not install dependencies
        if [ ! -d "node_modules" ]; then
            echo "Installing React dependencies..."
            npm install
        fi
        
        # Start React app in background
        BROWSER=none npm start > /tmp/react.log 2>&1 &
        REACT_PID=$!
        echo "React App starting in background (PID: $REACT_PID)"
        echo "Logs: /tmp/react.log"
        sleep 5
        echo -e "${GREEN}✓ React App started${NC}"
    else
        echo -e "${YELLOW}⚠ React directory not found at $REACT_DIR. Skipping...${NC}"
    fi
else
    echo -e "${YELLOW}⚠ React App may already be running on port 3000${NC}"
fi
echo ""

# Final status check
echo "=========================================="
echo "Service Status Check"
echo "=========================================="
echo ""

# Check MongoDB
if mongosh --eval "db.adminCommand('ping')" --quiet >/dev/null 2>&1; then
    echo -e "${GREEN}✓ MongoDB: Running${NC}"
else
    echo -e "${RED}✗ MongoDB: Not running${NC}"
fi

# Check JBoss/WildFly
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${GREEN}✓ JBoss/WildFly: Running on port 8080${NC}"
else
    echo -e "${YELLOW}⚠ JBoss/WildFly: Not running on port 8080${NC}"
fi

# Check Spring Boot
if lsof -Pi :8081 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${GREEN}✓ Spring Boot: Running on port 8081${NC}"
    echo ""
    echo "Spring Boot API: http://localhost:8081/kitchensink/v1/members"
    echo "Swagger UI: http://localhost:8081/kitchensink/swagger-ui/index.html"
    echo "Health Check: http://localhost:8081/kitchensink/actuator/health"
else
    echo -e "${YELLOW}⚠ Spring Boot: Not running on port 8081${NC}"
    echo "Check logs: tail -f /tmp/springboot.log"
fi

# Check React App
if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${GREEN}✓ React App: Running on port 3000${NC}"
    echo ""
    echo "React App: http://localhost:3000"
else
    echo -e "${YELLOW}⚠ React App: Not running on port 3000${NC}"
    echo "Check logs: tail -f /tmp/react.log"
fi

echo ""
echo "=========================================="
echo "All services started!"
echo "=========================================="

