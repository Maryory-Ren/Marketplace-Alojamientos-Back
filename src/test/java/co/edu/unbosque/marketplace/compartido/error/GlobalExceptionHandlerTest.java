package co.edu.unbosque.marketplace.compartido.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void una_excepcion_de_negocio_usa_el_estado_http_de_su_tipo() {
        ResponseEntity<ApiError> r = handler.negocio(
                new ExcepcionNegocio(TipoError.CONFLICTO, "PERIODO_CRUZADO", "Se cruza", List.of("detalle")));

        assertThat(r.getStatusCode().value()).isEqualTo(409);
        assertThat(r.getBody()).isNotNull();
        assertThat(r.getBody().codigo()).isEqualTo("PERIODO_CRUZADO");
        assertThat(r.getBody().mensaje()).isEqualTo("Se cruza");
        assertThat(r.getBody().detalles()).isEqualTo(List.of("detalle"));
    }

    @Test
    void cada_tipo_de_error_mapea_a_su_codigo_http() {
        assertThat(TipoError.SOLICITUD_INVALIDA.getEstadoHttp()).isEqualTo(400);
        assertThat(TipoError.NO_AUTENTICADO.getEstadoHttp()).isEqualTo(401);
        assertThat(TipoError.PROHIBIDO.getEstadoHttp()).isEqualTo(403);
        assertThat(TipoError.NO_ENCONTRADO.getEstadoHttp()).isEqualTo(404);
        assertThat(TipoError.CONFLICTO.getEstadoHttp()).isEqualTo(409);
        assertThat(TipoError.REGLA_NEGOCIO.getEstadoHttp()).isEqualTo(422);
        assertThat(TipoError.SERVICIO_NO_DISPONIBLE.getEstadoHttp()).isEqualTo(503);
    }

    @Test
    void un_error_inesperado_no_filtra_detalles() {
        ResponseEntity<ApiError> r = handler.inesperado(new IllegalStateException("secreto interno"));

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody()).isNotNull();
        assertThat(r.getBody().codigo()).isEqualTo("ERROR_INTERNO");
        assertThat(r.getBody().mensaje()).doesNotContain("secreto");
    }

    @Test
    void el_acceso_denegado_se_relanza_para_que_lo_atienda_spring_security() {
        AccessDeniedException ex = new AccessDeniedException("no");

        Throwable t = catchThrowable(() -> handler.accesoDenegado(ex));

        assertThat(t).isSameAs(ex);
    }

    @Test
    void los_errores_de_validacion_listan_los_campos_invalidos() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult resultado = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(resultado);
        when(resultado.getFieldErrors()).thenReturn(List.of(new FieldError("solicitud", "correo", "inválido")));

        ResponseEntity<Object> r = handler.handleMethodArgumentNotValid(ex, new HttpHeaders(),
                HttpStatus.BAD_REQUEST, null);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        ApiError api = (ApiError) r.getBody();
        assertThat(api).isNotNull();
        assertThat(api.codigo()).isEqualTo("SOLICITUD_INVALIDA");
        assertThat(api.detalles()).isEqualTo(List.of(new ApiError.DetalleCampo("correo", "inválido")));
    }

    @Test
    void los_errores_internos_de_spring_mvc_usan_codigos_propios() {
        assertThat(codigoPara(HttpStatus.BAD_REQUEST)).isEqualTo("SOLICITUD_INVALIDA");
        assertThat(codigoPara(HttpStatus.NOT_FOUND)).isEqualTo("RECURSO_NO_ENCONTRADO");
        assertThat(codigoPara(HttpStatus.METHOD_NOT_ALLOWED)).isEqualTo("METODO_NO_PERMITIDO");
        assertThat(codigoPara(HttpStatus.UNSUPPORTED_MEDIA_TYPE)).isEqualTo("TIPO_CONTENIDO_NO_SOPORTADO");
        assertThat(codigoPara(HttpStatus.NOT_ACCEPTABLE)).isEqualTo("ERROR_406");
    }

    private String codigoPara(HttpStatus estado) {
        ResponseEntity<Object> r = handler.handleExceptionInternal(new RuntimeException("x"), null,
                new HttpHeaders(), estado, null);
        assertThat(r.getStatusCode().value()).isEqualTo(estado.value());
        return ((ApiError) r.getBody()).codigo();
    }
}
