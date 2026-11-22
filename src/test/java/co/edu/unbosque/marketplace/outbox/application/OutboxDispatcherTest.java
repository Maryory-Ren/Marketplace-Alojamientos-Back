package co.edu.unbosque.marketplace.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.outbox.domain.OutboxEvent;
import co.edu.unbosque.marketplace.outbox.domain.OutboxRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class OutboxDispatcherTest {

    private final OutboxRepository repository = mock(OutboxRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final OutboxDispatcher dispatcher = new OutboxDispatcher(repository, publisher);

    @Test
    void publica_los_eventos_pendientes_y_los_marca_como_publicados() {
        OutboxEvent evento = new OutboxEvent("PAGO", "1", "PAGO_PROCESADO", "{}");
        when(repository.findTop100ByEstadoOrderByCreadoEnAsc("PENDIENTE")).thenReturn(List.of(evento));

        dispatcher.despachar();

        assertThat(evento.getEstado()).isEqualTo("PUBLICADO");
        assertThat(evento.getPublicadoEn()).isNotNull();
        verify(publisher).publishEvent(any(OutboxPublicadoEvent.class));
    }

    @Test
    void si_la_publicacion_falla_cuenta_el_intento_y_deja_el_evento_pendiente() {
        OutboxEvent evento = new OutboxEvent("PAGO", "1", "PAGO_PROCESADO", "{}");
        when(repository.findTop100ByEstadoOrderByCreadoEnAsc("PENDIENTE")).thenReturn(List.of(evento));
        doThrow(new IllegalStateException("fallo del listener")).when(publisher).publishEvent(any(Object.class));

        dispatcher.despachar();

        assertThat(evento.getEstado()).isEqualTo("PENDIENTE");
        assertThat(evento.getIntentos()).isEqualTo(1);
        assertThat(evento.getUltimoError()).isEqualTo("fallo del listener");
    }
}
