package com.synapse.taskmanager.service;

import com.synapse.taskmanager.dto.RegisterRequest;
import com.synapse.taskmanager.dto.UserDTO;
import com.synapse.taskmanager.model.User;
import com.synapse.taskmanager.model.UserRole;
import com.synapse.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("securepass123")
                .fullName("John Doe")
                .role(UserRole.USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testRegisterUserSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("new_user")
                .email("newuser@example.com")
                .password("password123")
                .fullName("New User")
                .build();

        when(userRepository.findByUsername("new_user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.register(request);

        assertNotNull(result);
        assertEquals("john_doe", result.getUsername());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUserWithDuplicateUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john_doe")
                .email("john2@example.com")
                .password("password123")
                .fullName("John Doe 2")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.register(request);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUserWithDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .username("new_user")
                .email("john@example.com")
                .password("password123")
                .fullName("John Doe 2")
                .build();

        when(userRepository.findByUsername("new_user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.register(request);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        when(userRepository.findByUsernameAndPassword("john_doe", "securepass123"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Optional<UserDTO> result = userService.login("john_doe", "securepass123");

        assertTrue(result.isPresent());
        assertEquals("john_doe", result.get().getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testLoginFailureInvalidCredentials() {
        when(userRepository.findByUsernameAndPassword("john_doe", "wrongpassword"))
                .thenReturn(Optional.empty());

        Optional<UserDTO> result = userService.login("john_doe", "wrongpassword");

        assertFalse(result.isPresent());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginFailureInactiveUser() {
        User inactiveUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("securepass123")
                .active(false)
                .build();

        when(userRepository.findByUsernameAndPassword("john_doe", "securepass123"))
                .thenReturn(Optional.of(inactiveUser));

        Optional<UserDTO> result = userService.login("john_doe", "securepass123");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByIdSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<UserDTO> result = userService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("john_doe", result.get().getUsername());
    }

    @Test
    void testFindByIdNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<UserDTO> result = userService.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByUsername() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

        Optional<UserDTO> result = userService.findByUsername("john_doe");

        assertTrue(result.isPresent());
        assertEquals("john@example.com", result.get().getEmail());
    }

    @Test
    void testUpdateUserRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        User updatedUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("securepass123")
                .role(UserRole.ADMIN)
                .active(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserDTO result = userService.updateUserRole(1L, UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, result.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUserRoleNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateUserRole(999L, UserRole.ADMIN);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testDeactivateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        User deactivatedUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("securepass123")
                .active(false)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(deactivatedUser);

        userService.deactivateUser(1L);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testDeactivateUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.deactivateUser(999L);
        });

        assertEquals("User not found", exception.getMessage());
    }
}
