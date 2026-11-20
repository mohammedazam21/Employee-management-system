package com.example.employee_management_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.employee_management_system.entity.AppUser;
import com.example.employee_management_system.entity.Role;
import com.example.employee_management_system.exception.BadRequestException;
import com.example.employee_management_system.exception.DuplicateResourceException;
import com.example.employee_management_system.exception.ResourceNotFoundException;
import com.example.employee_management_system.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository users;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService service;

    @Test
    void register_alwaysCreatesUserRole_andStoresHashNotPlainPassword() {
        when(users.existsByUsername("newbie")).thenReturn(false);
        when(encoder.encode("secret123")).thenReturn("HASHED");
        when(users.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser saved = service.register("  newbie ", "secret123");

        assertEquals("newbie", saved.getUsername());          // trimmed
        assertEquals(Role.USER, saved.getRole());              // never ADMIN/HR
        assertEquals("HASHED", saved.getPasswordHash());       // hash, not "secret123"
        assertNotEquals("secret123", saved.getPasswordHash());
    }

    @Test
    void register_duplicateUsername_throwsAndNeverSaves() {
        when(users.existsByUsername("admin")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.register("admin", "secret123"));

        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void changeRole_updatesRoleOfAnotherUser() {
        AppUser target = new AppUser("bob", "hash", Role.USER);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser updated = service.changeRole(2L, Role.HR, "admin");

        assertEquals(Role.HR, updated.getRole());
    }

    @Test
    void changeRole_ownAccount_isRejected() {
        AppUser self = new AppUser("admin", "hash", Role.ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(self));

        assertThrows(BadRequestException.class, () -> service.changeRole(1L, Role.USER, "admin"));

        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void changeRole_unknownUser_throwsNotFound() {
        when(users.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.changeRole(99L, Role.HR, "admin"));
    }
}
