package com.example.employee_management_system.dto;

import java.util.Map;

public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
