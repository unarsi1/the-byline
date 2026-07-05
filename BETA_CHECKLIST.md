# The Byline — Beta Deployment Checklist

Use this list before every beta release. Check each item off before pushing to production.

---

## 1. Environment & Secrets

- [ ] `.env` created from `.env.example` on the production host (never committed to git)
- [ ] `REMEMBER_ME_KEY` set to a random 48-byte base64 string (`openssl rand -base64 48`)
- [ ] `DB_PASS` set to a strong, unique password
- [ ] `APP_BASE_URL` matches the live domain (e.g. `https://thebyline.com`)
- [ ] `SPRING_PROFILES_ACTIVE=prod` set in the container environment

## 2. Build

- [ ] `./mvnw clean verify` passes locally with zero test failures
- [ ] `docker compose build` completes without errors
- [ ] Docker image size is reasonable (< 300 MB)

## 3. Database

- [ ] Flyway migrations run cleanly on a fresh schema (`flyway:migrate`)
- [ ] Production Postgres has the **pgvector extension available** — V1 runs
      `CREATE EXTENSION vector` and fails hard without it. Compose uses
      `pgvector/pgvector:pg16`; managed DBs (Neon, Supabase, RDS) support it too
- [ ] `ddl-auto: validate` passes — Hibernate entities match the schema
- [ ] Backup strategy confirmed for production Postgres volume

## 4. Application Health

- [ ] `docker compose up -d` brings all three services healthy
- [ ] `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`
- [ ] Homepage (`/`) loads with sample content
- [ ] Login (`/auth/login`) and register (`/auth/register`) pages load
- [ ] Category tabs navigate to `/topics/{slug}` without 404
- [ ] Article pages (`/articles/{slug}`) render correctly
- [ ] Newsletter subscribe form submits and shows success message
- [ ] RSS feed (`/rss/feed.xml`) returns valid XML
- [ ] Admin dashboard (`/admin`) accessible only to ADMIN-role users
- [ ] Author dashboard (`/author/dashboard`) accessible to AUTHOR/EDITOR/ADMIN
- [ ] Sign-out (POST `/auth/logout`) works and clears session
- [ ] Unknown URL (e.g. `/nonexistent`) shows the 404 page, not a login redirect (BUG-010)
- [ ] Newsletter subscribe works from a **fresh incognito window** — verifies the
      XSRF-TOKEN cookie fix (BUG-009)

## 5. Security

- [ ] HTTPS enforced (reverse proxy / load balancer terminates TLS)
- [ ] `REMEMBER_ME_KEY` is NOT the default `change-me` placeholder
- [ ] `server.error.include-stacktrace=never` confirmed (set in application.yml)
- [ ] CSRF cookie present on POST forms in browser dev tools
- [ ] `/admin` returns 403 for non-admin users
- [ ] Stripe webhook endpoint (`/webhooks/stripe`) validates signature before processing

## 6. Performance

- [ ] Thymeleaf cache enabled in prod profile (`spring.thymeleaf.cache=true`)
- [ ] Actuator endpoints restricted (`include: health` only in prod)
- [ ] Response compression enabled (set in application.yml)

## 7. Known Beta Limitations (not blocking, fix post-launch)

- Post body HTML is stored raw and rendered with `th:utext`. XSS risk is low because
  only trusted AUTHOR/EDITOR/ADMIN users can write posts, but a server-side HTML
  sanitizer (e.g. OWASP Java HTML Sanitizer) should be added before opening author
  sign-ups to the public.
- ~~Tailwind CDN~~ **Fixed 2026-07-04**: compiled 11.6 KB `app.css` replaces the CDN
  script (which our CSP was blocking anyway). Run `npm run css:build` after template changes.
- The admin dashboard loads all users without pagination — fine for beta, add pagination
  before the user count exceeds ~1 000.
- AI features (summarisation, tag suggestions) are stubbed out. Set `OPENAI_API_KEY`
  or `ANTHROPIC_API_KEY` to activate them.
- Email verification (`email_verified` column) is stored but not enforced. Users can
  log in without verifying their email.

---

*Last updated: 2026-07-04 — see `BETA_LAUNCH_GUIDE.md` for the free-stack deploy path.*
