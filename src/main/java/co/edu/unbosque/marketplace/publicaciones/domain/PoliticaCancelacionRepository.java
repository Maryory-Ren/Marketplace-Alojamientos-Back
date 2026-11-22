package co.edu.unbosque.marketplace.publicaciones.domain;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PoliticaCancelacionRepository {
    List<PoliticaCancelacion> findAll();
    boolean existsById(UUID id);
    Optional<PoliticaCancelacion> findById(UUID id);
}
