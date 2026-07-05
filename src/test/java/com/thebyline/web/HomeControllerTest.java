package com.thebyline.web;

import com.thebyline.config.SecurityConfig;
import com.thebyline.domain.post.*;
import com.thebyline.domain.user.User;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @Import(SecurityConfig.class) is required so that the custom SecurityFilterChain
 * is used instead of Spring Boot's default HTTP-Basic auto-configuration.
 * Without it, @WebMvcTest only scans web-layer beans and the default filter chain
 * (which responds 401 everywhere) would be applied instead.
 */
@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean PostService        postService;
    @MockBean CategoryRepository categoryRepository;

    /** Required by SecurityConfig's RememberMeConfigurer and AuthenticationManager. */
    @MockBean UserDetailsService userDetailsService;

    @Test
    @WithAnonymousUser
    @DisplayName("GET / returns 200 with empty post list")
    void home_anonymous_returns200() throws Exception {
        when(postService.findFeatured()).thenReturn(Optional.empty());
        when(postService.findLatest(anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(postService.findTrending(anyInt())).thenReturn(List.of());
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("posts", "trending", "categories"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET / surfaces the featured post when one exists")
    void home_withFeaturedPost_rendersFeatured() throws Exception {
        User author = User.builder()
                .id(UUID.randomUUID())
                .email("author@example.com")
                .username("author")
                .displayName("Test Author")
                .passwordHash("hash")
                .enabled(true)
                .build();
        Post featured = Post.builder()
                .id(UUID.randomUUID())
                .title("Featured Article")
                .slug("featured-article")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(Instant.now())
                .estimatedReadMinutes(5)
                .author(author)
                .build();

        when(postService.findFeatured()).thenReturn(Optional.of(featured));
        when(postService.findLatest(anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(postService.findTrending(anyInt())).thenReturn(List.of());
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("featured", featured));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /?page=1 passes page parameter to service")
    void home_withPageParam_passesPageToService() throws Exception {
        when(postService.findFeatured()).thenReturn(Optional.empty());
        when(postService.findLatest(eq(1), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(postService.findTrending(anyInt())).thenReturn(List.of());
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

        mockMvc.perform(get("/").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 1));
    }
}
