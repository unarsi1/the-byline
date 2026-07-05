package com.thebyline.web;

import com.thebyline.config.SecurityConfig;
import com.thebyline.domain.post.*;
import com.thebyline.domain.user.Role;
import com.thebyline.domain.user.User;
import com.thebyline.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorController.class)
@Import(SecurityConfig.class)
class AuthorControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean PostRepository     postRepository;
    @MockBean CategoryRepository categoryRepository;
    @MockBean UserRepository     userRepository;
    @MockBean UserDetailsService userDetailsService;

    private User author;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(UUID.randomUUID())
                .email("author@thebyline.local")
                .username("author")
                .displayName("Test Author")
                .passwordHash("$2a$12$hash")
                .role(Role.AUTHOR)
                .enabled(true)
                .build();
    }

    // ── Security ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /author/dashboard redirects unauthenticated users to login")
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/author/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login**"));
    }

    @Test
    @WithMockUser(username = "author@thebyline.local", roles = "READER")
    @DisplayName("GET /author/dashboard returns 403 for READER role")
    void dashboard_readerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/author/dashboard"))
                .andExpect(status().isForbidden());
    }

    // ── Dashboard ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "author@thebyline.local", roles = "AUTHOR")
    @DisplayName("GET /author/dashboard returns 200 for AUTHOR role")
    void dashboard_authorRole_returns200() throws Exception {
        when(userRepository.findByEmail("author@thebyline.local")).thenReturn(Optional.of(author));
        when(postRepository.findByAuthorIdOrderByCreatedAtDesc(any(UUID.class), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/author/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("author/dashboard"))
                .andExpect(model().attributeExists("posts", "user"));
    }

    // ── New post form ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "author@thebyline.local", roles = "AUTHOR")
    @DisplayName("GET /author/posts/new returns 200 with empty form")
    void newPostForm_returns200() throws Exception {
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

        mockMvc.perform(get("/author/posts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("author/post-form"))
                .andExpect(model().attribute("editing", false))
                .andExpect(model().attributeExists("categories", "statuses", "post"));
    }

    // ── Create post ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "author@thebyline.local", roles = "AUTHOR")
    @DisplayName("POST /author/posts/new with valid data redirects to dashboard")
    void createPost_validData_redirectsToDashboard() throws Exception {
        when(userRepository.findByEmail("author@thebyline.local")).thenReturn(Optional.of(author));
        when(postRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/author/posts/new").with(csrf())
                        .param("title",    "My First Post")
                        .param("subtitle", "A subtitle")
                        .param("bodyHtml", "<p>Content here</p>")
                        .param("status",   "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/author/dashboard"))
                .andExpect(flash().attribute("success", "Post created successfully."));
    }

    // ── Edit post ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "author@thebyline.local", roles = "AUTHOR")
    @DisplayName("GET /author/posts/{id}/edit returns 200 for post owned by current author")
    void editPostForm_ownPost_returns200() throws Exception {
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .title("Owned Post")
                .slug("owned-post")
                .status(PostStatus.DRAFT)
                .visibility(PostVisibility.PUBLIC)
                .author(author)
                .build();

        when(userRepository.findByEmail("author@thebyline.local")).thenReturn(Optional.of(author));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

        mockMvc.perform(get("/author/posts/{id}/edit", post.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("author/post-form"))
                .andExpect(model().attribute("editing", true));
    }

    @Test
    @WithMockUser(username = "author@thebyline.local", roles = "AUTHOR")
    @DisplayName("GET /author/posts/{id}/edit returns 403 for post owned by another author")
    void editPostForm_otherAuthorPost_returns403() throws Exception {
        User otherAuthor = User.builder()
                .id(UUID.randomUUID())
                .email("other@thebyline.local")
                .username("other")
                .displayName("Other Author")
                .passwordHash("hash")
                .role(Role.AUTHOR)
                .enabled(true)
                .build();

        Post post = Post.builder()
                .id(UUID.randomUUID())
                .title("Other's Post")
                .slug("others-post")
                .status(PostStatus.DRAFT)
                .visibility(PostVisibility.PUBLIC)
                .author(otherAuthor)
                .build();

        when(userRepository.findByEmail("author@thebyline.local")).thenReturn(Optional.of(author));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        mockMvc.perform(get("/author/posts/{id}/edit", post.getId()))
                .andExpect(status().isForbidden());
    }

    // ── Delete post ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "author@thebyline.local", roles = "AUTHOR")
    @DisplayName("POST /author/posts/{id}/delete deletes own post and redirects")
    void deletePost_ownPost_redirectsToDashboard() throws Exception {
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .title("Post to Delete")
                .slug("post-to-delete")
                .status(PostStatus.DRAFT)
                .visibility(PostVisibility.PUBLIC)
                .author(author)
                .build();

        when(userRepository.findByEmail("author@thebyline.local")).thenReturn(Optional.of(author));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        mockMvc.perform(post("/author/posts/{id}/delete", post.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/author/dashboard"))
                .andExpect(flash().attribute("success", "Post deleted."));
    }
}
