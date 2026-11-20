package com.example.employee_management_system.dto;

import com.example.employee_management_system.entity.Department;
import com.example.employee_management_system.entity.EmployeeStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** What the client is allowed to send when creating or updating an employee. */
public record EmployeeRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotNull(message = "Department is required")
        Department department,

        @Size(max = 100, message = "Role must be at most 100 characters")
        String role,

        @Pattern(regexp = "^$|^[0-9+()\\- ]{7,20}$", message = "Phone must be 7-20 characters: digits, +, -, ( ) or spaces")
        String phone,

        @NotNull(message = "Status is required")
        EmployeeStatus status,

        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", message = "Salary cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Salary must have at most 10 digits and 2 decimals")
        BigDecimal salary
) {
}
