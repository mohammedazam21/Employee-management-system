# WorkSmart Hub: Employee Management System

A full-stack employee management application: a Spring Boot REST API backed by **MySQL**, with
role-based security and a Bootstrap dashboard (search, filters, pagination, charts, PDF export).

## Features

- **Secure login**: Spring Security, BCrypt-hashed passwords stored in MySQL, session cookies
- **Three roles, enforced on the server** (see table below), plus public **sign-up** that always creates a read-only `USER`
- **Admin user management**: an admin can promote or demote accounts from the dashboard (cannot change their own role)
- **Salary privacy**: the `USER` role never receives salary data, the API sends `null`
- **Employee CRUD** with validation, duplicate-email detection (409) and clean JSON error responses
- **Server-side pagination, sorting, search and filtering** (department, status), implemented with JPA Specifications
- **Dashboard stats** computed by the database with `GROUP BY` (not by loading everything into the browser)
- **PDF export** of all employees, **dark mode**, responsive UI
- **Dockerized**: `docker compose up` starts MySQL and the app together
- **Unit tests** for the service layer (JUnit 5 + Mockito)

## Tech stack

Java 17 · Spring Boot 4 · Spring Web MVC · Spring Data JPA (Hibernate) · Spring Security ·
Bean Validation · MySQL 8 · Maven · Docker · JUnit 5 / Mockito · Bootstrap 5 · Chart.js · jsPDF

## Architecture

```
Browser (HTML/JS)
   │  fetch() + session cookie
   ▼
Spring Security filter chain ── checks login + role on every request
   ▼
Controller      (HTTP only: parse request, return JSON, status codes)
   ▼
Service         (business rules: duplicate emails, "not found", transactions)
   ▼
Repository      (Spring Data JPA + dynamic Specifications)
   ▼
MySQL
```

DTOs (`EmployeeRequest` / `EmployeeResponse`) sit between the API and the database entity, so clients
can only send and see the fields intended. `GlobalExceptionHandler` converts exceptions to HTTP errors in one place.

## Run it

### Option A: Docker (easiest, needs Docker only)

```bash
cp .env.example .env        # then edit the passwords
docker compose up --build
```
Open http://localhost:8080

### Option B: Local MySQL

1. Install MySQL 8 and make sure it is running.
2. Set your password and start the app:
   ```bash
   # Linux / macOS
   export DB_PASSWORD=your_mysql_password
   ./mvnw spring-boot:run

   # Windows PowerShell
   $env:DB_PASSWORD="your_mysql_password"
   .\mvnw.cmd spring-boot:run
   ```
   The database `ems_db` and all tables are created automatically on first start.
3. Open http://localhost:8080

**Demo logins** (created on first start, override with `ADMIN_PASSWORD` / `HR_PASSWORD`):

| Username | Password   | Role  |
|----------|------------|-------|
| admin    | Admin@123  | ADMIN |
| hr       | Hr@12345   | HR    |

Anyone can also click **Create an account** on the login page. New accounts are always `USER`.

### What each role can do

| Action | USER | HR | ADMIN |
|--------|:----:|:--:|:-----:|
| View / search / filter employees | yes | yes | yes |
| See salaries | no | yes | yes |
| Add / edit employees | no | yes | yes |
| Download PDF | no | yes | yes |
| Delete employees | no | no | yes |
| Manage user roles | no | no | yes |

Change these before deploying anywhere public. Eight sample employees are inserted when the table is empty
(turn off with `SEED_SAMPLE_DATA=false`).

## REST API

All endpoints require login except sign-up. Access per role is in the table above.

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/employees?q=&department=&status=&page=0&size=10&sortBy=name&direction=asc` | Paged, filtered, sorted list |
| GET | `/api/employees/{id}` | One employee |
| POST | `/api/employees` | Create (201) |
| PUT | `/api/employees/{id}` | Update |
| DELETE | `/api/employees/{id}` | Delete (204), ADMIN only |
| GET | `/api/employees/stats` | Counts by department and status |
| GET | `/api/employees/export` | Full list (used by the PDF button) |
| GET | `/api/auth/me` | Current user and role |
| POST | `/api/auth/register` | Public sign-up (always creates a `USER`) |
| POST | `/api/auth/login` · `/api/auth/logout` | Session login / logout |
| GET | `/api/admin/users` | List accounts, ADMIN only |
| PUT | `/api/admin/users/{id}/role` | Change an account's role, ADMIN only |

Errors look like `{"status":409,"error":"Conflict","message":"...","fieldErrors":null}`.
Validation errors return 400 with a `fieldErrors` map.

## Run the tests

```bash
./mvnw test
```
The tests use an in-memory H2 database (test scope only); production uses MySQL.

## Known limitations / next steps

Being honest about what is not done yet:

- CSRF protection is disabled (mitigated with `SameSite=Lax` cookies). A production system should use CSRF tokens or JWT.
- Schema is managed by `ddl-auto=update`. Production should use **Flyway** migrations.
- No integration tests for the HTTP layer yet (MockMvc), only service unit tests.
- Seeded demo passwords are defaults. Always override them via environment variables.
- Ideas to extend: leave management, attendance, audit log, refresh-token JWT auth, CI with GitHub Actions.
