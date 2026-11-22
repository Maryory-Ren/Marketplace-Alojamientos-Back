package co.edu.unbosque.marketplace.pagos.application;
import co.edu.unbosque.marketplace.pagos.domain.EstadoPago; import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record ResultadoPagoVista(UUID pagoId,UUID reservaId,EstadoPago estadoPago,EstadoReserva estadoReserva,
        BigDecimal total,String moneda,Instant procesadoEn,int httpStatus) {}
