#!/bin/bash

# Set Java 21
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java version
echo "Using Java version:"
java -version

# Navigate to project directory
cd "$(dirname "$0")"

# Run Spring Boot application
echo "Starting Spring Boot application..."
mvn spring-boot:run

