package com.synapse.taskmanager.controller;

import com.synapse.taskmanager.dto.LoginRequest;
import com.synapse.taskmanager.dto.RegisterRequest;
import com.synapse.taskmanager.dto.UserDTO;
import com.synapse.taskmanager.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest request,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        Optional<UserDTO> user = userService.login(request.getUsername(), request.getPassword());

        if (user.isPresent()) {
            session.setAttribute("user", user.get());
            log.info("User {} logged in successfully", request.getUsername());
            return "redirect:/";
        }

        redirectAttributes.addFlashAttribute("error", "Invalid username or password");
        return "redirect:/auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            UserDTO user = userService.register(request);
            log.info("User {} registered successfully", user.getUsername());
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Logged out successfully");
        return "redirect:/auth/login";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        UserDTO user = (UserDTO) session.getAttribute("user");
        if (user == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("user", user);
        return "profile";
    }
}
