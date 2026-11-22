package co.edu.unbosque.marketplace.usuarios.presentation;

import co.edu.unbosque.marketplace.usuarios.application.RecuperacionResponse;
import co.edu.unbosque.marketplace.usuarios.application.RecuperacionService;
import co.edu.unbosque.marketplace.usuarios.application.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CuentaController {

    private final RegistroService registro;
    private final RecuperacionService recuperacion;

    public CuentaController(RegistroService registro, RecuperacionService recuperacion) {
        this.registro = registro;
        this.recuperacion = recuperacion;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto registrar(@Valid @RequestBody RegistroRequest req) {
        return UsuarioDto.desde(registro.registrar(req.nombre(), req.correo(), req.telefono(),
                req.contrasena(), req.rol()));
    }

    @PostMapping("/recuperar-contrasena")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RecuperacionResponse recuperar(@Valid @RequestBody RecuperarContrasenaRequest req) {
        return recuperacion.solicitar(req.correo());
    }

    @PostMapping("/restablecer-contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@Valid @RequestBody RestablecerContrasenaRequest req) {
        recuperacion.restablecer(req.token(), req.nuevaContrasena());
    }
}
