package co.edu.unbosque.marketplace.outbox.infrastructure;
import co.edu.unbosque.marketplace.outbox.domain.*; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
interface OutboxJpaRepository extends JpaRepository<OutboxEvent,UUID>,OutboxRepository {}
