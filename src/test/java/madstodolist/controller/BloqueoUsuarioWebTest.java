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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class BloqueoUsuarioWebTest {

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
    public void listadoMuestraBotonBloquear() throws Exception {
        UsuarioData admin = addUsuarioBD("admin@umh.es", true);
        UsuarioData usuario = addUsuarioBD("user@umh.es", false);
        when(managerUserSession.usuarioLogeado()).thenReturn(admin.getId());

        this.mockMvc.perform(get("/registrados"))
                .andExpect(content().string(containsString("/registrados/" + usuario.getId() + "/bloqueo")))
                .andExpect(content().string(containsString("Bloquear")));
    }

    @Test
    public void administradorBloqueaYHabilitaUsuario() throws Exception {
        UsuarioData admin = addUsuarioBD("admin@umh.es", true);
        UsuarioData usuario = addUsuarioBD("user@umh.es", false);
        when(managerUserSession.usuarioLogeado()).thenReturn(admin.getId());

        this.mockMvc.perform(post("/registrados/" + usuario.getId() + "/bloqueo"))
                .andExpect(redirectedUrl("/registrados"));
        assertThat(usuarioService.findById(usuario.getId()).isBloqueado()).isTrue();

        this.mockMvc.perform(get("/registrados"))
                .andExpect(content().string(containsString("Habilitar")));

        this.mockMvc.perform(post("/registrados/" + usuario.getId() + "/bloqueo"))
                .andExpect(redirectedUrl("/registrados"));
        assertThat(usuarioService.findById(usuario.getId()).isBloqueado()).isFalse();
    }

    @Test
    public void usuarioNoAdministradorNoPuedeBloquear() throws Exception {
        UsuarioData u1 = addUsuarioBD("u1@umh.es", false);
        UsuarioData u2 = addUsuarioBD("u2@umh.es", false);
        when(managerUserSession.usuarioLogeado()).thenReturn(u1.getId());

        this.mockMvc.perform(post("/registrados/" + u2.getId() + "/bloqueo"))
                .andExpect(status().isUnauthorized());
        assertThat(usuarioService.findById(u2.getId()).isBloqueado()).isFalse();
    }

    @Test
    public void noSePuedeBloquearAlAdministrador() throws Exception {
        UsuarioData admin = addUsuarioBD("admin@umh.es", true);
        when(managerUserSession.usuarioLogeado()).thenReturn(admin.getId());

        this.mockMvc.perform(post("/registrados/" + admin.getId() + "/bloqueo"))
                .andExpect(redirectedUrl("/registrados"));
        assertThat(usuarioService.findById(admin.getId()).isBloqueado()).isFalse();
    }

    @Test
    public void usuarioBloqueadoNoPuedeLogearse() throws Exception {
        UsuarioData usuario = addUsuarioBD("user@umh.es", false);
        usuarioService.cambiarBloqueo(usuario.getId(), true);

        this.mockMvc.perform(post("/login")
                        .param("eMail", "user@umh.es")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tu acceso ha sido bloqueado por el administrador")));
    }

    @Test
    public void usuarioHabilitadoVuelveAPoderLogearse() throws Exception {
        UsuarioData usuario = addUsuarioBD("user@umh.es", false);
        usuarioService.cambiarBloqueo(usuario.getId(), true);
        usuarioService.cambiarBloqueo(usuario.getId(), false);

        this.mockMvc.perform(post("/login")
                        .param("eMail", "user@umh.es")
                        .param("password", "123"))
                .andExpect(status().is3xxRedirection());
    }
}
