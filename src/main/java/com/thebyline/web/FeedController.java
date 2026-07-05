package com.thebyline.web;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedOutput;
import com.thebyline.domain.post.Post;
import com.thebyline.domain.post.PostRepository;
import com.thebyline.domain.post.PostStatus;
import com.thebyline.domain.post.PostVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import java.io.Writer;
import java.util.Date;
import java.util.List;

/**
 * BUG-FIX: Every page footer links to /rss/feed.xml but no controller existed.
 * Uses the Rome library (already declared in pom.xml) to generate a valid RSS 2.0 feed.
 */
@Controller
@RequiredArgsConstructor
public class FeedController {

    private final PostRepository postRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.name:The Byline}")
    private String appName;

    @GetMapping(value = "/rss/feed.xml", produces = "application/rss+xml;charset=UTF-8")
    @ResponseBody
    public void rssFeed(HttpServletResponse response) throws Exception {
        List<Post> posts = postRepository.findByStatusAndVisibilityOrderByPublishedAtDesc(
                PostStatus.PUBLISHED, PostVisibility.PUBLIC,
                PageRequest.of(0, 20)).getContent();

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle(appName);
        feed.setLink(baseUrl);
        feed.setDescription("The latest from " + appName);
        feed.setLanguage("en");

        List<SyndEntry> entries = posts.stream().map(post -> {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(post.getTitle());
            entry.setLink(baseUrl + "/articles/" + post.getSlug());
            entry.setUri(baseUrl + "/articles/" + post.getSlug());
            if (post.getPublishedAt() != null) {
                entry.setPublishedDate(Date.from(post.getPublishedAt()));
            }
            if (post.getAuthor() != null) {
                entry.setAuthor(post.getAuthor().getDisplayName());
            }
            if (post.getSubtitle() != null) {
                SyndContent description = new SyndContentImpl();
                description.setType("text/plain");
                description.setValue(post.getSubtitle());
                entry.setDescription(description);
            }
            return entry;
        }).toList();

        feed.setEntries(entries);

        response.setContentType("application/rss+xml;charset=UTF-8");
        try (Writer writer = response.getWriter()) {
            new SyndFeedOutput().output(feed, writer);
        }
    }
}
