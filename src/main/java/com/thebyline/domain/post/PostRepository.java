package com.thebyline.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    @EntityGraph(attributePaths = {"author", "category"})
    Optional<Post> findBySlug(String slug);

    // ── Homepage queries ──────────────────────────────────────────────

    /** Single featured post — latest published public article */
    @EntityGraph(attributePaths = {"author", "category"})
    Optional<Post> findTopByStatusAndVisibilityOrderByPublishedAtDesc(
            PostStatus status, PostVisibility visibility);

    /** Feed: latest published posts, paged */
    @EntityGraph(attributePaths = {"author", "category"})
    Page<Post> findByStatusAndVisibilityOrderByPublishedAtDesc(
            PostStatus status, PostVisibility visibility, Pageable pageable);

    /** Feed filtered by category */
    @EntityGraph(attributePaths = {"author", "category"})
    Page<Post> findByStatusAndVisibilityAndCategoryOrderByPublishedAtDesc(
            PostStatus status, PostVisibility visibility, Category category, Pageable pageable);

    /** Trending sidebar — top viewed (JOIN FETCH required; @EntityGraph doesn't apply to @Query) */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.status = 'PUBLISHED' ORDER BY p.viewCount DESC")
    List<Post> findTopByViewCount(Pageable pageable);

    /** Author dashboard — own posts, author + category eagerly loaded */
    @EntityGraph(attributePaths = {"author", "category"})
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    /** Editor queue — posts awaiting review */
    List<Post> findByStatusOrderByCreatedAtAsc(PostStatus status);

    /** Admin: all posts paged, author + category eagerly loaded */
    @EntityGraph(attributePaths = {"author", "category"})
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Full-text search */
    @Query(value = """
            SELECT * FROM posts
            WHERE status = 'PUBLISHED'
              AND search_vector @@ plainto_tsquery('english', :query)
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC
            """, nativeQuery = true)
    List<Post> fullTextSearch(String query, Pageable pageable);
}
