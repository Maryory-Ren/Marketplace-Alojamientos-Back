package co.edu.unbosque.marketplace.reservas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.compartido.error.ExcepcionNegocio;
import co.edu.unbosque.marketplace.publicaciones.domain.PoliticaCancelacion;
import co.edu.unbosque.marketplace.publicaciones.domain.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
import co.edu.unbosque.marketplace.reservas.domain.Reserva;
import co.edu.unbosque.marketplace.reservas.domain.ReservaRepository;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import co.edu.unbosque.marketplace.trazabilidad.AuditoriaPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CancelacionServiceTest {

    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final PoliticaCancelacionRepository politicas = mock(PoliticaCancelacionRepository.class);
    private final ReembolsoPort reembolsos = mock(ReembolsoPort.class);
    private final AuditoriaPort auditoria = mock(AuditoriaPort.class);
    private final CancelacionService service = new CancelacionService(reservas, politicas, reembolsos, auditoria);
    private final UsuarioAutenticado huesped = new UsuarioAutenticado(UUID.randomUUID(), "HUESPED");

    @BeforeEach
    void politica() {
        PoliticaCancelacion flexible = mock(PoliticaCancelacion.class);
        when(flexible.getNombre()).thenReturn("FLEXIBLE");
        when(flexible.getHorasAntesLlegada()).thenReturn(24);
        when(flexible.getPorcentajeDevolucion()).thenReturn(new BigDecimal("100.00"));
        when(politicas.findById(any())).thenReturn(Optional.of(flexible));
    }

    private Reserva reserva(EstadoReserva estado, LocalDate llegada) {
        Reserva r = new Reserva(huesped.id(), UUID.randomUUID(), UUID.randomUUID(), llegada, llegada.plusDays(2),
                2, estado, "clave", "huella", 2, new BigDecimal("100000.00"), new BigDecimal("200000.00"),
                new BigDecimal("10000.00"), new BigDecimal("17000.00"), new BigDecimal("8.5000"),
                new BigDecimal("227000.00"));
        when(reservas.findByIdAndHuespedId(r.getId(), huesped.id())).thenReturn(Optional.of(r));
        when(reembolsos.totalPagado(r.getId())).thenReturn(new BigDecimal("200000.00"));
        return r;
    }

    @Test
    void con_anticipacion_suficiente_se_devuelve_todo() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(10));

        CancelacionVista vista = service.previsualizar(r.getId(), huesped);

        assertThat(vista.cancelable()).isTrue();
        assertThat(vista.politicaNombre()).isEqualTo("FLEXIBLE");
        assertThat(vista.porcentajeDevolucion()).isEqualByComparingTo("100");
        assertThat(vista.montoDevolucion()).isEqualByComparingTo("200000");
        assertThat(vista.montoRetenido()).isEqualByComparingTo("0");
    }

    @Test
    void sin_anticipacion_no_hay_devolucion() {
        Reserva r = reserva(EstadoReserva.PENDIENTE_PAGO, LocalDate.now());

        CancelacionVista vista = service.previsualizar(r.getId(), huesped);

        assertThat(vista.cancelable()).isTrue();
        assertThat(vista.porcentajeDevolucion()).isEqualByComparingTo("0");
        assertThat(vista.montoDevolucion()).isEqualByComparingTo("0");
        assertThat(vista.montoRetenido()).isEqualByComparingTo("200000");
    }

    @Test
    void una_reserva_ya_cancelada_no_es_cancelable() {
        Reserva r = reserva(EstadoReserva.CANCELADA, LocalDate.now().plusDays(10));

        CancelacionVista vista = service.previsualizar(r.getId(), huesped);

        assertThat(vista.cancelable()).isFalse();
        assertThat(vista.montoDevolucion()).isEqualByComparingTo("0");
    }

    @Test
    void cancelar_con_devolucion_reembolsa_y_audita() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(10));
        when(reembolsos.estadoPago(r.getId())).thenReturn("REEMBOLSADO");

        ResultadoCancelacion resultado = service.cancelar(r.getId(), huesped);

        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(resultado.estadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(resultado.estadoPago()).isEqualTo("REEMBOLSADO");
        assertThat(resultado.montoDevolucion()).isEqualByComparingTo("200000");
        verify(reembolsos).reembolsar(r.getId());
        verify(auditoria).registrar(any());
    }

    @Test
    void cancelar_sin_devolucion_no_toca_el_pago() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, LocalDate.now());

        ResultadoCancelacion resultado = service.cancelar(r.getId(), huesped);

        assertThat(resultado.montoDevolucion()).isEqualByComparingTo("0");
        verify(reembolsos, never()).reembolsar(any());
    }

    @Test
    void no_se_puede_cancelar_una_reserva_en_estado_final() {
        Reserva r = reserva(EstadoReserva.COMPLETADA, LocalDate.now().plusDays(10));

        Throwable t = catchThrowable(() -> service.cancelar(r.getId(), huesped));

        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        assertThat(((ExcepcionNegocio) t).getCodigo()).isEqualTo("ESTADO_RESERVA_INVALIDO");
        verify(reservas, never()).saveAndFlush(any(Reserva.class));
    }

    @Test
    void una_reserva_ajena_no_se_encuentra() {
        Throwable t = catchThrowable(() -> service.cancelar(UUID.randomUUID(), huesped));

        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        assertThat(((ExcepcionNegocio) t).getCodigo()).isEqualTo("RESERVA_NO_ENCONTRADA");
    }
}
