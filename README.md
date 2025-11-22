# Kitchensink Migration Project

This project demonstrates the migration of a Jakarta EE application to Spring Boot with MongoDB and React frontend.

## Project Structure

```
kitchensink-migration/
├── kitchensink-springboot/    # Spring Boot REST API
└── kitchensink-react/         # React Frontend Application
```

## Technologies

### Backend (Spring Boot)
- Spring Boot 3.5.7
- Spring Data MongoDB
- Spring Security (API Key Authentication)
- Swagger/OpenAPI Documentation
- Spring Actuator
- Caffeine Cache
- Jasypt (PII Encryption)

### Frontend (React)
- React 18
- Axios for API calls
- Modern UI with pagination and search

### Database
- MongoDB 8.0+

## Features

### Spring Boot API
- RESTful API with versioning (`/kitchensink/v1/members`)
- CRUD operations for members
- Pagination support
- Search functionality
- API Key authentication
- CORS configuration
- Input sanitization
- Rate limiting
- PII encryption (email, phone)
- Audit logging
- Caching
- Async processing
- Global exception handling
- Correlation ID tracking

### React Frontend
- Member registration
- Member list with pagination
- Member search
- Member edit/delete
- Modern, responsive UI

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- Node.js 16+
- MongoDB 8.0+

### Backend Setup

1. Navigate to Spring Boot application:
```bash
cd kitchensink-springboot
```

2. Configure MongoDB connection in `src/main/resources/application.properties`

3. Build and run:
```bash
mvn clean install
mvn spring-boot:run
```

The API will be available at: `http://localhost:8081/kitchensink/v1/members`

### Frontend Setup

1. Navigate to React application:
```bash
cd kitchensink-react
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

The frontend will be available at: `http://localhost:3000`

### MongoDB Setup

1. Start MongoDB service
2. Create database and collection:
```bash
mongosh kitchensink < setup-mongodb.js
```

## API Documentation

Once the Spring Boot application is running, access Swagger UI at:
- http://localhost:8081/swagger-ui.html

## API Endpoints

- `GET /kitchensink/v1/members` - Get all members (paginated)
- `GET /kitchensink/v1/members/{id}` - Get member by ID
- `POST /kitchensink/v1/members` - Create new member
- `PUT /kitchensink/v1/members/{id}` - Update member
- `DELETE /kitchensink/v1/members/{id}` - Delete member
- `GET /kitchensink/v1/members/search?name={name}` - Search members by name

## Authentication

All API requests require an API key in the header:
```
X-API-Key: your-secret-api-key-change-in-production
```

## License

This project is open source and available for public use.

