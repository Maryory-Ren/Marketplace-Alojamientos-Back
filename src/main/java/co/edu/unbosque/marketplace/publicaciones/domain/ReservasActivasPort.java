package co.edu.unbosque.marketplace.publicaciones.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Puerto que publicaciones necesita para conocer qué fechas están ocupadas.
 * Lo implementa el módulo de reservas; así la dependencia entre módulos va solo de
 * reservas hacia publicaciones y se evita el ciclo que Spring Modulith rechaza.
 */
public interface ReservasActivasPort {

    List<ReservaResumen> reservasActivasEnRango(UUID publicacionId, LocalDate inicio, LocalDate finInclusivo);
}
