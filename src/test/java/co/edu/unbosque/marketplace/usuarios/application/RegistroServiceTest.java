package co.edu.unbosque.marketplace.usuarios.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.compartido.error.ExcepcionNegocio;
import co.edu.unbosque.marketplace.trazabilidad.AuditoriaPort;
import co.edu.unbosque.marketplace.trazabilidad.EventoAuditoria;
import co.edu.unbosque.marketplace.usuarios.domain.EstadoCuenta;
import co.edu.unbosque.marketplace.usuarios.domain.Rol;
import co.edu.unbosque.marketplace.usuarios.domain.Usuario;
import co.edu.unbosque.marketplace.usuarios.domain.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class RegistroServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditoriaPort auditoria = mock(AuditoriaPort.class);
    private final RegistroService service = new RegistroService(usuarios, encoder, auditoria);

    @BeforeEach
    void preparar() {
        when(encoder.encode("Clave#123")).thenReturn("hash-seguro");
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });
    }

    private static String codigo(Throwable t) {
        assertThat(t).isInstanceOf(ExcepcionNegocio.class);
        return ((ExcepcionNegocio) t).getCodigo();
    }

    @Test
    void registra_normalizando_los_datos_y_guardando_el_hash() {
        Usuario u = service.registrar("  Ana Pérez ", "  ANA@Correo.com ", "  ", "Clave#123", Rol.ANFITRION);

        assertThat(u.getNombre()).isEqualTo("Ana Pérez");
        assertThat(u.getCorreo()).isEqualTo("ana@correo.com");
        assertThat(u.getTelefono()).isNull();
        assertThat(u.getContrasenaHash()).isEqualTo("hash-seguro");
        assertThat(u.getRol()).isEqualTo(Rol.ANFITRION);
        assertThat(u.getEstadoCuenta()).isEqualTo(EstadoCuenta.ACTIVA);
        verify(auditoria).registrar(any(EventoAuditoria.class));
    }

    @Test
    void conserva_el_telefono_recortado() {
        Usuario u = service.registrar("Ana", "ana@correo.com", " 3001234567 ", "Clave#123", Rol.HUESPED);

        assertThat(u.getTelefono()).isEqualTo("3001234567");
    }

    @Test
    void no_permite_registrarse_como_administrador() {
        assertThat(codigo(catchThrowable(() ->
                service.registrar("Ana", "ana@correo.com", null, "Clave#123", Rol.ADMINISTRADOR))))
                .isEqualTo("ROL_NO_PERMITIDO");
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void rechaza_un_correo_ya_registrado() {
        when(usuarios.findByCorreoIgnoreCase("ana@correo.com"))
                .thenReturn(Optional.of(new Usuario("Otra", "ana@correo.com", null, "h", Rol.HUESPED)));

        assertThat(codigo(catchThrowable(() ->
                service.registrar("Ana", "ANA@correo.com", null, "Clave#123", Rol.HUESPED))))
                .isEqualTo("CORREO_YA_REGISTRADO");
        verify(auditoria).registrar(any(EventoAuditoria.class));
    }

    @Test
    void un_registro_simultaneo_con_el_mismo_correo_tambien_es_duplicado() {
        doThrow(new DataIntegrityViolationException("uq_usuario_correo")).when(usuarios).save(any(Usuario.class));

        assertThat(codigo(catchThrowable(() ->
                service.registrar("Ana", "ana@correo.com", null, "Clave#123", Rol.HUESPED))))
                .isEqualTo("CORREO_YA_REGISTRADO");
    }
}
