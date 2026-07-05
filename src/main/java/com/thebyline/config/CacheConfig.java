package com.thebyline.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine — in-process cache for hot, low-churn data.
     * TTLs are intentionally short; Redis handles shared/distributed state.
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats());
        return manager;
    }

    // Named cache constants — reference these in @Cacheable annotations
    public static final String POSTS_LATEST    = "posts.latest";
    public static final String POSTS_FEATURED  = "posts.featured";
    public static final String POST_BY_SLUG    = "posts.bySlug";
    public static final String TOPICS_ALL      = "topics.all";
    public static final String AUTHORS_ALL     = "authors.all";
    public static final String AUTHOR_BY_SLUG  = "authors.bySlug";
}
