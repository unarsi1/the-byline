package com.thebyline.web;

import com.thebyline.domain.user.Role;
import com.thebyline.domain.user.User;
import com.thebyline.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String displayName,
            @RequestParam String password,
            RedirectAttributes redirectAttrs,
            Model model) {

        // Helper: repopulate form fields so the user doesn't retype everything
        model.addAttribute("email",       email);
        model.addAttribute("username",    username);
        model.addAttribute("displayName", displayName);

        // BUG-003 fix: server-side email format validation
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            model.addAttribute("error", "Please enter a valid email address.");
            return "auth/register";
        }

        // BUG-002 fix: reject blank display name
        if (displayName == null || displayName.isBlank()) {
            model.addAttribute("error", "Display name is required.");
            return "auth/register";
        }

        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "An account with that email already exists.");
            return "auth/register";
        }
        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "That username is already taken.");
            return "auth/register";
        }

        // First registered user becomes ADMIN; all subsequent users are READER
        boolean isFirstUser = userRepository.count() == 0;
        Role role = isFirstUser ? Role.ADMIN : Role.READER;

        User user = User.builder()
                .email(email)
                .username(username)
                .displayName(displayName)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);

        redirectAttrs.addFlashAttribute("registered", true);
        return "redirect:/auth/login";
    }
}
