package com.example.employee_management_system.config;

import com.example.employee_management_system.entity.*;
import com.example.employee_management_system.repository.AppUserRepository;
import com.example.employee_management_system.repository.EmployeeRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Runs once at startup: creates the login users and some demo employees if the tables are empty. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final String adminPassword;
    private final String hrPassword;
    private final boolean seedSampleData;

    public DataSeeder(AppUserRepository users,
                      EmployeeRepository employees,
                      PasswordEncoder encoder,
                      @Value("${app.seed.admin-password}") String adminPassword,
                      @Value("${app.seed.hr-password}") String hrPassword,
                      @Value("${app.seed.sample-data:false}") boolean seedSampleData) {
        this.users = users;
        this.employees = employees;
        this.encoder = encoder;
        this.adminPassword = adminPassword;
        this.hrPassword = hrPassword;
        this.seedSampleData = seedSampleData;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            users.save(new AppUser("admin", encoder.encode(adminPassword), Role.ADMIN));
            users.save(new AppUser("hr", encoder.encode(hrPassword), Role.HR));
        }
        if (seedSampleData && employees.count() == 0) {
            add("Aarav Sharma", "aarav.sharma@example.com", Department.IT, "Backend Developer", "9876543210", EmployeeStatus.ACTIVE, "85000");
            add("Priya Nair", "priya.nair@example.com", Department.HR, "HR Manager", "9876501234", EmployeeStatus.ACTIVE, "72000");
            add("Karthik Reddy", "karthik.reddy@example.com", Department.FINANCE, "Accountant", "9812345678", EmployeeStatus.ON_LEAVE, "60000");
            add("Sneha Iyer", "sneha.iyer@example.com", Department.MARKETING, "Content Strategist", "9823456781", EmployeeStatus.ACTIVE, "55000");
            add("Rahul Verma", "rahul.verma@example.com", Department.IT, "DevOps Engineer", "9834567812", EmployeeStatus.ACTIVE, "95000");
            add("Divya Menon", "divya.menon@example.com", Department.IT, "QA Engineer", "9845678123", EmployeeStatus.RESIGNED, "58000");
            add("Arjun Das", "arjun.das@example.com", Department.FINANCE, "Financial Analyst", "9856781234", EmployeeStatus.ACTIVE, "70000");
            add("Meera Pillai", "meera.pillai@example.com", Department.MARKETING, "Marketing Lead", "9867812345", EmployeeStatus.ACTIVE, "88000");
        }
    }

    private void add(String name, String email, Department dept, String role, String phone,
                     EmployeeStatus status, String salary) {
        Employee e = new Employee();
        e.setName(name);
        e.setEmail(email);
        e.setDepartment(dept);
        e.setRole(role);
        e.setPhone(phone);
        e.setStatus(status);
        e.setSalary(new BigDecimal(salary));
        employees.save(e);
    }
}
