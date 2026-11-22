package co.edu.unbosque.marketplace.publicaciones.application;
import co.edu.unbosque.marketplace.publicaciones.domain.*;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record PublicacionRevisionResumen(UUID id,String titulo,String ciudad,TipoAlojamiento tipo,int capacidad,
        BigDecimal precioNoche,EstadoPublicacion estado,UUID anfitrionId,String anfitrionNombre,
        Instant fechaEnvio,String imagenPortada) {}
