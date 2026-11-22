package co.edu.unbosque.marketplace.outbox.application;
import co.edu.unbosque.marketplace.outbox.domain.*; import org.springframework.context.ApplicationEventPublisher; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;
@Component public class OutboxDispatcher {
 private final OutboxRepository repository; private final ApplicationEventPublisher publisher;
 public OutboxDispatcher(OutboxRepository repository,ApplicationEventPublisher publisher){this.repository=repository;this.publisher=publisher;}
 @Scheduled(fixedDelayString="${app.outbox.dispatch-delay-ms:2000}") @Transactional
 public void despachar(){for(OutboxEvent e:repository.findTop100ByEstadoOrderByCreadoEnAsc("PENDIENTE")){try{publisher.publishEvent(new OutboxPublicadoEvent(e.getId(),e.getAgregado(),e.getAgregadoId(),e.getTipoEvento(),e.getPayload()));e.publicado();}catch(RuntimeException ex){e.fallo(ex.getMessage());}}}
}
