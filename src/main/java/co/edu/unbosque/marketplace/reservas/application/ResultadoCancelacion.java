package co.edu.unbosque.marketplace.reservas.application;
import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record ResultadoCancelacion(UUID reservaId,EstadoReserva estadoReserva,String estadoPago,
        BigDecimal montoDevolucion,String moneda,Instant canceladaEn) {}
