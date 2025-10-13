package com.tabii.helpers;

import java.util.Arrays;
import java.util.List;

public class TableManager {
	
	// The tables to deal with, sequence is important !
	public static final List<String> tableNameList = Arrays.asList(
			"users_id_seq", "users", "alter_users_id_seq",
			"images_id_seq", "images", "alter_images_id_seq",
			"content_images",
			"lookup_objects", "contents",
			"content_lookup_relations_id_seq", "content_lookup_relations"
	/* , "content_images", "content_genres", "content_badges" */);

	public static String create_users_id_seq() {

		return """
				CREATE SEQUENCE IF NOT EXISTS public.users_id_seq
				    INCREMENT 1
				    START 1
				    MINVALUE 1
				    MAXVALUE 2147483647
				    CACHE 1;
				""";
	}
	
	public static String create_users() {

		return """
				CREATE TABLE IF NOT EXISTS public.users
				(
				    id integer NOT NULL DEFAULT nextval('users_id_seq'::regclass),
				    first_name character varying(100) COLLATE pg_catalog."default" NOT NULL,
				    last_name character varying(100) COLLATE pg_catalog."default" NOT NULL,
				    email character varying(100) COLLATE pg_catalog."default" NOT NULL,
				    modified_user character varying(100) COLLATE pg_catalog."default",
				    modified_date timestamp without time zone,
				    created_user character varying(100) COLLATE pg_catalog."default",
				    created_date timestamp without time zone DEFAULT now(),
				    CONSTRAINT users_pkey PRIMARY KEY (id)
				)
				""";
	}

	public static String alter_users_id_seq() {
		return """
				ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;
				
				-- insert default users
				INSERT INTO users (
					first_name, last_name, email, modified_user, modified_date, created_user, created_date
				) VALUES
				    ('Name', 'Sirname','name.sirname@trt.net.com',
				    'name.sirname@trt.net.com', '2025-05-13T18:47:14.904Z',
				    'name.sirname@trt.net.com', '2025-05-13T18:47:14.904Z'),
				    ('Name', 'Sirname','name.sirname@trt.net.com',
				    'helin.tuncel@trtworld.com', '2025-05-13T18:47:14.904Z',
				    'name.sirname@trt.net.com', '2025-05-13T18:47:14.904Z')
				  
				;
				""";
	}
	
	public static String create_images_id_seq() {
		return """
				CREATE SEQUENCE IF NOT EXISTS public.images_id_seq
			    INCREMENT 1
			    START 1
			    MINVALUE 1
			    MAXVALUE 2147483647
			    CACHE 1;
				""";
	}

	public static String create_images() {

		return """
				CREATE TABLE IF NOT EXISTS public.images
				(
				    id integer NOT NULL DEFAULT nextval('images_id_seq'::regclass),
				    image_type text COLLATE pg_catalog."default",
				    filename text COLLATE pg_catalog."default",
				    title text COLLATE pg_catalog."default",
				    url text COLLATE pg_catalog."default",
				    CONSTRAINT images_pkey PRIMARY KEY (id),
				    CONSTRAINT images_filename_key UNIQUE (filename)
				)
				""";
	}
	
	public static String alter_images_id_seq() {
		return """
				ALTER SEQUENCE public.images_id_seq OWNED BY public.images.id;
				""";
	}
	
	public static String create_content_images() {
		return """
			CREATE TABLE IF NOT EXISTS public.content_images
			(
			    content_id bigint NOT NULL,
			    image_id integer NOT NULL,
			    CONSTRAINT content_images_pkey PRIMARY KEY (content_id, image_id),
			    CONSTRAINT content_images_content_id_fkey FOREIGN KEY (content_id)
			        REFERENCES public.contents (id) MATCH SIMPLE
			        ON UPDATE NO ACTION
			        ON DELETE NO ACTION,
			    CONSTRAINT content_images_image_id_fkey FOREIGN KEY (image_id)
			        REFERENCES public.images (id) MATCH SIMPLE
			        ON UPDATE NO ACTION
			        ON DELETE NO ACTION
			)				
				""";
	}
	
	public static String create_lookup_objects() {

		return """
				CREATE TABLE IF NOT EXISTS public.lookup_objects
				(
				    id bigint NOT NULL,
				    type text COLLATE pg_catalog."default" NOT NULL,
				    audit json,
				    body json,
				    fields json,
				    isactive boolean,
				    metadata json,
				    path text COLLATE pg_catalog."default",
				    paths json,
				    published json,
				    site text COLLATE pg_catalog."default",
				    title text COLLATE pg_catalog."default",
				    viewcount bigint,
				    audit_user_id bigint,
				    CONSTRAINT lookup_objects_pkey PRIMARY KEY (id),
				    CONSTRAINT lookup_objects_audit_user_id_fkey FOREIGN KEY (audit_user_id)
				        REFERENCES public.users (id) MATCH SIMPLE
				        ON UPDATE NO ACTION
				        ON DELETE NO ACTION
				)
				""";
	}
	public static String create_content_lookup_relations_id_seq() {
		return """
				CREATE SEQUENCE IF NOT EXISTS public.content_lookup_relations_id_seq
			    INCREMENT 1
			    START 1
			    MINVALUE 1
			    MAXVALUE 9223372036854775807
			    CACHE 1;				
				""";
	}
	
	public static String create_content_lookup_relations() {

		return """
				CREATE TABLE IF NOT EXISTS public.content_lookup_relations
				(
				    id bigint NOT NULL DEFAULT nextval('content_lookup_relations_id_seq'::regclass),
				    content_id bigint NOT NULL,
				    lookup_object_id bigint NOT NULL,
				    relation_type text COLLATE pg_catalog."default" NOT NULL,
				    created_at timestamp with time zone DEFAULT now(),
				    CONSTRAINT content_lookup_relations_pkey PRIMARY KEY (id),
				    CONSTRAINT content_lookup_relations_content_id_fkey FOREIGN KEY (content_id)
				        REFERENCES public.contents (id) MATCH SIMPLE
				        ON UPDATE NO ACTION
				        ON DELETE CASCADE,
				    CONSTRAINT content_lookup_relations_lookup_object_id_fkey FOREIGN KEY (lookup_object_id)
				        REFERENCES public.lookup_objects (id) MATCH SIMPLE
				        ON UPDATE NO ACTION
				        ON DELETE CASCADE
)
				""";
	}
	
	public static String alter_content_lookup_relations_id_seq() {
		return """
				ALTER SEQUENCE public.content_lookup_relations_id_seq OWNED BY public.content_lookup_relations.id;
				""";
	}
	
	public static String create_contents() {

		return """
				CREATE TABLE IF NOT EXISTS public.contents
				(
				    id bigint NOT NULL,
				    title text COLLATE pg_catalog."default",
				    description text COLLATE pg_catalog."default",
				    spot text COLLATE pg_catalog."default",
				    made_year integer,
				    content_type text COLLATE pg_catalog."default",
				    exclusive_badges jsonb,
				    created_at timestamp with time zone DEFAULT now(),
				    updated_at timestamp with time zone DEFAULT now(),
				    CONSTRAINT contents_pkey PRIMARY KEY (id)
				)
				""";
	}

}
