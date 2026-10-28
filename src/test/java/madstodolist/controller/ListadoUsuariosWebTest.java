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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class ListadoUsuariosWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private ManagerUserSession managerUserSession;

    private UsuarioData addUsuarioBD(String email, boolean admin) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setPassword("123");
        usuario.setAdmin(admin);
        return usuarioService.registrar(usuario);
    }

    @Test
    public void listadoMuestraUsuariosRegistrados() throws Exception {
        // GIVEN
        UsuarioData admin = addUsuarioBD("admin@umh.es", true);
        UsuarioData usuario = addUsuarioBD("listado@umh.es", false);
        when(managerUserSession.usuarioLogeado()).thenReturn(admin.getId());

        // WHEN, THEN
        this.mockMvc.perform(get("/registrados"))
                .andExpect(content().string(allOf(
                        containsString("Listado de usuarios"),
                        containsString("listado@umh.es"),
                        containsString(usuario.getId().toString()))));
    }

    @Test
    public void listadoNoAccesibleParaUsuarioNoAdministrador() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("normal@umh.es", false);
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(get("/registrados"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(containsString("No tienes permisos suficientes")));
    }

    @Test
    public void listadoNoAccesibleSinLogin() throws Exception {
        // GIVEN
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        this.mockMvc.perform(get("/registrados"))
                .andExpect(status().isUnauthorized());
    }
}
