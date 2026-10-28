package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
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
public class MiembrosEquipoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EquipoService equipoService;

    @MockBean
    private ManagerUserSession managerUserSession;

    private UsuarioData addUsuarioBD(String email, String nombre) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setPassword("123");
        return usuarioService.registrar(usuario);
    }

    @Test
    public void miembrosEquipoMuestraUsuariosDelEquipo() throws Exception {
        // GIVEN
        UsuarioData usuario1 = addUsuarioBD("user1@umh.es", "Usuario Uno");
        UsuarioData usuario2 = addUsuarioBD("user2@umh.es", "Usuario Dos");
        addUsuarioBD("user3@umh.es", "Usuario Tres");
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario1.getId());
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario2.getId());
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario1.getId());

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/" + equipo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Miembros del equipo Proyecto AAA"),
                        containsString("user1@umh.es"),
                        containsString("user2@umh.es"),
                        not(containsString("user3@umh.es")))));
    }

    @Test
    public void miembrosEquipoNoAccesibleSinLogin() throws Exception {
        // GIVEN
        EquipoData equipo = equipoService.crearEquipo("Proyecto AAA");
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/" + equipo.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void miembrosEquipoInexistenteDevuelveNotFound() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user1@umh.es", "Usuario Uno");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/99999"))
                .andExpect(status().isNotFound())
                .andExpect(status().reason(containsString("Equipo no encontrado")));
    }
}
