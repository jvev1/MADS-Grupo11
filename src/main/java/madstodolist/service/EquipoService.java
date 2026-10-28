package madstodolist.service;

import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.model.Equipo;
import madstodolist.model.Usuario;
import madstodolist.repository.EquipoRepository;
import madstodolist.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoService {

    @Autowired
    EquipoRepository equipoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public EquipoData crearEquipo(String nombre) {
        comprobarNombreValido(nombre);
        Equipo equipo = new Equipo(nombre);
        equipo = equipoRepository.save(equipo);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional(readOnly = true)
    public EquipoData recuperarEquipo(Long id) {
        Equipo equipo = getEquipoOrThrow(id);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional
    public EquipoData modificarNombreEquipo(Long idEquipo, String nuevoNombre) {
        Equipo equipo = getEquipoOrThrow(idEquipo);
        comprobarNombreValido(nuevoNombre);
        equipo.setNombre(nuevoNombre);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional
    public void borrarEquipo(Long idEquipo) {
        Equipo equipo = getEquipoOrThrow(idEquipo);
        for (Usuario usuario : new ArrayList<>(equipo.getUsuarios())) {
            equipo.removeUsuario(usuario);
        }
        equipoRepository.delete(equipo);
    }

    @Transactional(readOnly = true)
    public List<EquipoData> findAllOrdenadoPorNombre() {
        return equipoRepository.findAll().stream()
                .sorted(Comparator.comparing(Equipo::getNombre))
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public void añadirUsuarioAEquipo(Long idEquipo, Long idUsuario) {
        Equipo equipo = getEquipoOrThrow(idEquipo);
        Usuario usuario = getUsuarioOrThrow(idUsuario);
        if (equipo.getUsuarios().contains(usuario))
            throw new EquipoServiceException("El usuario " + idUsuario + " ya pertenece al equipo " + idEquipo);
        equipo.addUsuario(usuario);
    }

    @Transactional
    public void eliminarUsuarioDeEquipo(Long idEquipo, Long idUsuario) {
        Equipo equipo = getEquipoOrThrow(idEquipo);
        Usuario usuario = getUsuarioOrThrow(idUsuario);
        if (!equipo.getUsuarios().contains(usuario))
            throw new EquipoServiceException("El usuario " + idUsuario + " no pertenece al equipo " + idEquipo);
        equipo.removeUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioData> usuariosEquipo(Long id) {
        Equipo equipo = getEquipoOrThrow(id);
        // Hacemos uso de Java Stream API para mapear la lista de entidades a DTOs.
        return equipo.getUsuarios().stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioData.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EquipoData> equiposUsuario(Long idUsuario) {
        Usuario usuario = getUsuarioOrThrow(idUsuario);
        return usuario.getEquipos().stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
    }

    private void comprobarNombreValido(String nombre) {
        if (nombre == null || nombre.trim().isEmpty())
            throw new EquipoServiceException("El nombre del equipo no puede estar vacío");
    }

    private Equipo getEquipoOrThrow(Long idEquipo) {
        return equipoRepository.findById(idEquipo)
                .orElseThrow(() -> new EquipoServiceException("No existe el equipo con id " + idEquipo));
    }

    private Usuario getUsuarioOrThrow(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EquipoServiceException("No existe el usuario con id " + idUsuario));
    }
}
