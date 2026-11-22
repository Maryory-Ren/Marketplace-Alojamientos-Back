package co.edu.unbosque.marketplace.resenas.domain;
import java.util.List; import java.util.Optional; import java.util.UUID;
public interface ResenaRepository { Optional<Resena> findByReservaId(UUID reservaId); List<Resena> findByPublicacionIdOrderByFechaCreacionDesc(UUID publicacionId); <S extends Resena> S saveAndFlush(S resena); }
