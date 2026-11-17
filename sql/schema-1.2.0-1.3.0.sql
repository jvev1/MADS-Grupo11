-- Migración de la base de datos de la versión 1.2.0 a la 1.3.0
-- Nueva columna descripcion en la tabla equipos (issue #4)
ALTER TABLE public.equipos
ADD COLUMN descripcion character varying(255);
