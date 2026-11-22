package co.edu.unbosque.marketplace.configuracion.presentation;
import co.edu.unbosque.marketplace.configuracion.application.TarifaServicioService;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/configuracion/tarifa-servicio")
public class TarifaServicioController {
    private final TarifaServicioService service;
    public TarifaServicioController(TarifaServicioService service) { this.service=service; }
    @GetMapping public Map<String,BigDecimal> consultar() { return Map.of("porcentaje",service.consultar()); }
    @PutMapping public Map<String,BigDecimal> actualizar(@Valid @RequestBody TarifaServicioRequest req,
            @AuthenticationPrincipal UsuarioAutenticado admin) { return Map.of("porcentaje",service.actualizar(req.porcentaje(),admin)); }
}
