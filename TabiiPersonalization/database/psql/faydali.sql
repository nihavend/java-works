SELECT * FROM public.content_images
ORDER BY content_id ASC, image_id ASC 

SELECT * FROM public.contents where id='191180'

SELECT * FROM public.content_lookup_relations where content_id=191180

SELECT count(*) FROM public.contents


-- "parental-guide", "age-restriction", "category", "exclusive-badge", "badge", "genre", "badges"
SELECT distinct(type) FROM public.lookup_objects 

SELECT * FROM public.lookup_objects 

SELECT * FROM public.content_images where content_id=454

SELECT * FROM public.content_lookup_relations where content_id=191180 order by lookup_object_id asc

SELECT distinct(lookup_object_id) FROM public.content_lookup_relations where content_id=191180

SELECT * FROM public.content_lookup_relations where lookup_object_id=182921 and content_id=191180

SELECT * FROM public.content_lookup_relations where id=61848

SELECT * FROM public.content_lookup_relations where content_id=454 and relation_type='badges'

SELECT * FROM public.content_lookup_relations where content_id=454 and relation_type='genre'

SELECT * FROM public.content_lookup_relations where content_id=454 and relation_type='exclusive_badge'

SELECT * FROM public.lookup_objects where type='parental-guide' 	--3  kayıt
SELECT * FROM public.lookup_objects where type='age-restriction'	--6  kayıt
SELECT * FROM public.lookup_objects where type='category'			--12 kayıt
SELECT * FROM public.lookup_objects where type='exclusive-badge'	--3  kayıt
SELECT * FROM public.lookup_objects where type='badge'				--6  kayıt
SELECT * FROM public.lookup_objects where type='genre'				--44 kayıt
SELECT * FROM public.lookup_objects where type='badges'				--63 kayıt


SELECT * FROM public.lookup_objects --where id=182921

SELECT * FROM public.lookup_objects where id=182921

SELECT * FROM content_lookup_relations WHERE content_id = 504477 --and relation_type='badges'

SELECT * FROM content_lookup_relations -- where content_id=15963

SELECT lookup_object_id FROM content_lookup_relations WHERE relation_type='badges' --and content_id = 1   --  543 kayıt
SELECT lookup_object_id FROM content_lookup_relations WHERE relation_type='genre' --and content_id = 1	  -- 2706 kayıt

SELECT id, title, description, spot, made_year, content_type, exclusive_badges FROM contents


SELECT * FROM public.lookup_objects where id=149704
ORDER BY id ASC 

SELECT * FROM public.images where filename='22156_0-0-3840-2400.jpeg'


SELECT * FROM public.content_lookup_relations where id=149704
ORDER BY id ASC 

SELECT count(*) FROM public.images --8290



