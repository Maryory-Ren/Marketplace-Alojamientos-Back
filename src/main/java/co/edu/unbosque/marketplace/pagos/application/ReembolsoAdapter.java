package co.edu.unbosque.marketplace.pagos.application;
import co.edu.unbosque.marketplace.pagos.domain.*; import co.edu.unbosque.marketplace.reservas.application.ReembolsoPort;
import java.math.BigDecimal; import java.util.UUID; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;
@Component public class ReembolsoAdapter implements ReembolsoPort {
 private final PagoRepository pagos; public ReembolsoAdapter(PagoRepository pagos){this.pagos=pagos;}
 @Transactional(readOnly=true) public BigDecimal totalPagado(UUID reservaId){return pagos.findByReservaId(reservaId).filter(p->p.getEstado()==EstadoPago.APROBADO).map(Pago::getTotal).orElse(BigDecimal.ZERO);}
 @Transactional(readOnly=true) public String estadoPago(UUID reservaId){return pagos.findByReservaId(reservaId).map(p->p.getEstado().name()).orElse("PENDIENTE");}
 @Transactional public void reembolsar(UUID reservaId){pagos.findByReservaId(reservaId).filter(p->p.getEstado()==EstadoPago.APROBADO).ifPresent(p->{p.reembolsar();pagos.saveAndFlush(p);});}
}
