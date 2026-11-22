package co.edu.unbosque.marketplace.publicaciones.presentation;

import co.edu.unbosque.marketplace.publicaciones.application.*;
import co.edu.unbosque.marketplace.publicaciones.domain.EstadoPublicacion;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/publicaciones")
public class AdminPublicacionController {
    private final PublicacionService service;
    public AdminPublicacionController(PublicacionService service) { this.service=service; }
    @GetMapping public List<PublicacionRevisionResumen> pendientes(@RequestParam(required=false) EstadoPublicacion estado) { return service.listarPendientesModeracion(estado); }
    @GetMapping("/{publicacionId}") public PublicacionDetalle detalle(@PathVariable UUID publicacionId) { return service.detalleModeracion(publicacionId); }
    @PostMapping("/{publicacionId}/aprobar") public PublicacionDetalle aprobar(@PathVariable UUID publicacionId,
            @AuthenticationPrincipal UsuarioAutenticado admin) { return service.aprobarModeracion(publicacionId,admin); }
    @PostMapping("/{publicacionId}/rechazar") public PublicacionDetalle rechazar(@PathVariable UUID publicacionId,
            @Valid @RequestBody RechazoPublicacionRequest request,
            @AuthenticationPrincipal UsuarioAutenticado admin) { return service.rechazarModeracion(publicacionId,request.motivo(),admin); }
}
