-- ============================================================
--  The Byline — V1 Baseline Schema
--  Managed by Flyway. Never modify; add V2__ for changes.
-- ============================================================

-- Enable pgvector for AI embeddings
CREATE EXTENSION IF NOT EXISTS vector;
-- Enable pg_trgm for fuzzy full-text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ── Users ─────────────────────────────────────────────────────────────

CREATE TABLE users (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                       VARCHAR(254) NOT NULL UNIQUE,
    username                    VARCHAR(50)  NOT NULL UNIQUE,
    display_name                VARCHAR(100) NOT NULL,
    password_hash               TEXT         NOT NULL,
    bio                         VARCHAR(500),
    avatar_url                  VARCHAR(500),
    website_url                 VARCHAR(500),
    twitter_handle              VARCHAR(50),
    role                        VARCHAR(20)  NOT NULL DEFAULT 'READER'
                                    CHECK (role IN ('READER','SUBSCRIBER','AUTHOR','EDITOR','ADMIN')),
    subscription_active         BOOLEAN      NOT NULL DEFAULT FALSE,
    stripe_customer_id          VARCHAR(100),
    email_verified              BOOLEAN      NOT NULL DEFAULT FALSE,
    email_verification_token    VARCHAR(100),
    password_reset_token        VARCHAR(100),
    password_reset_expires_at   TIMESTAMPTZ,
    enabled                     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_role    ON users (role);
CREATE INDEX idx_users_enabled ON users (enabled);

-- ── Categories ────────────────────────────────────────────────────────

CREATE TABLE categories (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL UNIQUE,
    slug          VARCHAR(120) NOT NULL UNIQUE,
    description   VARCHAR(300),
    color_hex     CHAR(7),        -- e.g. #185FA5
    bg_hex        CHAR(7),        -- e.g. #E6F1FB
    display_order INT          NOT NULL DEFAULT 0
);

-- Seed default categories (matching our design system colors)
INSERT INTO categories (name, slug, color_hex, bg_hex, display_order) VALUES
    ('Technology',   'technology',   '#185FA5', '#E6F1FB', 1),
    ('Culture',      'culture',      '#993556', '#FBEAF0', 2),
    ('Science',      'science',      '#854F0B', '#FAEEDA', 3),
    ('Opinion',      'opinion',      '#534AB7', '#EEEDFE', 4),
    ('Environment',  'environment',  '#0F6E56', '#E1F5EE', 5);

-- ── Tags ──────────────────────────────────────────────────────────────

CREATE TABLE tags (
    id    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(100) NOT NULL UNIQUE,
    slug  VARCHAR(120) NOT NULL UNIQUE
);

-- ── Posts ─────────────────────────────────────────────────────────────

CREATE TABLE posts (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title                   VARCHAR(300) NOT NULL,
    slug                    VARCHAR(320) NOT NULL UNIQUE,
    subtitle                VARCHAR(500),
    body_html               TEXT,
    body_text               TEXT,
    cover_image_url         VARCHAR(500),
    cover_image_alt         VARCHAR(300),

    -- SEO
    meta_title              VARCHAR(70),
    meta_description        VARCHAR(160),
    canonical_url           VARCHAR(500),

    -- Workflow
    status                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                                CHECK (status IN ('DRAFT','IN_REVIEW','SCHEDULED','PUBLISHED','ARCHIVED')),
    visibility              VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC'
                                CHECK (visibility IN ('PUBLIC','SUBSCRIBER_ONLY','PAID_ONLY')),
    published_at            TIMESTAMPTZ,
    scheduled_at            TIMESTAMPTZ,
    editor_note             TEXT,

    -- People
    author_id               UUID         NOT NULL REFERENCES users(id),
    editor_id               UUID         REFERENCES users(id),

    -- Taxonomy
    category_id             UUID         REFERENCES categories(id),

    -- Stats
    view_count              BIGINT       NOT NULL DEFAULT 0,
    estimated_read_minutes  INT          NOT NULL DEFAULT 0,

    -- AI
    ai_summary              TEXT,
    ai_tags_suggested       VARCHAR(500),

    -- FTS vector (populated by trigger)
    search_vector           TSVECTOR,

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_status      ON posts (status);
CREATE INDEX idx_posts_visibility  ON posts (visibility);
CREATE INDEX idx_posts_author_id   ON posts (author_id);
CREATE INDEX idx_posts_category_id ON posts (category_id);
CREATE INDEX idx_posts_published   ON posts (published_at DESC NULLS LAST);
CREATE INDEX idx_posts_search      ON posts USING GIN (search_vector);
CREATE INDEX idx_posts_slug_trgm   ON posts USING GIN (slug gin_trgm_ops);

-- Auto-update search_vector from title + body_text
CREATE OR REPLACE FUNCTION posts_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')),    'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.subtitle, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.body_text, '')), 'C');
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER posts_search_vector_trigger
    BEFORE INSERT OR UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION posts_search_vector_update();

-- ── Post ↔ Tag join ───────────────────────────────────────────────────

CREATE TABLE post_tags (
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag_id  UUID NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

-- ── Post embeddings (pgvector) ────────────────────────────────────────

CREATE TABLE post_embeddings (
    id         UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID    NOT NULL UNIQUE REFERENCES posts(id) ON DELETE CASCADE,
    embedding  VECTOR(1536),           -- OpenAI text-embedding-3-small dimensions
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_post_embeddings_vector ON post_embeddings
    USING hnsw (embedding vector_cosine_ops);

-- ── Comments ──────────────────────────────────────────────────────────

CREATE TABLE comments (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id),
    parent_id       UUID        REFERENCES comments(id) ON DELETE CASCADE,
    body            TEXT        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','APPROVED','SPAM','REMOVED')),
    spam_score      NUMERIC(4,3),
    like_count      INT         NOT NULL DEFAULT 0,
    moderator_note  VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post_id   ON comments (post_id, status);
CREATE INDEX idx_comments_user_id   ON comments (user_id);
CREATE INDEX idx_comments_parent_id ON comments (parent_id);

-- ── Comment likes (prevent duplicates) ───────────────────────────────

CREATE TABLE comment_likes (
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (comment_id, user_id)
);

-- ── Newsletter subscribers ────────────────────────────────────────────

CREATE TABLE newsletter_subscribers (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(254) NOT NULL UNIQUE,
    first_name          VARCHAR(100),
    confirmed           BOOLEAN      NOT NULL DEFAULT FALSE,
    confirmation_token  VARCHAR(100),
    confirmed_at        TIMESTAMPTZ,
    unsubscribed_at     TIMESTAMPTZ,
    external_id         VARCHAR(100),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Post views (analytics) ────────────────────────────────────────────

CREATE TABLE post_views (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id     UUID        REFERENCES users(id),   -- null for anonymous
    ip_hash     CHAR(64),                           -- SHA-256 of IP for dedup
    referrer    VARCHAR(500),
    user_agent  VARCHAR(500),
    viewed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_post_views_post_id   ON post_views (post_id);
CREATE INDEX idx_post_views_viewed_at ON post_views (viewed_at DESC);

-- ── Stripe subscriptions ──────────────────────────────────────────────

CREATE TABLE stripe_subscriptions (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID         NOT NULL REFERENCES users(id),
    stripe_subscription_id  VARCHAR(100) NOT NULL UNIQUE,
    stripe_price_id         VARCHAR(100),
    status                  VARCHAR(50)  NOT NULL,   -- mirrors Stripe status
    current_period_start    TIMESTAMPTZ,
    current_period_end      TIMESTAMPTZ,
    cancel_at_period_end    BOOLEAN      NOT NULL DEFAULT FALSE,
    canceled_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stripe_subs_user_id ON stripe_subscriptions (user_id);

-- ── Updated_at auto-trigger (shared) ─────────────────────────────────

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_updated_at
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER comments_updated_at
    BEFORE UPDATE ON comments FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER stripe_subs_updated_at
    BEFORE UPDATE ON stripe_subscriptions FOR EACH ROW EXECUTE FUNCTION set_updated_at();
