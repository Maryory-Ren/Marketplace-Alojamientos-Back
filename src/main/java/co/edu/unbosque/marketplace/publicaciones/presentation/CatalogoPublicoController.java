package co.edu.unbosque.marketplace.publicaciones.presentation;

import co.edu.unbosque.marketplace.publicaciones.application.PublicacionService;
import co.edu.unbosque.marketplace.publicaciones.domain.Servicio;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicios")
public class CatalogoPublicoController {
    private final PublicacionService publicaciones;
    public CatalogoPublicoController(PublicacionService publicaciones) { this.publicaciones=publicaciones; }
    @GetMapping public List<ServicioDto> listar() {
        return publicaciones.catalogoServicios().stream().map(s -> new ServicioDto(s.getId(),s.getNombre())).toList();
    }
}
