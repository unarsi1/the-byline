package com.thebyline.domain.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for PostService — no Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    private Post publishedPost;

    @BeforeEach
    void setUp() {
        publishedPost = Post.builder()
                .id(UUID.randomUUID())
                .title("Test Article")
                .slug("test-article")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(Instant.now())
                .estimatedReadMinutes(3)
                .build();
    }

    @Test
    @DisplayName("findBySlug returns present Optional for known slug")
    void findBySlug_knownSlug_returnsPost() {
        when(postRepository.findBySlug("test-article")).thenReturn(Optional.of(publishedPost));

        Optional<Post> result = postService.findBySlug("test-article");

        assertThat(result).isPresent();
        assertThat(result.get().getSlug()).isEqualTo("test-article");
    }

    @Test
    @DisplayName("findBySlug returns empty Optional for unknown slug")
    void findBySlug_unknownSlug_returnsEmpty() {
        when(postRepository.findBySlug("does-not-exist")).thenReturn(Optional.empty());

        Optional<Post> result = postService.findBySlug("does-not-exist");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatest returns paged results")
    void findLatest_returnsPage() {
        Page<Post> page = new PageImpl<>(List.of(publishedPost));
        when(postRepository.findByStatusAndVisibilityOrderByPublishedAtDesc(
                eq(PostStatus.PUBLISHED), eq(PostVisibility.PUBLIC), any(PageRequest.class)))
                .thenReturn(page);

        Page<Post> result = postService.findLatest(0, 4);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Article");
    }

    @Test
    @DisplayName("findFeatured returns the most recent published post")
    void findFeatured_returnsLatestPost() {
        when(postRepository.findTopByStatusAndVisibilityOrderByPublishedAtDesc(
                PostStatus.PUBLISHED, PostVisibility.PUBLIC))
                .thenReturn(Optional.of(publishedPost));

        Optional<Post> result = postService.findFeatured();

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Test Article");
    }

    @Test
    @DisplayName("findFeatured returns empty when no published posts exist")
    void findFeatured_noPosts_returnsEmpty() {
        when(postRepository.findTopByStatusAndVisibilityOrderByPublishedAtDesc(
                PostStatus.PUBLISHED, PostVisibility.PUBLIC))
                .thenReturn(Optional.empty());

        Optional<Post> result = postService.findFeatured();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findTrending delegates to repository with correct page size")
    void findTrending_delegatesToRepository() {
        when(postRepository.findTopByViewCount(any(PageRequest.class)))
                .thenReturn(List.of(publishedPost));

        List<Post> result = postService.findTrending(4);

        assertThat(result).hasSize(1);
        verify(postRepository).findTopByViewCount(PageRequest.of(0, 4));
    }

    @Test
    @DisplayName("findByCategory delegates with correct status and visibility filters")
    void findByCategory_delegatesWithFilters() {
        Category tech = Category.builder()
                .id(UUID.randomUUID()).name("Technology").slug("technology").build();
        Page<Post> page = new PageImpl<>(List.of(publishedPost));
        when(postRepository.findByStatusAndVisibilityAndCategoryOrderByPublishedAtDesc(
                eq(PostStatus.PUBLISHED), eq(PostVisibility.PUBLIC), eq(tech), any()))
                .thenReturn(page);

        Page<Post> result = postService.findByCategory(tech, 0, 12);

        assertThat(result.getContent()).hasSize(1);
    }
}
