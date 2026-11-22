package co.edu.unbosque.marketplace.pagos.presentation;

import co.edu.unbosque.marketplace.compartido.CorrelationId;
import co.edu.unbosque.marketplace.compartido.error.ApiError;
import co.edu.unbosque.marketplace.pagos.application.PagoService;
import co.edu.unbosque.marketplace.pagos.application.ResultadoPagoVista;
import co.edu.unbosque.marketplace.pagos.domain.EstadoPago;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/huesped/reservas/{reservaId}/pago")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Object> pagar(@PathVariable UUID reservaId,
            @RequestHeader("Idempotency-Key") String clave,
            @RequestBody(required = false) PagoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado huesped) {
        ResultadoPagoVista resultado = service.pagar(reservaId, clave,
                request == null ? null : request.escenarioPrueba(), huesped);
        // Cobro rechazado: el contrato exige 402 con el formato de error estándar (también en reintentos con la misma clave).
        if (resultado.estadoPago() == EstadoPago.FALLIDO) {
            return ResponseEntity.status(402).body(new ApiError("PAGO_RECHAZADO",
                    "El cobro no se aprobó o expiró. La reserva pasó a PAGO_FALLIDO y las fechas quedaron libres.",
                    null, CorrelationId.actual()));
        }
        return ResponseEntity.status(resultado.httpStatus()).body(resultado);
    }
}
