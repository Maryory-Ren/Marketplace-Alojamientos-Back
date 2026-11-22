package co.edu.unbosque.marketplace.reservas.presentation;
import co.edu.unbosque.marketplace.reservas.application.SolicitudEstadia;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
public record EstadiaRequest(@NotNull UUID alojamientoId,@NotNull LocalDate fechaLlegada,
        @NotNull LocalDate fechaSalida,@Min(1) int numeroHuespedes) {
    public SolicitudEstadia aSolicitud() { return new SolicitudEstadia(alojamientoId,fechaLlegada,fechaSalida,numeroHuespedes); }
}
