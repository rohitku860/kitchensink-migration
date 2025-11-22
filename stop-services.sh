#!/bin/bash

# Stop Services Script for Kitchensink Application
# This script stops all services in reverse order:
# 1. React Application
# 2. Spring Boot Application
# 3. JBoss EAP/WildFly
# 4. MongoDB

set -e  # Exit on error

echo "=========================================="
echo "Stopping Kitchensink Services"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to kill process on port
kill_port() {
    local port=$1
    local service=$2
    local pids=$(lsof -ti:$port 2>/dev/null)
    
    if [ -z "$pids" ]; then
        echo -e "${YELLOW}⚠ $service is not running on port $port${NC}"
        return 0
    fi
    
    echo "Stopping $service on port $port..."
    for pid in $pids; do
        echo "  Killing process $pid"
        kill -9 $pid 2>/dev/null || true
    done
    sleep 2
    
    # Verify it's stopped
    if lsof -ti:$port >/dev/null 2>&1; then
        echo -e "${RED}✗ Failed to stop $service${NC}"
        return 1
    else
        echo -e "${GREEN}✓ $service stopped successfully${NC}"
        return 0
    fi
}

# Step 1: Stop React Application
echo "Step 1: Stopping React Application..."
kill_port 3000 "React App"
echo ""

# Step 2: Stop Spring Boot Application
echo "Step 2: Stopping Spring Boot Application..."
kill_port 8081 "Spring Boot"
echo ""

# Step 3: Stop JBoss EAP/WildFly
echo "Step 3: Stopping JBoss EAP/WildFly..."
kill_port 8080 "JBoss/WildFly"
echo ""

# Step 4: Stop MongoDB
echo "Step 4: Stopping MongoDB..."
if systemctl is-active --quiet mongod 2>/dev/null; then
    echo "Stopping MongoDB via systemctl..."
    if sudo systemctl stop mongod 2>/dev/null; then
        echo -e "${GREEN}✓ MongoDB stopped successfully${NC}"
    else
        echo -e "${YELLOW}⚠ Could not stop MongoDB via systemctl. Trying direct kill...${NC}"
        # Try to find and kill mongod process
        MONGOD_PIDS=$(pgrep -f mongod 2>/dev/null || true)
        if [ -n "$MONGOD_PIDS" ]; then
            for pid in $MONGOD_PIDS; do
                echo "  Killing MongoDB process $pid"
                kill -9 $pid 2>/dev/null || true
            done
            sleep 2
            if ! pgrep -f mongod > /dev/null; then
                echo -e "${GREEN}✓ MongoDB stopped successfully${NC}"
            else
                echo -e "${RED}✗ Failed to stop MongoDB${NC}"
            fi
        else
            echo -e "${YELLOW}⚠ MongoDB process not found${NC}"
        fi
    fi
elif pgrep -f mongod > /dev/null; then
    echo "Stopping MongoDB process..."
    MONGOD_PIDS=$(pgrep -f mongod)
    for pid in $MONGOD_PIDS; do
        echo "  Killing MongoDB process $pid"
        kill -9 $pid 2>/dev/null || true
    done
    sleep 2
    if ! pgrep -f mongod > /dev/null; then
        echo -e "${GREEN}✓ MongoDB stopped successfully${NC}"
    else
        echo -e "${RED}✗ Failed to stop MongoDB${NC}"
    fi
else
    echo -e "${YELLOW}⚠ MongoDB is not running${NC}"
fi
echo ""

# Final status check
echo "=========================================="
echo "Service Status Check"
echo "=========================================="
echo ""

# Check React App
if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${RED}✗ React App: Still running on port 3000${NC}"
else
    echo -e "${GREEN}✓ React App: Stopped${NC}"
fi

# Check Spring Boot
if lsof -Pi :8081 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${RED}✗ Spring Boot: Still running on port 8081${NC}"
else
    echo -e "${GREEN}✓ Spring Boot: Stopped${NC}"
fi

# Check JBoss/WildFly
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${RED}✗ JBoss/WildFly: Still running on port 8080${NC}"
else
    echo -e "${GREEN}✓ JBoss/WildFly: Stopped${NC}"
fi

# Check MongoDB
if pgrep -f mongod > /dev/null || systemctl is-active --quiet mongod 2>/dev/null; then
    echo -e "${RED}✗ MongoDB: Still running${NC}"
else
    echo -e "${GREEN}✓ MongoDB: Stopped${NC}"
fi

echo ""
echo "=========================================="
echo "All services stopped!"
echo "=========================================="

