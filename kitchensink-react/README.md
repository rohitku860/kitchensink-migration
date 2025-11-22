# Kitchensink React Frontend

A React web application for Member Registration and Management, integrated with the Spring Boot API.

## Features

- **Member Registration**: Register new members with validation
- **Member List**: View all members with pagination
- **Search**: Search members by name or filter by email domain
- **Update**: Edit member information
- **Delete**: Remove members from the system

## Prerequisites

- Node.js (v14 or higher)
- npm or yarn
- Spring Boot API running on http://localhost:8081

## Installation

1. Install dependencies:
```bash
npm install
```

2. Configure API settings:
   - Update `src/services/api.js` with your API key if different from default
   - Ensure API base URL matches your Spring Boot server

## Running the Application

1. Start the Spring Boot API server first:
```bash
cd ../kitchensink-springboot
mvn spring-boot:run
```

2. Start the React development server:
```bash
npm start
```

3. Open http://localhost:3000 in your browser

## API Configuration

The application connects to:
- **Base URL**: http://localhost:8081/kitchensink/v1/members
- **API Key**: Configured in `src/services/api.js`

## Build for Production

```bash
npm run build
```

This creates an optimized production build in the `build` folder.

## Project Structure

```
src/
  ├── components/
  │   ├── MemberRegistration.js    # Registration form
  │   ├── MemberList.js            # Member list with pagination
  │   ├── SearchMembers.js         # Search functionality
  │   └── MemberEditModal.js       # Edit member modal
  ├── services/
  │   └── api.js                   # API service layer
  ├── App.js                       # Main application component
  ├── App.css                      # Application styles
  ├── index.js                     # Entry point
  └── index.css                    # Global styles
```

## Features Matching Original Application

- Simple, clean UI similar to the original JBoss application
- Member registration form with validation
- Member list display
- All CRUD operations (Create, Read, Update, Delete)
- Search functionality
- Error and success message handling

