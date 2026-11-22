package co.edu.unbosque.marketplace.publicaciones.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.compartido.error.ExcepcionNegocio;
import co.edu.unbosque.marketplace.publicaciones.domain.EstadoPublicacion;
import co.edu.unbosque.marketplace.publicaciones.domain.ImagenPublicacion;
import co.edu.unbosque.marketplace.publicaciones.domain.ModoReserva;
import co.edu.unbosque.marketplace.publicaciones.domain.PeriodoDisponible;
import co.edu.unbosque.marketplace.publicaciones.domain.PeriodoDisponibleRepository;
import co.edu.unbosque.marketplace.publicaciones.domain.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.publicaciones.domain.Publicacion;
import co.edu.unbosque.marketplace.publicaciones.domain.PublicacionRepository;
import co.edu.unbosque.marketplace.publicaciones.domain.ReservaResumen;
import co.edu.unbosque.marketplace.publicaciones.domain.ReservasActivasPort;
import co.edu.unbosque.marketplace.publicaciones.domain.ServicioRepository;
import co.edu.unbosque.marketplace.publicaciones.domain.TipoAlojamiento;
import co.edu.unbosque.marketplace.usuarios.domain.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlojamientoPublicoServiceTest {

    private final PublicacionRepository publicaciones = mock(PublicacionRepository.class);
    private final PeriodoDisponibleRepository periodos = mock(PeriodoDisponibleRepository.class);
    private final ReservasActivasPort reservas = mock(ReservasActivasPort.class);
    private final ServicioRepository servicios = mock(ServicioRepository.class);
    private final PoliticaCancelacionRepository politicas = mock(PoliticaCancelacionRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AlojamientoPublicoService service = new AlojamientoPublicoService(publicaciones, periodos,
            reservas, servicios, politicas, usuarios);

    private Publicacion publicacion(String ciudad, TipoAlojamiento tipo, int capacidad, String precio) {
        Publicacion p = mock(Publicacion.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getTitulo()).thenReturn("Alojamiento en " + ciudad);
        when(p.getCiudad()).thenReturn(ciudad);
        when(p.getTipo()).thenReturn(tipo);
        when(p.getCapacidad()).thenReturn(capacidad);
        when(p.getPrecioNoche()).thenReturn(new BigDecimal(precio));
        when(p.getModoReserva()).thenReturn(ModoReserva.INMEDIATA);
        when(p.getEstado()).thenReturn(EstadoPublicacion.ACTIVA);
        when(p.getImagenes()).thenReturn(List.of(new ImagenPublicacion("portada.jpg", 0)));
        return p;
    }

    private void activas(Publicacion... lista) {
        when(publicaciones.findByEstadoOrderByFechaActualizacionDesc(EstadoPublicacion.ACTIVA))
                .thenReturn(List.of(lista));
    }

    private ResultadoBusquedaAlojamientos buscarTodo() {
        return service.buscar(null, null, null, null, null, null, null, null, 0, 10);
    }

    private static String codigo(Throwable t) {
        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        return ((ExcepcionNegocio) t).getCodigo();
    }

    @Test
    void lista_los_alojamientos_activos_con_su_portada() {
        activas(publicacion("Medellín", TipoAlojamiento.CASA, 4, "100000"));

        ResultadoBusquedaAlojamientos r = buscarTodo();

        assertThat(r.total()).isEqualTo(1);
        assertThat(r.totalPaginas()).isEqualTo(1);
        assertThat(r.contenido().get(0).imagenPortada()).isEqualTo("portada.jpg");
    }

    @Test
    void la_ciudad_se_busca_sin_distinguir_tildes_ni_mayusculas() {
        activas(publicacion("Medellín", TipoAlojamiento.CASA, 4, "100000"),
                publicacion("Bogotá", TipoAlojamiento.APARTAMENTO, 2, "80000"));

        ResultadoBusquedaAlojamientos r = service.buscar("MEDELLIN", null, null, null, null, null, null, null, 0, 10);

        assertThat(r.contenido()).hasSize(1);
        assertThat(r.contenido().get(0).ciudad()).isEqualTo("Medellín");
    }

    @Test
    void aplica_filtros_de_tipo_huespedes_y_precio() {
        activas(publicacion("Cali", TipoAlojamiento.CASA, 6, "200000"),
                publicacion("Cali", TipoAlojamiento.APARTAMENTO, 2, "90000"),
                publicacion("Cali", TipoAlojamiento.CASA, 2, "50000"));

        assertThat(service.buscar(null, TipoAlojamiento.CASA, null, null, null, null, null, null, 0, 10).total())
                .isEqualTo(2);
        assertThat(service.buscar(null, null, 4, null, null, null, null, null, 0, 10).total()).isEqualTo(1);
        assertThat(service.buscar(null, null, null, new BigDecimal("60000"), new BigDecimal("150000"),
                null, null, null, 0, 10).total()).isEqualTo(1);
    }

    @Test
    void filtra_por_servicios_requeridos() {
        UUID wifi = UUID.randomUUID();
        Publicacion conWifi = publicacion("Cali", TipoAlojamiento.CASA, 4, "100000");
        when(conWifi.getServiciosIds()).thenReturn(java.util.Set.of(wifi));
        Publicacion sinWifi = publicacion("Cali", TipoAlojamiento.CASA, 4, "100000");
        activas(conWifi, sinWifi);

        ResultadoBusquedaAlojamientos r = service.buscar(null, null, null, null, null, null, null,
                List.of(wifi), 0, 10);

        assertThat(r.contenido()).hasSize(1);
    }

    @Test
    void pagina_los_resultados() {
        activas(publicacion("A", TipoAlojamiento.CASA, 2, "1"), publicacion("B", TipoAlojamiento.CASA, 2, "1"),
                publicacion("C", TipoAlojamiento.CASA, 2, "1"));

        ResultadoBusquedaAlojamientos r = service.buscar(null, null, null, null, null, null, null, null, 1, 2);

        assertThat(r.total()).isEqualTo(3);
        assertThat(r.totalPaginas()).isEqualTo(2);
        assertThat(r.contenido()).hasSize(1);
        assertThat(service.buscar(null, null, null, null, null, null, null, null, 5, 2).contenido()).isEmpty();
    }

    @Test
    void filtra_por_disponibilidad_en_fechas() {
        Publicacion libre = publicacion("Cali", TipoAlojamiento.CASA, 4, "100000");
        Publicacion ocupada = publicacion("Cali", TipoAlojamiento.CASA, 4, "100000");
        Publicacion sinPeriodo = publicacion("Cali", TipoAlojamiento.CASA, 4, "100000");
        activas(libre, ocupada, sinPeriodo);
        LocalDate llegada = LocalDate.now().plusDays(3);
        LocalDate salida = llegada.plusDays(2);
        PeriodoDisponible periodo = mock(PeriodoDisponible.class);
        when(periodo.getFechaInicio()).thenReturn(LocalDate.now());
        when(periodo.getFechaFin()).thenReturn(LocalDate.now().plusDays(30));
        when(periodos.findByPublicacionIdOrderByFechaInicio(libre.getId())).thenReturn(List.of(periodo));
        when(periodos.findByPublicacionIdOrderByFechaInicio(ocupada.getId())).thenReturn(List.of(periodo));
        when(reservas.reservasActivasEnRango(ocupada.getId(), llegada, salida.minusDays(1))).thenReturn(List.of(
                new ReservaResumen(UUID.randomUUID(), llegada, salida, 2, "CONFIRMADA")));

        ResultadoBusquedaAlojamientos r = service.buscar(null, null, null, null, null, llegada, salida, null, 0, 10);

        assertThat(r.contenido()).hasSize(1);
        assertThat(r.contenido().get(0).id()).isEqualTo(libre.getId());
    }

    @Test
    void valida_los_parametros_de_busqueda() {
        LocalDate hoy = LocalDate.now();
        assertThat(codigo(catchThrowable(() -> service.buscar(null, null, null, null, null, hoy.plusDays(2), null,
                null, 0, 10)))).isEqualTo("FECHAS_INVALIDAS");
        assertThat(codigo(catchThrowable(() -> service.buscar(null, null, null, null, null, hoy.plusDays(3),
                hoy.plusDays(2), null, 0, 10)))).isEqualTo("FECHAS_INVALIDAS");
        assertThat(codigo(catchThrowable(() -> service.buscar(null, null, null, null, null, null, null, null, -1,
                10)))).isEqualTo("PAGINACION_INVALIDA");
        assertThat(codigo(catchThrowable(() -> service.buscar(null, null, null, null, null, null, null, null, 0,
                51)))).isEqualTo("PAGINACION_INVALIDA");
        assertThat(codigo(catchThrowable(() -> service.buscar(null, null, 0, null, null, null, null, null, 0,
                10)))).isEqualTo("FILTROS_INVALIDOS");
        assertThat(codigo(catchThrowable(() -> service.buscar(null, null, null, new BigDecimal("-1"), null, null,
                null, null, 0, 10)))).isEqualTo("FILTROS_INVALIDOS");
        assertThat(codigo(catchThrowable(() -> service.buscar(null, null, null, new BigDecimal("10"),
                new BigDecimal("5"), null, null, null, 0, 10)))).isEqualTo("RANGO_PRECIO_INVALIDO");
    }

    @Test
    void detalle_de_un_alojamiento_activo() {
        Publicacion p = publicacion("Cali", TipoAlojamiento.CASA, 4, "100000");
        when(publicaciones.findById(p.getId())).thenReturn(Optional.of(p));

        AlojamientoDetallePublico d = service.detalle(p.getId());

        assertThat(d.id()).isEqualTo(p.getId());
        assertThat(d.ciudad()).isEqualTo("Cali");
        assertThat(d.imagenes()).containsExactly("portada.jpg");
        assertThat(d.politicaCancelacion()).isNull();
        assertThat(d.anfitrionNombre()).isEmpty();
    }

    @Test
    void detalle_de_un_alojamiento_inactivo_o_inexistente_no_se_encuentra() {
        Publicacion pausada = publicacion("Cali", TipoAlojamiento.CASA, 4, "100000");
        when(pausada.getEstado()).thenReturn(EstadoPublicacion.PAUSADA);
        when(publicaciones.findById(pausada.getId())).thenReturn(Optional.of(pausada));

        assertThat(codigo(catchThrowable(() -> service.detalle(pausada.getId()))))
                .isEqualTo("ALOJAMIENTO_NO_ENCONTRADO");
        assertThat(codigo(catchThrowable(() -> service.detalle(UUID.randomUUID()))))
                .isEqualTo("ALOJAMIENTO_NO_ENCONTRADO");
    }

    @Test
    void calendario_marca_los_dias_ocupados_como_no_disponibles() {
        Publicacion p = publicacion("Cali", TipoAlojamiento.CASA, 4, "100000");
        when(publicaciones.findById(p.getId())).thenReturn(Optional.of(p));
        LocalDate desde = LocalDate.now().plusDays(1);
        LocalDate hasta = desde.plusDays(2);
        PeriodoDisponible periodo = mock(PeriodoDisponible.class);
        when(periodo.getFechaInicio()).thenReturn(desde);
        when(periodo.getFechaFin()).thenReturn(hasta);
        when(periodos.findByPublicacionIdOrderByFechaInicio(p.getId())).thenReturn(List.of(periodo));
        when(reservas.reservasActivasEnRango(any(), any(), any())).thenReturn(List.of(
                new ReservaResumen(UUID.randomUUID(), desde.plusDays(1), desde.plusDays(2), 2, "CONFIRMADA")));

        List<AlojamientoPublicoService.DiaCalendario> dias = service.calendario(p.getId(), desde, hasta);

        assertThat(dias).hasSize(3);
        assertThat(dias.get(0).disponible()).isTrue();
        assertThat(dias.get(1).disponible()).isFalse();
        assertThat(dias.get(2).disponible()).isTrue();
    }

    @Test
    void calendario_limita_el_rango_a_un_anio() {
        Publicacion p = publicacion("Cali", TipoAlojamiento.CASA, 4, "100000");
        when(publicaciones.findById(p.getId())).thenReturn(Optional.of(p));
        LocalDate desde = LocalDate.now().plusDays(1);

        assertThat(codigo(catchThrowable(() -> service.calendario(p.getId(), desde, desde.plusDays(400)))))
                .isEqualTo("RANGO_EXCESIVO");
    }
}
