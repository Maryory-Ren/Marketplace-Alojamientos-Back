package co.edu.unbosque.marketplace.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unbosque.marketplace.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    private OutboxEvent nuevo() {
        return new OutboxEvent("RESERVA", "1", "RESERVA_CREADA", "{}");
    }

    @Test
    void nace_pendiente_y_sin_intentos() {
        OutboxEvent e = nuevo();
        assertThat(e.getEstado()).isEqualTo("PENDIENTE");
        assertThat(e.getIntentos()).isZero();
        assertThat(e.getPublicadoEn()).isNull();
    }

    @Test
    void publicado_limpia_el_ultimo_error() {
        OutboxEvent e = nuevo();
        e.fallo("error previo");

        e.publicado();

        assertThat(e.getEstado()).isEqualTo("PUBLICADO");
        assertThat(e.getUltimoError()).isNull();
        assertThat(e.getPublicadoEn()).isNotNull();
    }

    @Test
    void fallo_sin_mensaje_guarda_error_desconocido() {
        OutboxEvent e = nuevo();
        e.fallo(null);
        assertThat(e.getUltimoError()).isEqualTo("Error desconocido");
    }

    @Test
    void fallo_recorta_mensajes_largos_a_1000_caracteres() {
        OutboxEvent e = nuevo();
        e.fallo("x".repeat(1500));
        assertThat(e.getUltimoError()).hasSize(1000);
    }

    @Test
    void tras_diez_intentos_fallidos_pasa_a_error() {
        OutboxEvent e = nuevo();
        for (int i = 0; i < 9; i++) {
            e.fallo("boom");
        }
        assertThat(e.getEstado()).isEqualTo("PENDIENTE");

        e.fallo("boom");

        assertThat(e.getIntentos()).isEqualTo(10);
        assertThat(e.getEstado()).isEqualTo("ERROR");
    }
}
