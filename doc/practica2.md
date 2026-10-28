# Práctica 2 – Documentación técnica

Este documento resume la evolución de la aplicación **ToDoList** durante la práctica 2 (versiones 1.0.1 y 1.1.0). Está pensado para el resto del equipo de desarrollo: explica qué se ha añadido, dónde está y cómo se ha probado.

## Resumen de funcionalidades

| Funcionalidad | Ruta |
|---|---|
| Página *Acerca de* | `GET /about` |
| Barra de menú | fragmento `menu` |
| Listado de usuarios  | `GET /registrados` |
| Descripción de usuario  | `GET /registrados/{id}` |
| Usuario administrador  `/registro` y `/login` |

## Nuevas clases y métodos

- **`HomeController`** (nuevo): contiene `about()`, que devuelve la vista `about`.
- **`UsuarioController`** (nuevo):
  - `listadoUsuarios()` → `GET /registrados`.
  - `descripcionUsuario(Long id)` → `GET /registrados/{id}`.
- **`UsuarioService`**:
  - `allUsuarios()` devuelve todos los usuarios como `List<UsuarioData>`.
  - `existeAdministrador()` indica si ya hay un administrador registrado.
  - `registrar()` ahora lanza `UsuarioServiceException` si se intenta registrar un segundo administrador.
- **`UsuarioRepository`**: nuevo método derivado `existsByAdminTrue()`. Spring Data genera la consulta a partir del nombre.
- **`ManagerUserSession`**: `logearUsuario(id, nombre)` guarda también el nombre en la sesión (`nombreUsuarioLogeado`) para mostrarlo en la barra de menú. `logout()` lo borra.
- **`Usuario`, `UsuarioData` y `RegistroData`**: nuevo atributo `boolean admin` (por defecto `false`) con sus getters y setters.
- **`LoginController`**:
  - Al hacer login, si el usuario es administrador redirige a `/registrados` en lugar de a sus tareas.
  - En el registro pasa a la vista el atributo `existeAdmin`.

## Plantillas Thymeleaf

- **`about.html`** (nueva): nombre de la aplicación, desarrollador, versión y fecha de release.
- **`listaUsuarios.html`** (nueva): tabla con el id y el email de cada usuario y un botón *Ver descripción*.
- **`descripcionUsuario.html`** (nueva): muestra id, email, nombre y fecha de nacimiento. **No muestra la contraseña.**
- **`fragments.html`**: nuevo fragmento `menu` con la barra de navegación de Bootstrap. Se incluye con `th:replace="fragments :: menu"` en `about`, `listaTareas`, `listaUsuarios` y `descripcionUsuario`.
- **`formRegistro.html`**: nuevo *check box* para registrarse como administrador.

## Código relevante

### Barra de menú según la sesión

La barra no necesita ningún dato del controlador: lee directamente los atributos de la sesión HTTP. Con `th:if` se muestran unas opciones u otras según haya usuario logeado o no:

```html
<li class="nav-item" th:if="${session.idUsuarioLogeado != null}">
    <a class="nav-link"
       th:href="@{/usuarios/{id}/tareas(id=${session.idUsuarioLogeado})}">Tareas</a>
</li>
...
<li class="nav-item" th:if="${session.idUsuarioLogeado == null}">
    <a class="nav-link" th:href="@{/login}">Login</a>
</li>
```

Por eso fue necesario ampliar `ManagerUserSession` para guardar el nombre además del id.

### Un único administrador

La restricción se comprueba en dos capas. En el servicio, como regla de negocio:

```java
else if (usuario.isAdmin() && usuarioRepository.existsByAdminTrue())
    throw new UsuarioServiceException("Ya existe un usuario administrador");
```

En el controlador, para que la interfaz sea coherente:

```java
boolean existeAdmin = usuarioService.existeAdministrador();
model.addAttribute("existeAdmin", existeAdmin);
...
usuario.setAdmin(registroData.isAdmin() && !existeAdmin);
```

La plantilla solo pinta el *check box* si `existeAdmin` es `false`. Además, aunque alguien envíe `admin=true` manipulando el formulario, el controlador ignora el valor si ya existe un administrador y el usuario se registra como normal.

## Tests implementados

Todos los tests web usan `MockMvc` con `@SpringBootTest` y `@AutoConfigureMockMvc`. Los que acceden a la base de datos limpian las tablas con `@Sql(scripts = "/clean-db.sql")`.

- **`AcercaDeWebTest`**: `/about` contiene el nombre de la aplicación.
- **`NavbarWebTest`**:
  - Sin sesión, la barra muestra *Login* y *Registro* y no muestra *Tareas* ni *Cerrar sesión*.
  - Con sesión, simulada con `.sessionAttr(...)`, muestra *Tareas* y el nombre del usuario, y oculta *Login* y *Registro*.
- **`ListadoUsuariosWebTest`**: un usuario registrado aparece en `/registrados` con su id y su email.
- **`DescripcionUsuarioWebTest`**:
  - La descripción muestra los datos del usuario pero no su contraseña.
  - El listado contiene el enlace `/registrados/{id}`.
- **`AdministradorWebTest`**:
  - El *check box* aparece solo si no hay administrador.
  - Registrarse con el *check box* marcado crea un administrador.
  - No se puede crear un segundo administrador.
  - El login del administrador redirige a `/registrados`.
- **`UsuarioServiceTest`**: dos tests nuevos que comprueban el registro de un administrador y la excepción al registrar un segundo.

Para ejecutar todos los tests:

```bash
./mvnw test
```

## Pendiente 

- `/registrados` y `/registrados/{id}` **no están protegidos**: cualquier usuario, incluso sin login, puede acceder. Lo lógico sería restringirlos al administrador en una próxima iteración.
- La opción *Cuenta* del desplegable de la barra de menú todavía apunta a `#`.
