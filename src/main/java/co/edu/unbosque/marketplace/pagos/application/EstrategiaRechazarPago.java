package co.edu.unbosque.marketplace.pagos.application;
import co.edu.unbosque.marketplace.pagos.domain.EstadoPago; import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
import org.springframework.stereotype.Component;
@Component public class EstrategiaRechazarPago implements EstrategiaPago {
 public EscenarioPago escenario(){return EscenarioPago.RECHAZAR;}
 public EstrategiaResultadoPago procesar(){return new EstrategiaResultadoPago(EstadoPago.FALLIDO,EstadoReserva.PAGO_FALLIDO,402);}
}
