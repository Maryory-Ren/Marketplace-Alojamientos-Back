package co.edu.unbosque.marketplace.reservas.domain;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface ReservaRepository {
    List<Reserva> buscarSolapadas(UUID publicacionId, LocalDate inicio, LocalDate finInclusivo, Collection<EstadoReserva> estados);
    Optional<Reserva> findById(UUID id);
    Optional<Reserva> findByIdAndHuespedId(UUID id, UUID huespedId);
    Optional<Reserva> findByHuespedIdAndClaveIdempotencia(UUID huespedId, String claveIdempotencia);
    List<Reserva> findByHuespedIdOrderByFechaCreacionDesc(UUID huespedId);
    <S extends Reserva> S saveAndFlush(S reserva);
    List<Reserva> buscarPorAnfitrion(UUID anfitrionId);
}
