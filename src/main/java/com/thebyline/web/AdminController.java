package com.thebyline.web;

import com.thebyline.domain.post.PostRepository;
import com.thebyline.domain.post.PostStatus;
import com.thebyline.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @GetMapping({"", "/"})
    public String dashboard(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("users",       userRepository.findAll());
        model.addAttribute("posts",       postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 20)));
        model.addAttribute("draftCount",  postRepository.findByStatusOrderByCreatedAtAsc(PostStatus.DRAFT).size());
        model.addAttribute("reviewCount", postRepository.findByStatusOrderByCreatedAtAsc(PostStatus.IN_REVIEW).size());
        model.addAttribute("currentPage", page);
        return "admin/dashboard";
    }
}
