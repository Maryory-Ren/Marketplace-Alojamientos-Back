package co.edu.unbosque.marketplace.arquitectura;

import co.edu.unbosque.marketplace.MarketplaceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifica la estructura modular con Spring Modulith (ADR-001, ADR-013):
 * sin dependencias cíclicas entre módulos y sin acceso a paquetes internos de otro módulo.
 */
class ModularidadTest {

    private static final ApplicationModules MODULOS = ApplicationModules.of(MarketplaceApplication.class);

    @Test
    void los_modulos_respetan_sus_limites_y_no_tienen_ciclos() {
        MODULOS.verify();
    }
}
