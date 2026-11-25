-- Migración de la base de datos de la versión 1.3.0 a la siguiente
-- Nueva columna bloqueado en la tabla usuarios (bloqueo de usuarios por el administrador)
ALTER TABLE public.usuarios
ADD COLUMN bloqueado boolean NOT NULL DEFAULT false;
