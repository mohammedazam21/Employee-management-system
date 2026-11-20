package com.example.employee_management_system.controller;

import com.example.employee_management_system.dto.RoleUpdateRequest;
import com.example.employee_management_system.dto.UserResponse;
import com.example.employee_management_system.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** ADMIN only (enforced in SecurityConfig). Lets an admin promote or demote accounts. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.listAll().stream().map(UserResponse::from).toList();
    }

    @PutMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id,
                                   @Valid @RequestBody RoleUpdateRequest request,
                                   Authentication authentication) {
        return UserResponse.from(userService.changeRole(id, request.role(), authentication.getName()));
    }
}
