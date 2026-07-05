package com.thebyline.domain.post;

import com.thebyline.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_posts_slug",       columnList = "slug",       unique = true),
    @Index(name = "idx_posts_status",     columnList = "status"),
    @Index(name = "idx_posts_author",     columnList = "author_id"),
    @Index(name = "idx_posts_published",  columnList = "published_at"),
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, unique = true, length = 320)
    private String slug;

    @Column(length = 500)
    private String subtitle;

    /** Raw HTML from TinyMCE / Tiptap */
    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    /** Plain text for FTS and AI summarisation */
    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "cover_image_alt", length = 300)
    private String coverImageAlt;

    // ── SEO ──────────────────────────────────────────────────────────────

    @Column(name = "meta_title", length = 70)
    private String metaTitle;

    @Column(name = "meta_description", length = 160)
    private String metaDescription;

    @Column(name = "canonical_url", length = 500)
    private String canonicalUrl;

    // ── Workflow ──────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "editor_note", columnDefinition = "TEXT")
    private String editorNote;

    // ── People ────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editor_id")
    private User editor;

    // ── Taxonomy ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    // ── Stats ─────────────────────────────────────────────────────────────

    @Column(name = "view_count")
    @Builder.Default
    private long viewCount = 0;

    @Column(name = "estimated_read_minutes")
    private int estimatedReadMinutes;

    // ── AI-generated fields ───────────────────────────────────────────────

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_tags_suggested", length = 500)
    private String aiTagsSuggested;

    // ── Timestamps ────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Computed helpers (not persisted) ─────────────────────────────────

    /** Returns publishedAt formatted as "MMM d" (e.g. "Jun 20") for use in templates. */
    public String getPublishedAtFormatted() {
        if (publishedAt == null) return "";
        return DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                .format(publishedAt.atZone(ZoneOffset.UTC).toLocalDate());
    }

    /** Two-letter display abbreviation for the author avatar (e.g. "AB"). */
    public String getAuthorInitials() {
        if (author == null || author.getDisplayName() == null || author.getDisplayName().isBlank()) return "??";
        String name = author.getDisplayName().trim();
        return name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.ENGLISH);
    }
}
