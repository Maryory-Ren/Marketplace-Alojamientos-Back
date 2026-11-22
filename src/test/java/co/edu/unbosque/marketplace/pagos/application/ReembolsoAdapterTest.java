package co.edu.unbosque.marketplace.pagos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.pagos.domain.EstadoPago;
import co.edu.unbosque.marketplace.pagos.domain.Pago;
import co.edu.unbosque.marketplace.pagos.domain.PagoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReembolsoAdapterTest {

    private final PagoRepository pagos = mock(PagoRepository.class);
    private final ReembolsoAdapter adapter = new ReembolsoAdapter(pagos);
    private final UUID reservaId = UUID.randomUUID();

    private Pago pago(EstadoPago estado) {
        Pago p = new Pago(reservaId, UUID.randomUUID(), "k", "h", estado, new BigDecimal("150000.00"));
        when(pagos.findByReservaId(reservaId)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void total_pagado_solo_cuenta_pagos_aprobados() {
        pago(EstadoPago.APROBADO);
        assertThat(adapter.totalPagado(reservaId)).isEqualByComparingTo("150000.00");
    }

    @Test
    void total_pagado_es_cero_si_el_pago_no_esta_aprobado_o_no_existe() {
        pago(EstadoPago.PAGO_INCIERTO);
        assertThat(adapter.totalPagado(reservaId)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(adapter.totalPagado(UUID.randomUUID())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void estado_de_pago_devuelve_el_nombre_o_pendiente() {
        pago(EstadoPago.FALLIDO);
        assertThat(adapter.estadoPago(reservaId)).isEqualTo("FALLIDO");
        assertThat(adapter.estadoPago(UUID.randomUUID())).isEqualTo("PENDIENTE");
    }

    @Test
    void reembolsar_marca_el_pago_aprobado_como_reembolsado() {
        Pago p = pago(EstadoPago.APROBADO);

        adapter.reembolsar(reservaId);

        assertThat(p.getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);
        verify(pagos).saveAndFlush(p);
    }

    @Test
    void reembolsar_ignora_pagos_que_no_estan_aprobados() {
        Pago p = pago(EstadoPago.FALLIDO);

        adapter.reembolsar(reservaId);

        assertThat(p.getEstado()).isEqualTo(EstadoPago.FALLIDO);
        verify(pagos, never()).saveAndFlush(any(Pago.class));
    }
}
