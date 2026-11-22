package co.edu.unbosque.marketplace.configuracion.domain;
import java.util.Optional;
public interface ConfiguracionRepository { Optional<ConfiguracionMarketplace> findById(String clave); <S extends ConfiguracionMarketplace> S save(S config); }
