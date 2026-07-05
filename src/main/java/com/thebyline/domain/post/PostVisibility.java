package com.thebyline.domain.post;

public enum PostVisibility {
    PUBLIC,           // free, visible to all
    SUBSCRIBER_ONLY,  // requires free account (SUBSCRIBER role)
    PAID_ONLY         // requires active paid subscription
}
