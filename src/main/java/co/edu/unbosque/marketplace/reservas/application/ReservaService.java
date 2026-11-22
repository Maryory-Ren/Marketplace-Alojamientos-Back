package co.edu.unbosque.marketplace.reservas.application;

import co.edu.unbosque.marketplace.compartido.error.*;
import co.edu.unbosque.marketplace.configuracion.application.TarifaServicioService;
import co.edu.unbosque.marketplace.publicaciones.domain.*;
import co.edu.unbosque.marketplace.reservas.ReservaFacade;
import co.edu.unbosque.marketplace.reservas.domain.*;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import co.edu.unbosque.marketplace.trazabilidad.*;
import co.edu.unbosque.marketplace.outbox.application.OutboxService;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {
    private final ReservaRepository reservas; private final PublicacionRepository publicaciones;
    private final PeriodoDisponibleRepository periodos; private final ReservaFacade facade;
    private final TarifaServicioService tarifa; private final AuditoriaPort auditoria; private final OutboxService outbox;
    public ReservaService(ReservaRepository reservas,PublicacionRepository publicaciones,PeriodoDisponibleRepository periodos,
            ReservaFacade facade,TarifaServicioService tarifa,AuditoriaPort auditoria,OutboxService outbox) {
        this.reservas=reservas;this.publicaciones=publicaciones;this.periodos=periodos;this.facade=facade;this.tarifa=tarifa;this.auditoria=auditoria;this.outbox=outbox;
    }
    @Transactional(readOnly=true)
    public Cotizacion cotizar(SolicitudEstadia s) {
        Publicacion p=validarSolicitud(s);
        return cotizacion(p,s);
    }
    @Transactional
    public CreacionReservaResultado crear(SolicitudEstadia s,String clave,UsuarioAutenticado huesped) {
        if (clave==null || clave.isBlank() || clave.length()>80)
            throw new ExcepcionNegocio(TipoError.SOLICITUD_INVALIDA,"IDEMPOTENCIA_REQUERIDA","Debes enviar Idempotency-Key");
        String fingerprint=huella(s);
        var existente=reservas.findByHuespedIdAndClaveIdempotencia(huesped.id(),clave);
        if (existente.isPresent()) {
            if (!Objects.equals(existente.get().getHuellaSolicitud(),fingerprint))
                throw new ExcepcionNegocio(TipoError.CONFLICTO,"IDEMPOTENCIA_CONFLICTO","La clave ya fue usada con otra solicitud");
            return new CreacionReservaResultado(vista(existente.get()),200);
        }
        Publicacion p=validarSolicitud(s);
        EstadoReserva estado=p.getModoReserva()==ModoReserva.INMEDIATA?EstadoReserva.PENDIENTE_PAGO:EstadoReserva.PENDIENTE_APROBACION;
        Cotizacion cotizacion=cotizacion(p,s);
        Reserva r=new Reserva(huesped.id(),p.getId(),p.getPoliticaCancelacionId(),s.fechaLlegada(),s.fechaSalida(),s.numeroHuespedes(),estado,clave,fingerprint,
                Math.toIntExact(cotizacion.noches()),cotizacion.precioNoche(),cotizacion.subtotal(),cotizacion.tarifaLimpieza(),
                cotizacion.tarifaServicio(),cotizacion.porcentajeTarifaServicio(),cotizacion.total());
        try { reservas.saveAndFlush(r); }
        catch (DataIntegrityViolationException e) { throw new ExcepcionNegocio(TipoError.CONFLICTO,"PERIODO_NO_DISPONIBLE","El alojamiento ya fue reservado para esas fechas"); }
        auditoria.registrar(new EventoAuditoria(huesped.id(),"RESERVAS","RESERVA_CREADA","RESERVA",r.getId().toString(),EventoAuditoria.EXITO,null));
        outbox.registrar("RESERVA",r.getId().toString(),"RESERVA_CREADA",Map.of("reservaId",r.getId(),"alojamientoId",p.getId()));
        return new CreacionReservaResultado(vista(r),201);
    }
    @Transactional(readOnly=true)
    public List<ReservaVista> listar(UsuarioAutenticado usuario) {
        return reservas.findByHuespedIdOrderByFechaCreacionDesc(usuario.id()).stream().map(this::vista).toList();
    }
    @Transactional(readOnly=true)
    public ReservaVista obtener(UUID id,UsuarioAutenticado usuario) {
        Reserva r=reservas.findByIdAndHuespedId(id,usuario.id()).orElseThrow(() ->
                new ExcepcionNegocio(TipoError.NO_ENCONTRADO,"RESERVA_NO_ENCONTRADA","La reserva no existe"));
        return vista(r);
    }
    @Transactional(readOnly=true)
    public List<ReservaVista> listarParaAnfitrion(UsuarioAutenticado anfitrion) {
        return reservas.buscarPorAnfitrion(anfitrion.id()).stream().map(this::vista).toList();
    }
    @Transactional
    public ReservaVista decidirReserva(UUID id, boolean aprobar, UsuarioAutenticado anfitrion) {
        Reserva r=reservas.findById(id).orElseThrow(() -> new ExcepcionNegocio(TipoError.NO_ENCONTRADO,"RESERVA_NO_ENCONTRADA","La reserva no existe"));
        Publicacion p=publicaciones.findById(r.getPublicacionId()).orElseThrow();
        if(!p.getAnfitrionId().equals(anfitrion.id())) throw new ExcepcionNegocio(TipoError.PROHIBIDO,"ACCESO_DENEGADO","No eres el anfitrión de este alojamiento");
        if(r.getEstado()!=EstadoReserva.PENDIENTE_APROBACION) throw new ExcepcionNegocio(TipoError.CONFLICTO,"ESTADO_RESERVA_INVALIDO","La reserva no está pendiente de aprobación");
        r.cambiarEstado(aprobar?EstadoReserva.CONFIRMADA:EstadoReserva.CANCELADA); reservas.saveAndFlush(r);
        auditoria.registrar(new EventoAuditoria(anfitrion.id(),"RESERVAS",aprobar?"RESERVA_APROBADA":"RESERVA_RECHAZADA","RESERVA",id.toString(),EventoAuditoria.EXITO,null));
        outbox.registrar("RESERVA",id.toString(),aprobar?"RESERVA_APROBADA":"RESERVA_RECHAZADA",Map.of("reservaId",id));
        return vista(r);
    }

    public record CreacionReservaResultado(ReservaVista reserva,int httpStatus) {}

    private Publicacion validarSolicitud(SolicitudEstadia s) {
        if (s==null || s.alojamientoId()==null || s.fechaLlegada()==null || s.fechaSalida()==null || s.numeroHuespedes()<1)
            throw new ExcepcionNegocio(TipoError.SOLICITUD_INVALIDA,"DATOS_RESERVA_INVALIDOS","Completa los datos de la estadía");
        if (!s.fechaSalida().isAfter(s.fechaLlegada()) || s.fechaLlegada().isBefore(LocalDate.now()))
            throw new ExcepcionNegocio(TipoError.SOLICITUD_INVALIDA,"FECHAS_INVALIDAS","La llegada no puede ser pasada y la salida debe ser posterior");
        Publicacion p=publicaciones.findById(s.alojamientoId()).filter(x -> x.getEstado()==EstadoPublicacion.ACTIVA)
                .orElseThrow(() -> new ExcepcionNegocio(TipoError.NO_ENCONTRADO,"ALOJAMIENTO_NO_ENCONTRADO","El alojamiento no existe"));
        if (s.numeroHuespedes()>p.getCapacidad()) throw new ExcepcionNegocio(TipoError.REGLA_NEGOCIO,"CAPACIDAD_EXCEDIDA","El alojamiento no tiene capacidad para ese número de huéspedes");
        LocalDate ultimaNoche=s.fechaSalida().minusDays(1);
        boolean cubierto=periodos.findByPublicacionIdOrderByFechaInicio(p.getId()).stream()
                .anyMatch(r -> !s.fechaLlegada().isBefore(r.getFechaInicio()) && !ultimaNoche.isAfter(r.getFechaFin()));
        if (!cubierto) throw new ExcepcionNegocio(TipoError.CONFLICTO,"PERIODO_NO_DISPONIBLE","El alojamiento no está disponible para todo el rango solicitado");
        if (!facade.reservasActivasEnRango(p.getId(),s.fechaLlegada(),ultimaNoche).isEmpty())
            throw new ExcepcionNegocio(TipoError.CONFLICTO,"PERIODO_NO_DISPONIBLE","El alojamiento ya tiene una reserva en esas fechas");
        return p;
    }
    private Cotizacion cotizacion(Publicacion p,SolicitudEstadia s) {
        long noches=ChronoUnit.DAYS.between(s.fechaLlegada(),s.fechaSalida());
        BigDecimal subtotal=p.getPrecioNoche().multiply(BigDecimal.valueOf(noches)).setScale(2,RoundingMode.HALF_UP);
        BigDecimal porcentaje=tarifa.consultar();
        BigDecimal fee=subtotal.multiply(porcentaje).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
        BigDecimal limpieza=p.getTarifaLimpieza()==null?BigDecimal.ZERO:p.getTarifaLimpieza();
        return new Cotizacion(noches,p.getPrecioNoche(),subtotal,limpieza,fee,porcentaje,subtotal.add(limpieza).add(fee),"COP");
    }
    private ReservaVista vista(Reserva r) {
        Publicacion p=publicaciones.findById(r.getPublicacionId()).orElseThrow();
        String portada=p.getImagenes().stream().min(Comparator.comparingInt(ImagenPublicacion::getOrden)).map(ImagenPublicacion::getRuta).orElse(null);
        Cotizacion cotizacion=new Cotizacion(r.getNoches(),r.getPrecioNoche(),r.getSubtotal(),r.getTarifaLimpieza(),
                r.getTarifaServicio(),r.getPorcentajeTarifaServicio(),r.getTotal(),"COP");
        return new ReservaVista(r.getId(),p.getId(),p.getTitulo(),p.getCiudad(),portada,r.getHuespedId(),r.getFechaLlegada(),
                r.getFechaSalida(),r.getNumeroHuespedes(),r.getEstado(),r.getTotal(),cotizacion,r.getFechaCreacion());
    }
    private static String huella(SolicitudEstadia s) {
        try { String raw=s.toString(); return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e) { throw new IllegalStateException(e); }
    }
}
