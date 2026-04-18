# Task Tracker API

A secure and production-ready Task Management REST API built using **Spring Boot**, **Spring Security**, **JWT**, and **PostgreSQL**.

---

## Features

* User signup and login
* JWT-based authentication
* Secure APIs using Bearer token
* User-based task ownership (each user can access only their own tasks)
* Create, update, and delete tasks
* Partial task updates (status)
* Search tasks by title and description
* Filter tasks by status and priority
* Pagination and sorting
* DTO-based clean API design
* Global exception handling
* Request validation
* Logging for important actions and errors
* Standard API response structure

---

## Tech Stack

* Java 21
* Spring Boot
* Spring Security
* JWT (JSON Web Token)
* Spring Data JPA
* PostgreSQL
* Maven
* Docker

---

## Architecture

This project follows a layered architecture:

**Controller → Service → Repository → Database**

---

## Authentication Flow

1. User signs up
2. User logs in
3. API returns a JWT token
4. Client sends the token in the `Authorization: Bearer <token>` header
5. Protected endpoints validate the token before allowing access

---

## Live Application

> Frontend (Full Application):
https://task-tracker-frontend-devendra1176s-projects.vercel.app/

>Backend API:
https://task-tracker-api-kgsq.onrender.com

---

## Main Endpoints

### Auth

* `POST /auth/signup`
* `POST /auth/login`

### Tasks

* `POST /tasks`
* `GET /tasks`
* `GET /tasks/{id}`
* `PUT /tasks/{id}`
* `PATCH /tasks/{id}/status`
* `DELETE /tasks/{id}`

### Search / Filter / Pagination

* `GET /tasks/search`
* `GET /tasks/filter`
* `GET /tasks?page=0&size=5&sortBy=dueDate&direction=desc`

---

## Example Features in Action

* Only authenticated users can access task APIs
* Each user can access only their own tasks
* Validation ensures correct request data
* Standard JSON responses improve frontend integration

---

## Run Locally

### Requirements

* Java 17+
* Maven
* PostgreSQL

### Clone the repository

```bash
git clone https://github.com/devendra1176/task-tracker-api.git
cd task-tracker-api
```

### Configure database

Update your `application-local.properties` with your PostgreSQL credentials.

### Run the application

```bash
./mvnw spring-boot:run
```

### Application URL

After running locally, access the API at:

http://localhost:8080

---

## Deployment

* Backend deployed on Render
* Frontend deployed separately on Vercel

---

## Future Improvements

* Refresh token support
* Role-based authorization (Admin/User)
* AI-based task suggestions (Spring AI)
* Notifications and reminders
* Unit and integration testing

---

## Author

**Devendra Sahu**

Aspiring Java Backend Developer | Spring Boot | REST APIs | JWT | PostgreSQL

---

## Project Goal

This project was built to demonstrate real-world backend development concepts including:

* Secure authentication (JWT)
* Clean layered architecture
* Scalable API design
* User-based data access control
* Frontend integration with a REST API

---
---
