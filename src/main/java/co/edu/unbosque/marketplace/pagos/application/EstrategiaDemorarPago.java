package co.edu.unbosque.marketplace.pagos.application;
import co.edu.unbosque.marketplace.pagos.domain.EstadoPago; import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
import org.springframework.stereotype.Component;
@Component public class EstrategiaDemorarPago implements EstrategiaPago {
 public EscenarioPago escenario(){return EscenarioPago.DEMORAR;}
 public EstrategiaResultadoPago procesar(){return new EstrategiaResultadoPago(EstadoPago.PAGO_INCIERTO,EstadoReserva.EN_REVISION,202);}
}
