package com.thebyline.domain.newsletter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, UUID> {

    boolean existsByEmail(String email);

    Optional<NewsletterSubscriber> findByConfirmationToken(String token);

    Optional<NewsletterSubscriber> findByEmail(String email);
}
