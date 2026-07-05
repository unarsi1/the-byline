package com.thebyline.web;

import com.thebyline.domain.newsletter.NewsletterSubscriber;
import com.thebyline.domain.newsletter.NewsletterSubscriberRepository;
import com.thebyline.domain.post.Category;
import com.thebyline.domain.post.CategoryRepository;
import com.thebyline.domain.post.PostRepository;
import com.thebyline.domain.post.PostService;
import com.thebyline.domain.post.PostStatus;
import com.thebyline.domain.post.PostVisibility;
import com.thebyline.domain.user.Role;
import com.thebyline.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PublicController {

    private final CategoryRepository            categoryRepository;
    private final UserRepository                userRepository;
    private final PostService                   postService;
    private final PostRepository                postRepository;
    private final NewsletterSubscriberRepository subscriberRepository;

    // ── /topics ──────────────────────────────────────────────────────────

    @GetMapping("/topics")
    public String topics(Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByDisplayOrderAsc());
        return "topics";
    }

    // ── /topics/{slug} ───────────────────────────────────────────────────

    /**
     * BUG-FIX: This route was missing. Every category tab on the homepage and
     * every article page links here. Without it, clicking a category → 404.
     */
    @GetMapping("/topics/{slug}")
    public String topic(@PathVariable String slug,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var posts = postService.findByCategory(category, page, 12);
        model.addAttribute("category",    category);
        model.addAttribute("posts",       posts);
        model.addAttribute("categories",  categoryRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("currentPage", page);
        return "topic";
    }

    // ── /authors ─────────────────────────────────────────────────────────

    @GetMapping("/authors")
    public String authors(Model model) {
        var authors = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.AUTHOR
                          || u.getRole() == Role.EDITOR
                          || u.getRole() == Role.ADMIN)
                .toList();
        model.addAttribute("authors", authors);
        return "authors";
    }

    // ── /archive ─────────────────────────────────────────────────────────

    @GetMapping("/archive")
    public String archive(Model model,
                          @RequestParam(defaultValue = "0") int page) {
        var posts = postRepository.findByStatusAndVisibilityOrderByPublishedAtDesc(
                PostStatus.PUBLISHED, PostVisibility.PUBLIC,
                PageRequest.of(page, 20));
        model.addAttribute("posts",       posts);
        model.addAttribute("currentPage", page);
        return "archive";
    }

    // ── /about ───────────────────────────────────────────────────────────

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    // ── /newsletter/subscribe ────────────────────────────────────────────

    /**
     * BUG-FIX: This endpoint was missing. The homepage subscribe form POSTs here.
     * Saves the email if not already subscribed, then redirects back with a flash message.
     */
    @PostMapping("/newsletter/subscribe")
    public String subscribe(@RequestParam String email,
                            RedirectAttributes redirectAttrs) {
        String normalised = email == null ? "" : email.trim().toLowerCase();
        if (normalised.isEmpty() || !normalised.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            redirectAttrs.addFlashAttribute("newsletterError", "Please enter a valid email address.");
            return "redirect:/";
        }

        if (!subscriberRepository.existsByEmail(normalised)) {
            NewsletterSubscriber subscriber = NewsletterSubscriber.builder()
                    .email(normalised)
                    .confirmed(false)
                    .confirmationToken(UUID.randomUUID().toString())
                    .build();
            subscriberRepository.save(subscriber);
        }

        redirectAttrs.addFlashAttribute("newsletterSuccess",
                "You're subscribed! Check your inbox to confirm.");
        return "redirect:/";
    }
}
