package co.edu.unbosque.marketplace.compartido.error;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unbosque.marketplace.compartido.CorrelationId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationIdTest {

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void sin_valor_en_mdc_actual_es_nulo_y_se_genera_uno_nuevo() {
        assertThat(CorrelationId.actual()).isNull();
        assertThat(CorrelationId.actualONuevo()).isNotNull();
    }

    @Test
    void reutiliza_el_identificador_valido_del_mdc() {
        UUID id = UUID.randomUUID();
        MDC.put(CorrelationId.CLAVE_MDC, id.toString());

        assertThat(CorrelationId.actual()).isEqualTo(id.toString());
        assertThat(CorrelationId.actualONuevo()).isEqualTo(id);
    }

    @Test
    void si_el_identificador_no_es_uuid_genera_uno_nuevo() {
        MDC.put(CorrelationId.CLAVE_MDC, "no-es-uuid");

        assertThat(CorrelationId.actualONuevo()).isNotNull();
    }
}
