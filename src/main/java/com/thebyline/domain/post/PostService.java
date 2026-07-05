package com.thebyline.domain.post;

import com.thebyline.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Cacheable(CacheConfig.POSTS_FEATURED)
    public Optional<Post> findFeatured() {
        return postRepository.findTopByStatusAndVisibilityOrderByPublishedAtDesc(
                PostStatus.PUBLISHED, PostVisibility.PUBLIC);
    }

    @Cacheable(CacheConfig.POSTS_LATEST)
    public Page<Post> findLatest(int page, int size) {
        return postRepository.findByStatusAndVisibilityOrderByPublishedAtDesc(
                PostStatus.PUBLISHED, PostVisibility.PUBLIC,
                PageRequest.of(page, size));
    }

    public Page<Post> findByCategory(Category category, int page, int size) {
        return postRepository.findByStatusAndVisibilityAndCategoryOrderByPublishedAtDesc(
                PostStatus.PUBLISHED, PostVisibility.PUBLIC, category,
                PageRequest.of(page, size));
    }

    public List<Post> findTrending(int limit) {
        return postRepository.findTopByViewCount(PageRequest.of(0, limit));
    }

    @Cacheable(CacheConfig.POST_BY_SLUG)
    public Optional<Post> findBySlug(String slug) {
        return postRepository.findBySlug(slug);
    }
}
