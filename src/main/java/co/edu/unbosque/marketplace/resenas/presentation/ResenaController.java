package co.edu.unbosque.marketplace.resenas.presentation;
import co.edu.unbosque.marketplace.resenas.application.ResenaService; import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid; import java.util.UUID; import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController public class ResenaController {
 private final ResenaService service; public ResenaController(ResenaService service){this.service=service;}
 @PostMapping("/api/huesped/reservas/{reservaId}/resena") @ResponseStatus(HttpStatus.CREATED)
 public ResenaService.Vista crear(@PathVariable UUID reservaId,@Valid @RequestBody ResenaRequest request,@AuthenticationPrincipal UsuarioAutenticado huesped){return service.crear(reservaId,request.calificacion(),request.comentario(),huesped);}
 @GetMapping("/api/alojamientos/{alojamientoId}/resenas")
 public ResenaService.PaginaResenas listar(@PathVariable UUID alojamientoId,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="10") int tamano){return service.listarPublicas(alojamientoId,pagina,tamano);}
}
