package co.edu.unbosque.marketplace.reservas.application;
import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;
public record ReservaVista(UUID id,UUID alojamientoId,String alojamientoTitulo,String alojamientoCiudad,
        String imagenPortada,UUID huespedId,LocalDate fechaLlegada,LocalDate fechaSalida,int numeroHuespedes,
        EstadoReserva estado,BigDecimal total,Cotizacion cotizacion,Instant creadaEn) {}
