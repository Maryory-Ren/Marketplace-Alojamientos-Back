package co.edu.unbosque.marketplace.resenas.application;
import co.edu.unbosque.marketplace.compartido.error.*; import co.edu.unbosque.marketplace.publicaciones.domain.*;
import co.edu.unbosque.marketplace.resenas.domain.*; import co.edu.unbosque.marketplace.reservas.domain.*;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado; import co.edu.unbosque.marketplace.trazabilidad.*;
import co.edu.unbosque.marketplace.usuarios.domain.UsuarioRepository;
import java.time.LocalDate; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class ResenaService {
 private final ResenaRepository resenas; private final ReservaRepository reservas; private final PublicacionRepository publicaciones;
 private final UsuarioRepository usuarios; private final AuditoriaPort auditoria;
 public ResenaService(ResenaRepository resenas,ReservaRepository reservas,PublicacionRepository publicaciones,UsuarioRepository usuarios,AuditoriaPort auditoria){this.resenas=resenas;this.reservas=reservas;this.publicaciones=publicaciones;this.usuarios=usuarios;this.auditoria=auditoria;}
 @Transactional public Vista crear(UUID reservaId,int calificacion,String comentario,UsuarioAutenticado huesped){
  if(calificacion<1||calificacion>5||comentario!=null&&comentario.length()>1000)throw new ExcepcionNegocio(TipoError.SOLICITUD_INVALIDA,"RESENA_INVALIDA","La calificación debe ser de 1 a 5 y el comentario de máximo 1000 caracteres");
  Reserva r=reservas.findByIdAndHuespedId(reservaId,huesped.id()).orElseThrow(()->new ExcepcionNegocio(TipoError.NO_ENCONTRADO,"RESERVA_NO_ENCONTRADA","La reserva no existe"));
  if(r.getEstado()!=EstadoReserva.COMPLETADA)throw new ExcepcionNegocio(TipoError.CONFLICTO,"ESTADO_RESERVA_INVALIDO","Solo puedes reseñar reservas completadas");
  if(resenas.findByReservaId(reservaId).isPresent())throw new ExcepcionNegocio(TipoError.CONFLICTO,"RESENA_YA_EXISTE","Esta reserva ya tiene una reseña");
  Resena resena=resenas.saveAndFlush(new Resena(reservaId,r.getPublicacionId(),huesped.id(),calificacion,comentario==null?"":comentario.trim()));
  auditoria.registrar(new EventoAuditoria(huesped.id(),"RESENAS","RESENA_CREADA","RESENA",resena.getId().toString(),EventoAuditoria.EXITO,null));return vista(resena);
 }
 @Transactional(readOnly=true) public PaginaResenas listarPublicas(UUID publicacionId,int pagina,int tamano){
  publicaciones.findById(publicacionId).filter(p->p.getEstado()==EstadoPublicacion.ACTIVA).orElseThrow(()->new ExcepcionNegocio(TipoError.NO_ENCONTRADO,"ALOJAMIENTO_NO_ENCONTRADO","El alojamiento no existe"));
  if(pagina<0||tamano<1||tamano>50)throw new ExcepcionNegocio(TipoError.SOLICITUD_INVALIDA,"PAGINACION_INVALIDA","La paginación no es válida");
  List<Resena> todas=resenas.findByPublicacionIdOrderByFechaCreacionDesc(publicacionId); long total=todas.size();
  int inicio=Math.min(pagina*tamano,todas.size()),fin=Math.min(inicio+tamano,todas.size());
  Double promedio=todas.isEmpty()?null:todas.stream().mapToInt(Resena::getCalificacion).average().orElse(0);
  return new PaginaResenas(todas.subList(inicio,fin).stream().map(this::vista).toList(),pagina,tamano,total,(int)Math.ceil((double)total/tamano),promedio);
 }
 private Vista vista(Resena r){String autor=usuarios.findById(r.getHuespedId()).map(u->u.getNombre()).orElse("Huésped");return new Vista(r.getId(),r.getPublicacionId(),autor,r.getCalificacion(),r.getComentario(),r.getFechaCreacion());}
 public record Vista(UUID id,UUID alojamientoId,String autorNombre,int calificacion,String comentario,java.time.Instant publicadaEn){}
 public record PaginaResenas(List<Vista> contenido,int pagina,int tamano,long total,int totalPaginas,Double calificacionPromedio){}
}
