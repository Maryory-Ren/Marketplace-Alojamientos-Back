package co.edu.unbosque.marketplace.pagos.domain;
import java.util.Optional; import java.util.UUID;
public interface PagoRepository { Optional<Pago> findByHuespedIdAndClaveIdempotencia(UUID huespedId,String clave); Optional<Pago> findByReservaId(UUID reservaId); <S extends Pago> S saveAndFlush(S pago); }
