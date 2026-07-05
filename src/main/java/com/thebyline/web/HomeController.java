package com.thebyline.web;

import com.thebyline.domain.post.CategoryRepository;
import com.thebyline.domain.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PostService postService;
    private final CategoryRepository categoryRepository;

    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(defaultValue = "0") int page) {

        postService.findFeatured().ifPresent(p -> model.addAttribute("featured", p));
        model.addAttribute("posts",      postService.findLatest(page, 4));
        model.addAttribute("trending",   postService.findTrending(4));
        model.addAttribute("categories", categoryRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("currentPage", page);
        return "index";
    }
}
