package com.example.employee_management_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Boots the whole application (security, JPA, controllers) against an in-memory H2 database
@SpringBootTest
@ActiveProfiles("test")
class EmployeeManagementSystemApplicationTests {

    @Test
    void contextLoads() {
    }
}
