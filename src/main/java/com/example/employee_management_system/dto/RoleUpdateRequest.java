package com.example.employee_management_system.dto;

import com.example.employee_management_system.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull(message = "Role is required") Role role) {
}
