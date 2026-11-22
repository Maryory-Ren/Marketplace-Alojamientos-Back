package co.edu.unbosque.marketplace.publicaciones.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.compartido.error.ExcepcionNegocio;
import co.edu.unbosque.marketplace.publicaciones.domain.PeriodoDisponible;
import co.edu.unbosque.marketplace.publicaciones.domain.PeriodoDisponibleRepository;
import co.edu.unbosque.marketplace.publicaciones.domain.Publicacion;
import co.edu.unbosque.marketplace.publicaciones.domain.PublicacionRepository;
import co.edu.unbosque.marketplace.publicaciones.domain.ReservaResumen;
import co.edu.unbosque.marketplace.publicaciones.domain.ReservasActivasPort;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import co.edu.unbosque.marketplace.trazabilidad.AuditoriaPort;
import co.edu.unbosque.marketplace.trazabilidad.EventoAuditoria;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class PeriodoDisponibleServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final PublicacionRepository publicaciones = mock(PublicacionRepository.class);
    private final PeriodoDisponibleRepository periodos = mock(PeriodoDisponibleRepository.class);
    private final ReservasActivasPort reservas = mock(ReservasActivasPort.class);
    private final AuditoriaPort auditoria = mock(AuditoriaPort.class);
    private final PeriodoDisponibleService service =
            new PeriodoDisponibleService(publicaciones, periodos, reservas, auditoria);

    private final UUID publicacionId = UUID.randomUUID();
    private final UsuarioAutenticado anfitrion = new UsuarioAutenticado(UUID.randomUUID(), "ANFITRION");
    private final LocalDate hoy = LocalDate.now(ZONA);

    @BeforeEach
    void preparar() {
        Publicacion p = mock(Publicacion.class);
        when(p.getAnfitrionId()).thenReturn(anfitrion.id());
        when(publicaciones.findById(publicacionId)).thenReturn(Optional.of(p));
        when(periodos.saveAndFlush(any(PeriodoDisponible.class))).thenAnswer(inv -> {
            PeriodoDisponible nuevo = inv.getArgument(0);
            if (nuevo.getId() == null) {
                ReflectionTestUtils.setField(nuevo, "id", UUID.randomUUID());
            }
            return nuevo;
        });
    }

    private PeriodoDisponible existente(LocalDate inicio, LocalDate fin) {
        PeriodoDisponible p = new PeriodoDisponible(publicacionId, inicio, fin);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        when(periodos.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private static String codigo(Throwable t) {
        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        return ((ExcepcionNegocio) t).getCodigo();
    }

    @Test
    void lista_los_periodos_del_anfitrion() {
        PeriodoDisponible p = existente(hoy.plusDays(1), hoy.plusDays(5));
        when(periodos.findByPublicacionIdOrderByFechaInicio(publicacionId)).thenReturn(List.of(p));

        assertThat(service.listar(publicacionId, anfitrion)).containsExactly(p);
    }

    @Test
    void registra_un_periodo_sin_cruce() {
        PeriodoDisponible nuevo = service.registrar(publicacionId, hoy.plusDays(2), hoy.plusDays(9), anfitrion);

        assertThat(nuevo.getFechaInicio()).isEqualTo(hoy.plusDays(2));
        assertThat(nuevo.getFechaFin()).isEqualTo(hoy.plusDays(9));
        verify(auditoria).registrar(any(EventoAuditoria.class));
    }

    @Test
    void un_periodo_que_se_cruza_es_conflicto() {
        when(periodos.existeCruce(publicacionId, hoy.plusDays(2), hoy.plusDays(9))).thenReturn(true);

        assertThat(codigo(catchThrowable(() -> service.registrar(publicacionId, hoy.plusDays(2), hoy.plusDays(9),
                anfitrion)))).isEqualTo("PERIODO_CRUZADO");
    }

    @Test
    void la_restriccion_de_la_base_tambien_se_reporta_como_cruce() {
        doThrow(new DataIntegrityViolationException("x", new RuntimeException("viola ex_periodo_sin_cruce")))
                .when(periodos).saveAndFlush(any(PeriodoDisponible.class));

        assertThat(codigo(catchThrowable(() -> service.registrar(publicacionId, hoy.plusDays(2), hoy.plusDays(9),
                anfitrion)))).isEqualTo("PERIODO_CRUZADO");
    }

    @Test
    void otros_errores_de_integridad_se_propagan() {
        doThrow(new DataIntegrityViolationException("x", new RuntimeException("otra restriccion")))
                .when(periodos).saveAndFlush(any(PeriodoDisponible.class));

        Throwable t = catchThrowable(() -> service.registrar(publicacionId, hoy.plusDays(2), hoy.plusDays(9),
                anfitrion));

        assertThat(t).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void valida_las_fechas_del_periodo() {
        assertThat(codigo(catchThrowable(() -> service.registrar(publicacionId, hoy.plusDays(5), hoy.plusDays(2),
                anfitrion)))).isEqualTo("FECHAS_INVALIDAS");
        assertThat(codigo(catchThrowable(() -> service.registrar(publicacionId, hoy.minusDays(1), hoy.plusDays(2),
                anfitrion)))).isEqualTo("PERIODO_EN_EL_PASADO");
    }

    @Test
    void solo_el_dueno_gestiona_los_periodos_y_el_intento_se_audita() {
        UsuarioAutenticado intruso = new UsuarioAutenticado(UUID.randomUUID(), "ANFITRION");

        assertThat(codigo(catchThrowable(() -> service.listar(publicacionId, intruso)))).isEqualTo("ACCESO_DENEGADO");
        verify(auditoria).registrar(any(EventoAuditoria.class));
    }

    @Test
    void una_publicacion_inexistente_no_se_encuentra() {
        assertThat(codigo(catchThrowable(() -> service.listar(UUID.randomUUID(), anfitrion))))
                .isEqualTo("PUBLICACION_NO_ENCONTRADA");
    }

    @Test
    void edita_un_periodo_sin_reservas_afectadas() {
        PeriodoDisponible p = existente(hoy.plusDays(2), hoy.plusDays(10));

        PeriodoDisponible editado = service.editar(publicacionId, p.getId(), hoy.plusDays(3), hoy.plusDays(12),
                anfitrion);

        assertThat(editado.getFechaInicio()).isEqualTo(hoy.plusDays(3));
        assertThat(editado.getFechaFin()).isEqualTo(hoy.plusDays(12));
    }

    @Test
    void no_edita_si_el_nuevo_rango_deja_reservas_fuera() {
        PeriodoDisponible p = existente(hoy.plusDays(2), hoy.plusDays(10));
        when(reservas.reservasActivasEnRango(publicacionId, p.getFechaInicio(), p.getFechaFin())).thenReturn(List.of(
                new ReservaResumen(UUID.randomUUID(), hoy.plusDays(2), hoy.plusDays(4), 2, "CONFIRMADA")));

        Throwable t = catchThrowable(() -> service.editar(publicacionId, p.getId(), hoy.plusDays(6), hoy.plusDays(10),
                anfitrion));

        assertThat(codigo(t)).isEqualTo("PERIODO_CON_RESERVAS");
        assertThat(((ExcepcionNegocio) t).getDetalles()).isNotNull();
    }

    @Test
    void no_edita_si_el_nuevo_rango_se_cruza_con_otro_periodo() {
        PeriodoDisponible p = existente(hoy.plusDays(2), hoy.plusDays(10));
        when(periodos.existeCruceExcluyendo(publicacionId, hoy.plusDays(3), hoy.plusDays(12), p.getId()))
                .thenReturn(true);

        assertThat(codigo(catchThrowable(() -> service.editar(publicacionId, p.getId(), hoy.plusDays(3),
                hoy.plusDays(12), anfitrion)))).isEqualTo("PERIODO_CRUZADO");
    }

    @Test
    void editar_un_periodo_ajeno_o_inexistente_no_se_encuentra() {
        assertThat(codigo(catchThrowable(() -> service.editar(publicacionId, UUID.randomUUID(), hoy.plusDays(3),
                hoy.plusDays(4), anfitrion)))).isEqualTo("PERIODO_NO_ENCONTRADO");

        PeriodoDisponible deOtra = new PeriodoDisponible(UUID.randomUUID(), hoy.plusDays(3), hoy.plusDays(4));
        ReflectionTestUtils.setField(deOtra, "id", UUID.randomUUID());
        when(periodos.findById(deOtra.getId())).thenReturn(Optional.of(deOtra));
        assertThat(codigo(catchThrowable(() -> service.editar(publicacionId, deOtra.getId(), hoy.plusDays(3),
                hoy.plusDays(4), anfitrion)))).isEqualTo("PERIODO_NO_ENCONTRADO");
    }

    @Test
    void elimina_un_periodo_sin_reservas() {
        PeriodoDisponible p = existente(hoy.plusDays(2), hoy.plusDays(10));

        service.eliminar(publicacionId, p.getId(), anfitrion);

        verify(periodos).delete(p);
        verify(periodos).flush();
    }

    @Test
    void no_elimina_un_periodo_con_reservas_activas() {
        PeriodoDisponible p = existente(hoy.plusDays(2), hoy.plusDays(10));
        when(reservas.reservasActivasEnRango(publicacionId, p.getFechaInicio(), p.getFechaFin())).thenReturn(List.of(
                new ReservaResumen(UUID.randomUUID(), hoy.plusDays(3), hoy.plusDays(5), 2, "CONFIRMADA")));

        assertThat(codigo(catchThrowable(() -> service.eliminar(publicacionId, p.getId(), anfitrion))))
                .isEqualTo("PERIODO_CON_RESERVAS");
        verify(periodos, never()).delete(any(PeriodoDisponible.class));
    }
}
