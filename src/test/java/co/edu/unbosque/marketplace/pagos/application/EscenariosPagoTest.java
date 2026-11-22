package co.edu.unbosque.marketplace.pagos.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EscenariosPagoTest {

    @Test
    void cada_estrategia_declara_su_escenario() {
        assertThat(new EstrategiaAprobarPago().escenario()).isEqualTo(EscenarioPago.APROBAR);
        assertThat(new EstrategiaRechazarPago().escenario()).isEqualTo(EscenarioPago.RECHAZAR);
        assertThat(new EstrategiaDemorarPago().escenario()).isEqualTo(EscenarioPago.DEMORAR);
    }

    @Test
    void el_escenario_caer_no_tiene_estrategia_propia() {
        assertThat(EscenarioPago.values()).contains(EscenarioPago.CAER);
        assertThat(EscenarioPago.valueOf("CAER")).isEqualTo(EscenarioPago.CAER);
    }
}
