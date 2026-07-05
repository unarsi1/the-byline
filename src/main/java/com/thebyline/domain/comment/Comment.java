package com.thebyline.domain.comment;

import com.thebyline.domain.post.Post;
import com.thebyline.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_post",   columnList = "post_id"),
    @Index(name = "idx_comments_parent", columnList = "parent_id"),
    @Index(name = "idx_comments_status", columnList = "status"),
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Null for top-level comments; set for threaded replies (one level deep). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CommentStatus status = CommentStatus.PENDING;

    @Column(name = "spam_score", columnDefinition = "DOUBLE PRECISION")
    private Double spamScore;

    @Column(name = "like_count")
    @Builder.Default
    private int likeCount = 0;

    @Column(name = "moderator_note", length = 500)
    private String moderatorNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
