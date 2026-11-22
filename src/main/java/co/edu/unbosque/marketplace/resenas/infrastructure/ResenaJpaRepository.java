package co.edu.unbosque.marketplace.resenas.infrastructure;
import co.edu.unbosque.marketplace.resenas.domain.*; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
interface ResenaJpaRepository extends JpaRepository<Resena,UUID>,ResenaRepository {}
