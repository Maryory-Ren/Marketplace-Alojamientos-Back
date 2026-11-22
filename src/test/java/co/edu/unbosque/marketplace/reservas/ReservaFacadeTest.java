package co.edu.unbosque.marketplace.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.publicaciones.domain.ReservaResumen;
import co.edu.unbosque.marketplace.reservas.domain.EstadoReserva;
import co.edu.unbosque.marketplace.reservas.domain.Reserva;
import co.edu.unbosque.marketplace.reservas.domain.ReservaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservaFacadeTest {

    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final ReservaFacade facade = new ReservaFacade(reservas);

    @Test
    void convierte_las_reservas_que_ocupan_fechas_en_resumenes() {
        UUID publicacionId = UUID.randomUUID();
        LocalDate inicio = LocalDate.now().plusDays(2);
        LocalDate fin = LocalDate.now().plusDays(6);
        Reserva r = new Reserva(UUID.randomUUID(), publicacionId, UUID.randomUUID(), inicio, inicio.plusDays(2), 3,
                EstadoReserva.CONFIRMADA, "k", "h", 2, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN);
        when(reservas.buscarSolapadas(eq(publicacionId), eq(inicio), eq(fin), anyCollection()))
                .thenReturn(List.of(r));

        List<ReservaResumen> resumenes = facade.reservasActivasEnRango(publicacionId, inicio, fin);

        assertThat(resumenes).hasSize(1);
        ReservaResumen resumen = resumenes.get(0);
        assertThat(resumen.id()).isEqualTo(r.getId());
        assertThat(resumen.fechaLlegada()).isEqualTo(inicio);
        assertThat(resumen.fechaSalida()).isEqualTo(inicio.plusDays(2));
        assertThat(resumen.numeroHuespedes()).isEqualTo(3);
        assertThat(resumen.estado()).isEqualTo("CONFIRMADA");
    }

    @Test
    void sin_reservas_devuelve_lista_vacia() {
        assertThat(facade.reservasActivasEnRango(UUID.randomUUID(), LocalDate.now(), LocalDate.now())).isEmpty();
    }
}
