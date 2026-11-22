package co.edu.unbosque.marketplace.configuracion.application;
import co.edu.unbosque.marketplace.compartido.error.*;
import co.edu.unbosque.marketplace.configuracion.domain.*;
import co.edu.unbosque.marketplace.seguridad.UsuarioAutenticado;
import co.edu.unbosque.marketplace.trazabilidad.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TarifaServicioService {
    public static final String CLAVE="TARIFA_SERVICIO_PORCENTAJE";
    private final ConfiguracionRepository configuraciones; private final AuditoriaPort auditoria;
    public TarifaServicioService(ConfiguracionRepository configuraciones, AuditoriaPort auditoria) { this.configuraciones=configuraciones; this.auditoria=auditoria; }
    @Transactional(readOnly=true) public BigDecimal consultar() { return configuraciones.findById(CLAVE).orElseThrow(() ->
        new ExcepcionNegocio(TipoError.REGLA_NEGOCIO,"CONFIGURACION_AUSENTE","No está configurada la tarifa de servicio")).getValor(); }
    @Transactional public BigDecimal actualizar(BigDecimal valor, UsuarioAutenticado admin) {
        if (valor==null || valor.compareTo(BigDecimal.ZERO)<0 || valor.compareTo(new BigDecimal("100"))>0)
            throw new ExcepcionNegocio(TipoError.SOLICITUD_INVALIDA,"TARIFA_INVALIDA","La tarifa debe estar entre 0 y 100");
        var c=configuraciones.findById(CLAVE).orElseThrow(() -> new ExcepcionNegocio(TipoError.REGLA_NEGOCIO,"CONFIGURACION_AUSENTE","No está configurada la tarifa de servicio"));
        c.actualizar(valor); configuraciones.save(c);
        auditoria.registrar(new EventoAuditoria(admin.id(),"CONFIGURACION","TARIFA_SERVICIO_ACTUALIZADA","CONFIGURACION",CLAVE,EventoAuditoria.EXITO,valor.toPlainString()));
        return valor;
    }
}
