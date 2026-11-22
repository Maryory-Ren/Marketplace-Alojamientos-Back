package co.edu.unbosque.marketplace.reservas.application;
import java.math.BigDecimal; import java.util.UUID;
public record CancelacionVista(UUID reservaId,boolean cancelable,long horasHastaLlegada,String politicaNombre,
        BigDecimal porcentajeDevolucion,BigDecimal totalPagado,BigDecimal montoDevolucion,
        BigDecimal montoRetenido,String moneda) {}
