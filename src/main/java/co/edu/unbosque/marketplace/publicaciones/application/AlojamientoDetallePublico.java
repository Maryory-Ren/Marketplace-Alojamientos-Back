package co.edu.unbosque.marketplace.publicaciones.application;
import co.edu.unbosque.marketplace.publicaciones.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public record AlojamientoDetallePublico(UUID id,String titulo,String descripcion,String ciudad,
        String ubicacionTextual,TipoAlojamiento tipo,int capacidad,BigDecimal precioNoche,
        BigDecimal tarifaLimpieza,String reglas,ModoReserva modoReserva,String anfitrionNombre,
        List<ServicioVista> servicios,List<AlojamientoPublico.ServicioAdicional> serviciosAdicionales,
        PoliticaVista politicaCancelacion,List<String> imagenes) {
    public record ServicioVista(UUID id,String nombre) {}
    public record PoliticaVista(UUID id,String nombre,int horasAntesLlegada,BigDecimal porcentajeDevolucion) {}
}
