package co.edu.unbosque.marketplace.usuarios.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.compartido.error.ExcepcionNegocio;
import co.edu.unbosque.marketplace.seguridad.JwtService;
import co.edu.unbosque.marketplace.seguridad.TokenEmitido;
import co.edu.unbosque.marketplace.trazabilidad.AuditoriaPort;
import co.edu.unbosque.marketplace.trazabilidad.EventoAuditoria;
import co.edu.unbosque.marketplace.usuarios.domain.EstadoCuenta;
import co.edu.unbosque.marketplace.usuarios.domain.Rol;
import co.edu.unbosque.marketplace.usuarios.domain.Usuario;
import co.edu.unbosque.marketplace.usuarios.domain.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AuditoriaPort auditoria = mock(AuditoriaPort.class);
    private final AuthService service = new AuthService(usuarios, encoder, jwt, auditoria);

    private Usuario usuario(EstadoCuenta estado) {
        Usuario u = new Usuario("Ana", "ana@correo.com", null, "hash-guardado", Rol.HUESPED);
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(u, "estadoCuenta", estado);
        return u;
    }

    private static String codigo(Throwable t) {
        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        return ((ExcepcionNegocio) t).getCodigo();
    }

    @Test
    void login_exitoso_normaliza_el_correo_y_emite_token() {
        Usuario u = usuario(EstadoCuenta.ACTIVA);
        when(usuarios.findByCorreoIgnoreCase("ana@correo.com")).thenReturn(Optional.of(u));
        when(encoder.matches("Clave#123", "hash-guardado")).thenReturn(true);
        TokenEmitido token = new TokenEmitido("jwt", Instant.now().plusSeconds(60));
        when(jwt.emitir(u.getId(), "HUESPED")).thenReturn(token);

        AuthService.ResultadoLogin resultado = service.login("  ANA@Correo.com ", "Clave#123");

        assertThat(resultado.token()).isEqualTo(token);
        assertThat(resultado.usuario()).isSameAs(u);
        verify(auditoria).registrar(any(EventoAuditoria.class));
    }

    @Test
    void un_correo_no_registrado_da_credenciales_invalidas_y_se_audita() {
        assertThat(codigo(catchThrowable(() -> service.login("nadie@correo.com", "x"))))
                .isEqualTo("CREDENCIALES_INVALIDAS");
        verify(auditoria).registrar(any(EventoAuditoria.class));
    }

    @Test
    void una_contrasena_incorrecta_da_credenciales_invalidas() {
        Usuario u = usuario(EstadoCuenta.ACTIVA);
        when(usuarios.findByCorreoIgnoreCase("ana@correo.com")).thenReturn(Optional.of(u));
        when(encoder.matches("mala", "hash-guardado")).thenReturn(false);

        assertThat(codigo(catchThrowable(() -> service.login("ana@correo.com", "mala"))))
                .isEqualTo("CREDENCIALES_INVALIDAS");
    }

    @Test
    void las_cuentas_no_activas_no_inician_sesion() {
        Usuario desactivada = usuario(EstadoCuenta.DESACTIVADA);
        when(usuarios.findByCorreoIgnoreCase("ana@correo.com")).thenReturn(Optional.of(desactivada));
        when(encoder.matches("Clave#123", "hash-guardado")).thenReturn(true);
        assertThat(codigo(catchThrowable(() -> service.login("ana@correo.com", "Clave#123"))))
                .isEqualTo("CUENTA_DESACTIVADA");

        Usuario restringida = usuario(EstadoCuenta.RESTRINGIDA);
        when(usuarios.findByCorreoIgnoreCase("ana@correo.com")).thenReturn(Optional.of(restringida));
        assertThat(codigo(catchThrowable(() -> service.login("ana@correo.com", "Clave#123"))))
                .isEqualTo("CUENTA_RESTRINGIDA");
    }

    @Test
    void el_perfil_devuelve_al_usuario_activo() {
        Usuario u = usuario(EstadoCuenta.ACTIVA);
        when(usuarios.findById(u.getId())).thenReturn(Optional.of(u));

        assertThat(service.perfil(u.getId())).isSameAs(u);
    }

    @Test
    void el_perfil_valida_existencia_y_estado() {
        assertThat(codigo(catchThrowable(() -> service.perfil(UUID.randomUUID()))))
                .isEqualTo("USUARIO_NO_ENCONTRADO");

        Usuario u = usuario(EstadoCuenta.RESTRINGIDA);
        when(usuarios.findById(u.getId())).thenReturn(Optional.of(u));
        assertThat(codigo(catchThrowable(() -> service.perfil(u.getId())))).isEqualTo("CUENTA_RESTRINGIDA");
    }
}
