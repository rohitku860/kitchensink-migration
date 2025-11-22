# Kitchensink Spring Boot Application

## Overview
Spring Boot REST API for Member Registration and Management using MongoDB.

## Features

### Existing Features (Migrated from EJB)
- ✅ Create Member (POST)
- ✅ Get All Members (GET)
- ✅ Get Member by ID (GET)
- ✅ View members ordered by name

### New Features
- ✅ Update Member (PUT)
- ✅ Delete Member (DELETE)
- ✅ Search by name (fuzzy search)
- ✅ Filter by email domain
- ✅ Get members by status
- ✅ Registration date tracking
- ✅ Member status (ACTIVE/INACTIVE)

## Technology Stack
- **Java**: 21
- **Spring Boot**: 3.2.0
- **MongoDB**: 8.0
- **Maven**: Build tool
- **Spring Boot DevTools**: Hot reloading for development
- **Spring Boot Actuator**: Health checks and monitoring
- **SpringDoc OpenAPI (Swagger)**: API documentation
- **SLF4J + Logback**: Logging framework

## Prerequisites
- Java 21+
- Maven 3.6+
- MongoDB 8.0+ (running on localhost:27017)

## Setup

### 1. Build the project
```bash
cd kitchensink-springboot
mvn clean install
```

### 2. Run the application
```bash
mvn spring-boot:run
```

Or:
```bash
java -jar target/kitchensink-springboot-1.0.0.jar
```

## API Documentation

### Swagger UI
Access interactive API documentation at:
```
http://localhost:8081/api/swagger-ui.html
```

### OpenAPI JSON
Raw API specification:
```
http://localhost:8081/api/api-docs
```

## Monitoring & Health Checks

### Actuator Endpoints
- **Health**: `http://localhost:8081/api/actuator/health`
- **Info**: `http://localhost:8081/api/actuator/info`
- **Metrics**: `http://localhost:8081/api/actuator/metrics`
- **Prometheus**: `http://localhost:8081/api/actuator/prometheus`

## API Endpoints

### Base URL
```
http://localhost:8081/api
```

**Note**: Port 8081 is used to avoid conflicts with other services. Change in `application.properties` if needed.

### Endpoints

#### Get All Members (Paginated)
```
GET /api/members?page=0&size=10&sort=name
```
**Query Parameters:**
- `page` (optional, default: 0) - Page number (0-indexed)
- `size` (optional, default: 10) - Number of items per page
- `sort` (optional, default: name) - Sort field and direction (e.g., `name`, `name,desc`)

**Response:** Returns a paginated response with `content`, `totalElements`, `totalPages`, etc.

#### Get Member by ID
```
GET /api/members/{id}
```

#### Create Member
```
POST /api/members
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "1234567890",
  "status": "ACTIVE"
}
```

#### Update Member
```
PUT /api/members/{id}
Content-Type: application/json

{
  "name": "John Doe Updated",
  "email": "john.doe@example.com",
  "phoneNumber": "1234567890",
  "status": "ACTIVE"
}
```

#### Delete Member
```
DELETE /api/members/{id}
```

#### Search Members
```
GET /api/members/search?name=John
GET /api/members/search?domain=gmail.com
```

## MongoDB Connection
- **URI**: `mongodb://localhost:27017/kitchensink`
- **Database**: `kitchensink`
- **Collection**: `members`

## Schema Management
**IMPORTANT**: This application does NOT create any schema changes. All schema changes (collections, indexes, validators) must be managed at the database level using MongoDB scripts. See `SCHEMA_MANAGEMENT.md` for details.

## Validation Rules
- **Name**: 1-25 characters, no numbers
- **Email**: Valid email format, unique
- **Phone Number**: 10-12 digits, unique
- **Status**: ACTIVE or INACTIVE (default: ACTIVE)

## Development Features

### Spring Boot DevTools
- **Hot Reloading**: Automatically restarts the application when code changes
- **LiveReload**: Browser auto-refresh on code changes
- **Property Defaults**: Development-friendly defaults

### Logging
- **Console Logging**: Colored output with timestamps
- **File Logging**: Logs saved to `logs/kitchensink-api.log`
- **Log Rotation**: Automatic rotation (10MB max, 30 days retention)
- **Log Levels**: 
  - Application: DEBUG
  - Spring Framework: INFO
  - MongoDB: INFO

## Error Responses

### Validation Error (400)
```json
{
  "name": "Name must not contain numbers",
  "email": "Email must be a valid email address"
}
```

### Conflict Error (409)
```json
{
  "email": "Email already exists"
}
```

### Not Found (404)
```json
{
  "error": "Member not found with id: ..."
}
```

