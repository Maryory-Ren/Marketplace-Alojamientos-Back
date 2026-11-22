package co.edu.unbosque.marketplace.publicaciones.domain;

import java.time.LocalDate;
import java.util.UUID;

/** Vista mínima de una reserva que ocupa fechas de una publicación (la entrega el módulo de reservas). */
public record ReservaResumen(UUID id, LocalDate fechaLlegada, LocalDate fechaSalida,
                             int numeroHuespedes, String estado) {}
