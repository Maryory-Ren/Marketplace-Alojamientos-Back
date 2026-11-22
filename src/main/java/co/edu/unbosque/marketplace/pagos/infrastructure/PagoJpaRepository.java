package co.edu.unbosque.marketplace.pagos.infrastructure;
import co.edu.unbosque.marketplace.pagos.domain.*; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
interface PagoJpaRepository extends JpaRepository<Pago,UUID>,PagoRepository {}
