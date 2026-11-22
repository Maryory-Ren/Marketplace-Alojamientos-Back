package co.edu.unbosque.marketplace.reservas.presentation;
import co.edu.unbosque.marketplace.reservas.application.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/huesped/cotizaciones")
public class CotizacionController {
    private final ReservaService service;
    public CotizacionController(ReservaService service) { this.service=service; }
    @PostMapping public Cotizacion cotizar(@Valid @RequestBody EstadiaRequest request) { return service.cotizar(request.aSolicitud()); }
}
