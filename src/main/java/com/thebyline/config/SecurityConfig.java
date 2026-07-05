package com.thebyline.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.LinkedHashMap;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Injected from app.security.remember-me-key in application.yml / env. */
    @Value("${app.security.remember-me-key}")
    private String rememberMeKey;

    /**
     * Role hierarchy:
     *   ADMIN  > EDITOR > AUTHOR > SUBSCRIBER > READER (anonymous)
     *
     * Roles are stored as-is in the DB and granted as ROLE_<NAME> by
     * UserDetailsServiceImpl so Spring Security's hasRole() works normally.
     */

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // ── Public ────────────────────────────────────────────────
                .requestMatchers(
                    "/", "/articles/**", "/authors/**", "/topics/**",
                    "/archive", "/about", "/rss/**", "/sitemap.xml",
                    "/newsletter/subscribe", "/newsletter/confirm/**",
                    "/auth/**", "/webjars/**", "/css/**", "/js/**",
                    "/images/**", "/favicon.ico", "/actuator/health",
                    "/error"
                ).permitAll()

                // ── Subscriber-only content ────────────────────────────────
                .requestMatchers("/subscriber/**").hasAnyRole("SUBSCRIBER", "AUTHOR", "EDITOR", "ADMIN")

                // ── Author area ────────────────────────────────────────────
                .requestMatchers("/author/**").hasAnyRole("AUTHOR", "EDITOR", "ADMIN")

                // ── Editor area ────────────────────────────────────────────
                .requestMatchers("/editor/**").hasAnyRole("EDITOR", "ADMIN")

                // ── Admin area ─────────────────────────────────────────────
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // ── Stripe webhooks (authenticated separately via signature) ─
                .requestMatchers("/webhooks/stripe").permitAll()

                // ── Everything else requires login ─────────────────────────
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/", true)
                // BUG-004 fix: save the attempted username to the session so the login
                // template can pre-fill the email field after a failed attempt.
                .failureHandler((request, response, exception) -> {
                    String attempted = request.getParameter("username");
                    HttpSession session = request.getSession(true);
                    if (attempted != null && !attempted.isBlank()) {
                        session.setAttribute("LAST_USERNAME", attempted);
                    }
                    response.sendRedirect("/auth/login?error");
                })
                .permitAll()
            )
            // BUG-001 fix: redirect unauthenticated users with ?required so the login
            // page can display a contextual "please sign in" message.
            // BUG-010 fix: only redirect to login for known protected areas. Everything
            // else (unknown/mistyped URLs) gets a proper 404 via the error dispatch
            // instead of bouncing anonymous visitors to the login page (bad UX + SEO).
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(loginOr404EntryPoint())
            )
            .logout(logout -> logout
                // BUG-007 fix: Spring Security 6 requires POST for logout by default.
                // The nav now uses a <form method="post"> so this URL is correct.
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout")
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .tokenValiditySeconds(30 * 24 * 60 * 60) // 30 days
                .key(rememberMeKey)
            )
            .csrf(csrf -> csrf
                // Use cookie-based CSRF so HTMX can read the token client-side
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Stripe webhook endpoint bypasses CSRF (verified by signature)
                .ignoringRequestMatchers("/webhooks/stripe")
            )
            // BUG-009 fix: Spring Security 6 defers CSRF token loading, so the
            // XSRF-TOKEN cookie was only written if the token happened to be
            // resolved before the response buffer flushed. On large pages (e.g.
            // the homepage, where the newsletter form sits below the first 8 KB)
            // the Set-Cookie was silently dropped and fresh visitors got 403 on
            // form POSTs. Eagerly resolve the token on every request so the
            // cookie is written before any body is streamed.
            .addFilterAfter(new CsrfCookieFilter(), RememberMeAuthenticationFilter.class)
            .headers(headers -> headers
                .referrerPolicy(r ->
                    r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .frameOptions(f -> f.sameOrigin()) // allow TinyMCE iframes
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://cdn.tiny.cloud https://js.stripe.com; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "img-src 'self' data: https:; " +
                    "connect-src 'self'; " +
                    "frame-src https://js.stripe.com"
                ))
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Anonymous requests to known protected areas → login page.
     * Anonymous requests to anything else (unknown URLs caught by
     * {@code anyRequest().authenticated()}) → 404 error dispatch, rendered by
     * error.html. Deliberately 404 (not 401/403) so unknown paths don't leak
     * which URL prefixes exist.
     */
    private static AuthenticationEntryPoint loginOr404EntryPoint() {
        RequestMatcher protectedAreas = new OrRequestMatcher(
            new AntPathRequestMatcher("/subscriber/**"),
            new AntPathRequestMatcher("/author/**"),
            new AntPathRequestMatcher("/editor/**"),
            new AntPathRequestMatcher("/admin/**")
        );
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(protectedAreas, new LoginUrlAuthenticationEntryPoint("/auth/login?required"));

        DelegatingAuthenticationEntryPoint delegating = new DelegatingAuthenticationEntryPoint(entryPoints);
        delegating.setDefaultEntryPoint((request, response, authException) ->
            response.sendError(HttpStatus.NOT_FOUND.value()));
        return delegating;
    }

    /**
     * Forces resolution of the deferred {@link CsrfToken} so
     * {@link CookieCsrfTokenRepository} writes the XSRF-TOKEN cookie before the
     * response is committed. Standard pattern from the Spring Security 6 docs.
     */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken(); // resolve deferred token → cookie saved
            }
            filterChain.doFilter(request, response);
        }
    }
}
