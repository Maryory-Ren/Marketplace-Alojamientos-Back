package co.edu.unbosque.marketplace.pagos.application;
import co.edu.unbosque.marketplace.pagos.domain.EstadoPago;
import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
public record EstrategiaResultadoPago(EstadoPago estadoPago, EstadoReserva estadoReserva, int httpStatus) {}
