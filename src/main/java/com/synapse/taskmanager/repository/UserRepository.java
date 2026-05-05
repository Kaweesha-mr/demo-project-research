package com.synapse.taskmanager.repository;

import com.synapse.taskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByActive(Boolean active);
    Optional<User> findByUsernameAndPassword(String username, String password);
}
