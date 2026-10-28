package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.EquipoNotFoundException;
import madstodolist.controller.exception.UsuarioNoAutorizadoException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.EquipoServiceException;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EquipoController {

    @Autowired
    EquipoService equipoService;

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    ManagerUserSession managerUserSession;

    private Long comprobarUsuarioLogeado() {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado == null)
            throw new UsuarioNoLogeadoException();
        return idUsuarioLogeado;
    }

    private boolean esAdministrador(Long idUsuario) {
        UsuarioData usuario = usuarioService.findById(idUsuario);
        return usuario != null && usuario.isAdmin();
    }

    private void comprobarUsuarioAdministrador() {
        Long idUsuarioLogeado = comprobarUsuarioLogeado();
        if (!esAdministrador(idUsuarioLogeado))
            throw new UsuarioNoAutorizadoException();
    }

    private void comprobarEquipoExiste(Long idEquipo) {
        try {
            equipoService.recuperarEquipo(idEquipo);
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
    }

    @GetMapping("/equipos")
    public String listadoEquipos(Model model) {
        Long idUsuario = comprobarUsuarioLogeado();
        model.addAttribute("equipos", equipoService.findAllOrdenadoPorNombre());
        model.addAttribute("equiposUsuario", equipoService.equiposUsuario(idUsuario));
        model.addAttribute("esAdmin", esAdministrador(idUsuario));
        return "listaEquipos";
    }

    @GetMapping("/equipos/{id}")
    public String miembrosEquipo(@PathVariable(value="id") Long idEquipo, Model model) {
        comprobarUsuarioLogeado();
        try {
            model.addAttribute("equipo", equipoService.recuperarEquipo(idEquipo));
            model.addAttribute("usuarios", equipoService.usuariosEquipo(idEquipo));
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
        return "listaMiembrosEquipo";
    }

    @GetMapping("/equipos/nuevo")
    public String formNuevoEquipo(@ModelAttribute EquipoData equipoData) {
        comprobarUsuarioLogeado();
        return "formNuevoEquipo";
    }

    @PostMapping("/equipos/nuevo")
    public String nuevoEquipo(@ModelAttribute EquipoData equipoData, RedirectAttributes flash) {
        comprobarUsuarioLogeado();
        try {
            equipoService.crearEquipo(equipoData.getNombre());
        } catch (EquipoServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/equipos/nuevo";
        }
        flash.addFlashAttribute("mensaje", "Equipo creado correctamente");
        return "redirect:/equipos";
    }

    @GetMapping("/equipos/{id}/editar")
    public String formEditarEquipo(@PathVariable(value="id") Long idEquipo, @ModelAttribute EquipoData equipoData,
                                   Model model) {
        comprobarUsuarioAdministrador();
        try {
            EquipoData equipo = equipoService.recuperarEquipo(idEquipo);
            model.addAttribute("equipo", equipo);
            equipoData.setNombre(equipo.getNombre());
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
        return "formEditarEquipo";
    }

    @PostMapping("/equipos/{id}/editar")
    public String grabarEquipoModificado(@PathVariable(value="id") Long idEquipo, @ModelAttribute EquipoData equipoData,
                                         RedirectAttributes flash) {
        comprobarUsuarioAdministrador();
        comprobarEquipoExiste(idEquipo);
        try {
            equipoService.modificarNombreEquipo(idEquipo, equipoData.getNombre());
        } catch (EquipoServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/equipos/" + idEquipo + "/editar";
        }
        flash.addFlashAttribute("mensaje", "Equipo modificado correctamente");
        return "redirect:/equipos";
    }

    @DeleteMapping("/equipos/{id}")
    @ResponseBody
    public String borrarEquipo(@PathVariable(value="id") Long idEquipo) {
        comprobarUsuarioAdministrador();
        try {
            equipoService.borrarEquipo(idEquipo);
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
        return "";
    }

    @PostMapping("/equipos/{id}/unirme")
    public String unirmeAEquipo(@PathVariable(value="id") Long idEquipo, RedirectAttributes flash) {
        Long idUsuario = comprobarUsuarioLogeado();
        comprobarEquipoExiste(idEquipo);
        try {
            equipoService.añadirUsuarioAEquipo(idEquipo, idUsuario);
            flash.addFlashAttribute("mensaje", "Te has unido al equipo");
        } catch (EquipoServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipos";
    }

    @PostMapping("/equipos/{id}/salir")
    public String salirDeEquipo(@PathVariable(value="id") Long idEquipo, RedirectAttributes flash) {
        Long idUsuario = comprobarUsuarioLogeado();
        comprobarEquipoExiste(idEquipo);
        try {
            equipoService.eliminarUsuarioDeEquipo(idEquipo, idUsuario);
            flash.addFlashAttribute("mensaje", "Has salido del equipo");
        } catch (EquipoServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipos";
    }
}
