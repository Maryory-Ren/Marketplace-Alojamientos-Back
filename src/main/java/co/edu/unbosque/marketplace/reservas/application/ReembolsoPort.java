package co.edu.unbosque.marketplace.reservas.application;
import java.math.BigDecimal; import java.util.UUID;
public interface ReembolsoPort { BigDecimal totalPagado(UUID reservaId); String estadoPago(UUID reservaId); void reembolsar(UUID reservaId); }
