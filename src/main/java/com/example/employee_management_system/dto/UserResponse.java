package com.example.employee_management_system.dto;

import com.example.employee_management_system.entity.AppUser;
import com.example.employee_management_system.entity.Role;

/** Never includes the password hash. */
public record UserResponse(Long id, String username, Role role) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole());
    }
}
