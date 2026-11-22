package co.edu.unbosque.marketplace.reservas.application;
import java.time.LocalDate;
import java.util.UUID;
public record SolicitudEstadia(UUID alojamientoId, LocalDate fechaLlegada, LocalDate fechaSalida, int numeroHuespedes) {}
