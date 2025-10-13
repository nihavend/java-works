
DROP TABLE IF EXISTS public.contents CASCADE
DROP TABLE IF EXISTS public.lookup_objects CASCADE
DROP TABLE IF EXISTS public.images CASCADE
DROP TABLE IF EXISTS public.content_lookup_relations CASCADE
DROP TABLE IF EXISTS public.content_images CASCADE



CREATE TABLE IF NOT EXISTS public.contents
(
    id BIGINT PRIMARY KEY,
    title TEXT,
    description TEXT,
    spot TEXT,
    made_year INTEGER,
    content_type TEXT,
    exclusive_badges JSONB,                -- now JSONB for better performance
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.images
(
    id SERIAL PRIMARY KEY,
    image_type TEXT,
    filename TEXT UNIQUE,
    title TEXT,
    url TEXT
);

-- Content to Image mapping
CREATE TABLE content_images (
    content_id BIGINT REFERENCES contents(id),
    image_id INT REFERENCES images(id),
    PRIMARY KEY (content_id, image_id)
);

CREATE TABLE IF NOT EXISTS public.lookup_objects
(
    id BIGINT PRIMARY KEY,
    type TEXT NOT NULL NOT NULL,        -- e.g. genre, badge, category
    audit JSON,
    body JSON,
    fields JSON,
    isactive BOOLEAN,
    metadata JSON,
    path TEXT,
    paths JSON,
    published JSON,
    site TEXT,
    title TEXT,
    viewcount BIGINT,
    audit_user_id BIGINT,
    CONSTRAINT lookup_objects_audit_user_id_fkey FOREIGN KEY (audit_user_id)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.content_lookup_relations
(
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL REFERENCES public.contents (id) ON DELETE CASCADE,
    lookup_object_id BIGINT NOT NULL REFERENCES public.lookup_objects (id) ON DELETE CASCADE,
    relation_type TEXT NOT NULL,     -- e.g. "genre", "badge", "category"
    created_at TIMESTAMPTZ DEFAULT NOW()
);




CREATE INDEX idx_content_lookup_type ON public.content_lookup_relations (relation_type);
CREATE INDEX idx_content_lookup_content_id ON public.content_lookup_relations (content_id);
CREATE INDEX idx_content_lookup_lookup_id ON public.content_lookup_relations (lookup_object_id);

