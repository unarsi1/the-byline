package com.thebyline.web;

import com.thebyline.config.SecurityConfig;
import com.thebyline.domain.post.PostRepository;
import com.thebyline.domain.post.PostStatus;
import com.thebyline.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean UserRepository     userRepository;
    @MockBean PostRepository     postRepository;
    @MockBean UserDetailsService userDetailsService;

    // ── Security ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin redirects unauthenticated users to login")
    void adminDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    @DisplayName("GET /admin returns 403 for AUTHOR role")
    void adminDashboard_authorRole_isForbidden() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    @DisplayName("GET /admin returns 403 for EDITOR role")
    void adminDashboard_editorRole_isForbidden() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    // ── Dashboard content ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin returns 200 with dashboard attributes for ADMIN role")
    void adminDashboard_adminRole_returns200() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of());
        when(postRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(new PageImpl<>(List.of()));
        when(postRepository.findByStatusOrderByCreatedAtAsc(PostStatus.DRAFT)).thenReturn(List.of());
        when(postRepository.findByStatusOrderByCreatedAtAsc(PostStatus.IN_REVIEW)).thenReturn(List.of());

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("users", "posts", "draftCount", "reviewCount"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/ (trailing slash) also returns 200")
    void adminDashboard_trailingSlash_returns200() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of());
        when(postRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(new PageImpl<>(List.of()));
        when(postRepository.findByStatusOrderByCreatedAtAsc(any(PostStatus.class))).thenReturn(List.of());

        mockMvc.perform(get("/admin/"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }
}
