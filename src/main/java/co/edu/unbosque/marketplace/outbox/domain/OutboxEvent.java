package co.edu.unbosque.marketplace.outbox.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes;
import lombok.AccessLevel; import lombok.Getter; import lombok.NoArgsConstructor;
@Entity @Table(name="outbox_event") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class OutboxEvent {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(nullable=false,length=80) private String agregado;
 @Column(name="agregado_id",nullable=false,length=80) private String agregadoId;
 @Column(name="tipo_evento",nullable=false,length=120) private String tipoEvento;
 @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private String payload;
 @Column(nullable=false,length=20) private String estado="PENDIENTE";
 @Column(nullable=false) private int intentos;
 @Column(name="creado_en",nullable=false,insertable=false,updatable=false) private Instant creadoEn;
 @Column(name="publicado_en") private Instant publicadoEn;
 @Column(name="ultimo_error",length=1000) private String ultimoError;
 public OutboxEvent(String agregado,String agregadoId,String tipoEvento,String payload){this.agregado=agregado;this.agregadoId=agregadoId;this.tipoEvento=tipoEvento;this.payload=payload;}
 public void publicado(){this.estado="PUBLICADO";this.publicadoEn=Instant.now();this.ultimoError=null;}
 public void fallo(String error){this.intentos++;this.ultimoError=error==null?"Error desconocido":error.substring(0,Math.min(error.length(),1000));if(intentos>=10)this.estado="ERROR";}
}
