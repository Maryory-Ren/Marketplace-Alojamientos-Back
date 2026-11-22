package co.edu.unbosque.marketplace.configuracion.presentation;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record TarifaServicioRequest(@NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal porcentaje) {}
