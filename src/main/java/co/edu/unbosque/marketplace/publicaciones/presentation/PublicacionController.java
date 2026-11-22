package co.edu.unbosque.marketplace.publicaciones.presentation;

import co.edu.unbosque.marketplace.publicaciones.application.PublicacionDetalle;
import co.edu.unbosque.marketplace.publicaciones.application.PublicacionResumen;
import co.edu.unbosque.marketplace.publicaciones.application.PublicacionService;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/anfitrion")
public class PublicacionController {

    private final PublicacionService service;

    public PublicacionController(PublicacionService service) {
        this.service = service;
    }

    @GetMapping("/servicios")
    public List<ServicioDto> servicios() {
        return service.catalogoServicios().stream().map(ServicioDto::desde).toList();
    }

    @GetMapping("/politicas-cancelacion")
    public List<PoliticaCancelacionDto> politicas() {
        return service.catalogoPoliticas().stream().map(PoliticaCancelacionDto::desde).toList();
    }

    @GetMapping("/publicaciones")
    public List<PublicacionResumen> listar(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.listarPropias(actual);
    }

    @PostMapping("/publicaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionDetalle crear(@Valid @RequestBody PublicacionRequest req,
                                    @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.crear(req.aDatos(), actual);
    }

    @GetMapping("/publicaciones/{id}")
    public PublicacionDetalle obtener(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.obtener(id, actual);
    }

    @PutMapping("/publicaciones/{id}")
    public PublicacionDetalle editar(@PathVariable UUID id, @Valid @RequestBody PublicacionRequest req,
                                     @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.editar(id, req.aDatos(), actual);
    }

    @PostMapping("/publicaciones/{id}/enviar")
    public PublicacionDetalle enviar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.enviarARevision(id, actual);
    }
}
