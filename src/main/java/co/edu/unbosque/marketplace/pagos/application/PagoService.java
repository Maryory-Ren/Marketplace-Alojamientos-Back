package co.edu.unbosque.marketplace.pagos.application;

import co.edu.unbosque.marketplace.compartido.error.*;
import co.edu.unbosque.marketplace.pagos.domain.*;
import co.edu.unbosque.marketplace.publicaciones.domain.*;
import co.edu.unbosque.marketplace.reservas.domain.*;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import co.edu.unbosque.marketplace.trazabilidad.*;
import co.edu.unbosque.marketplace.outbox.application.OutboxService;
import java.math.*; import java.nio.charset.StandardCharsets; import java.security.MessageDigest;
import java.time.*; import java.util.*; import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.DataIntegrityViolationException; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service
public class PagoService {
 private final PagoRepository pagos; private final ReservaRepository reservas; private final PublicacionRepository publicaciones;
 private final List<EstrategiaPago> estrategias; private final AuditoriaPort auditoria; private final OutboxService outbox; private final AtomicLong abiertoHasta=new AtomicLong(0);
 public PagoService(PagoRepository pagos,ReservaRepository reservas,PublicacionRepository publicaciones,
        List<EstrategiaPago> estrategias,AuditoriaPort auditoria,OutboxService outbox){this.pagos=pagos;this.reservas=reservas;this.publicaciones=publicaciones;this.estrategias=estrategias;this.auditoria=auditoria;this.outbox=outbox;}
 @Transactional public ResultadoPagoVista pagar(UUID reservaId,String clave,EscenarioPago escenario,UsuarioAutenticado usuario){
  if(clave==null||clave.isBlank()||clave.length()>80) throw new ExcepcionNegocio(TipoError.SOLICITUD_INVALIDA,"IDEMPOTENCIA_REQUERIDA","Debes enviar Idempotency-Key");
  EscenarioPago elegido=escenario==null?EscenarioPago.APROBAR:escenario;
  String fingerprint=hash(reservaId+"|"+elegido);
  var repetido=pagos.findByHuespedIdAndClaveIdempotencia(usuario.id(),clave);
  if(repetido.isPresent()){
   Pago p=repetido.get(); if(!p.getHuellaSolicitud().equals(fingerprint)) throw new ExcepcionNegocio(TipoError.CONFLICTO,"IDEMPOTENCIA_CONFLICTO","La clave ya fue usada con otra operación");
   Reserva r=reservas.findById(p.getReservaId()).orElseThrow(); return vista(p,r.getEstado(),200);
  }
  if(elegido==EscenarioPago.CAER){
   long ahora=System.currentTimeMillis(); if(ahora<abiertoHasta.get()) throw new ExcepcionNegocio(TipoError.SERVICIO_NO_DISPONIBLE,"SERVICIO_PAGOS_NO_DISPONIBLE","El servicio de pagos está temporalmente no disponible");
   abiertoHasta.set(ahora+30_000); throw new ExcepcionNegocio(TipoError.SERVICIO_NO_DISPONIBLE,"SERVICIO_PAGOS_NO_DISPONIBLE","El proveedor de pagos no respondió");
  }
  if(System.currentTimeMillis()<abiertoHasta.get()) throw new ExcepcionNegocio(TipoError.SERVICIO_NO_DISPONIBLE,"SERVICIO_PAGOS_NO_DISPONIBLE","El servicio de pagos está temporalmente no disponible");
  Reserva r=reservas.findById(reservaId).filter(x->x.getHuespedId().equals(usuario.id())).orElseThrow(()->new ExcepcionNegocio(TipoError.NO_ENCONTRADO,"RESERVA_NO_ENCONTRADA","La reserva no existe"));
  if(r.getEstado()!=EstadoReserva.PENDIENTE_PAGO) throw new ExcepcionNegocio(TipoError.CONFLICTO,"ESTADO_RESERVA_INVALIDO","La reserva no está pendiente de pago");
  Publicacion p=publicaciones.findById(r.getPublicacionId()).orElseThrow();
  BigDecimal total=r.getTotal();
  EstrategiaPago estrategia=estrategias.stream().filter(e->e.escenario()==elegido).findFirst().orElseThrow();
  EstrategiaResultadoPago resultado=estrategia.procesar();
  Pago pago=new Pago(r.getId(),usuario.id(),clave,fingerprint,resultado.estadoPago(),total);
  try{pagos.saveAndFlush(pago);}catch(DataIntegrityViolationException e){throw new ExcepcionNegocio(TipoError.CONFLICTO,"PAGO_DUPLICADO","El pago ya fue procesado");}
  r.cambiarEstado(resultado.estadoReserva()); reservas.saveAndFlush(r);
  auditoria.registrar(new EventoAuditoria(usuario.id(),"PAGOS","PAGO_PROCESADO","PAGO",pago.getId().toString(),EventoAuditoria.EXITO,resultado.estadoPago().name()));
  outbox.registrar("PAGO",pago.getId().toString(),"PAGO_PROCESADO",Map.of("pagoId",pago.getId(),"reservaId",r.getId(),"estado",resultado.estadoPago().name()));
  return vista(pago,r.getEstado(),resultado.httpStatus());
 }
 private ResultadoPagoVista vista(Pago p,EstadoReserva estado,int http){return new ResultadoPagoVista(p.getId(),p.getReservaId(),p.getEstado(),estado,p.getTotal(),"COP",p.getProcesadoEn(),http);}
 private static String hash(String raw){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
