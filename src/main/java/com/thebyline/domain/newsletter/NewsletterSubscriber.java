package com.thebyline.domain.newsletter;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "newsletter_subscribers", indexes = {
    @Index(name = "idx_newsletter_email", columnList = "email", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(nullable = false)
    @Builder.Default
    private boolean confirmed = false;

    @Column(name = "confirmation_token", length = 100)
    private String confirmationToken;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "unsubscribed_at")
    private Instant unsubscribedAt;

    /** External ID from Mailchimp / Buttondown if synced */
    @Column(name = "external_id", length = 100)
    private String externalId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
