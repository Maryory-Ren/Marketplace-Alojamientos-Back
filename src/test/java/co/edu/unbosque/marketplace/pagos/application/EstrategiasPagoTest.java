package co.edu.unbosque.marketplace.pagos.application;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EstrategiasPagoTest {
    @Test void aprobar_confirma_reserva_y_pago() {
        var resultado=new EstrategiaAprobarPago().procesar();
        assertEquals("APROBADO",resultado.estadoPago().name());
        assertEquals("CONFIRMADA",resultado.estadoReserva().name());
        assertEquals(200,resultado.httpStatus());
    }
    @Test void rechazar_libera_reserva() {
        var resultado=new EstrategiaRechazarPago().procesar();
        assertEquals("FALLIDO",resultado.estadoPago().name());
        assertEquals("PAGO_FALLIDO",resultado.estadoReserva().name());
        assertEquals(402,resultado.httpStatus());
    }
    @Test void demora_deja_pago_en_revision() {
        var resultado=new EstrategiaDemorarPago().procesar();
        assertEquals("PAGO_INCIERTO",resultado.estadoPago().name());
        assertEquals("EN_REVISION",resultado.estadoReserva().name());
        assertEquals(202,resultado.httpStatus());
    }
}
