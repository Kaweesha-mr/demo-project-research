package com.synapse.taskmanager.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserBuilderWithDefaults() {
        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("securepass123")
                .fullName("John Doe")
                .build();

        assertEquals(1L, user.getId());
        assertEquals("john_doe", user.getUsername());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("securepass123", user.getPassword());
        assertEquals("John Doe", user.getFullName());
        assertEquals(UserRole.USER, user.getRole());
        assertTrue(user.getActive());
    }

    @Test
    void testUserBuilderWithCustomRole() {
        User admin = User.builder()
                .username("admin_user")
                .email("admin@example.com")
                .password("adminpass")
                .role(UserRole.ADMIN)
                .build();

        assertEquals(UserRole.ADMIN, admin.getRole());
    }

    @Test
    void testUserBuilderWithInactiveUser() {
        User inactiveUser = User.builder()
                .username("inactive_user")
                .email("inactive@example.com")
                .password("pass123")
                .active(false)
                .build();

        assertFalse(inactiveUser.getActive());
    }

    @Test
    void testUserEquality() {
        User user1 = User.builder()
                .id(1L)
                .username("test_user")
                .email("test@example.com")
                .password("pass123")
                .build();

        User user2 = User.builder()
                .id(1L)
                .username("test_user")
                .email("test@example.com")
                .password("pass123")
                .build();

        assertEquals(user1, user2);
    }
}
