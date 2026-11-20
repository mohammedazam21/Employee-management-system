package com.example.employee_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who can do what:
 *  - Anyone:      login + sign-up pages, static assets
 *  - USER:         view employees (salary hidden)
 *  - HR:           view / add / edit employees
 *  - ADMIN:        everything, including delete and user management
 * The server enforces this on every request. Hiding a button in the UI is NOT security.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Session-cookie login. CSRF protection is disabled to keep the learning project simple;
                // the mitigation here is SameSite=Lax cookies (see application.properties).
                // A production system would enable CSRF tokens or use JWT in a header.
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // public: login/sign-up pages, static assets, and the sign-up endpoint
                        .requestMatchers("/login.html", "/register.html", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        // rules are checked top to bottom, first match wins
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/employees/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PUT, "/api/employees/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.GET, "/api/employees/export").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.GET, "/api/employees/**").hasAnyRole("ADMIN", "HR", "USER")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> response.setStatus(200))
                        .failureHandler((request, response, exception) -> response.setStatus(401))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(200)))
                .exceptionHandling(ex -> ex
                        // API calls get a 401 status; browser page requests get redirected to the login page
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.sendError(401, "Unauthorized");
                            } else {
                                response.sendRedirect("/login.html");
                            }
                        }));
        return http.build();
    }
}
