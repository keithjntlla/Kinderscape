# AI Coding Agent Instructions for the Tuition System Project

## Project Overview
This project is a Tuition Management System built using Java and the Spring Framework. It includes the following major components:

1. **Controllers**: Handle HTTP requests and responses. Examples include `AuthController`, `HomeController`, and `TuitionController`.
2. **Services**: Contain the business logic. Examples include `AuthService` and `TuitionServices`.
3. **Repositories**: Interface with the database using Spring Data JPA. Examples include `UserRepository` and `StudentBalanceRepository`.
4. **Models/DTOs**: Represent data structures. Examples include `User`, `StudentBalance`, and `TuitionDTO`.
5. **Security**: Implements JWT-based authentication and authorization. Key files include `JwtAuthenticationFilter`, `JwtUtil`, and `SecurityConfig`.

## Key Workflows

### Building the Project
- Use the Maven wrapper scripts provided in the root directory:
  ```powershell
  ./mvnw clean install
  ```

### Running the Application
- The main entry point is `com.example.system.Launcher`.
- Run the application using the Maven wrapper:
  ```powershell
  ./mvnw spring-boot:run
  ```

### Testing
- Unit tests are located under `src/test/java`.
- Run all tests with Maven:
  ```powershell
  ./mvnw test
  ```

### Debugging
- Use the `Launcher` class to start the application in debug mode.
- Ensure the `application.properties` file is correctly configured for your environment.

## Project-Specific Conventions

1. **Package Structure**:
   - Follow the `com.example.system` hierarchy.
   - Group related files (e.g., `controller`, `services`, `models`).

2. **DTO Usage**:
   - Use DTOs (e.g., `LoginRequest`, `TuitionDTO`) for transferring data between layers.

3. **Error Handling**:
   - Use `@ControllerAdvice` for global exception handling.

4. **Security**:
   - JWT tokens are used for authentication. Refer to `JwtUtil` for token generation and validation.

## Integration Points

- **Database**: Configured via `application.properties`. Uses Spring Data JPA.
- **Frontend**: The `PaySTIPortal.fxml` file suggests a JavaFX-based UI.
- **Authentication**: JWT-based security implemented in the `security` package.

## Examples

### Adding a New Controller
1. Create a new class in the `controller` package.
2. Annotate with `@RestController`.
3. Define request mappings using `@RequestMapping` or `@GetMapping`.

### Adding a New Service
1. Create a new class in the `services` package.
2. Annotate with `@Service`.
3. Inject the required repository using `@Autowired`.

### Adding a New Repository
1. Create an interface in the `repositories` package.
2. Extend `JpaRepository` with the appropriate entity and ID type.

---

This document is a starting point. Update it as the project evolves to ensure AI agents remain effective. Feedback is welcome!