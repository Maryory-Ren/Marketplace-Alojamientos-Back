package co.edu.unbosque.marketplace.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import co.edu.unbosque.marketplace.outbox.domain.OutboxEvent;
import co.edu.unbosque.marketplace.outbox.domain.OutboxRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class OutboxServiceTest {

    private final OutboxRepository repository = mock(OutboxRepository.class);
    private final OutboxService service = new OutboxService(repository, JsonMapper.builder().build());

    @Test
    void registra_el_evento_con_el_payload_serializado_como_json() {
        service.registrar("RESERVA", "abc", "RESERVA_CREADA", Map.of("reservaId", "r-1"));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent evento = captor.getValue();
        assertThat(evento.getAgregado()).isEqualTo("RESERVA");
        assertThat(evento.getAgregadoId()).isEqualTo("abc");
        assertThat(evento.getTipoEvento()).isEqualTo("RESERVA_CREADA");
        assertThat(evento.getPayload()).isEqualTo("{\"reservaId\":\"r-1\"}");
        assertThat(evento.getEstado()).isEqualTo("PENDIENTE");
        assertThat(evento.getIntentos()).isZero();
    }
}
