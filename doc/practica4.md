# Práctica 4 – Trabajo en equipo con GitFlow y despliegue en producción

**Equipo 11** · Repositorio: [jvev1/MADS-Grupo11](https://github.com/jvev1/MADS-Grupo11) · Versión: **1.3.0**

| Miembro | Usuario GitHub | Responsable de |
|---|---|---|
| Juan Vicente Esclapez | `jvev1` | Despliegue en producción, número de miembros en el listado de equipos (#5) |
| Juan Carlos Ponce de León | `jcpd4` | Página Acerca de (#2), mensaje cuando no hay tareas (#12), rama `develop`, release 1.3.0 (#15) |
| David Mateo Iglesias | `dmi15-ua` | Imagen Docker (#1), descripción en los equipos (#4) |
| Vsevolod Nakonechnikov | `vn23-ua` | Esquema de datos y perfil de producción (#8), opción "Cuenta" del menú (#6), documentación |

---

## 1. Flujo de trabajo en equipo

Hemos partido del proyecto de Juan (versión 1.2.0, con equipos, usuario administrador, PostgreSQL y GitHub Actions). Lo subió al repositorio común y el resto lo clonamos.

Para cada issue seguimos estos pasos:

1. El issue se crea en el tablero de GitHub (columnas *Todo*, *In Progress*, *In Pull Request* y *Done*) con el milestone `1.3.0` y su etiqueta.
2. Al pasarlo a *In Progress* se asigna un responsable, que crea la rama y la sube (`git checkout -b rama` y `git push -u origin rama`).
3. Cuando termina, el responsable abre un pull request con `Closes #n` en la descripción y mueve la tarjeta a *In Pull Request*.
4. Otro miembro del equipo revisa el código (*Files changed → Review changes → Approve*). GitHub Actions tiene que estar en verde.
5. El responsable mezcla el PR y borra la rama. El issue se cierra solo y la tarjeta pasa a *Done*.

Las ramas `main` y `develop` están protegidas: no se puede mezclar en ellas sin un pull request con al menos una aprobación.

Además del tablero de GitHub, usamos un tablero de Trello con una tarjeta por tarea, coloreada por dificultad (verde: fácil, amarillo: media, rojo: difícil).

### Conflictos

En el issue *Actualizar la página Acerca de* (#2, PR #7) los cuatro trabajamos en la rama compartida `acerca-de-equipo`. Cada uno añadió su nombre a los autores en un commit propio. Además provocamos dos conflictos:

- **Entre dos miembros:** dos personas cambiaron la misma línea de `about.html`. Al segundo se le rechazó el `push`, hizo `git pull`, resolvió el conflicto en local y subió el commit de mezcla.
- **Con `main`:** con el PR ya abierto se modificó la misma línea directamente en `main`. El responsable lo resolvió en local, sin usar la web de GitHub:
  ```bash
  git checkout main
  git pull
  git checkout acerca-de-equipo
  git merge main
  # se edita el fichero, después git add y git commit
  git push
  ```

---

## 2. GitFlow

Después de las primeras tareas (página Acerca de e imagen Docker) creamos la rama `develop` y pasamos a trabajar con GitFlow:

- **`main`:** solo contiene versiones publicadas.
- **`develop`:** rama de desarrollo y rama por defecto del repositorio. Las ramas de funcionalidad salen de `develop` y vuelven a ella mediante un pull request.
- **`release-1.3.0`:** sale de `develop` cuando todas las funcionalidades de la versión están integradas (issue #15). En ella:
  - se cambia la versión a `1.3.0` en `pom.xml` y en la página *Acerca de*, con la fecha de publicación;
  - se genera el esquema de datos de la nueva versión ([`sql/schema-1.3.0.sql`](../sql/schema-1.3.0.sql));
  - se crea el script de migración ([`sql/schema-1.2.0-1.3.0.sql`](../sql/schema-1.2.0-1.3.0.sql)) y se prueba sobre una copia de la base de datos de producción (sección 7);
  - se añade esta documentación.

  Después se mezcla mediante pull request en `main`, donde se crea la release `v1.3.0` en GitHub, y también en `develop`, para que las dos ramas queden en la misma versión.

---

## 3. Imagen Docker

El `Dockerfile` original tenía el comando de arranque fijo, así que no se podía elegir el perfil de Spring ni la dirección de la base de datos. Ahora acepta parámetros:

```dockerfile
FROM eclipse-temurin:8-jre
COPY target/*.jar app.jar
ENTRYPOINT ["sh","-c","java -Djava.security.egd=file:/dev/urandom -jar /app.jar ${0} ${@}"]
```

Los parámetros que se escriben después del nombre de la imagen en `docker run` (por ejemplo `--spring.profiles.active=postgres`) llegan al comando `java`.

El perfil `postgres` de ejecución (`src/main/resources/application-postgres.properties`) usa variables con valores por defecto (`POSTGRES_HOST`, `POSTGRES_PORT`, `DB_USER` y `DB_PASSWD`). Así se pueden cambiar al lanzar el contenedor:

```bash
./mvnw clean package
docker build -t david2179/mads-todolist-equipo11:1.3.0-snapshot .
docker run --rm david2179/mads-todolist-equipo11:1.3.0-snapshot --spring.profiles.active=postgres --POSTGRES_HOST=host-prueba
```

La última orden da un error de conexión con la base de datos. Es lo esperado: demuestra que se ha cargado el perfil `postgres` con el host indicado.

**Imagen en Docker Hub:** [`david2179/mads-todolist-equipo11`](https://hub.docker.com/r/david2179/mads-todolist-equipo11), con las etiquetas `1.3.0-snapshot`, `1.3.0` y `latest`.

---

## 4. Despliegue en producción

La aplicación se despliega en el ordenador de Juan con dos contenedores conectados por una red de Docker: uno con PostgreSQL y otro con la aplicación.

```bash
# Red y base de datos de producción
docker network create network-equipo
docker run -d --network network-equipo --network-alias postgres -v ${PWD}:/mi-host --name db-equipo \
  -e POSTGRES_USER=mads -e POSTGRES_PASSWORD=mads -e POSTGRES_DB=mads postgres:13

# Aplicación
docker pull david2179/mads-todolist-equipo11:1.3.0
docker run --rm --network network-equipo -p 8080:8080 david2179/mads-todolist-equipo11:1.3.0 \
  --spring.profiles.active=postgres-prod --POSTGRES_HOST=postgres
```

- `--network-alias postgres` hace que, dentro de la red, la base de datos se llame `postgres`. Por eso la aplicación usa `--POSTGRES_HOST=postgres`.
- `-v ${PWD}:/mi-host` comparte la carpeta actual del ordenador con el contenedor. Sirve para guardar ahí copias de seguridad y aplicar scripts SQL.
- Los datos se mantienen al parar y volver a arrancar la aplicación, porque viven en el contenedor `db-equipo`.

Para comprobar que los datos están en la base de datos:

```bash
docker exec -it db-equipo bash
psql -U mads -W mads
\dt
SELECT * FROM usuarios;
```

---

## 5. Perfil de producción y esquema de datos (issue #8, PR #9)

### Esquema de la versión 1.2.0

Se lanzó la aplicación con el perfil `postgres` contra un contenedor vacío. Hibernate creó las tablas y se guardó solo la estructura, sin datos (`-s`):

```bash
docker run -d -p 5432:5432 -v ${PWD}:/mi-host --name db-equipo -e POSTGRES_USER=mads -e POSTGRES_PASSWORD=mads -e POSTGRES_DB=mads postgres:13
./mvnw spring-boot:run -Dprofiles=postgres
docker exec -it db-equipo bash
pg_dump -U mads -s mads > /mi-host/schema-1.2.0.sql
```

Fichero: [`sql/schema-1.2.0.sql`](../sql/schema-1.2.0.sql). Contiene las tablas `usuarios`, `tareas`, `equipos` y `equipo_usuario` (la relación muchos a muchos entre equipos y usuarios), con sus claves primarias y ajenas.

### Perfil `postgres-prod`

[`src/main/resources/application-postgres-prod.properties`](../src/main/resources/application-postgres-prod.properties) es igual que el perfil `postgres`, con una diferencia:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Con `update`, que es lo que usan los perfiles de desarrollo, Hibernate cambia las tablas solo cuando cambian las entidades. En producción eso podría hacer perder datos. Con `validate`, Hibernate solo comprueba que las tablas coinciden con las entidades: si no coinciden, la aplicación no arranca.

**Prueba realizada:**
1. Con la base de datos vacía, la aplicación no arranca: `Schema-validation: missing table [equipo_usuario]`.
2. Se carga el esquema (`psql -U mads mads < /mi-host/sql/schema-1.2.0.sql`) y la aplicación arranca correctamente, sin crear ninguna tabla.

---

## 6. Copias de seguridad

Con la base de datos en marcha se hace la copia dentro del contenedor y se guarda en la carpeta compartida, con la fecha en el nombre:

```bash
docker exec -it db-equipo bash
pg_dump -U mads --clean mads > /mi-host/sql/backupDDMMAAAA.sql
```

`--clean` añade al principio las órdenes para borrar las tablas, de forma que al restaurar se sustituye el contenido anterior. Para restaurar una copia:

```bash
docker exec -it db-equipo bash
psql -U mads mads < /mi-host/sql/backupDDMMAAAA.sql
```

Copias en el repositorio:
- [`sql/backup05112026.sql`](../sql/backup05112026.sql): datos de prueba de la versión 1.2.0 en todas las tablas (usuarios, uno de ellos administrador, tareas, equipos y miembros).
- La copia posterior a la migración a la 1.3.0 se guarda también en `sql/`, con la fecha en el nombre.

---

## 7. Migración de la base de datos de la 1.2.0 a la 1.3.0

La única funcionalidad de la 1.3.0 que cambia una entidad es la descripción de los equipos (#4). El esquema nuevo se generó igual que el de la 1.2.0: se lanzó la versión 1.3.0 con el perfil `postgres` sobre una base de datos vacía y se hizo `pg_dump -s`.

Al comparar los dos esquemas (`diff sql/schema-1.3.0.sql sql/schema-1.2.0.sql`), la única diferencia es la tabla `equipos`:

```sql
-- 1.2.0
CREATE TABLE public.equipos (
    id bigint NOT NULL,
    nombre character varying(255) NOT NULL
);

-- 1.3.0
CREATE TABLE public.equipos (
    id bigint NOT NULL,
    descripcion character varying(255),
    nombre character varying(255) NOT NULL
);
```

El script de migración [`sql/schema-1.2.0-1.3.0.sql`](../sql/schema-1.2.0-1.3.0.sql) solo añade esa columna, sin tocar los datos que ya existen:

```sql
ALTER TABLE public.equipos
ADD COLUMN descripcion character varying(255);
```

La columna admite valores nulos, así que los equipos creados con la 1.2.0 quedan sin descripción y la aplicación los muestra igual.

**Prueba de la actualización de producción:**
1. Base de datos de producción vacía.
2. Restaurar la copia de seguridad de la 1.2.0 (`sql/backup05112026.sql`).
3. Aplicar el script: `psql -U mads mads < /mi-host/sql/schema-1.2.0-1.3.0.sql` (responde `ALTER TABLE`).
4. Lanzar la imagen 1.3.0 con `--spring.profiles.active=postgres-prod`: arranca (la validación pasa) y los datos antiguos siguen ahí.
5. Añadir datos nuevos y hacer otra copia de seguridad con la fecha del día.

---

## 8. Nuevas funcionalidades de la versión 1.3.0

### 8.1 La opción "Cuenta" del menú no funciona (issue #6, PR #10) – Vsevolod

**Problema:** en el desplegable del usuario, la opción "Cuenta" tenía `href="#"` y no llevaba a ningún sitio.

**Solución:**
- Nuevo endpoint `GET /cuenta` en `UsuarioController`. Lee el usuario logeado de la sesión (`ManagerUserSession`) y muestra la plantilla nueva `cuenta.html` con su email, nombre y fecha de nacimiento. La contraseña no se muestra.
- Si no hay sesión, en lugar de devolver un error 401 se redirige a `/login` con el aviso *"Debes iniciar sesión para ver tu cuenta"*. El aviso se pasa como atributo *flash* (sobrevive a la redirección y se borra después de mostrarse), y `formLogin.html` ya lo pintaba.
- En `fragments.html`, la opción "Cuenta" enlaza ahora a `/cuenta`.

**Tests** (`CuentaWebTest`), escritos antes que el código (TDD):
- Con sesión, la página muestra los datos del usuario y no su contraseña.
- Sin sesión, se redirige a `/login` con el aviso.
- El menú contiene el enlace a `/cuenta`.

### 8.2 Número de miembros en el listado de equipos (issue #5, PR #11) – Juan

- `EquipoService.numUsuariosEquipo(idEquipo)` devuelve cuántos usuarios tiene un equipo. Si el equipo no existe, lanza `EquipoServiceException`.
- En `EquipoController.listadoEquipos()` se construye un mapa con el número de miembros de cada equipo (`id del equipo → miembros`) y se pasa a la vista.
- `listaEquipos.html` tiene una columna nueva, "Miembros".
- Tests: `contarUsuariosEquipo` y la excepción en `EquipoServiceTest`, y `listadoMuestraMiembrosdeEquipo` en `ListadoEquiposWebTest`.

### 8.3 Descripción en los equipos (issue #4, PR #14) – David

Es la funcionalidad que cambia una entidad y obliga a migrar la base de datos (sección 7).

- **Entidad y DTO:** nuevo atributo `descripcion` en `Equipo` (con un constructor `Equipo(nombre, descripcion)`) y en `EquipoData`.
- **Servicio:** `crearEquipo(nombre, descripcion)`. El `crearEquipo(nombre)` de antes se mantiene y crea el equipo sin descripción, así que el código y los tests existentes siguen funcionando. El antiguo `modificarNombreEquipo` se sustituye por `modificarEquipo(id, nombre, descripcion)`, que cambia las dos cosas a la vez. El nombre sigue siendo obligatorio; la descripción, no.
- **Controlador:** los formularios de nuevo equipo y de edición envían y reciben la descripción.
- **Vistas:** campo de texto "Descripción" en `formNuevoEquipo.html` y `formEditarEquipo.html`, columna "Descripción" en el listado de equipos, y la descripción bajo el título en la página de miembros del equipo (solo si tiene).
- **Tests:** en `EquipoServiceTest`, crear con y sin descripción y modificar nombre y descripción; en `GestionEquiposWebTest`, que el listado muestra la descripción.

### 8.4 Mensaje cuando el usuario no tiene tareas (issue #12, PR #13) – Juan Carlos

- Si la lista de tareas está vacía, `listaTareas.html` muestra el aviso *"No tienes tareas todavía. Pulsa "Nueva tarea" para crear una."* (un `alert-info` de Bootstrap, que solo aparece con `th:if="${#lists.isEmpty(tareas)}"`).
- Tests en `TareaWebTest`: el aviso aparece para un usuario sin tareas y no aparece si tiene tareas.

---

## 9. Diferencias con el enunciado

- **Imagen base de Docker:** el enunciado usa `openjdk:8-jdk-alpine`, que ya no existe en Docker Hub. Usamos `eclipse-temurin:8-jre` en el `Dockerfile` y `eclipse-temurin:8-jdk` en el workflow de GitHub Actions.
- **Perfil con `spring-boot:run`:** el `pom.xml` del proyecto define los perfiles con una propiedad propia, así que el perfil se indica con `-Dprofiles=postgres` en vez de `-D spring-boot.run.profiles=postgres`. En PowerShell el argumento va entre comillas: `"-Dprofiles=postgres"`.
- **Carpeta de las copias de seguridad:** el enunciado dice en un punto que la copia se guarde en `src/`. Las guardamos en `sql/`, junto a los esquemas, como se indica en el resto del enunciado.
- **Permiso de ejecución de `mvnw`:** el proyecto se subió desde Windows y `mvnw` perdió el permiso de ejecución, así que GitHub Actions fallaba con `Permission denied`. Se arregló con `git update-index --chmod=+x mvnw`.
