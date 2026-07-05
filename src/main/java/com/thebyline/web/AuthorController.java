package com.thebyline.web;

import com.thebyline.domain.post.*;
import com.thebyline.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.thebyline.domain.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequestMapping("/author")
@RequiredArgsConstructor
public class AuthorController {

    private final PostRepository     postRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository     userRepository;

    // ── Dashboard ─────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        User user = resolveUser(principal);
        var posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(page, 15));
        model.addAttribute("posts",       posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("user",        user);
        return "author/dashboard";
    }

    // ── New post ──────────────────────────────────────────────────────────

    @GetMapping("/posts/new")
    public String newPostForm(Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("statuses",   PostStatus.values());
        model.addAttribute("post",       new Post());
        model.addAttribute("editing",    false);
        return "author/post-form";
    }

    @PostMapping("/posts/new")
    @Transactional
    public String createPost(@AuthenticationPrincipal UserDetails principal,
                             @RequestParam String title,
                             @RequestParam(required = false) String subtitle,
                             @RequestParam(required = false) String bodyHtml,
                             @RequestParam(required = false) UUID categoryId,
                             @RequestParam String status,
                             RedirectAttributes redirectAttrs) {
        User author = resolveUser(principal);

        PostStatus postStatus = PostStatus.valueOf(status);
        Category category = categoryId != null
                ? categoryRepository.findById(categoryId).orElse(null)
                : null;

        String slug = slugify(title);
        // ensure uniqueness
        String finalSlug = slug;
        int suffix = 1;
        while (postRepository.findBySlug(finalSlug).isPresent()) {
            finalSlug = slug + "-" + suffix++;
        }

        Post post = Post.builder()
                .title(title)
                .subtitle(subtitle)
                .bodyHtml(bodyHtml)
                .slug(finalSlug)
                .author(author)
                .category(category)
                .status(postStatus)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(postStatus == PostStatus.PUBLISHED ? Instant.now() : null)
                .estimatedReadMinutes(estimateReadTime(bodyHtml))
                .build();

        postRepository.save(post);
        redirectAttrs.addFlashAttribute("success", "Post created successfully.");
        return "redirect:/author/dashboard";
    }

    // ── Edit post ─────────────────────────────────────────────────────────

    @GetMapping("/posts/{id}/edit")
    public String editPostForm(@PathVariable UUID id,
                               @AuthenticationPrincipal UserDetails principal,
                               Model model) {
        Post post = getOwnedPost(id, principal);
        model.addAttribute("post",       post);
        model.addAttribute("categories", categoryRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("statuses",   PostStatus.values());
        model.addAttribute("editing",    true);
        return "author/post-form";
    }

    @PostMapping("/posts/{id}/edit")
    @Transactional
    public String updatePost(@PathVariable UUID id,
                             @AuthenticationPrincipal UserDetails principal,
                             @RequestParam String title,
                             @RequestParam(required = false) String subtitle,
                             @RequestParam(required = false) String bodyHtml,
                             @RequestParam(required = false) UUID categoryId,
                             @RequestParam String status,
                             RedirectAttributes redirectAttrs) {
        Post post = getOwnedPost(id, principal);

        PostStatus newStatus = PostStatus.valueOf(status);
        Category category = categoryId != null
                ? categoryRepository.findById(categoryId).orElse(null)
                : null;

        post.setTitle(title);
        post.setSubtitle(subtitle);
        post.setBodyHtml(bodyHtml);
        post.setCategory(category);
        post.setStatus(newStatus);
        post.setEstimatedReadMinutes(estimateReadTime(bodyHtml));
        if (newStatus == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }
        postRepository.save(post);
        redirectAttrs.addFlashAttribute("success", "Post updated successfully.");
        return "redirect:/author/dashboard";
    }

    // ── Delete post ───────────────────────────────────────────────────────

    @PostMapping("/posts/{id}/delete")
    @Transactional
    public String deletePost(@PathVariable UUID id,
                             @AuthenticationPrincipal UserDetails principal,
                             RedirectAttributes redirectAttrs) {
        Post post = getOwnedPost(id, principal);
        postRepository.delete(post);
        redirectAttrs.addFlashAttribute("success", "Post deleted.");
        return "redirect:/author/dashboard";
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Post getOwnedPost(UUID id, UserDetails principal) {
        User user = resolveUser(principal);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Admins/editors can edit any post; authors only their own
        boolean isOwner = post.getAuthor().getId().equals(user.getId());
        boolean isPrivileged = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_EDITOR"));
        if (!isOwner && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return post;
    }

    private static String slugify(String title) {
        return title.toLowerCase(java.util.Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s]+", "-")
                .replaceAll("-{2,}", "-");
    }

    private static int estimateReadTime(String html) {
        if (html == null || html.isBlank()) return 0;
        String text = html.replaceAll("<[^>]+>", " ");
        int words = text.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 238.0)); // avg 238 wpm
    }
}
