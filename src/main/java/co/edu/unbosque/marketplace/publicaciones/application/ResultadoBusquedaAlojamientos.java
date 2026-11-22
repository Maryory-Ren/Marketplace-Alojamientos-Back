package co.edu.unbosque.marketplace.publicaciones.application;
import java.util.List;
public record ResultadoBusquedaAlojamientos(List<AlojamientoResumenPublico> contenido,int pagina,
        int tamano,long total,int totalPaginas) {}
