package com.example.employee_management_system.controller;

import com.example.employee_management_system.dto.EmployeeRequest;
import com.example.employee_management_system.dto.EmployeeResponse;
import com.example.employee_management_system.dto.PageResponse;
import com.example.employee_management_system.dto.StatsResponse;
import com.example.employee_management_system.entity.Department;
import com.example.employee_management_system.entity.EmployeeStatus;
import com.example.employee_management_system.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    // Only these columns may be used for sorting (prevents sorting by unknown fields)
    private static final Set<String> SORTABLE = Set.of("id", "name", "email", "department", "salary", "status");

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    /** GET /api/employees?q=ravi&department=IT&status=ACTIVE&page=0&size=10&sortBy=name&direction=asc */
    @GetMapping
    public PageResponse<EmployeeResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Authentication authentication) {

        String sortField = SORTABLE.contains(sortBy) ? sortBy : "id";
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // never let a client ask for 1,000,000 rows

        boolean showSalary = canSeeSalary(authentication);
        return PageResponse.from(
                service.search(q, department, status, PageRequest.of(safePage, safeSize, sort)),
                e -> EmployeeResponse.from(e, showSalary));
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return service.stats();
    }

    /** Full list used by the PDF export button. */
    @GetMapping("/export")
    public List<EmployeeResponse> export() {
        return service.exportAll().stream().map(EmployeeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id, Authentication authentication) {
        return EmployeeResponse.from(service.getById(id), canSeeSalary(authentication));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeResponse.from(service.create(request)));
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return EmployeeResponse.from(service.update(id, request));
    }

    // Only ADMIN may delete (enforced in SecurityConfig)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // Salary is confidential: only ADMIN and HR may see it. The read-only USER role gets null.
    private boolean canSeeSalary(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
    }
}
