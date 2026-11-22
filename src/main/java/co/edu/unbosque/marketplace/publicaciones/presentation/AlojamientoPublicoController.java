package co.edu.unbosque.marketplace.publicaciones.presentation;

import co.edu.unbosque.marketplace.publicaciones.application.*;
import co.edu.unbosque.marketplace.publicaciones.domain.TipoAlojamiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alojamientos")
public class AlojamientoPublicoController {
    private final AlojamientoPublicoService service;
    public AlojamientoPublicoController(AlojamientoPublicoService service) { this.service = service; }

    @GetMapping
    public ResultadoBusquedaAlojamientos buscar(@RequestParam(required=false) String ciudad,
            @RequestParam(required=false) TipoAlojamiento tipo,
            @RequestParam(required=false) Integer huespedes,
            @RequestParam(required=false) BigDecimal precioMin,
            @RequestParam(required=false) BigDecimal precioMax,
            @RequestParam(name="fechaLlegada",required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate llegada,
            @RequestParam(name="fechaSalida",required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam(name="serviciosIds",required=false) List<UUID> servicios,
            @RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="12") int tamano) {
        return service.buscar(ciudad,tipo,huespedes,precioMin,precioMax,llegada,salida,servicios,pagina,tamano);
    }
    @GetMapping("/{alojamientoId}")
    public AlojamientoDetallePublico detalle(@PathVariable UUID alojamientoId) { return service.detalle(alojamientoId); }
    @GetMapping("/{alojamientoId}/calendario")
    public List<AlojamientoPublicoService.DiaCalendario> calendario(@PathVariable UUID alojamientoId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.calendario(alojamientoId,desde,hasta);
    }
}
