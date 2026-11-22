package co.edu.unbosque.marketplace.configuracion.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity @Table(name="configuracion_marketplace") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ConfiguracionMarketplace {
    @Id @Column(length=80) private String clave;
    @Column(nullable=false,precision=8,scale=4) private BigDecimal valor;
    @Column(name="fecha_actualizacion",nullable=false,insertable=false,updatable=false) private Instant fechaActualizacion;
    public void actualizar(BigDecimal valor) { this.valor=valor; }
}
