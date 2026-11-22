package co.edu.unbosque.marketplace.outbox.application;
import java.util.UUID;
public record OutboxPublicadoEvent(UUID id,String agregado,String agregadoId,String tipoEvento,String payload){}
