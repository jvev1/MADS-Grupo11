package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class DescripcionUsuarioWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private ManagerUserSession managerUserSession;

    private void logearAdministrador() {
        UsuarioData admin = new UsuarioData();
        admin.setEmail("admin@ua.es");
        admin.setPassword("123");
        admin.setAdmin(true);
        admin = usuarioService.registrar(admin);
        when(managerUserSession.usuarioLogeado()).thenReturn(admin.getId());
    }

    @Test
    public void descripcionMuestraDatosUsuarioSinPassword() throws Exception {
        // GIVEN
        logearAdministrador();
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("descripcion@ua.es");
        usuario.setNombre("Usuario Descripcion");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        // WHEN, THEN
        this.mockMvc.perform(get("/registrados/" + usuario.getId()))
                .andExpect(content().string(allOf(
                        containsString("Descripción de usuario"),
                        containsString("descripcion@ua.es"),
                        containsString("Usuario Descripcion"),
                        not(containsString("123")))));
    }

    @Test
    public void listadoTieneEnlaceADescripcion() throws Exception {
        // GIVEN
        logearAdministrador();
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("enlace@ua.es");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        // WHEN, THEN
        this.mockMvc.perform(get("/registrados"))
                .andExpect(content().string(
                        containsString("/registrados/" + usuario.getId())));
    }

    @Test
    public void descripcionNoAccesibleParaUsuarioNoAdministrador() throws Exception {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("normal@ua.es");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(get("/registrados/" + usuario.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(containsString("No tienes permisos suficientes")));
    }

    @Test
    public void descripcionNoAccesibleSinLogin() throws Exception {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("otro@ua.es");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        this.mockMvc.perform(get("/registrados/" + usuario.getId()))
                .andExpect(status().isUnauthorized());
    }
}
