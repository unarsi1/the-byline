package com.thebyline.web;

import com.thebyline.domain.post.PostRepository;
import com.thebyline.domain.post.PostStatus;
import com.thebyline.domain.user.Role;
import com.thebyline.domain.user.User;
import com.thebyline.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @GetMapping({"", "/"})
    public String dashboard(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("users",          userRepository.findAll());
        model.addAttribute("pendingAuthors", userRepository.findByEnabledFalseAndRole(Role.AUTHOR));
        model.addAttribute("posts",          postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 20)));
        model.addAttribute("draftCount",     postRepository.findByStatusOrderByCreatedAtAsc(PostStatus.DRAFT).size());
        model.addAttribute("reviewCount",    postRepository.findByStatusOrderByCreatedAtAsc(PostStatus.IN_REVIEW).size());
        model.addAttribute("currentPage",    page);
        return "admin/dashboard";
    }

    @PostMapping("/users/{id}/approve")
    public String approveAuthor(@PathVariable UUID id, RedirectAttributes redirectAttrs) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setEnabled(true);
        userRepository.save(user);
        redirectAttrs.addFlashAttribute("success", user.getDisplayName() + " approved as author.");
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/reject")
    public String rejectAuthor(@PathVariable UUID id, RedirectAttributes redirectAttrs) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String name = user.getDisplayName();
        userRepository.delete(user);
        redirectAttrs.addFlashAttribute("success", name + "'s application rejected and removed.");
        return "redirect:/admin";
    }
}
