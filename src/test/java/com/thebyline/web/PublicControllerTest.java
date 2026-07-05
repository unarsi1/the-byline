package com.thebyline.web;

import com.thebyline.config.SecurityConfig;
import com.thebyline.domain.newsletter.NewsletterSubscriberRepository;
import com.thebyline.domain.post.*;
import com.thebyline.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicController.class)
@Import(SecurityConfig.class)
class PublicControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean CategoryRepository             categoryRepository;
    @MockBean UserRepository                 userRepository;
    @MockBean PostService                    postService;
    @MockBean PostRepository                 postRepository;
    @MockBean NewsletterSubscriberRepository subscriberRepository;
    @MockBean UserDetailsService             userDetailsService;

    // ── /topics ──────────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("GET /topics returns 200 with categories")
    void topics_returns200() throws Exception {
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

        mockMvc.perform(get("/topics"))
                .andExpect(status().isOk())
                .andExpect(view().name("topics"))
                .andExpect(model().attributeExists("categories"));
    }

    // ── /topics/{slug} ───────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("GET /topics/{slug} returns 200 for known category")
    void topic_knownSlug_returns200() throws Exception {
        Category tech = Category.builder()
                .id(UUID.randomUUID()).name("Technology").slug("technology").build();
        when(categoryRepository.findBySlug("technology")).thenReturn(Optional.of(tech));
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(tech));
        when(postService.findByCategory(eq(tech), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/topics/technology"))
                .andExpect(status().isOk())
                .andExpect(view().name("topic"))
                .andExpect(model().attribute("category", tech))
                .andExpect(model().attributeExists("posts"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /topics/{slug} returns 404 for unknown category")
    void topic_unknownSlug_returns404() throws Exception {
        when(categoryRepository.findBySlug("no-such-topic")).thenReturn(Optional.empty());

        mockMvc.perform(get("/topics/no-such-topic"))
                .andExpect(status().isNotFound());
    }

    // ── /archive ─────────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("GET /archive returns 200 with posts")
    void archive_returns200() throws Exception {
        when(postRepository.findByStatusAndVisibilityOrderByPublishedAtDesc(
                any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/archive"))
                .andExpect(status().isOk())
                .andExpect(view().name("archive"));
    }

    // ── /about ───────────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("GET /about returns 200")
    void about_returns200() throws Exception {
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("about"));
    }

    // ── /newsletter/subscribe ────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("POST /newsletter/subscribe with new email redirects with success flash")
    void subscribe_newEmail_redirectsWithSuccess() throws Exception {
        when(subscriberRepository.existsByEmail("new@example.com")).thenReturn(false);

        mockMvc.perform(post("/newsletter/subscribe").with(csrf())
                        .param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("newsletterSuccess"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /newsletter/subscribe with existing email still succeeds (idempotent)")
    void subscribe_existingEmail_redirectsWithSuccess() throws Exception {
        when(subscriberRepository.existsByEmail("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/newsletter/subscribe").with(csrf())
                        .param("email", "existing@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("newsletterSuccess"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST /newsletter/subscribe with invalid email redirects with error flash")
    void subscribe_invalidEmail_redirectsWithError() throws Exception {
        mockMvc.perform(post("/newsletter/subscribe").with(csrf())
                        .param("email", "not-valid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("newsletterError"));
    }
}
