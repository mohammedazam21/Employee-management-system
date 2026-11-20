package com.example.employee_management_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.employee_management_system.dto.EmployeeRequest;
import com.example.employee_management_system.dto.StatsResponse;
import com.example.employee_management_system.entity.Department;
import com.example.employee_management_system.entity.Employee;
import com.example.employee_management_system.entity.EmployeeStatus;
import com.example.employee_management_system.exception.DuplicateResourceException;
import com.example.employee_management_system.exception.ResourceNotFoundException;
import com.example.employee_management_system.repository.EmployeeRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pure unit tests: the repository is mocked, so no database or Spring context is needed. */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    private EmployeeRequest request(String email, EmployeeStatus status) {
        return new EmployeeRequest("  Asha Rao ", email, Department.IT, "Developer",
                "9999999999", status, new BigDecimal("50000.00"));
    }

    private Employee existingEmployee() {
        Employee e = new Employee();
        e.setName("Old Name");
        e.setEmail("old@example.com");
        e.setDepartment(Department.HR);
        e.setStatus(EmployeeStatus.ACTIVE);
        e.setSalary(new BigDecimal("10000"));
        return e;
    }

    @Test
    void create_savesEmployeeWithTrimmedFields() {
        when(repository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee saved = service.create(request("asha@example.com", EmployeeStatus.ACTIVE));

        assertEquals("Asha Rao", saved.getName());
        assertEquals(Department.IT, saved.getDepartment());
        assertEquals(EmployeeStatus.ACTIVE, saved.getStatus());
        assertEquals(new BigDecimal("50000.00"), saved.getSalary());
    }

    @Test
    void create_duplicateEmail_throwsAndNeverSaves() {
        when(repository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.create(request("asha@example.com", EmployeeStatus.ACTIVE)));

        verify(repository, never()).save(any(Employee.class));
    }

    // Regression test: the original project's update endpoint silently ignored "status"
    @Test
    void update_changesStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingEmployee()));
        when(repository.existsByEmailIgnoreCaseAndIdNot("asha@example.com", 1L)).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee updated = service.update(1L, request("asha@example.com", EmployeeStatus.ON_LEAVE));

        assertEquals(EmployeeStatus.ON_LEAVE, updated.getStatus());
        assertEquals("asha@example.com", updated.getEmail());
    }

    // Regression test: the original project threw NullPointerException here
    @Test
    void update_unknownId_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, request("asha@example.com", EmployeeStatus.ACTIVE)));
    }

    @Test
    void update_emailUsedByAnotherEmployee_throwsDuplicate() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingEmployee()));
        when(repository.existsByEmailIgnoreCaseAndIdNot("asha@example.com", 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.update(1L, request("asha@example.com", EmployeeStatus.ACTIVE)));

        verify(repository, never()).save(any(Employee.class));
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(repository.existsById(5L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.delete(5L));

        verify(repository, never()).deleteById(any());
    }

    @Test
    void delete_existingId_deletes() {
        when(repository.existsById(5L)).thenReturn(true);

        service.delete(5L);

        verify(repository).deleteById(5L);
    }

    @Test
    void stats_includesZeroForMissingDepartmentsAndStatuses() {
        List<Object[]> deptRows = new ArrayList<>();
        deptRows.add(new Object[]{Department.IT, 3L});
        List<Object[]> statusRows = new ArrayList<>();
        statusRows.add(new Object[]{EmployeeStatus.ACTIVE, 3L});

        when(repository.countGroupedByDepartment()).thenReturn(deptRows);
        when(repository.countGroupedByStatus()).thenReturn(statusRows);
        when(repository.count()).thenReturn(3L);

        StatsResponse stats = service.stats();

        assertEquals(3L, stats.total());
        assertEquals(3L, stats.byDepartment().get("IT"));
        assertEquals(0L, stats.byDepartment().get("MARKETING"));   // not in the DB rows -> 0
        assertEquals(0L, stats.byStatus().get("RESIGNED"));
    }
}
