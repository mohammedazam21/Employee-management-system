package com.example.employee_management_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Visiting http://localhost:8080/ opens the dashboard (login is enforced by SecurityConfig)
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
