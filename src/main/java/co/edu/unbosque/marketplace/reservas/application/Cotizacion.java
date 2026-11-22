package co.edu.unbosque.marketplace.reservas.application;
import java.math.BigDecimal;
public record Cotizacion(long noches, BigDecimal precioNoche, BigDecimal subtotal, BigDecimal tarifaLimpieza,
        BigDecimal tarifaServicio, BigDecimal porcentajeTarifaServicio, BigDecimal total, String moneda) {}
