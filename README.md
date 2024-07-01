
# E-commerce App with Spring Boot and React

This application tends to cover essential e-commerce functionalities, including user authentication, product management, shopping cart, and order processing.


## Features

- User authentication and authorization
- Product listing and management
- Shopping cart functionality
- Order processing and management
- User profile management
- Responsive design

## ## Tech Stack

### Backend
- Spring Boot
- Authentication and authorization with Spring Security
- Efficient data access with Spring Data JPA
- Implementation of Specification Pattern
- Data Integration using MySql & Redis via Docker

### Frontend
- React including redux, thunk api, and many more.
- Styling with Material UI, roboto.

## Installation

### Prerequisites
- Java 11 or higher
- Node.js and npm
- Docker

### Backend Setup
1. Clone the repository:

```bash
  > https://github.com/EtienneBel/ecom-Java-React/tree/develop
  > cd ecommerce-springboot-react/
```


2. Configure the MySQL database:
   Update the `application.yaml` file with your MySQL credentials, then run :
```bash
  > cd docker/
  > docker-compose up -d
```


3. Build and run the Spring Boot application:
```bash
  > ./mvnw clean install
  > ./mvnw spring-boot:run
```
### Frontend Setup
Navigate to the frontend directory:
```bash
  > cd client
  > npm install
  > npm run dev
```


## Usage

1. Open your browser and go to `http://localhost:5173` to access the frontend application.
2. Use `http://localhost:8080` for backend API endpoints.
