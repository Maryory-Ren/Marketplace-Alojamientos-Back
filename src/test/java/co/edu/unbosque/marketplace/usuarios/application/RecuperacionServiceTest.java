package co.edu.unbosque.marketplace.usuarios.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.compartido.error.ExcepcionNegocio;
import co.edu.unbosque.marketplace.trazabilidad.AuditoriaPort;
import co.edu.unbosque.marketplace.trazabilidad.EventoAuditoria;
import co.edu.unbosque.marketplace.usuarios.domain.EstadoCuenta;
import co.edu.unbosque.marketplace.usuarios.domain.Rol;
import co.edu.unbosque.marketplace.usuarios.domain.TokenRecuperacion;
import co.edu.unbosque.marketplace.usuarios.domain.TokenRecuperacionRepository;
import co.edu.unbosque.marketplace.usuarios.domain.Usuario;
import co.edu.unbosque.marketplace.usuarios.domain.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class RecuperacionServiceTest {

    private static final String URL_BASE = "http://localhost:5173/restablecer";

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final TokenRecuperacionRepository tokens = mock(TokenRecuperacionRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditoriaPort auditoria = mock(AuditoriaPort.class);

    private RecuperacionService servicio(boolean exponerEnlace) {
        return new RecuperacionService(usuarios, tokens, encoder, auditoria, URL_BASE, 30, exponerEnlace);
    }

    private Usuario usuario(EstadoCuenta estado) {
        Usuario u = new Usuario("Ana", "ana@correo.com", null, "hash-viejo", Rol.HUESPED);
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(u, "estadoCuenta", estado);
        return u;
    }

    private static String codigo(Throwable t) {
        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        return ((ExcepcionNegocio) t).getCodigo();
    }

    @Test
    void con_correo_registrado_genera_el_enlace_y_lo_expone_en_dev() {
        Usuario u = usuario(EstadoCuenta.ACTIVA);
        when(usuarios.findByCorreoIgnoreCase("ana@correo.com")).thenReturn(Optional.of(u));

        RecuperacionResponse r = servicio(true).solicitar("  ANA@correo.com ");

        assertThat(r.enlaceSimulado()).startsWith(URL_BASE + "?token=");
        verify(tokens).invalidarPendientes(eq(u.getId()), any(Instant.class));
        verify(tokens).save(any(TokenRecuperacion.class));
        verify(auditoria).registrar(any(EventoAuditoria.class));
    }

    @Test
    void en_produccion_no_expone_el_enlace() {
        Usuario u = usuario(EstadoCuenta.ACTIVA);
        when(usuarios.findByCorreoIgnoreCase("ana@correo.com")).thenReturn(Optional.of(u));

        RecuperacionResponse r = servicio(false).solicitar("ana@correo.com");

        assertThat(r.enlaceSimulado()).isNull();
        assertThat(r.mensaje()).isNotBlank();
        verify(tokens).save(any(TokenRecuperacion.class));
    }

    @Test
    void responde_igual_si_el_correo_no_existe_o_la_cuenta_esta_desactivada() {
        RecuperacionResponse noExiste = servicio(true).solicitar("nadie@correo.com");

        when(usuarios.findByCorreoIgnoreCase("ana@correo.com"))
                .thenReturn(Optional.of(usuario(EstadoCuenta.DESACTIVADA)));
        RecuperacionResponse desactivada = servicio(true).solicitar("ana@correo.com");

        assertThat(noExiste.enlaceSimulado()).isNull();
        assertThat(desactivada.enlaceSimulado()).isNull();
        assertThat(desactivada.mensaje()).isEqualTo(noExiste.mensaje());
        verify(tokens, never()).save(any(TokenRecuperacion.class));
    }

    @Test
    void restablece_la_contrasena_con_un_enlace_vigente() {
        Usuario u = usuario(EstadoCuenta.ACTIVA);
        TokenRecuperacion t = new TokenRecuperacion(u.getId(), "hash", Instant.now().plusSeconds(600));
        UUID tokenId = UUID.randomUUID();
        ReflectionTestUtils.setField(t, "id", tokenId);
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(t));
        when(tokens.consumir(eq(tokenId), any(Instant.class))).thenReturn(1);
        when(usuarios.findById(u.getId())).thenReturn(Optional.of(u));
        when(encoder.encode("Nueva#123")).thenReturn("hash-nuevo");

        servicio(true).restablecer(" token-valido ", "Nueva#123");

        assertThat(u.getContrasenaHash()).isEqualTo("hash-nuevo");
        verify(usuarios).save(u);
        verify(auditoria).registrar(any(EventoAuditoria.class));
    }

    @Test
    void rechaza_enlaces_desconocidos_vencidos_o_sin_usuario() {
        RecuperacionService servicio = servicio(true);
        assertThat(codigo(catchThrowable(() -> servicio.restablecer("desconocido", "Nueva#123"))))
                .isEqualTo("ENLACE_INVALIDO");

        UUID usuarioId = UUID.randomUUID();
        TokenRecuperacion t = new TokenRecuperacion(usuarioId, "hash", Instant.now().minusSeconds(10));
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(t));
        when(tokens.consumir(any(UUID.class), any(Instant.class))).thenReturn(0);
        assertThat(codigo(catchThrowable(() -> servicio.restablecer("vencido", "Nueva#123"))))
                .isEqualTo("ENLACE_INVALIDO");

        when(tokens.consumir(any(UUID.class), any(Instant.class))).thenReturn(1);
        assertThat(codigo(catchThrowable(() -> servicio.restablecer("sin-usuario", "Nueva#123"))))
                .isEqualTo("ENLACE_INVALIDO");
        verify(usuarios, never()).save(any(Usuario.class));
    }
}
