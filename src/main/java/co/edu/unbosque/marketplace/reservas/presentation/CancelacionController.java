package co.edu.unbosque.marketplace.reservas.presentation;
import co.edu.unbosque.marketplace.reservas.application.*; import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import java.util.UUID; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/huesped/reservas/{reservaId}")
public class CancelacionController {
 private final CancelacionService service; public CancelacionController(CancelacionService service){this.service=service;}
 @GetMapping("/cancelacion") public CancelacionVista previsualizar(@PathVariable UUID reservaId,@AuthenticationPrincipal UsuarioAutenticado huesped){return service.previsualizar(reservaId,huesped);}
 @PostMapping("/cancelar") public ResultadoCancelacion cancelar(@PathVariable UUID reservaId,@RequestBody(required=false) Object request,@AuthenticationPrincipal UsuarioAutenticado huesped){return service.cancelar(reservaId,huesped);}
}
