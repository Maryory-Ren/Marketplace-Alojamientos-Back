package co.edu.unbosque.marketplace.usuarios.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenRecuperacionRepository {

    <S extends TokenRecuperacion> S save(S token);

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    int invalidarPendientes(UUID usuarioId, Instant ahora);

    int consumir(UUID id, Instant ahora);
}
