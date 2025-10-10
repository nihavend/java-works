CREATE TABLE IF NOT EXISTS public.contents (
    id              BIGSERIAL PRIMARY KEY,
    mongo_id        BIGINT UNIQUE,
    title           TEXT,
    slug            TEXT,
    description     TEXT,
    made_year       INTEGER,
    contents        JSONB,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.images (
    id SERIAL PRIMARY KEY,
    image_type TEXT,
    filename TEXT UNIQUE,
    title TEXT
);

CREATE TABLE IF NOT EXISTS public.content_images (
    content_id BIGINT NOT NULL REFERENCES contents (id),
    image_id   INT NOT NULL REFERENCES images (id),
    PRIMARY KEY (content_id, image_id)
);

CREATE TABLE IF NOT EXISTS public.content_lookup_relations (
    content_id   BIGINT NOT NULL REFERENCES contents (id),
    lookup_id    BIGINT NOT NULL REFERENCES lookup_objects (id),
    relation_type TEXT NOT NULL,
    PRIMARY KEY (content_id, lookup_id, relation_type)
);
