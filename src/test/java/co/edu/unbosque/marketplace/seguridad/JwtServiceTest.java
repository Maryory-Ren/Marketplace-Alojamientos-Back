package co.edu.unbosque.marketplace.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "secreto-de-pruebas-con-mas-de-32-bytes-0123456789";

    private JwtService servicio(String issuer, long minutos) {
        return new JwtService(new JwtProperties(SECRET, minutos, issuer));
    }

    @Test
    void emite_un_token_que_se_valida_con_id_y_rol() {
        JwtService jwt = servicio("marketplace", 60);
        UUID id = UUID.randomUUID();

        TokenEmitido emitido = jwt.emitir(id, "ANFITRION");
        Optional<UsuarioAutenticado> usuario = jwt.validar(emitido.token());

        assertThat(emitido.expiraEn()).isAfter(Instant.now());
        assertThat(usuario).isPresent();
        assertThat(usuario.get().id()).isEqualTo(id);
        assertThat(usuario.get().rol()).isEqualTo("ANFITRION");
    }

    @Test
    void rechaza_tokens_alterados_o_mal_formados() {
        JwtService jwt = servicio("marketplace", 60);
        String token = jwt.emitir(UUID.randomUUID(), "HUESPED").token();

        assertThat(jwt.validar(token + "x")).isEmpty();
        assertThat(jwt.validar("no-es-un-jwt")).isEmpty();
    }

    @Test
    void rechaza_tokens_vencidos() {
        JwtService jwt = servicio("marketplace", -5);

        assertThat(jwt.validar(jwt.emitir(UUID.randomUUID(), "HUESPED").token())).isEmpty();
    }

    @Test
    void rechaza_tokens_de_otro_emisor() {
        String token = servicio("otro-emisor", 60).emitir(UUID.randomUUID(), "HUESPED").token();

        assertThat(servicio("marketplace", 60).validar(token)).isEmpty();
    }

    @Test
    void rechaza_tokens_sin_rol() {
        SecretKey llave = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String sinRol = Jwts.builder().issuer("marketplace").subject(UUID.randomUUID().toString())
                .signWith(llave).compact();

        assertThat(servicio("marketplace", 60).validar(sinRol)).isEmpty();
    }

    @Test
    void rechaza_tokens_con_subject_que_no_es_uuid() {
        SecretKey llave = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().issuer("marketplace").subject("no-uuid").claim("rol", "HUESPED")
                .signWith(llave).compact();

        assertThat(servicio("marketplace", 60).validar(token)).isEmpty();
    }

    @Test
    void exige_un_secreto_de_al_menos_32_bytes() {
        Throwable t = catchThrowable(() -> new JwtService(new JwtProperties("corto", 60, "marketplace")));

        assertThat(t).isInstanceOf(IllegalStateException.class);
    }
}
