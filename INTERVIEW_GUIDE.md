# Interview Guide: know this project cold

You must be able to explain every item below in your own words. Read the code while you read this.

## 1. Trace one request: "Add Employee"
1. You click the button, and `saveEmployee()` in `app.js` calls `fetch("/api/employees", {method:"POST", body: JSON})`.
2. The browser sends the session cookie. **Spring Security** checks you are logged in and your role allows `/api/**`.
3. `EmployeeController.create()` receives the JSON. `@RequestBody` turns it into an `EmployeeRequest` record. `@Valid` runs the validation annotations (`@NotBlank`, `@Email`, ...). If they fail, `GlobalExceptionHandler` returns 400.
4. The controller calls `EmployeeService.create()` (inside a transaction). The service checks the email is not taken (else `DuplicateResourceException` → 409), copies fields to an `Employee` entity, and calls `repository.save()`.
5. Spring Data JPA/Hibernate generates the `INSERT` and sends it to MySQL.
6. The entity is converted to an `EmployeeResponse` DTO and returned as JSON with status 201.
7. The UI shows a toast and reloads the table and stats.

## 2. Questions you WILL be asked
- **Why Controller → Service → Repository?** Separation of concerns: HTTP details, business rules and data access are independent, so each can be tested and changed on its own.
- **What does `@Autowired` / constructor injection do?** Spring creates objects (beans) and passes them in. Constructor injection makes dependencies explicit, fields `final`, and tests easy (we pass a mock).
- **What is a DTO and why use one?** A separate object for the API. It stops clients from setting fields they shouldn't (e.g. `id`) and keeps the API stable even if the table changes.
- **How is the password stored?** BCrypt hash (salted, slow on purpose). Never plain text. See `DataSeeder` and `SecurityConfig`.
- **How do you stop an HR user deleting?** `SecurityConfig` rule: `DELETE /api/employees/**` requires `ROLE_ADMIN`. Hiding the button in the UI is only convenience.
- **Why `BigDecimal` for salary?** `double` has rounding errors (0.1 + 0.2). Money needs exact decimals.
- **What is `@Transactional`?** All database work in the method succeeds together or is rolled back. `readOnly=true` lets the DB optimise reads.
- **How does search + filter + pagination work?** `EmployeeSpecifications` builds a dynamic `WHERE`; `PageRequest` adds `LIMIT/OFFSET` and `ORDER BY`. Only the requested page leaves the database.
- **How do you stop a visitor signing up as admin?** `RegisterRequest` has no role field, and `UserService.register()` hard-codes `Role.USER`. Even if someone adds `"role":"ADMIN"` to the request it is ignored. Only an existing ADMIN can promote an account.
- **Why can't an admin change their own role?** It guarantees the system always has at least one admin.
- **How is salary hidden from USER?** The controller checks the caller's authorities and builds the response with `salary = null`, so the data never leaves the server. Hiding a column in the UI alone would not be secure.
- **What does the order of `requestMatchers` mean?** Spring Security checks rules top to bottom and the first match wins. That is why `GET /api/employees/export` is listed before the general `GET /api/employees/**`.
- **What bugs did the first version have?** Hardcoded login in JavaScript, open API, in-memory DB (data lost on restart), status ignored on update, `NullPointerException` on unknown id, HTML injection (XSS) via `innerHTML`, unbounded data loading. Explaining what you fixed shows engineering judgement.
- **What would you improve next?** Flyway migrations, CSRF tokens or JWT, integration tests with MockMvc/Testcontainers, CI pipeline, audit logging.

## 3. Resume bullets (use these only if they are true for you after you understand the code)

**Project: WorkSmart Hub, Employee Management System** · Java 17, Spring Boot, MySQL, Spring Security, Docker
- Built a layered REST API (Controller/Service/Repository) with Spring Boot and MySQL, supporting employee CRUD with validation, duplicate detection and centralized exception handling.
- Implemented session-based authentication with BCrypt and three-tier role-based authorization (ADMIN/HR/USER), including self-registration, an admin user-management panel, and server-side salary masking for read-only users.
- Designed server-side pagination, sorting and dynamic search/filtering using JPA Specifications, plus database-level `GROUP BY` analytics powering a Chart.js dashboard.
- Wrote JUnit 5/Mockito unit tests for the service layer and containerized the app with Docker Compose (MySQL + Spring Boot).
- Audited and fixed security and correctness issues in the first version (hardcoded credentials, unauthenticated API, XSS, null-pointer errors, status not saved on update).
