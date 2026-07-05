# The Byline — Beta Launch Guide (zero-cost stack)

Step-by-step path from this repo to a live beta at no cost, including free domain options.
Companion to `BETA_CHECKLIST.md`. *Written 2026-07-04.*

---

## 0. What changed in this pass (rebuild required)

| Fix | File | Why |
|---|---|---|
| BUG-009: XSRF-TOKEN cookie dropped on large pages → newsletter form 403 for fresh visitors | `SecurityConfig.java` (`CsrfCookieFilter`) | Spring Security 6 defers CSRF token loading; homepage response buffer flushed before the cookie was written |
| BUG-010: unknown URLs redirected to login instead of 404 | `SecurityConfig.java` (`loginOr404EntryPoint`) | `anyRequest().authenticated()` bounced every mistyped URL to `/auth/login` — bad UX/SEO |
| Tailwind CDN removed; compiled 11.6 KB `app.css` shipped | `fragments/head.html`, `tailwind.config.js`, `static/css/app.css` | CDN script was ~3 MB **and blocked by our own CSP anyway**; preflight disabled so rendering is unchanged |
| Prod actuator no longer exposes DB details | `application.yml` (prod profile) | `show-details: when-authorized` |
| V4 migration (Life Story category) | already present, verified live | works; `/topics/life-story` returns 200 |

The jar in `target/` predates all of this **and** V4. Rebuild before deploying anything.

---

## 1. Rebuild & verify locally (10 min)

```bash
./mvnw clean verify                 # must pass — also compile-checks the two security fixes
docker compose build
docker compose up -d
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

Manual spot-checks (all verified against the running app in this session):
`/` 200 · `/auth/login` 200 · `/rss/feed.xml` valid XML · `/topics/life-story` 200 ·
`/admin` → 403 for non-admins, login redirect for anonymous · `/nonexistent` → **404 page** (new) ·
newsletter subscribe from a fresh incognito window succeeds (new) · `/css/app.css` 200, no `cdn.tailwindcss.com` in page source.

If you change templates later: `npm run css:build` before committing, so `app.css` stays in sync.

---

## 2. Recommended free stack

JVM needs ~300 MB before your code runs; every choice below fits a 512 MB instance because the
Dockerfile already sets `-XX:MaxRAMPercentage=75`.

| Layer | Pick | Free tier (as of mid-2026) | Notes |
|---|---|---|---|
| App | **Koyeb** or **Render** | Koyeb: 1 free web service. Render: 750 instance-hrs/mo, sleeps after 15 min idle | Both build from your Dockerfile via GitHub and give a subdomain + automatic HTTPS |
| Postgres | **Neon** | 0.5 GB, 100 CU-hrs/mo, no credit card, **pgvector supported** | Don't use Render's free Postgres — it expires after 30 days |
| Redis | **skip for beta** | — | Code uses Caffeine for caching; Redis starter is unused. Add Upstash (free) later if you need shared state |
| Media (S3) | Cloudflare R2 | 10 GB free, S3-compatible | Set `S3_ENDPOINT` accordingly |
| TLS + DNS | Cloudflare free plan | — | Only needed if you use your own domain (option B/C below) |

---

## 3. Free domain options

**A. Provider subdomain (launch today, zero setup)**
`thebyline.koyeb.app` / `thebyline.onrender.com`. HTTPS automatic. Set
`APP_BASE_URL=https://thebyline.koyeb.app` and you're done. Fine for a closed beta.

**B. Free real domain — `.pp.ua`** via [NIC.UA](https://nic.ua/en/domains/.pp.ua). Free, renewable.
Add it to Cloudflare (free), point a CNAME at your app's provider subdomain, enable "Full (strict)" TLS.

**C. Free real domain — `eu.org`** via [nic.eu.org](https://nic.eu.org/). Free forever, but manual
approval can take weeks — apply now, launch on option A meanwhile.

Avoid resurrected "free .tk/.ml" offers — Freenom-style domains get reclaimed and blacklisted.

---

## 4. Deploy (Koyeb example, ~30 min)

1. Push the repo to GitHub (confirm `.env` is untracked — it's in `.gitignore`).
2. **Neon**: create project → copy host/db/user/password from the connection string.
   Run nothing manually — Flyway migrates on first boot (V1 needs pgvector: `CREATE EXTENSION vector`
   is in the migration and Neon allows it).
3. **Koyeb**: New App → GitHub repo → Dockerfile build → instance: Free (512 MB) → port 8080.
4. Environment variables:

   ```
   SPRING_PROFILES_ACTIVE=prod
   DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASS   ← from Neon (+ append ?sslmode=require if needed)
   APP_BASE_URL=https://<your-subdomain-or-domain>
   REMEMBER_ME_KEY=<openssl rand -base64 48>
   SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.ai.openai.autoconfigure.OpenAiAutoConfiguration,org.springframework.ai.anthropic.autoconfigure.AnthropicAutoConfiguration,org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration,dev.langchain4j.openai.spring.AutoConfig,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
   MANAGEMENT_HEALTH_REDIS_ENABLED=false
   ```

   (The two Redis lines are only needed while you skip Redis. Leave Stripe/SendGrid/AI keys unset
   until those features go live.)
5. Deploy → health check `https://<app>/actuator/health` → run the §1 spot-checks against the live URL.
6. Optional: attach your `.pp.ua` domain via Cloudflare CNAME, update `APP_BASE_URL`, redeploy.

---

## 5. Beta-scale caveats & post-launch list

- **Sleep-on-idle** (Render, Koyeb free): first request after idle takes ~10–30 s. Acceptable for beta;
  a free [UptimeRobot](https://uptimerobot.com) ping every 10 min keeps it warm (mind the 750 h cap on Render).
- **Neon scale-to-zero**: first DB query after idle adds ~1 s. Harmless.
- Post-launch (unchanged from `BETA_CHECKLIST.md` §7): OWASP HTML sanitizer before public author
  sign-ups, admin user pagination past ~1 000 users, enforce `email_verified`, AI keys to activate
  summarisation/tags.

---

## Sources

Free-tier details verified 2026-07: [Render free tier](https://render.com/articles/platforms-with-a-real-free-tier-for-developers-in-2026) · [Koyeb Postgres free-tier roundup](https://www.koyeb.com/blog/top-postgresql-database-free-tiers-in-2026) · [Neon free tier](https://agentdeals.dev/vendor/neon) · [Spring Boot free hosting guide](https://docs.bswen.com/blog/2026-02-28-springboot-free-hosting/) · [nic.eu.org](https://nic.eu.org/) · [.pp.ua at NIC.UA](https://nic.ua/en/domains/.pp.ua) · [subdomain providers list](https://github.com/WebSnifferHQ/subdomain-providers)
