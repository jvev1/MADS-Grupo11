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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // Página "Cuenta": datos del usuario logeado (issue #6)
    @GetMapping("/cuenta")
    public String cuenta(Model model, RedirectAttributes flash) {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        // Si no hay sesión (o el usuario ya no existe), no hay cuenta que mostrar
        UsuarioData usuario = (idUsuarioLogeado == null) ? null : usuarioService.findById(idUsuarioLogeado);

        if (usuario == null) {
            // En vez de un error 401, mandamos al login con un aviso.
            // El atributo flash sobrevive a la redirección y formLogin.html ya lo muestra.
            flash.addFlashAttribute("error", "Debes iniciar sesión para ver tu cuenta");
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario); // la plantilla no muestra la contraseña
        return "cuenta";
    }
}
