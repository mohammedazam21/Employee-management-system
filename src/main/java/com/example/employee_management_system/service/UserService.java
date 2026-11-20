package com.example.employee_management_system.service;

import com.example.employee_management_system.entity.AppUser;
import com.example.employee_management_system.entity.Role;
import com.example.employee_management_system.exception.BadRequestException;
import com.example.employee_management_system.exception.DuplicateResourceException;
import com.example.employee_management_system.exception.ResourceNotFoundException;
import com.example.employee_management_system.repository.AppUserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public UserService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Public sign-up. The role is ALWAYS USER, whatever the client sends. */
    @Transactional
    public AppUser register(String username, String rawPassword) {
        String name = username.trim();
        if (users.existsByUsername(name)) {
            throw new DuplicateResourceException("Username '" + name + "' is already taken");
        }
        return users.save(new AppUser(name, encoder.encode(rawPassword), Role.USER));
    }

    @Transactional(readOnly = true)
    public List<AppUser> listAll() {
        return users.findAll(Sort.by("id"));
    }

    @Transactional
    public AppUser changeRole(Long id, Role newRole, String actingUsername) {
        AppUser target = users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
        // An admin cannot change their own role: this guarantees there is always at least one admin.
        if (target.getUsername().equals(actingUsername)) {
            throw new BadRequestException("You cannot change your own role");
        }
        target.setRole(newRole);
        return users.save(target);
    }
}
