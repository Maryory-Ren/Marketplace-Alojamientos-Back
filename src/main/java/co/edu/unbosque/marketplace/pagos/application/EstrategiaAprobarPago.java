package co.edu.unbosque.marketplace.pagos.application;
import co.edu.unbosque.marketplace.pagos.domain.EstadoPago; import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
import org.springframework.stereotype.Component;
@Component public class EstrategiaAprobarPago implements EstrategiaPago {
 public EscenarioPago escenario(){return EscenarioPago.APROBAR;}
 public EstrategiaResultadoPago procesar(){return new EstrategiaResultadoPago(EstadoPago.APROBADO,EstadoReserva.CONFIRMADA,200);}
}
