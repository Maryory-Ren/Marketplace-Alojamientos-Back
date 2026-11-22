package co.edu.unbosque.marketplace.publicaciones.application;
import co.edu.unbosque.marketplace.publicaciones.domain.*;
import java.math.BigDecimal;
import java.util.UUID;
public record AlojamientoResumenPublico(UUID id,String titulo,String ciudad,TipoAlojamiento tipo,
        int capacidad,BigDecimal precioNoche,String imagenPortada,ModoReserva modoReserva) {}
