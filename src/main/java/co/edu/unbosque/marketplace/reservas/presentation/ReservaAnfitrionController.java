package co.edu.unbosque.marketplace.reservas.presentation;
import co.edu.unbosque.marketplace.reservas.application.*;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import java.util.List; import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/anfitrion/reservas")
public class ReservaAnfitrionController {
 private final ReservaService service; public ReservaAnfitrionController(ReservaService service){this.service=service;}
 @GetMapping public List<ReservaVista> listar(@AuthenticationPrincipal UsuarioAutenticado anfitrion){return service.listarParaAnfitrion(anfitrion);}
 @PostMapping("/{reservaId}/aprobar") public ReservaVista aprobar(@PathVariable UUID reservaId,@AuthenticationPrincipal UsuarioAutenticado anfitrion){return service.decidirReserva(reservaId,true,anfitrion);}
 @PostMapping("/{reservaId}/rechazar") public ReservaVista rechazar(@PathVariable UUID reservaId,@AuthenticationPrincipal UsuarioAutenticado anfitrion){return service.decidirReserva(reservaId,false,anfitrion);}
}
