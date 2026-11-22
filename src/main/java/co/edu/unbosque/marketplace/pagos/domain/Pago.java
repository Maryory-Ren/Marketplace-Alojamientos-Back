package co.edu.unbosque.marketplace.pagos.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel; import lombok.Getter; import lombok.NoArgsConstructor;
@Entity @Table(name="pago") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Pago {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="reserva_id",nullable=false) private UUID reservaId;
 @Column(name="huesped_id",nullable=false) private UUID huespedId;
 @Column(name="clave_idempotencia",nullable=false,length=80) private String claveIdempotencia;
 @Column(name="huella_solicitud",nullable=false,length=64) private String huellaSolicitud;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EstadoPago estado;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal total;
 @Column(name="fecha_procesado",nullable=false,insertable=false,updatable=false) private Instant procesadoEn;
 public void reembolsar(){this.estado=EstadoPago.REEMBOLSADO;}
 public Pago(UUID reservaId,UUID huespedId,String clave,String huella,EstadoPago estado,BigDecimal total) {
  this.reservaId=reservaId;this.huespedId=huespedId;this.claveIdempotencia=clave;this.huellaSolicitud=huella;this.estado=estado;this.total=total;
 }
}
