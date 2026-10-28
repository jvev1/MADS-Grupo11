package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class AdministradorWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    private UsuarioData addAdminBD() {
        UsuarioData admin = new UsuarioData();
        admin.setEmail("admin@ua");
        admin.setPassword("123");
        admin.setAdmin(true);
        return usuarioService.registrar(admin);
    }

    @Test
    public void registroMuestraCheckboxSiNoHayAdministrador() throws Exception {
        this.mockMvc.perform(get("/registro"))
                .andExpect(content().string(containsString("Registrarse como administrador")));
    }

    @Test
    public void registroNoMuestraCheckboxSiYaHayAdministrador() throws Exception {
        addAdminBD();

        this.mockMvc.perform(get("/registro"))
                .andExpect(content().string(not(containsString("Registrarse como administrador"))));
    }

    @Test
    public void registroConCheckboxCreaAdministrador() throws Exception {
        this.mockMvc.perform(post("/registro")
                        .param("email", "admin@ua")
                        .param("password", "123")
                        .param("admin", "true"))
                .andExpect(redirectedUrl("/login"));

        assertThat(usuarioService.findByEmail("admin@ua").isAdmin()).isTrue();
    }

    @Test
    public void registroConCheckboxNoCreaSegundoAdministrador() throws Exception {
        addAdminBD();

        this.mockMvc.perform(post("/registro")
                        .param("email", "otro@ua")
                        .param("password", "123")
                        .param("admin", "true"))
                .andExpect(redirectedUrl("/login"));

        assertThat(usuarioService.findByEmail("otro@ua").isAdmin()).isFalse();
    }

    @Test
    public void loginAdministradorRedirigeAListadoUsuarios() throws Exception {
        addAdminBD();

        this.mockMvc.perform(post("/login")
                        .param("eMail", "admin@ua")
                        .param("password", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"));
    }
}
