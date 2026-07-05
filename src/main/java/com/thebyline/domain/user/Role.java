package com.thebyline.domain.user;

/**
 * Publication roles — ordered from lowest to highest privilege.
 * Stored as a VARCHAR in the users table.
 */
public enum Role {
    READER,      // anonymous / unregistered — read-only public content
    SUBSCRIBER,  // registered + (optionally) paying — access gated content
    AUTHOR,      // can create/edit own posts; submit for review
    EDITOR,      // can review, publish, and manage any post; moderate comments
    ADMIN        // full access including user management and billing
}
