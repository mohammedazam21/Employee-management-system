package com.example.employee_management_system.dto;

import java.util.Map;

public record StatsResponse(
        long total,
        Map<String, Long> byDepartment,
        Map<String, Long> byStatus
) {
}
