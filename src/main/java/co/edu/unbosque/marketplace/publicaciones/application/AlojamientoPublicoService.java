package co.edu.unbosque.marketplace.publicaciones.application;

import co.edu.unbosque.marketplace.compartido.error.ExcepcionNegocio;
import co.edu.unbosque.marketplace.compartido.error.TipoError;
import co.edu.unbosque.marketplace.publicaciones.domain.*;
import co.edu.unbosque.marketplace.usuarios.domain.UsuarioRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlojamientoPublicoService {
	private final PublicacionRepository publicaciones;
	private final PeriodoDisponibleRepository periodos;
	private final ReservasActivasPort reservas;
	private final ServicioRepository servicios;
	private final PoliticaCancelacionRepository politicas;
	private final UsuarioRepository usuarios;

	public AlojamientoPublicoService(PublicacionRepository publicaciones, PeriodoDisponibleRepository periodos,
			ReservasActivasPort reservas, ServicioRepository servicios, PoliticaCancelacionRepository politicas,
			UsuarioRepository usuarios) {
		this.publicaciones = publicaciones;
		this.periodos = periodos;
		this.reservas = reservas;
		this.servicios = servicios;
		this.politicas = politicas;
		this.usuarios = usuarios;
	}

	@Transactional(readOnly = true)
	public ResultadoBusquedaAlojamientos buscar(String ciudad, TipoAlojamiento tipo, Integer huespedes,
			BigDecimal precioMin, BigDecimal precioMax, LocalDate llegada, LocalDate salida, List<UUID> servicios,
			int pagina, int tamano) {
		validarRango(llegada, salida);
		if (pagina < 0 || tamano < 1 || tamano > 50)
			throw invalido("PAGINACION_INVALIDA", "La paginación no es válida");
		if ((huespedes != null && huespedes < 1) || (precioMin != null && precioMin.signum() < 0)
				|| (precioMax != null && precioMax.signum() < 0))
			throw invalido("FILTROS_INVALIDOS", "Los huéspedes y precios deben ser valores positivos");
		if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0)
			throw invalido("RANGO_PRECIO_INVALIDO", "El precio mínimo no puede superar el máximo");
		List<Publicacion> filtradas = publicaciones.findByEstadoOrderByFechaActualizacionDesc(EstadoPublicacion.ACTIVA)
				.stream().filter(p -> ciudad == null || normalizar(p.getCiudad()).contains(normalizar(ciudad)))
				.filter(p -> tipo == null || p.getTipo() == tipo)
				.filter(p -> huespedes == null || p.getCapacidad() >= huespedes)
				.filter(p -> precioMin == null || p.getPrecioNoche().compareTo(precioMin) >= 0)
				.filter(p -> precioMax == null || p.getPrecioNoche().compareTo(precioMax) <= 0)
				.filter(p -> servicios == null || servicios.isEmpty() || p.getServiciosIds().containsAll(servicios))
				.filter(p -> llegada == null || disponible(p, llegada, salida)).toList();
		long total = filtradas.size();
		int desde = Math.min(pagina * tamano, filtradas.size());
		int hasta = Math.min(desde + tamano, filtradas.size());
		return new ResultadoBusquedaAlojamientos(filtradas.subList(desde, hasta).stream().map(this::resumen).toList(),
				pagina, tamano, total, (int) Math.ceil((double) total / tamano));
	}

	@Transactional(readOnly = true)
	public AlojamientoDetallePublico detalle(UUID id) {
		Publicacion p = publicaciones.findById(id).filter(x -> x.getEstado() == EstadoPublicacion.ACTIVA)
				.orElseThrow(() -> new ExcepcionNegocio(TipoError.NO_ENCONTRADO, "ALOJAMIENTO_NO_ENCONTRADO",
						"El alojamiento no existe"));
		return detalleVista(p);
	}

	@Transactional(readOnly = true)
	public List<DiaCalendario> calendario(UUID id, LocalDate desde, LocalDate hasta) {
		detalle(id);
		validarRangoCalendario(desde, hasta);

		if (ChronoUnit.DAYS.between(desde, hasta) >= 365)
			throw invalido("RANGO_EXCESIVO", "El calendario admite máximo 365 días");

		List<PeriodoDisponible> rangos = periodos.findByPublicacionIdOrderByFechaInicio(id);
		Set<LocalDate> ocupadas = new HashSet<>();

		for (var r : reservas.reservasActivasEnRango(id, desde, hasta)) {
			LocalDate d = r.fechaLlegada();
			while (d.isBefore(r.fechaSalida())) {
				ocupadas.add(d);
				d = d.plusDays(1);
			}
		}

		List<DiaCalendario> resultado = new ArrayList<>();

		for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
			LocalDate fechaEvaluada = d;
			boolean dentro = rangos.stream().anyMatch(
					r -> !fechaEvaluada.isBefore(r.getFechaInicio()) && !fechaEvaluada.isAfter(r.getFechaFin()));

			resultado.add(new DiaCalendario(d, dentro && !ocupadas.contains(d)));
		}

		return resultado;
	}

	private static void validarRango(LocalDate llegada, LocalDate salida) {
		if ((llegada == null) != (salida == null)
				|| (llegada != null && (!salida.isAfter(llegada) || llegada.isBefore(LocalDate.now()))))
			throw invalido("FECHAS_INVALIDAS", "Indica fechas válidas: llegada futura y salida posterior");
	}

	private static void validarRangoCalendario(LocalDate desde, LocalDate hasta) {
	    if (desde == null || hasta == null || hasta.isBefore(desde)) {
	        throw invalido(
	                "FECHAS_INVALIDAS",
	                "La fecha final no puede ser anterior a la inicial"
	        );
	    }
	}
	private boolean disponible(Publicacion p, LocalDate llegada, LocalDate salida) {
		boolean cubierto = periodos.findByPublicacionIdOrderByFechaInicio(p.getId()).stream()
				.anyMatch(r -> !llegada.isBefore(r.getFechaInicio()) && !salida.minusDays(1).isAfter(r.getFechaFin()));
		if (!cubierto)
			return false;
		return reservas.reservasActivasEnRango(p.getId(), llegada, salida.minusDays(1)).isEmpty();
	}

	private AlojamientoResumenPublico resumen(Publicacion p) {
		String portada = p.getImagenes().stream().min(Comparator.comparingInt(ImagenPublicacion::getOrden))
				.map(ImagenPublicacion::getRuta).orElse(null);
		return new AlojamientoResumenPublico(p.getId(), p.getTitulo(), p.getCiudad(), p.getTipo(), p.getCapacidad(),
				p.getPrecioNoche(), portada, p.getModoReserva());
	}

	private AlojamientoDetallePublico detalleVista(Publicacion p) {
		String nombreAnfitrion = usuarios.findById(p.getAnfitrionId()).map(u -> u.getNombre()).orElse("");
		List<AlojamientoDetallePublico.ServicioVista> serviciosVista = servicios.findAllById(p.getServiciosIds())
				.stream().map(s -> new AlojamientoDetallePublico.ServicioVista(s.getId(), s.getNombre())).toList();
		PoliticaCancelacion politica = politicas.findById(p.getPoliticaCancelacionId()).orElse(null);
		AlojamientoDetallePublico.PoliticaVista politicaVista = politica == null ? null
				: new AlojamientoDetallePublico.PoliticaVista(politica.getId(), politica.getNombre(),
						politica.getHorasAntesLlegada(), politica.getPorcentajeDevolucion());
		return new AlojamientoDetallePublico(p.getId(), p.getTitulo(), p.getDescripcion(), p.getCiudad(),
				p.getUbicacionTextual(), p.getTipo(), p.getCapacidad(), p.getPrecioNoche(), p.getTarifaLimpieza(),
				p.getReglas(), p.getModoReserva(), nombreAnfitrion, serviciosVista,
				p.getServiciosAdicionales().stream()
						.map(s -> new AlojamientoPublico.ServicioAdicional(s.getNombre(), s.getCosto())).toList(),
				politicaVista, p.getImagenes().stream().map(ImagenPublicacion::getRuta).toList());
	}

	private static String normalizar(String s) {
		return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
	}


	private static ExcepcionNegocio invalido(String codigo, String mensaje) {
		return new ExcepcionNegocio(TipoError.SOLICITUD_INVALIDA, codigo, mensaje);
	}

	public record DiaCalendario(LocalDate fecha, boolean disponible) {
	}
}
