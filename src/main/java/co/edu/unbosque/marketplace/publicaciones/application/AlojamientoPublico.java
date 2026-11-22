package co.edu.unbosque.marketplace.publicaciones.application;

import co.edu.unbosque.marketplace.publicaciones.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Vista pública: nunca expone identificadores internos del anfitrión ni datos privados. */
public record AlojamientoPublico(UUID id, String titulo, String descripcion, String ciudad,
        String ubicacionTextual, TipoAlojamiento tipo, int capacidad, BigDecimal precioNoche,
        BigDecimal tarifaLimpieza, ModoReserva modoReserva, List<String> imagenes,
        Set<UUID> serviciosIds, List<ServicioAdicional> serviciosAdicionales) {
    public record ServicioAdicional(String nombre, BigDecimal costo) {}
}
