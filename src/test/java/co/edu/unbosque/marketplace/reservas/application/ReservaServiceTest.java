package co.edu.unbosque.marketplace.reservas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.compartido.error.ExcepcionNegocio;
import co.edu.unbosque.marketplace.configuracion.application.TarifaServicioService;
import co.edu.unbosque.marketplace.outbox.application.OutboxService;
import co.edu.unbosque.marketplace.publicaciones.domain.EstadoPublicacion;
import co.edu.unbosque.marketplace.publicaciones.domain.ImagenPublicacion;
import co.edu.unbosque.marketplace.publicaciones.domain.ModoReserva;
import co.edu.unbosque.marketplace.publicaciones.domain.PeriodoDisponible;
import co.edu.unbosque.marketplace.publicaciones.domain.PeriodoDisponibleRepository;
import co.edu.unbosque.marketplace.publicaciones.domain.Publicacion;
import co.edu.unbosque.marketplace.publicaciones.domain.PublicacionRepository;
import co.edu.unbosque.marketplace.publicaciones.domain.ReservaResumen;
import co.edu.unbosque.marketplace.reservas.ReservaFacade;
import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
import co.edu.unbosque.marketplace.reservas.domain.Reserva;
import co.edu.unbosque.marketplace.reservas.domain.ReservaRepository;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import co.edu.unbosque.marketplace.trazabilidad.AuditoriaPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ReservaServiceTest {

    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final PublicacionRepository publicaciones = mock(PublicacionRepository.class);
    private final PeriodoDisponibleRepository periodos = mock(PeriodoDisponibleRepository.class);
    private final ReservaFacade facade = mock(ReservaFacade.class);
    private final TarifaServicioService tarifa = mock(TarifaServicioService.class);
    private final AuditoriaPort auditoria = mock(AuditoriaPort.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final List<Reserva> guardadas = new ArrayList<>();

    private ReservaService service;
    private Publicacion publicacion;
    private UsuarioAutenticado huesped;
    private UsuarioAutenticado anfitrion;

    @BeforeEach
    void preparar() {
        service = new ReservaService(reservas, publicaciones, periodos, facade, tarifa, auditoria, outbox);
        huesped = new UsuarioAutenticado(UUID.randomUUID(), "HUESPED");
        anfitrion = new UsuarioAutenticado(UUID.randomUUID(), "ANFITRION");

        publicacion = mock(Publicacion.class);
        when(publicacion.getId()).thenReturn(UUID.randomUUID());
        when(publicacion.getAnfitrionId()).thenReturn(anfitrion.id());
        when(publicacion.getPoliticaCancelacionId()).thenReturn(UUID.randomUUID());
        when(publicacion.getEstado()).thenReturn(EstadoPublicacion.ACTIVA);
        when(publicacion.getCapacidad()).thenReturn(4);
        when(publicacion.getPrecioNoche()).thenReturn(new BigDecimal("100000.00"));
        when(publicacion.getTarifaLimpieza()).thenReturn(new BigDecimal("10000.00"));
        when(publicacion.getModoReserva()).thenReturn(ModoReserva.INMEDIATA);
        when(publicacion.getTitulo()).thenReturn("Casa en Medellín");
        when(publicacion.getCiudad()).thenReturn("Medellín");
        when(publicacion.getImagenes()).thenReturn(List.of(new ImagenPublicacion("b.jpg", 2),
                new ImagenPublicacion("a.jpg", 1)));
        when(publicaciones.findById(any())).thenReturn(Optional.of(publicacion));

        PeriodoDisponible periodo = mock(PeriodoDisponible.class);
        when(periodo.getFechaInicio()).thenReturn(LocalDate.now());
        when(periodo.getFechaFin()).thenReturn(LocalDate.now().plusDays(90));
        when(periodos.findByPublicacionIdOrderByFechaInicio(any())).thenReturn(List.of(periodo));

        when(tarifa.consultar()).thenReturn(new BigDecimal("8.5"));
        when(reservas.saveAndFlush(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            guardadas.add(r);
            return r;
        });
    }

    private SolicitudEstadia solicitud(int noches, int huespedes) {
        LocalDate llegada = LocalDate.now().plusDays(5);
        return new SolicitudEstadia(publicacion.getId(), llegada, llegada.plusDays(noches), huespedes);
    }

    private Reserva reservaDe(EstadoReserva estado) {
        Reserva r = new Reserva(huesped.id(), publicacion.getId(), UUID.randomUUID(), LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7), 2, estado, "k", "huella-previa", 2, new BigDecimal("100000.00"),
                new BigDecimal("200000.00"), new BigDecimal("10000.00"), new BigDecimal("17000.00"),
                new BigDecimal("8.5000"), new BigDecimal("227000.00"));
        when(reservas.findById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    private static String codigo(Throwable t) {
        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        return ((ExcepcionNegocio) t).getCodigo();
    }

    // ---------- cotizar ----------

    @Test
    void cotiza_noches_tarifa_de_servicio_y_total() {
        Cotizacion c = service.cotizar(solicitud(2, 2));

        assertThat(c.noches()).isEqualTo(2);
        assertThat(c.subtotal()).isEqualByComparingTo("200000.00");
        assertThat(c.tarifaLimpieza()).isEqualByComparingTo("10000.00");
        assertThat(c.tarifaServicio()).isEqualByComparingTo("17000.00");
        assertThat(c.porcentajeTarifaServicio()).isEqualByComparingTo("8.5");
        assertThat(c.total()).isEqualByComparingTo("227000.00");
        assertThat(c.moneda()).isEqualTo("COP");
    }

    @Test
    void cotiza_sin_tarifa_de_limpieza() {
        when(publicacion.getTarifaLimpieza()).thenReturn(null);

        Cotizacion c = service.cotizar(solicitud(1, 1));

        assertThat(c.tarifaLimpieza()).isEqualByComparingTo("0");
        assertThat(c.total()).isEqualByComparingTo("108500.00");
    }

    @Test
    void rechaza_solicitudes_incompletas() {
        LocalDate hoy = LocalDate.now();
        assertThat(codigo(catchThrowable(() -> service.cotizar(null)))).isEqualTo("DATOS_RESERVA_INVALIDOS");
        assertThat(codigo(catchThrowable(() -> service.cotizar(new SolicitudEstadia(null, hoy, hoy.plusDays(1), 1)))))
                .isEqualTo("DATOS_RESERVA_INVALIDOS");
        assertThat(codigo(catchThrowable(() -> service.cotizar(
                new SolicitudEstadia(publicacion.getId(), hoy.plusDays(1), hoy.plusDays(2), 0)))))
                .isEqualTo("DATOS_RESERVA_INVALIDOS");
    }

    @Test
    void rechaza_fechas_invalidas() {
        LocalDate hoy = LocalDate.now();
        assertThat(codigo(catchThrowable(() -> service.cotizar(
                new SolicitudEstadia(publicacion.getId(), hoy.plusDays(3), hoy.plusDays(3), 1)))))
                .isEqualTo("FECHAS_INVALIDAS");
        assertThat(codigo(catchThrowable(() -> service.cotizar(
                new SolicitudEstadia(publicacion.getId(), hoy.minusDays(1), hoy.plusDays(1), 1)))))
                .isEqualTo("FECHAS_INVALIDAS");
    }

    @Test
    void rechaza_alojamientos_inexistentes_o_inactivos() {
        when(publicacion.getEstado()).thenReturn(EstadoPublicacion.PAUSADA);
        assertThat(codigo(catchThrowable(() -> service.cotizar(solicitud(2, 2)))))
                .isEqualTo("ALOJAMIENTO_NO_ENCONTRADO");

        when(publicaciones.findById(any())).thenReturn(Optional.empty());
        assertThat(codigo(catchThrowable(() -> service.cotizar(solicitud(2, 2)))))
                .isEqualTo("ALOJAMIENTO_NO_ENCONTRADO");
    }

    @Test
    void rechaza_si_se_excede_la_capacidad() {
        assertThat(codigo(catchThrowable(() -> service.cotizar(solicitud(2, 5))))).isEqualTo("CAPACIDAD_EXCEDIDA");
    }

    @Test
    void rechaza_si_las_fechas_no_estan_en_un_periodo_disponible() {
        when(periodos.findByPublicacionIdOrderByFechaInicio(any())).thenReturn(List.of());

        assertThat(codigo(catchThrowable(() -> service.cotizar(solicitud(2, 2)))))
                .isEqualTo("PERIODO_NO_DISPONIBLE");
    }

    @Test
    void rechaza_si_ya_hay_reservas_en_el_rango() {
        when(facade.reservasActivasEnRango(any(), any(), any())).thenReturn(List.of(
                new ReservaResumen(UUID.randomUUID(), LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), 2,
                        "CONFIRMADA")));

        assertThat(codigo(catchThrowable(() -> service.cotizar(solicitud(2, 2)))))
                .isEqualTo("PERIODO_NO_DISPONIBLE");
    }

    // ---------- crear ----------

    @Test
    void exige_clave_de_idempotencia_para_crear() {
        assertThat(codigo(catchThrowable(() -> service.crear(solicitud(2, 2), null, huesped))))
                .isEqualTo("IDEMPOTENCIA_REQUERIDA");
        assertThat(codigo(catchThrowable(() -> service.crear(solicitud(2, 2), "", huesped))))
                .isEqualTo("IDEMPOTENCIA_REQUERIDA");
        assertThat(codigo(catchThrowable(() -> service.crear(solicitud(2, 2), "x".repeat(81), huesped))))
                .isEqualTo("IDEMPOTENCIA_REQUERIDA");
    }

    @Test
    void crea_reserva_inmediata_pendiente_de_pago() {
        ReservaService.CreacionReservaResultado r = service.crear(solicitud(2, 2), "k1", huesped);

        assertThat(r.httpStatus()).isEqualTo(201);
        assertThat(r.reserva().estado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
        assertThat(r.reserva().total()).isEqualByComparingTo("227000.00");
        assertThat(r.reserva().imagenPortada()).isEqualTo("a.jpg");
        assertThat(r.reserva().alojamientoTitulo()).isEqualTo("Casa en Medellín");
        verify(auditoria).registrar(any());
        verify(outbox).registrar(eq("RESERVA"), anyString(), eq("RESERVA_CREADA"), any());
    }

    @Test
    void crea_reserva_con_aprobacion_pendiente_de_aprobacion() {
        when(publicacion.getModoReserva()).thenReturn(ModoReserva.CON_APROBACION);

        ReservaService.CreacionReservaResultado r = service.crear(solicitud(1, 1), "k1", huesped);

        assertThat(r.reserva().estado()).isEqualTo(EstadoReserva.PENDIENTE_APROBACION);
    }

    @Test
    void repetir_la_creacion_devuelve_la_misma_reserva() {
        SolicitudEstadia s = solicitud(2, 2);
        service.crear(s, "k1", huesped);
        Reserva primera = guardadas.get(0);
        when(reservas.findByHuespedIdAndClaveIdempotencia(huesped.id(), "k1")).thenReturn(Optional.of(primera));

        ReservaService.CreacionReservaResultado repetida = service.crear(s, "k1", huesped);

        assertThat(repetida.httpStatus()).isEqualTo(200);
        assertThat(repetida.reserva().id()).isEqualTo(primera.getId());
        assertThat(guardadas).hasSize(1);
    }

    @Test
    void reutilizar_la_clave_con_otra_solicitud_es_conflicto() {
        Reserva previa = reservaDe(EstadoReserva.PENDIENTE_PAGO);
        when(reservas.findByHuespedIdAndClaveIdempotencia(huesped.id(), "k1")).thenReturn(Optional.of(previa));

        assertThat(codigo(catchThrowable(() -> service.crear(solicitud(2, 2), "k1", huesped))))
                .isEqualTo("IDEMPOTENCIA_CONFLICTO");
    }

    @Test
    void una_colision_en_la_base_se_reporta_como_periodo_no_disponible() {
        doThrow(new DataIntegrityViolationException("ex_reserva_sin_superposicion"))
                .when(reservas).saveAndFlush(any(Reserva.class));

        assertThat(codigo(catchThrowable(() -> service.crear(solicitud(2, 2), "k1", huesped))))
                .isEqualTo("PERIODO_NO_DISPONIBLE");
        verify(outbox, never()).registrar(anyString(), anyString(), anyString(), any());
    }

    // ---------- consultas ----------

    @Test
    void lista_y_obtiene_reservas_del_huesped() {
        Reserva r = reservaDe(EstadoReserva.CONFIRMADA);
        when(reservas.findByHuespedIdOrderByFechaCreacionDesc(huesped.id())).thenReturn(List.of(r));
        when(reservas.findByIdAndHuespedId(r.getId(), huesped.id())).thenReturn(Optional.of(r));

        assertThat(service.listar(huesped)).hasSize(1);
        assertThat(service.obtener(r.getId(), huesped).id()).isEqualTo(r.getId());
        assertThat(codigo(catchThrowable(() -> service.obtener(UUID.randomUUID(), huesped))))
                .isEqualTo("RESERVA_NO_ENCONTRADA");
    }

    @Test
    void lista_las_reservas_de_un_anfitrion() {
        Reserva r = reservaDe(EstadoReserva.PENDIENTE_APROBACION);
        when(reservas.buscarPorAnfitrion(anfitrion.id())).thenReturn(List.of(r));

        assertThat(service.listarParaAnfitrion(anfitrion)).hasSize(1);
    }

    // ---------- decisión del anfitrión ----------

    @Test
    void el_anfitrion_aprueba_una_reserva_pendiente() {
        Reserva r = reservaDe(EstadoReserva.PENDIENTE_APROBACION);

        ReservaVista vista = service.decidirReserva(r.getId(), true, anfitrion);

        assertThat(vista.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(outbox).registrar(eq("RESERVA"), anyString(), eq("RESERVA_APROBADA"), any());
    }

    @Test
    void el_anfitrion_rechaza_una_reserva_pendiente() {
        Reserva r = reservaDe(EstadoReserva.PENDIENTE_APROBACION);

        ReservaVista vista = service.decidirReserva(r.getId(), false, anfitrion);

        assertThat(vista.estado()).isEqualTo(EstadoReserva.CANCELADA);
        verify(outbox).registrar(eq("RESERVA"), anyString(), eq("RESERVA_RECHAZADA"), any());
    }

    @Test
    void la_decision_valida_existencia_propiedad_y_estado() {
        assertThat(codigo(catchThrowable(() -> service.decidirReserva(UUID.randomUUID(), true, anfitrion))))
                .isEqualTo("RESERVA_NO_ENCONTRADA");

        Reserva r = reservaDe(EstadoReserva.PENDIENTE_APROBACION);
        UsuarioAutenticado otro = new UsuarioAutenticado(UUID.randomUUID(), "ANFITRION");
        assertThat(codigo(catchThrowable(() -> service.decidirReserva(r.getId(), true, otro))))
                .isEqualTo("ACCESO_DENEGADO");

        Reserva confirmada = reservaDe(EstadoReserva.CONFIRMADA);
        assertThat(codigo(catchThrowable(() -> service.decidirReserva(confirmada.getId(), true, anfitrion))))
                .isEqualTo("ESTADO_RESERVA_INVALIDO");
    }
}
