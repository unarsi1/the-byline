package com.thebyline.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Webjars (HTMX, Alpine.js) with cache-busting
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(true);

        // Static assets
        registry.addResourceHandler("/css/**", "/js/**", "/images/**")
                .addResourceLocations(
                    "classpath:/static/css/",
                    "classpath:/static/js/",
                    "classpath:/static/images/"
                )
                .resourceChain(true);
    }

    // /auth/login and /auth/register are handled by AuthController @GetMapping methods;
    // duplicate view-controller registrations removed. Spring Boot error handling serves
    // templates/error.html for 4xx/5xx — no need for explicit /403, /404 view controllers.
}
