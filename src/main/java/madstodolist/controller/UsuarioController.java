package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.UsuarioNoAutorizadoException;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class UsuarioController {

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    ManagerUserSession managerUserSession;

    private void comprobarUsuarioAdministrador() {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado == null)
            throw new UsuarioNoAutorizadoException();

        UsuarioData usuario = usuarioService.findById(idUsuarioLogeado);
        if (usuario == null || !usuario.isAdmin())
            throw new UsuarioNoAutorizadoException();
    }

    @GetMapping("/registrados")
    public String listadoUsuarios(Model model) {
        comprobarUsuarioAdministrador();
        model.addAttribute("usuarios", usuarioService.allUsuarios());
        return "listaUsuarios";
    }

    @GetMapping("/registrados/{id}")
    public String descripcionUsuario(@PathVariable(value="id") Long idUsuario, Model model) {
        comprobarUsuarioAdministrador();
        model.addAttribute("usuario", usuarioService.findById(idUsuario));
        return "descripcionUsuario";
    }
}
