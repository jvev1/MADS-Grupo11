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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests web de la página "Cuenta" (issue #6):
// el usuario logeado puede ver sus propios datos desde la opción "Cuenta" del menú.
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")   // vacía la BD de test antes de cada test
public class CuentaWebTest {

    @Autowired
    private MockMvc mockMvc;              // simula peticiones HTTP sin arrancar el servidor

    @Autowired
    private UsuarioService usuarioService; // servicio real: guarda usuarios en la BD de test

    @MockBean
    private ManagerUserSession managerUserSession; // sesión falsa: decidimos quién está logeado

    // Registra un usuario de prueba y lo devuelve con su id ya asignado
    private UsuarioData addUsuarioBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("cuenta@ua.es");
        usuario.setNombre("Usuario Cuenta");
        usuario.setPassword("secreta123");
        usuario.setFechaNacimiento(java.sql.Date.valueOf("2004-09-02"));
        return usuarioService.registrar(usuario);
    }

    @Test
    public void cuentaMuestraDatosDelUsuarioLogeadoSinPassword() throws Exception {
        // GIVEN: un usuario registrado y logeado
        UsuarioData usuario = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN: la página muestra sus datos, pero nunca la contraseña
        this.mockMvc.perform(get("/cuenta"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Mi cuenta"),
                        containsString("cuenta@ua.es"),
                        containsString("Usuario Cuenta"),
                        containsString("02-09-2004"),
                        not(containsString("secreta123")))));
    }

    @Test
    public void cuentaSinLoginRedirigeALoginConMensaje() throws Exception {
        // GIVEN: nadie ha iniciado sesión
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN: en vez de un error 401, se redirige al login con un aviso
        this.mockMvc.perform(get("/cuenta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Debes iniciar sesión para ver tu cuenta"));
    }

    @Test
    public void menuEnlazaConLaPaginaDeCuenta() throws Exception {
        // GIVEN: sesión iniciada (el menú lee estos atributos directamente de la sesión)
        // WHEN, THEN: la opción "Cuenta" del menú apunta a /cuenta (antes era "#")
        this.mockMvc.perform(get("/about")
                        .sessionAttr("idUsuarioLogeado", 1L)
                        .sessionAttr("nombreUsuarioLogeado", "Juan"))
                .andExpect(content().string(containsString("href=\"/cuenta\"")));
    }
}
