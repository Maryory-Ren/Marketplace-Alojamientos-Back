package co.edu.unbosque.marketplace.pagos.application;

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
import co.edu.unbosque.marketplace.compartido.error.TipoError;
import co.edu.unbosque.marketplace.outbox.application.OutboxService;
import co.edu.unbosque.marketplace.pagos.domain.EstadoPago;
import co.edu.unbosque.marketplace.pagos.domain.Pago;
import co.edu.unbosque.marketplace.pagos.domain.PagoRepository;
import co.edu.unbosque.marketplace.publicaciones.domain.Publicacion;
import co.edu.unbosque.marketplace.publicaciones.domain.PublicacionRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

class PagoServiceTest {

    private final PagoRepository pagos = mock(PagoRepository.class);
    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final PublicacionRepository publicaciones = mock(PublicacionRepository.class);
    private final AuditoriaPort auditoria = mock(AuditoriaPort.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final List<Pago> guardados = new ArrayList<>();

    private PagoService service;
    private UsuarioAutenticado huesped;

    @BeforeEach
    void preparar() {
        service = new PagoService(pagos, reservas, publicaciones,
                List.of(new EstrategiaAprobarPago(), new EstrategiaRechazarPago(), new EstrategiaDemorarPago()),
                auditoria, outbox);
        huesped = new UsuarioAutenticado(UUID.randomUUID(), "HUESPED");
        when(publicaciones.findById(any())).thenReturn(Optional.of(mock(Publicacion.class)));
        when(pagos.saveAndFlush(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            guardados.add(p);
            return p;
        });
    }

    private Reserva reserva(UUID huespedId, EstadoReserva estado) {
        return new Reserva(huespedId, UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), 2, estado, "clave", "huella", 2,
                new BigDecimal("100000.00"), new BigDecimal("200000.00"), new BigDecimal("10000.00"),
                new BigDecimal("17000.00"), new BigDecimal("8.5000"), new BigDecimal("227000.00"));
    }

    private Reserva reservaPendiente() {
        Reserva r = reserva(huesped.id(), EstadoReserva.PENDIENTE_PAGO);
        when(reservas.findById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    private static String codigo(Throwable t) {
        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        return ((ExcepcionNegocio) t).getCodigo();
    }

    @Test
    void exige_clave_de_idempotencia() {
        UUID id = UUID.randomUUID();
        assertThat(codigo(catchThrowable(() -> service.pagar(id, null, EscenarioPago.APROBAR, huesped))))
                .isEqualTo("IDEMPOTENCIA_REQUERIDA");
        assertThat(codigo(catchThrowable(() -> service.pagar(id, "  ", EscenarioPago.APROBAR, huesped))))
                .isEqualTo("IDEMPOTENCIA_REQUERIDA");
        assertThat(codigo(catchThrowable(() -> service.pagar(id, "x".repeat(81), EscenarioPago.APROBAR, huesped))))
                .isEqualTo("IDEMPOTENCIA_REQUERIDA");
    }

    @Test
    void pago_aprobado_confirma_la_reserva() {
        Reserva r = reservaPendiente();

        ResultadoPagoVista vista = service.pagar(r.getId(), "k1", EscenarioPago.APROBAR, huesped);

        assertThat(vista.estadoPago()).isEqualTo(EstadoPago.APROBADO);
        assertThat(vista.estadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(vista.httpStatus()).isEqualTo(200);
        assertThat(vista.total()).isEqualByComparingTo("227000.00");
        assertThat(vista.moneda()).isEqualTo("COP");
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(auditoria).registrar(any());
        verify(outbox).registrar(eq("PAGO"), anyString(), eq("PAGO_PROCESADO"), any());
    }

    @Test
    void sin_escenario_se_aprueba_por_defecto() {
        Reserva r = reservaPendiente();

        ResultadoPagoVista vista = service.pagar(r.getId(), "k1", null, huesped);

        assertThat(vista.estadoPago()).isEqualTo(EstadoPago.APROBADO);
    }

    @Test
    void pago_rechazado_deja_la_reserva_en_pago_fallido() {
        Reserva r = reservaPendiente();

        ResultadoPagoVista vista = service.pagar(r.getId(), "k1", EscenarioPago.RECHAZAR, huesped);

        assertThat(vista.estadoPago()).isEqualTo(EstadoPago.FALLIDO);
        assertThat(vista.estadoReserva()).isEqualTo(EstadoReserva.PAGO_FALLIDO);
        assertThat(vista.httpStatus()).isEqualTo(402);
    }

    @Test
    void pago_demorado_deja_la_reserva_en_revision() {
        Reserva r = reservaPendiente();

        ResultadoPagoVista vista = service.pagar(r.getId(), "k1", EscenarioPago.DEMORAR, huesped);

        assertThat(vista.estadoPago()).isEqualTo(EstadoPago.PAGO_INCIERTO);
        assertThat(vista.estadoReserva()).isEqualTo(EstadoReserva.EN_REVISION);
        assertThat(vista.httpStatus()).isEqualTo(202);
    }

    @Test
    void repetir_la_misma_solicitud_devuelve_el_resultado_previo() {
        Reserva r = reservaPendiente();
        service.pagar(r.getId(), "k1", EscenarioPago.APROBAR, huesped);
        Pago previo = guardados.get(0);
        when(pagos.findByHuespedIdAndClaveIdempotencia(huesped.id(), "k1")).thenReturn(Optional.of(previo));

        ResultadoPagoVista repetido = service.pagar(r.getId(), "k1", EscenarioPago.APROBAR, huesped);

        assertThat(repetido.pagoId()).isEqualTo(previo.getId());
        assertThat(repetido.httpStatus()).isEqualTo(200);
        assertThat(guardados).hasSize(1);
    }

    @Test
    void reutilizar_la_clave_con_otra_solicitud_es_conflicto() {
        Pago otro = new Pago(UUID.randomUUID(), huesped.id(), "k1", "otra-huella", EstadoPago.APROBADO,
                BigDecimal.TEN);
        when(pagos.findByHuespedIdAndClaveIdempotencia(huesped.id(), "k1")).thenReturn(Optional.of(otro));

        Throwable t = catchThrowable(() -> service.pagar(UUID.randomUUID(), "k1", EscenarioPago.APROBAR, huesped));

        assertThat(codigo(t)).isEqualTo("IDEMPOTENCIA_CONFLICTO");
        assertThat(((ExcepcionNegocio) t).getTipo()).isEqualTo(TipoError.CONFLICTO);
    }

    @Test
    void reserva_inexistente_o_ajena_no_se_encuentra() {
        UUID noExiste = UUID.randomUUID();
        assertThat(codigo(catchThrowable(() -> service.pagar(noExiste, "k1", EscenarioPago.APROBAR, huesped))))
                .isEqualTo("RESERVA_NO_ENCONTRADA");

        Reserva ajena = reserva(UUID.randomUUID(), EstadoReserva.PENDIENTE_PAGO);
        when(reservas.findById(ajena.getId())).thenReturn(Optional.of(ajena));
        assertThat(codigo(catchThrowable(() -> service.pagar(ajena.getId(), "k2", EscenarioPago.APROBAR, huesped))))
                .isEqualTo("RESERVA_NO_ENCONTRADA");
    }

    @Test
    void solo_se_paga_una_reserva_pendiente_de_pago() {
        Reserva r = reserva(huesped.id(), EstadoReserva.CONFIRMADA);
        when(reservas.findById(r.getId())).thenReturn(Optional.of(r));

        assertThat(codigo(catchThrowable(() -> service.pagar(r.getId(), "k1", EscenarioPago.APROBAR, huesped))))
                .isEqualTo("ESTADO_RESERVA_INVALIDO");
        verify(pagos, never()).saveAndFlush(any(Pago.class));
    }

    @Test
    void un_pago_duplicado_concurrente_se_reporta_como_conflicto() {
        Reserva r = reservaPendiente();
        doThrow(new DataIntegrityViolationException("duplicado")).when(pagos).saveAndFlush(any(Pago.class));

        assertThat(codigo(catchThrowable(() -> service.pagar(r.getId(), "k1", EscenarioPago.APROBAR, huesped))))
                .isEqualTo("PAGO_DUPLICADO");
    }

    @Test
    void el_escenario_caer_abre_el_circuito_temporalmente() {
        Reserva r = reservaPendiente();

        Throwable primero = catchThrowable(() -> service.pagar(r.getId(), "k1", EscenarioPago.CAER, huesped));
        assertThat(codigo(primero)).isEqualTo("SERVICIO_PAGOS_NO_DISPONIBLE");
        assertThat(((ExcepcionNegocio) primero).getTipo()).isEqualTo(TipoError.SERVICIO_NO_DISPONIBLE);

        // con el circuito abierto, tanto CAER como un pago normal se rechazan sin tocar la reserva
        Throwable segundo = catchThrowable(() -> service.pagar(r.getId(), "k2", EscenarioPago.CAER, huesped));
        assertThat(codigo(segundo)).isEqualTo("SERVICIO_PAGOS_NO_DISPONIBLE");
        Throwable normal = catchThrowable(() -> service.pagar(r.getId(), "k3", EscenarioPago.APROBAR, huesped));
        assertThat(codigo(normal)).isEqualTo("SERVICIO_PAGOS_NO_DISPONIBLE");
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.PENDIENTE_PAGO);
    }
}
