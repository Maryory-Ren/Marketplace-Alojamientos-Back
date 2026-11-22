package co.edu.unbosque.marketplace.reservas.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reserva")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reserva {

    @Id
    private UUID id;

    @Column(name = "huesped_id", nullable = false)
    private UUID huespedId;

    @Column(name = "publicacion_id", nullable = false)
    private UUID publicacionId;

    @Column(name = "politica_cancelacion_id", nullable = false)
    private UUID politicaCancelacionId;

    @Column(name = "clave_idempotencia", nullable = false, length = 80)
    private String claveIdempotencia;

    @Column(name = "huella_solicitud", length = 64)
    private String huellaSolicitud;

    @Column(name = "fecha_llegada", nullable = false)
    private LocalDate fechaLlegada;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "numero_huespedes", nullable = false)
    private int numeroHuespedes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoReserva estado;

    @Column(name = "noches", nullable = false) private int noches;
    @Column(name = "precio_noche", nullable = false, precision=12, scale=2) private BigDecimal precioNoche;
    @Column(name = "subtotal", nullable = false, precision=12, scale=2) private BigDecimal subtotal;
    @Column(name = "tarifa_limpieza", nullable = false, precision=12, scale=2) private BigDecimal tarifaLimpieza;
    @Column(name = "tarifa_servicio", nullable = false, precision=12, scale=2) private BigDecimal tarifaServicio;
    @Column(name = "porcentaje_tarifa_servicio", nullable = false, precision=8, scale=4) private BigDecimal porcentajeTarifaServicio;
    @Column(name = "total", nullable = false, precision=12, scale=2) private BigDecimal total;

    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private Instant fechaCreacion;

    public Reserva(UUID huespedId, UUID publicacionId, UUID politicaCancelacionId, LocalDate llegada,
            LocalDate salida, int numeroHuespedes, EstadoReserva estado, String claveIdempotencia, String huellaSolicitud, int noches,
            BigDecimal precioNoche, BigDecimal subtotal, BigDecimal tarifaLimpieza, BigDecimal tarifaServicio,
            BigDecimal porcentajeTarifaServicio, BigDecimal total) {
        this.id = UUID.randomUUID(); this.huespedId=huespedId; this.publicacionId=publicacionId;
        this.politicaCancelacionId=politicaCancelacionId; this.fechaLlegada=llegada; this.fechaSalida=salida;
        this.numeroHuespedes=numeroHuespedes; this.estado=estado; this.claveIdempotencia=claveIdempotencia; this.huellaSolicitud=huellaSolicitud;
        this.noches=noches;this.precioNoche=precioNoche;this.subtotal=subtotal;this.tarifaLimpieza=tarifaLimpieza;
        this.tarifaServicio=tarifaServicio;this.porcentajeTarifaServicio=porcentajeTarifaServicio;this.total=total;
    }

    public void cambiarEstado(EstadoReserva nuevo) { this.estado=nuevo; }
}
