package co.edu.unbosque.marketplace.resenas.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID; import lombok.AccessLevel; import lombok.Getter; import lombok.NoArgsConstructor;
@Entity @Table(name="resena") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Resena {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="reserva_id",nullable=false,unique=true) private UUID reservaId;
 @Column(name="publicacion_id",nullable=false) private UUID publicacionId;
 @Column(name="huesped_id",nullable=false) private UUID huespedId;
 @Column(nullable=false) private int calificacion;
 @Column(nullable=false,length=1000) private String comentario;
 @Column(name="fecha_creacion",nullable=false,insertable=false,updatable=false) private Instant fechaCreacion;
 public Resena(UUID reservaId,UUID publicacionId,UUID huespedId,int calificacion,String comentario){this.reservaId=reservaId;this.publicacionId=publicacionId;this.huespedId=huespedId;this.calificacion=calificacion;this.comentario=comentario;}
}
