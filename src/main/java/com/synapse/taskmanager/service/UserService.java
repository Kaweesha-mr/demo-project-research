package com.synapse.taskmanager.service;

import com.synapse.taskmanager.dto.RegisterRequest;
import com.synapse.taskmanager.dto.UserDTO;
import com.synapse.taskmanager.model.User;
import com.synapse.taskmanager.model.UserRole;
import com.synapse.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserDTO register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .role(UserRole.USER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());
        return toDTO(savedUser);
    }

    public Optional<UserDTO> login(String username, String password) {
        log.debug("Login attempt for user: {}", username);
        Optional<User> user = userRepository.findByUsernameAndPassword(username, password);

        if (user.isPresent() && user.get().getActive()) {
            User u = user.get();
            u.setLastLogin(LocalDateTime.now());
            userRepository.save(u);
            log.info("User logged in successfully: {}", username);
            return Optional.of(toDTO(u));
        }

        log.warn("Login failed for user: {}", username);
        return Optional.empty();
    }

    public Optional<UserDTO> findById(Long id) {
        return userRepository.findById(id).map(this::toDTO);
    }

    public Optional<UserDTO> findByUsername(String username) {
        return userRepository.findByUsername(username).map(this::toDTO);
    }

    public List<UserDTO> findAllActive() {
        return userRepository.findByActive(true).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO updateUserRole(Long userId, UserRole role) {
        log.info("Updating user {} role to {}", userId, role);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(role);
        return toDTO(userRepository.save(user));
    }

    public void deactivateUser(Long userId) {
        log.info("Deactivating user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }


    
    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
