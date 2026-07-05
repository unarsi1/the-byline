package com.thebyline.web;

import com.thebyline.config.SecurityConfig;
import com.thebyline.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean UserRepository     userRepository;
    @MockBean PasswordEncoder    passwordEncoder;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /auth/login ───────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("GET /auth/login returns 200 with login view")
    void loginPage_returns200() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    // ── GET /auth/register ────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("GET /auth/register returns 200 with register view")
    void registerPage_returns200() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    // ── POST /auth/register — happy path ──────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/register with valid data redirects to login with flash")
    void register_validData_redirectsToLogin() throws Exception {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("jane")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");

        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email",       "jane@example.com")
                        .param("username",    "jane")
                        .param("displayName", "Jane Doe")
                        .param("password",    "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attribute("registered", true));
    }

    // ── POST /auth/register — duplicate email ─────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/register with existing email re-renders form with error")
    void register_duplicateEmail_showsError() throws Exception {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email",       "taken@example.com")
                        .param("username",    "newuser")
                        .param("displayName", "New User")
                        .param("password",    "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }

    // ── POST /auth/register — duplicate username ──────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/register with existing username re-renders form with error")
    void register_duplicateUsername_showsError() throws Exception {
        when(userRepository.existsByEmail("unique@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("takenuser")).thenReturn(true);

        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email",       "unique@example.com")
                        .param("username",    "takenuser")
                        .param("displayName", "Unique User")
                        .param("password",    "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }

    // ── POST /auth/register — invalid email ───────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/register with invalid email re-renders form with error")
    void register_invalidEmail_showsError() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email",       "not-an-email")
                        .param("username",    "validuser")
                        .param("displayName", "Valid User")
                        .param("password",    "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }

    // ── POST /auth/register — blank display name ──────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/register with blank display name re-renders form with error")
    void register_blankDisplayName_showsError() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email",       "valid@example.com")
                        .param("username",    "validuser")
                        .param("displayName", "  ")
                        .param("password",    "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }

    // ── POST /auth/register — form fields retained on error ──────────────

    @Test
    @WithAnonymousUser
    @DisplayName("POST /auth/register error retains submitted email and username in model")
    void register_error_retainsFormFields() throws Exception {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("email",       "taken@example.com")
                        .param("username",    "myuser")
                        .param("displayName", "My User")
                        .param("password",    "secret123"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("email",    "taken@example.com"))
                .andExpect(model().attribute("username", "myuser"));
    }
}
