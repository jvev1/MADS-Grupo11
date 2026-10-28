package madstodolist.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "equipos")
public class Equipo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private String nombre;

    // Declaramos el tipo de recuperación como LAZY.
    // Al recuperar un equipo NO SE RECUPERAN AUTOMÁTICAMENTE sus usuarios.
    // Sólo se recuperan cuando se accede al atributo 'usuarios'.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "equipo_usuario",
            joinColumns = { @JoinColumn(name = "fk_equipo") },
            inverseJoinColumns = { @JoinColumn(name = "fk_usuario") })
    Set<Usuario> usuarios = new HashSet<>();

    public Equipo() {}

    public Equipo(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Set<Usuario> getUsuarios() {
        return usuarios;
    }

    // Hay que actualizar ambas colecciones, porque
    // JPA/Hibernate no lo hace automáticamente
    public void addUsuario(Usuario usuario) {
        this.getUsuarios().add(usuario);
        usuario.getEquipos().add(this);
    }

    public void removeUsuario(Usuario usuario) {
        this.getUsuarios().remove(usuario);
        usuario.getEquipos().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Equipo equipo = (Equipo) o;
        if (id != null && equipo.id != null)
            return Objects.equals(id, equipo.id);
        return nombre.equals(equipo.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }
}
