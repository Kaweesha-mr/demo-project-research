package com.synapse.taskmanager.repository;

import com.synapse.taskmanager.model.User;
import com.synapse.taskmanager.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("securepass123")
                .fullName("John Doe")
                .role(UserRole.USER)
                .active(true)
                .build();
    }

    @Test
    void testSaveUser() {
        User saved = userRepository.save(testUser);

        assertNotNull(saved.getId());
        assertEquals("john_doe", saved.getUsername());
        assertEquals("john@example.com", saved.getEmail());
    }

    @Test
    void testFindByUsername() {
        userRepository.save(testUser);

        Optional<User> found = userRepository.findByUsername("john_doe");

        assertTrue(found.isPresent());
        assertEquals("john@example.com", found.get().getEmail());
    }

    @Test
    void testFindByUsernameNotFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void testFindByEmail() {
        userRepository.save(testUser);

        Optional<User> found = userRepository.findByEmail("john@example.com");

        assertTrue(found.isPresent());
        assertEquals("john_doe", found.get().getUsername());
    }

    @Test
    void testFindByEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void testFindByUsernameAndPassword() {
        userRepository.save(testUser);

        Optional<User> found = userRepository.findByUsernameAndPassword("john_doe", "securepass123");

        assertTrue(found.isPresent());
    }

    @Test
    void testFindByUsernameAndPasswordWrongPassword() {
        userRepository.save(testUser);

        Optional<User> found = userRepository.findByUsernameAndPassword("john_doe", "wrongpassword");

        assertFalse(found.isPresent());
    }

    @Test
    void testFindByActive() {
        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .password("password123")
                .active(true)
                .build();

        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .password("password456")
                .active(false)
                .build();

        userRepository.save(user1);
        userRepository.save(user2);

        List<User> activeUsers = userRepository.findByActive(true);

        assertTrue(activeUsers.size() > 0);
        assertTrue(activeUsers.stream().allMatch(User::getActive));
    }

    @Test
    void testUniqueUsernameConstraint() {
        userRepository.save(testUser);

        User duplicate = User.builder()
                .username("john_doe")
                .email("different@example.com")
                .password("pass123")
                .build();

        assertThrows(Exception.class, () -> {
            userRepository.save(duplicate);
            userRepository.flush();
        });
    }

    @Test
    void testUniqueEmailConstraint() {
        userRepository.save(testUser);

        User duplicate = User.builder()
                .username("different_user")
                .email("john@example.com")
                .password("pass123")
                .build();

        assertThrows(Exception.class, () -> {
            userRepository.save(duplicate);
            userRepository.flush();
        });
    }

    @Test
    void testUpdateUser() {
        User saved = userRepository.save(testUser);
        saved.setFullName("Jane Doe");
        saved.setRole(UserRole.MANAGER);

        User updated = userRepository.save(saved);

        assertEquals("Jane Doe", updated.getFullName());
        assertEquals(UserRole.MANAGER, updated.getRole());
    }

    @Test
    void testDeleteUser() {
        User saved = userRepository.save(testUser);
        Long id = saved.getId();

        userRepository.deleteById(id);

        Optional<User> found = userRepository.findById(id);
        assertFalse(found.isPresent());
    }
}
