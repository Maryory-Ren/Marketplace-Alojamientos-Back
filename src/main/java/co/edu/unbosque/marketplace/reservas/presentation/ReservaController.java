package co.edu.unbosque.marketplace.reservas.presentation;
import co.edu.unbosque.marketplace.reservas.application.*;
import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/huesped/reservas")
public class ReservaController {
    private final ReservaService service;
    public ReservaController(ReservaService service) { this.service=service; }
    @GetMapping public List<ReservaVista> listar(@AuthenticationPrincipal UsuarioAutenticado huesped,
            @RequestParam(required=false) EstadoReserva estado) {
        List<ReservaVista> lista=service.listar(huesped);
        return estado==null?lista:lista.stream().filter(r -> r.estado()==estado).toList();
    }
    @PostMapping
    public ResponseEntity<ReservaVista> crear(@Valid @RequestBody EstadiaRequest request,
            @RequestHeader("Idempotency-Key") String clave,
            @AuthenticationPrincipal UsuarioAutenticado huesped) {
        var resultado=service.crear(request.aSolicitud(),clave,huesped);
        return ResponseEntity.status(resultado.httpStatus()).body(resultado.reserva());
    }
    @GetMapping("/{reservaId}") public ReservaVista obtener(@PathVariable UUID reservaId,
            @AuthenticationPrincipal UsuarioAutenticado huesped) { return service.obtener(reservaId,huesped); }
}
