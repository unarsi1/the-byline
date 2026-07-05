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

@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean PostService        postService;
    @MockBean PostRepository     postRepository;
    @MockBean UserDetailsService userDetailsService;

    private Post buildPublishedPost(String slug) {
        User author = User.builder()
                .id(UUID.randomUUID())
                .email("author@example.com")
                .username("author")
                .displayName("Test Author")
                .passwordHash("hash")
                .enabled(true)
                .build();
        return Post.builder()
                .id(UUID.randomUUID())
                .title("Test Article")
                .slug(slug)
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(Instant.now())
                .estimatedReadMinutes(5)
                .author(author)
                .viewCount(0L)
                .build();
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /articles/{slug} returns 200 for published public post")
    void article_published_returns200() throws Exception {
        Post post = buildPublishedPost("test-article");
        when(postService.findBySlug("test-article")).thenReturn(Optional.of(post));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postRepository.findByStatusAndVisibilityAndCategoryOrderByPublishedAtDesc(
                any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/articles/test-article"))
                .andExpect(status().isOk())
                .andExpect(view().name("post"))
                .andExpect(model().attributeExists("post", "related"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /articles/{slug} returns 404 for unknown slug")
    void article_unknownSlug_returns404() throws Exception {
        when(postService.findBySlug("no-such-post")).thenReturn(Optional.empty());

        mockMvc.perform(get("/articles/no-such-post"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /articles/{slug} returns 404 for draft post")
    void article_draftPost_returns404() throws Exception {
        Post draft = buildPublishedPost("draft-article");
        draft.setStatus(PostStatus.DRAFT);
        when(postService.findBySlug("draft-article")).thenReturn(Optional.of(draft));

        mockMvc.perform(get("/articles/draft-article"))
                .andExpect(status().isNotFound());
    }
}
