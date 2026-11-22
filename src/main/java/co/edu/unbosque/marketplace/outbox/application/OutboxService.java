package co.edu.unbosque.marketplace.outbox.application;

import co.edu.unbosque.marketplace.outbox.domain.OutboxEvent;
import co.edu.unbosque.marketplace.outbox.domain.OutboxRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Registra eventos en la tabla outbox dentro de la transacción del llamador.
 * Usa el ObjectMapper de Jackson 3, el que Spring Boot 4 expone como bean.
 */
@Service
public class OutboxService {

    private final OutboxRepository repository;
    private final ObjectMapper mapper;

    public OutboxService(OutboxRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public void registrar(String agregado, String id, String tipo, Object payload) {
        repository.save(new OutboxEvent(agregado, id, tipo, mapper.writeValueAsString(payload)));
    }
}
