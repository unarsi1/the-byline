package com.thebyline.web;

import com.thebyline.domain.post.Post;
import com.thebyline.domain.post.PostRepository;
import com.thebyline.domain.post.PostService;
import com.thebyline.domain.post.PostStatus;
import com.thebyline.domain.post.PostVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService    postService;
    private final PostRepository postRepository;

    @GetMapping("/articles/{slug}")
    @Transactional
    public String article(@PathVariable String slug, Model model) {

        Post post = postService.findBySlug(slug)
                .filter(p -> p.getStatus()     == PostStatus.PUBLISHED)
                .filter(p -> p.getVisibility() == PostVisibility.PUBLIC)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Increment view count (transactional write)
        postRepository.findById(post.getId()).ifPresent(p -> {
            p.setViewCount(p.getViewCount() + 1);
            postRepository.save(p);
        });

        // Related posts: same category, excluding current
        var related = post.getCategory() != null
                ? postRepository.findByStatusAndVisibilityAndCategoryOrderByPublishedAtDesc(
                        PostStatus.PUBLISHED, PostVisibility.PUBLIC,
                        post.getCategory(), PageRequest.of(0, 3))
                    .getContent()
                    .stream().filter(p -> !p.getId().equals(post.getId())).toList()
                : java.util.List.of();

        model.addAttribute("post",    post);
        model.addAttribute("related", related);
        return "post";
    }
}
