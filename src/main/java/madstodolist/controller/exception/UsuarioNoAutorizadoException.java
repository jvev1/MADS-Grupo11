package madstodolist.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason="No tienes permisos suficientes para acceder a esta página")
public class UsuarioNoAutorizadoException extends RuntimeException {
}
