package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class ListadoEquiposWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EquipoService equipoService;

    @MockBean
    private ManagerUserSession managerUserSession;

    private UsuarioData addUsuarioBD(String email) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setPassword("123");
        return usuarioService.registrar(usuario);
    }

    @Test
    public void listadoMuestraEquipos() throws Exception {
        // GIVEN
        UsuarioData usuario = addUsuarioBD("user@umh.es");
        equipoService.crearEquipo("Proyecto AAA");
        equipoService.crearEquipo("Proyecto BBB");
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Listado de equipos"),
                        containsString("Proyecto AAA"),
                        containsString("Proyecto BBB"))));
    }

    @Test
    public void listadoNoAccesibleSinLogin() throws Exception {
        // GIVEN
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isUnauthorized());
    }
}
