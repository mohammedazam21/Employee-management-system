package com.example.employee_management_system.dto;

import com.example.employee_management_system.entity.Department;
import com.example.employee_management_system.entity.Employee;
import com.example.employee_management_system.entity.EmployeeStatus;
import java.math.BigDecimal;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        Department department,
        String role,
        String phone,
        EmployeeStatus status,
        BigDecimal salary
) {
    public static EmployeeResponse from(Employee e) {
        return from(e, true);
    }

    /** includeSalary=false is used for the read-only USER role: salary is sent as null. */
    public static EmployeeResponse from(Employee e, boolean includeSalary) {
        return new EmployeeResponse(e.getId(), e.getName(), e.getEmail(), e.getDepartment(),
                e.getRole(), e.getPhone(), e.getStatus(), includeSalary ? e.getSalary() : null);
    }
}
