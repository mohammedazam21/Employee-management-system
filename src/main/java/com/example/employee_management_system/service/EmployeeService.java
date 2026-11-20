package com.example.employee_management_system.service;

import com.example.employee_management_system.dto.EmployeeRequest;
import com.example.employee_management_system.dto.StatsResponse;
import com.example.employee_management_system.entity.Department;
import com.example.employee_management_system.entity.Employee;
import com.example.employee_management_system.entity.EmployeeStatus;
import com.example.employee_management_system.exception.DuplicateResourceException;
import com.example.employee_management_system.exception.ResourceNotFoundException;
import com.example.employee_management_system.repository.EmployeeRepository;
import com.example.employee_management_system.repository.EmployeeSpecifications;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository repository;

    // Constructor injection (preferred over field @Autowired: easier to test, field can be final)
    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Employee> search(String q, Department department, EmployeeStatus status, Pageable pageable) {
        return repository.findAll(EmployeeSpecifications.filter(q, department, status), pageable);
    }

    @Transactional(readOnly = true)
    public Employee getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    @Transactional(readOnly = true)
    public List<Employee> exportAll() {
        return repository.findAll(Sort.by("id"));
    }

    @Transactional
    public Employee create(EmployeeRequest request) {
        String email = request.email().trim();
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An employee with email '" + email + "' already exists");
        }
        Employee employee = new Employee();
        apply(employee, request);
        return repository.save(employee);
    }

    @Transactional
    public Employee update(Long id, EmployeeRequest request) {
        Employee employee = getById(id);
        String email = request.email().trim();
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Another employee already uses email '" + email + "'");
        }
        apply(employee, request);
        return repository.save(employee);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id " + id);
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        // Pre-fill every enum value with 0 so the UI always receives all departments/statuses
        Map<String, Long> byDepartment = new LinkedHashMap<>();
        for (Department d : Department.values()) {
            byDepartment.put(d.name(), 0L);
        }
        for (Object[] row : repository.countGroupedByDepartment()) {
            byDepartment.put(((Department) row[0]).name(), (Long) row[1]);
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (EmployeeStatus s : EmployeeStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : repository.countGroupedByStatus()) {
            byStatus.put(((EmployeeStatus) row[0]).name(), (Long) row[1]);
        }

        return new StatsResponse(repository.count(), byDepartment, byStatus);
    }

    // Copies ALL fields, including status (the original project forgot status on update)
    private void apply(Employee employee, EmployeeRequest r) {
        employee.setName(r.name().trim());
        employee.setEmail(r.email().trim());
        employee.setDepartment(r.department());
        employee.setRole(blankToNull(r.role()));
        employee.setPhone(blankToNull(r.phone()));
        employee.setStatus(r.status());
        employee.setSalary(r.salary());
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
