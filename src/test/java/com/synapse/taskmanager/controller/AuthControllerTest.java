package com.synapse.taskmanager.controller;

import com.synapse.taskmanager.dto.LoginRequest;
import com.synapse.taskmanager.dto.RegisterRequest;
import com.synapse.taskmanager.dto.UserDTO;
import com.synapse.taskmanager.model.UserRole;
import com.synapse.taskmanager.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUserDTO = UserDTO.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .role(UserRole.USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testShowLoginForm() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testLoginSuccess() throws Exception {
        when(userService.login("john_doe", "password123"))
                .thenReturn(Optional.of(testUserDTO));

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .param("username", "john_doe")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        HttpSession session = result.getRequest().getSession();
        UserDTO sessionUser = (UserDTO) session.getAttribute("user");
        assert sessionUser != null;
        assert sessionUser.getUsername().equals("john_doe");
    }

    @Test
    void testLoginFailure() throws Exception {
        when(userService.login("john_doe", "wrongpassword"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                        .param("username", "john_doe")
                        .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void testShowRegisterForm() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void testRegisterSuccess() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(testUserDTO);

        mockMvc.perform(post("/auth/register")
                        .param("username", "john_doe")
                        .param("email", "john@example.com")
                        .param("password", "password123")
                        .param("fullName", "John Doe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        verify(userService).register(any(RegisterRequest.class));
    }

    @Test
    void testRegisterDuplicateUsername() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/auth/register")
                        .param("username", "john_doe")
                        .param("email", "john@example.com")
                        .param("password", "password123")
                        .param("fullName", "John Doe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"));
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(get("/auth/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void testProfileWithAuthentication() throws Exception {
        mockMvc.perform(get("/auth/profile")
                        .sessionAttr("user", testUserDTO))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", testUserDTO));
    }

    @Test
    void testProfileWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }
}
