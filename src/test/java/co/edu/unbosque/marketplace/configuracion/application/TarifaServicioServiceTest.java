package co.edu.unbosque.marketplace.configuracion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.compartido.error.ExcepcionNegocio;
import co.edu.unbosque.marketplace.configuracion.domain.ConfiguracionMarketplace;
import co.edu.unbosque.marketplace.configuracion.domain.ConfiguracionRepository;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import co.edu.unbosque.marketplace.trazabilidad.AuditoriaPort;
import co.edu.unbosque.marketplace.trazabilidad.EventoAuditoria;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TarifaServicioServiceTest {

    private final ConfiguracionRepository configuraciones = mock(ConfiguracionRepository.class);
    private final AuditoriaPort auditoria = mock(AuditoriaPort.class);
    private final TarifaServicioService service = new TarifaServicioService(configuraciones, auditoria);
    private final UsuarioAutenticado admin = new UsuarioAutenticado(UUID.randomUUID(), "ADMINISTRADOR");

    private static String codigo(Throwable t) {
        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        return ((ExcepcionNegocio) t).getCodigo();
    }

    @Test
    void consulta_la_tarifa_configurada() {
        ConfiguracionMarketplace c = mock(ConfiguracionMarketplace.class);
        when(c.getValor()).thenReturn(new BigDecimal("8.5000"));
        when(configuraciones.findById(TarifaServicioService.CLAVE)).thenReturn(Optional.of(c));

        assertThat(service.consultar()).isEqualByComparingTo("8.5");
    }

    @Test
    void sin_configuracion_falla_con_regla_de_negocio() {
        assertThat(codigo(catchThrowable(service::consultar))).isEqualTo("CONFIGURACION_AUSENTE");
        assertThat(codigo(catchThrowable(() -> service.actualizar(BigDecimal.TEN, admin))))
                .isEqualTo("CONFIGURACION_AUSENTE");
    }

    @Test
    void actualiza_la_tarifa_y_audita() {
        ConfiguracionMarketplace c = mock(ConfiguracionMarketplace.class);
        when(configuraciones.findById(TarifaServicioService.CLAVE)).thenReturn(Optional.of(c));

        BigDecimal resultado = service.actualizar(new BigDecimal("12.5"), admin);

        assertThat(resultado).isEqualByComparingTo("12.5");
        verify(c).actualizar(new BigDecimal("12.5"));
        verify(configuraciones).save(c);
        verify(auditoria).registrar(any(EventoAuditoria.class));
    }

    @Test
    void rechaza_tarifas_fuera_de_0_a_100() {
        assertThat(codigo(catchThrowable(() -> service.actualizar(null, admin)))).isEqualTo("TARIFA_INVALIDA");
        assertThat(codigo(catchThrowable(() -> service.actualizar(new BigDecimal("-0.1"), admin))))
                .isEqualTo("TARIFA_INVALIDA");
        assertThat(codigo(catchThrowable(() -> service.actualizar(new BigDecimal("100.1"), admin))))
                .isEqualTo("TARIFA_INVALIDA");
        verify(configuraciones, never()).save(any(ConfiguracionMarketplace.class));
    }

    @Test
    void acepta_los_limites_0_y_100() {
        ConfiguracionMarketplace c = mock(ConfiguracionMarketplace.class);
        when(configuraciones.findById(TarifaServicioService.CLAVE)).thenReturn(Optional.of(c));

        assertThat(service.actualizar(BigDecimal.ZERO, admin)).isEqualByComparingTo("0");
        assertThat(service.actualizar(new BigDecimal("100"), admin)).isEqualByComparingTo("100");
    }
}
