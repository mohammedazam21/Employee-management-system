package com.example.employee_management_system.repository;

import com.example.employee_management_system.entity.Employee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    // One GROUP BY query done by the database, instead of loading every employee into memory
    @Query("select e.department, count(e) from Employee e group by e.department")
    List<Object[]> countGroupedByDepartment();

    @Query("select e.status, count(e) from Employee e group by e.status")
    List<Object[]> countGroupedByStatus();
}
