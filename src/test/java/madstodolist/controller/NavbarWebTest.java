package madstodolist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class NavbarWebTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    public void sinSesionNavbarMuestraLoginYRegistro() throws Exception {
        // GIVEN - usuario no logeado
        // WHEN, THEN
        // GET /about devuelve HTML con los enlaces Login y Registro
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(allOf(
                        containsString("Login"),
                        containsString("Registro")
                )));
    }

    @Test
    public void sinSesionNavbarNoMuestraTareasNiCerrarSesion() throws Exception {
        // GIVEN - usuario no logeado
        // WHEN, THEN
        // GET /about devuelve HTML sin enlace a Tareas ni opción de cerrar sesión
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(allOf(
                        not(containsString("Tareas")),
                        not(containsString("Cerrar sesión"))
                )));
    }


    @Test
    public void conSesionNavbarMuestraTareasYNombreUsuario() throws Exception {
        // GIVEN
        // Sesión con idUsuarioLogeado y nombreUsuarioLogeado
        // WHEN, THEN
        // GET /about devuelve HTML con el enlace Tareas y el nombre del usuario en el desplegable
        this.mockMvc.perform(get("/about")
                        .sessionAttr("idUsuarioLogeado", 1L)
                        .sessionAttr("nombreUsuarioLogeado", "Juan"))
                .andExpect(content().string(allOf(
                        containsString("Tareas"),
                        containsString("Juan"),
                        containsString("Cerrar sesión Juan")
                )));
    }

    @Test
    public void conSesionNavbarMuestraEnlaceEquipos() throws Exception {
        // GIVEN
        // Sesión con idUsuarioLogeado y nombreUsuarioLogeado
        // WHEN, THEN
        // GET /about devuelve HTML con el enlace a Equipos
        this.mockMvc.perform(get("/about")
                        .sessionAttr("idUsuarioLogeado", 1L)
                        .sessionAttr("nombreUsuarioLogeado", "Juan"))
                .andExpect(content().string(containsString("href=\"/equipos\"")));
    }

    @Test
    public void sinSesionNavbarNoMuestraEnlaceEquipos() throws Exception {
        // GIVEN - usuario no logeado
        // WHEN, THEN
        // GET /about devuelve HTML sin el enlace a Equipos
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(not(containsString("href=\"/equipos\""))));
    }

    @Test
    public void conSesionNavbarNoMuestraLoginNiRegistro() throws Exception {
        // GIVEN
        // Sesión con idUsuarioLogeado y nombreUsuarioLogeado
        // WHEN, THEN
        // GET /about devuelve HTML sin los enlaces Login ni Registro
        this.mockMvc.perform(get("/about")
                        .sessionAttr("idUsuarioLogeado", 1L)
                        .sessionAttr("nombreUsuarioLogeado", "Juan"))
                .andExpect(content().string(allOf(
                        not(containsString(">Login<")),
                        not(containsString(">Registro<"))
                )));
    }
}
