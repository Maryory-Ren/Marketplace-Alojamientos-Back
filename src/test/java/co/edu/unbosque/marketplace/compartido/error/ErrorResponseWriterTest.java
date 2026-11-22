package co.edu.unbosque.marketplace.compartido.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class ErrorResponseWriterTest {

    @Test
    void escribe_el_error_como_json_con_el_estado_indicado() throws Exception {
        ErrorResponseWriter writer = new ErrorResponseWriter(JsonMapper.builder().build());
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        writer.escribir(respuesta, 401, "NO_AUTENTICADO", "Debes iniciar sesión");

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getContentType()).startsWith("application/json");
        assertThat(respuesta.getContentAsString()).contains("\"codigo\":\"NO_AUTENTICADO\"")
                .contains("Debes iniciar sesión");
    }
}
