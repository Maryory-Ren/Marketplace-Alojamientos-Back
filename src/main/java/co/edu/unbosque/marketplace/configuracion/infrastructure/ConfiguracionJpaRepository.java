package co.edu.unbosque.marketplace.configuracion.infrastructure;
import co.edu.unbosque.marketplace.configuracion.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
interface ConfiguracionJpaRepository extends JpaRepository<ConfiguracionMarketplace,String>, ConfiguracionRepository {}
