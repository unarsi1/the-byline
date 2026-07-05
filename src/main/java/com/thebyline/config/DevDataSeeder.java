package com.thebyline.config;

import com.thebyline.domain.post.*;
import com.thebyline.domain.user.Role;
import com.thebyline.domain.user.User;
import com.thebyline.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Seeds realistic sample data in the "dev" Spring profile only.
 * Skips seeding if posts already exist, making re-runs safe.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository      userRepository;
    private final CategoryRepository  categoryRepository;
    private final PostRepository      postRepository;
    private final PasswordEncoder     passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        if (postRepository.count() > 0) {
            log.info("[DevDataSeeder] Posts already exist — skipping seed.");
            return;
        }

        log.info("[DevDataSeeder] Seeding dev data…");

        // ── Admin / author user ─────────────────────────────────────────
        User admin = userRepository.findByEmail("admin@thebyline.local").orElseGet(() -> {
            User u = new User();
            u.setEmail("admin@thebyline.local");
            u.setUsername("admin");
            u.setDisplayName("Alex Rivera");
            u.setPasswordHash(passwordEncoder.encode("password"));
            u.setRole(Role.ADMIN);
            u.setEmailVerified(true);
            u.setEnabled(true);
            u.setBio("Editor-in-chief and co-founder of The Byline.");
            return userRepository.save(u);
        });

        User author2 = userRepository.findByEmail("sam@thebyline.local").orElseGet(() -> {
            User u = new User();
            u.setEmail("sam@thebyline.local");
            u.setUsername("sam");
            u.setDisplayName("Sam Chen");
            u.setPasswordHash(passwordEncoder.encode("password"));
            u.setRole(Role.AUTHOR);
            u.setEmailVerified(true);
            u.setEnabled(true);
            u.setBio("Science and technology correspondent.");
            return userRepository.save(u);
        });

        // ── Load categories (seeded by V1 migration) ────────────────────
        Category tech    = categoryRepository.findBySlug("technology").orElseThrow();
        Category culture = categoryRepository.findBySlug("culture").orElseThrow();
        Category science = categoryRepository.findBySlug("science").orElseThrow();
        Category opinion = categoryRepository.findBySlug("opinion").orElseThrow();
        Category env     = categoryRepository.findBySlug("environment").orElseThrow();

        // ── 5 published posts ───────────────────────────────────────────
        savePost(postRepository, Post.builder()
                .title("The Quiet Revolution in Open Source AI")
                .slug("quiet-revolution-open-source-ai")
                .subtitle("How small teams with big ideas are outpacing the labs.")
                .bodyHtml("<p>Something shifted in 2024. The assumption that frontier AI required billion-dollar compute budgets started cracking…</p>")
                .bodyText("Something shifted in 2024. The assumption that frontier AI required billion-dollar compute budgets started cracking…")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .estimatedReadMinutes(6)
                .viewCount(1240)
                .author(admin)
                .category(tech)
                .build());

        savePost(postRepository, Post.builder()
                .title("What the Streaming Wars Actually Cost Viewers")
                .slug("streaming-wars-cost-viewers")
                .subtitle("Three platforms, a dozen logins, and somehow still nothing to watch.")
                .bodyHtml("<p>The golden age of television was supposed to end scarcity. Instead it created a different kind of scarcity: time and attention.</p>")
                .bodyText("The golden age of television was supposed to end scarcity. Instead it created a different kind of scarcity: time and attention.")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .estimatedReadMinutes(5)
                .viewCount(890)
                .author(author2)
                .category(culture)
                .build());

        savePost(postRepository, Post.builder()
                .title("A New Kind of Antibiotic Found in Antarctic Soil")
                .slug("antibiotic-antarctic-soil")
                .subtitle("Researchers report a compound that may work against drug-resistant bacteria.")
                .bodyHtml("<p>Scientists have long suspected that extreme environments harbour microbes with unusual chemistry. A paper published this week suggests they were right.</p>")
                .bodyText("Scientists have long suspected that extreme environments harbour microbes with unusual chemistry.")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(Instant.now().minus(3, ChronoUnit.DAYS))
                .estimatedReadMinutes(4)
                .viewCount(2100)
                .author(author2)
                .category(science)
                .build());

        savePost(postRepository, Post.builder()
                .title("Stop Pretending Productivity Culture Is Neutral")
                .slug("productivity-culture-not-neutral")
                .subtitle("The hustle gospel has a politics, and it's time we named it.")
                .bodyHtml("<p>Every era produces its dominant moral vocabulary. Ours is the language of optimisation.</p>")
                .bodyText("Every era produces its dominant moral vocabulary. Ours is the language of optimisation.")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(Instant.now().minus(4, ChronoUnit.DAYS))
                .estimatedReadMinutes(7)
                .viewCount(650)
                .author(admin)
                .category(opinion)
                .build());

        savePost(postRepository, Post.builder()
                .title("The River That Learned to Sue")
                .slug("river-legal-rights-case")
                .subtitle("Inside the landmark case that could give waterways the same standing as corporations.")
                .bodyHtml("<p>In a small courtroom in New Zealand, a judge read aloud a decision that would ripple outward for decades.</p>")
                .bodyText("In a small courtroom in New Zealand, a judge read aloud a decision that would ripple outward for decades.")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .estimatedReadMinutes(8)
                .viewCount(1760)
                .author(author2)
                .category(env)
                .build());

        log.info("[DevDataSeeder] Done — seeded 2 users and 5 posts.");
    }

    private void savePost(PostRepository repo, Post post) {
        repo.save(post);
        log.debug("[DevDataSeeder] Saved post: {}", post.getSlug());
    }
}
