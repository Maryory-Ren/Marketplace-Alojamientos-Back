package co.edu.unbosque.marketplace.reservas.application;
import co.edu.unbosque.marketplace.compartido.error.*;
import co.edu.unbosque.marketplace.publicaciones.domain.*;
import co.edu.unbosque.marketplace.reservas.domain.*;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import co.edu.unbosque.marketplace.trazabilidad.*;
import java.math.*; import java.time.*; import java.util.UUID;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class CancelacionService {
 private final ReservaRepository reservas; private final PoliticaCancelacionRepository politicas;
 private final ReembolsoPort reembolsos; private final AuditoriaPort auditoria;
 public CancelacionService(ReservaRepository reservas,PoliticaCancelacionRepository politicas,ReembolsoPort reembolsos,AuditoriaPort auditoria){this.reservas=reservas;this.politicas=politicas;this.reembolsos=reembolsos;this.auditoria=auditoria;}
 @Transactional(readOnly=true) public CancelacionVista previsualizar(UUID id,UsuarioAutenticado huesped){return calcular(obtenerPropia(id,huesped));}
 @Transactional public ResultadoCancelacion cancelar(UUID id,UsuarioAutenticado huesped){
  Reserva r=obtenerPropia(id,huesped); CancelacionVista vista=calcular(r);
  if(!vista.cancelable()) throw new ExcepcionNegocio(TipoError.CONFLICTO,"ESTADO_RESERVA_INVALIDO","La reserva ya no se puede cancelar");
  r.cambiarEstado(EstadoReserva.CANCELADA); reservas.saveAndFlush(r);
  if(vista.montoDevolucion().signum()>0) reembolsos.reembolsar(id);
  auditoria.registrar(new EventoAuditoria(huesped.id(),"RESERVAS","RESERVA_CANCELADA","RESERVA",id.toString(),EventoAuditoria.EXITO,null));
  return new ResultadoCancelacion(id,EstadoReserva.CANCELADA,reembolsos.estadoPago(id),vista.montoDevolucion(),"COP",java.time.Instant.now());
 }
 private Reserva obtenerPropia(UUID id,UsuarioAutenticado u){return reservas.findByIdAndHuespedId(id,u.id()).orElseThrow(()->new ExcepcionNegocio(TipoError.NO_ENCONTRADO,"RESERVA_NO_ENCONTRADA","La reserva no existe"));}
 private CancelacionVista calcular(Reserva r){
  boolean estadoValido=r.getEstado()==EstadoReserva.PENDIENTE_PAGO||r.getEstado()==EstadoReserva.PENDIENTE_APROBACION||r.getEstado()==EstadoReserva.CONFIRMADA;
  if(!estadoValido)return new CancelacionVista(r.getId(),false,0,"",BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,"COP");
  PoliticaCancelacion politica=politicas.findById(r.getPoliticaCancelacionId()).orElseThrow();
  long horasHastaLlegada=Math.max(0,Duration.between(LocalDateTime.now(),r.getFechaLlegada().atStartOfDay()).toHours());
  BigDecimal porcentaje=horasHastaLlegada>=politica.getHorasAntesLlegada()?politica.getPorcentajeDevolucion():BigDecimal.ZERO;
  BigDecimal total=reembolsos.totalPagado(r.getId());
  BigDecimal devolucion=total.multiply(porcentaje).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
  return new CancelacionVista(r.getId(),true,horasHastaLlegada,politica.getNombre(),porcentaje,total,devolucion,total.subtract(devolucion),"COP");
 }
}
