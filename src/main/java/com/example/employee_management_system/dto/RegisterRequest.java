package com.example.employee_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Note: there is deliberately NO "role" field. A visitor can never choose their own role. */
public record RegisterRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Username may contain only letters, digits, . _ -")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        String password
) {
}
