package co.edu.unbosque.marketplace.outbox.domain;
import java.util.List; import java.util.UUID;
public interface OutboxRepository { <S extends OutboxEvent> S save(S event); List<OutboxEvent> findTop100ByEstadoOrderByCreadoEnAsc(String estado); }
