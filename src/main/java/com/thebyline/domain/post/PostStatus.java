package com.thebyline.domain.post;

public enum PostStatus {
    DRAFT,       // author is writing
    IN_REVIEW,   // submitted to editor queue
    SCHEDULED,   // approved, waiting for publish_at
    PUBLISHED,   // live
    ARCHIVED     // removed from feeds, still accessible by URL
}
