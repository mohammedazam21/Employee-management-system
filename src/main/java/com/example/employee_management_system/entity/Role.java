package com.example.employee_management_system.entity;

/**
 * ADMIN: full access, can manage users.
 * HR:    can view, add and edit employees (cannot delete).
 * USER:  read-only; salaries are hidden. This is the role self-registration creates.
 */
public enum Role {
    ADMIN, HR, USER
}
