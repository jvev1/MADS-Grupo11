package madstodolist.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class EquipoServiceTest {

    @Autowired
    EquipoService equipoService;

    @Autowired
    UsuarioService usuarioService;

    @Test
    public void crearRecuperarEquipo() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        assertThat(equipo.getId()).isNotNull();

        EquipoData equipoBd = equipoService.recuperarEquipo(equipo.getId());
        assertThat(equipoBd).isNotNull();
        assertThat(equipoBd.getNombre()).isEqualTo("Proyecto 1");
    }

    @Test
    public void crearEquipoNombreVacioLanzaExcepcion() {
        assertThatThrownBy(() -> equipoService.crearEquipo(""))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.crearEquipo("   "))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.crearEquipo(null))
                .isInstanceOf(EquipoServiceException.class);
    }

    @Test
    public void modificarNombreEquipo() {
        // GIVEN
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");

        // WHEN
        EquipoData equipoModificado = equipoService.modificarNombreEquipo(equipo.getId(), "Proyecto 2");

        // THEN
        assertThat(equipoModificado.getNombre()).isEqualTo("Proyecto 2");
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto 2");
    }

    @Test
    public void modificarNombreEquipoExcepciones() {
        // Equipo inexistente
        assertThatThrownBy(() -> equipoService.modificarNombreEquipo(1L, "Proyecto 2"))
                .isInstanceOf(EquipoServiceException.class);

        // Nombre vacío, en blanco o nulo
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        assertThatThrownBy(() -> equipoService.modificarNombreEquipo(equipo.getId(), ""))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.modificarNombreEquipo(equipo.getId(), "   "))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.modificarNombreEquipo(equipo.getId(), null))
                .isInstanceOf(EquipoServiceException.class);

        // El nombre no ha cambiado
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto 1");
    }

    @Test
    public void listadoEquiposOrdenAlfabetico() {
        // GIVEN
        // Dos equipos en la base de datos
        equipoService.crearEquipo("Proyecto BBB");
        equipoService.crearEquipo("Proyecto AAA");

        // WHEN
        // Recuperamos los equipos
        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();

        // THEN
        // Los equipos están ordenados por nombre
        assertThat(equipos).hasSize(2);
        assertThat(equipos.get(0).getNombre()).isEqualTo("Proyecto AAA");
        assertThat(equipos.get(1).getNombre()).isEqualTo("Proyecto BBB");
    }

    @Test
    public void añadirUsuarioAEquipo() {
        // GIVEN
        // Un usuario y un equipo en la base de datos
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");

        // WHEN
        // Añadimos el usuario al equipo
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());

        // THEN
        // El usuario pertenece al equipo
        List<UsuarioData> usuarios = equipoService.usuariosEquipo(equipo.getId());
        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get(0).getEmail()).isEqualTo("user@ua");
    }

    @Test
    public void añadirUsuarioYaPerteneceAEquipoLanzaExcepcion() {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        UsuarioData usuarioBd = usuarioService.registrar(usuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuarioBd.getId());

        // WHEN, THEN
        // Volver a añadirlo
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(equipo.getId(), usuarioBd.getId()))
                .isInstanceOf(EquipoServiceException.class);
    }

    @Test
    public void recuperarEquiposDeUsuario() {
        // GIVEN
        // Un usuario y dos equipos en la base de datos
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo1 = equipoService.crearEquipo("Proyecto 1");
        EquipoData equipo2 = equipoService.crearEquipo("Proyecto 2");
        equipoService.añadirUsuarioAEquipo(equipo1.getId(), usuario.getId());
        equipoService.añadirUsuarioAEquipo(equipo2.getId(), usuario.getId());

        // WHEN
        // Recuperamos los equipos del usuario
        List<EquipoData> equipos = equipoService.equiposUsuario(usuario.getId());

        // THEN
        // El usuario pertenece a los dos equipos
        assertThat(equipos).hasSize(2);
        assertThat(equipos.get(0).getNombre()).isEqualTo("Proyecto 1");
        assertThat(equipos.get(1).getNombre()).isEqualTo("Proyecto 2");
    }

    @Test
    public void eliminarUsuarioDeEquipo() {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());

        // WHEN
        equipoService.eliminarUsuarioDeEquipo(equipo.getId(), usuario.getId());

        // THEN
        assertThat(equipoService.usuariosEquipo(equipo.getId())).isEmpty();
        assertThat(equipoService.equiposUsuario(usuario.getId())).isEmpty();
    }

    @Test
    public void borrarEquipo() {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo1 = equipoService.crearEquipo("Proyecto 1");
        EquipoData equipo2 = equipoService.crearEquipo("Proyecto 2");
        equipoService.añadirUsuarioAEquipo(equipo1.getId(), usuario.getId());
        equipoService.añadirUsuarioAEquipo(equipo2.getId(), usuario.getId());

        // WHEN
        equipoService.borrarEquipo(equipo1.getId());

        // THEN
        assertThatThrownBy(() -> equipoService.recuperarEquipo(equipo1.getId()))
                .isInstanceOf(EquipoServiceException.class);
        assertThat(usuarioService.findById(usuario.getId())).isNotNull();
        List<EquipoData> equiposUsuario = equipoService.equiposUsuario(usuario.getId());
        assertThat(equiposUsuario).hasSize(1);
        assertThat(equiposUsuario.get(0).getNombre()).isEqualTo("Proyecto 2");
    }

    @Test
    public void borrarEquipoInexistenteLanzaExcepcion() {
        // GIVEN
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");

        // WHEN, THEN
        assertThatThrownBy(() -> equipoService.borrarEquipo(equipo.getId() + 1))
                .isInstanceOf(EquipoServiceException.class);
        assertThat(equipoService.findAllOrdenadoPorNombre()).hasSize(1);
    }

    @Test
    public void eliminarUsuarioDeEquipoExcepciones() {
        // No existe ni el equipo ni el usuario
        assertThatThrownBy(() -> equipoService.eliminarUsuarioDeEquipo(1L, 1L))
                .isInstanceOf(EquipoServiceException.class);

        // Existe el equipo pero no el usuario
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        assertThatThrownBy(() -> equipoService.eliminarUsuarioDeEquipo(equipo.getId(), 1L))
                .isInstanceOf(EquipoServiceException.class);

        // Existen los dos, pero el usuario no pertenece al equipo
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        UsuarioData usuarioBd = usuarioService.registrar(usuario);
        assertThatThrownBy(() -> equipoService.eliminarUsuarioDeEquipo(equipo.getId(), usuarioBd.getId()))
                .isInstanceOf(EquipoServiceException.class);
    }

    @Test
    public void comprobarExcepciones() {
        // Comprobamos las excepciones lanzadas por los métodos
        // recuperarEquipo, añadirUsuarioAEquipo, usuariosEquipo y equiposUsuario
        assertThatThrownBy(() -> equipoService.recuperarEquipo(1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(1L, 1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.usuariosEquipo(1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.equiposUsuario(1L))
                .isInstanceOf(EquipoServiceException.class);

        // Creamos un equipo pero no un usuario y comprobamos que también se lanza una excepción
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(equipo.getId(), 1L))
                .isInstanceOf(EquipoServiceException.class);
    }
}
