package com.thebyline.domain.comment;

public enum CommentStatus {
    PENDING,   // awaiting moderation
    APPROVED,  // visible
    SPAM,      // flagged as spam, hidden
    REMOVED    // removed by moderator
}
