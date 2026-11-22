# Marketplace-Alojamientos
Sistema Marketplace diseñado para la empresa LR, para la gestión de alojamientos temporales que permite conectar anfitriones y huéspedes mediante una plataforma para publicación, búsqueda, cotización y reserva de propiedades

## Extensiones del backend (noviembre de 2026)

Se completaron los flujos principales consumidos por el frontend:

- Consulta pública de alojamientos (`GET /api/alojamientos`), detalle y calendario; filtros combinables, búsqueda de ciudad sin distinguir tildes/mayúsculas y paginación.
- Catálogo público de servicios (`GET /api/servicios`).
- Moderación administrativa de publicaciones: bandeja, detalle, aprobación y rechazo con motivo.
- Cotización y creación de reservas con control de disponibilidad, restricciones PostgreSQL e idempotencia mediante `Idempotency-Key`.
- Pago simulado con estrategias `APROBAR`, `RECHAZAR` y `DEMORAR`, idempotencia y apertura temporal del circuito ante `CAER`.
- Gestión de reservas por anfitrión, cancelación del huésped y reseñas.
- Tarifa de servicio persistida en `configuracion_marketplace`.
- Outbox transaccional para eventos de publicación, reserva y pago.
- Pruebas unitarias de estrategias de pago y reglas ArchUnit para respetar las capas.

Las migraciones V9–V12 agregan configuración, idempotencia/pagos/reseñas, Outbox y snapshot de cotización. La tarifa y el total se congelan al crear la reserva para que los cambios futuros de configuración no alteren el importe histórico.

### Nota de verificación

Las pruebas unitarias (sin Docker) cubren los servicios de negocio, el manejo de errores, JWT y Outbox; `MarketplaceApplicationTests` levanta el contexto completo con PostgreSQL vía Testcontainers y por eso necesita Docker. Ejecutar `./mvnw verify` en un entorno con Maven y Docker antes de desplegar.

### Estructura modular (Spring Modulith)

`ModularidadTest` ejecuta `ApplicationModules.verify()` y falla si hay ciclos entre módulos o accesos a paquetes internos. Dependencias permitidas (siempre en un solo sentido):

`pagos → reservas, publicaciones` · `resenas → reservas, publicaciones, usuarios` · `reservas → publicaciones, configuracion` · `publicaciones → usuarios` · todos → `seguridad`, `trazabilidad`, `outbox`.

`publicaciones` necesita saber qué fechas están ocupadas, pero no puede depender de `reservas` (sería un ciclo). Por eso define el puerto `publicaciones.domain.ReservasActivasPort` y `reservas.ReservaFacade` lo implementa.

### Cobertura

JaCoCo genera `target/site/jacoco/jacoco.xml` en la fase `verify` y SonarCloud lo lee. `lombok.config` marca el código generado por Lombok para que JaCoCo no lo cuente. Las clases de arranque, los `*Request`/`*Dto` y `OpenApiConfig` se excluyen de la cobertura en `pom.xml`.

### Ejecución local

```bash
# Base de datos + backend

docker compose up --build -d

# Ver logs del backend
docker compose logs -f backend

# Detener los contenedores (conserva el volumen de PostgreSQL)
docker compose down
```

Para ejecutar pruebas fuera de Docker, usar `bash mvnw test` (Linux/macOS) o `mvnw.cmd test` (Windows), con Maven Wrapper descargable y Docker disponible para Testcontainers.

## Integración continua (GitHub Actions + SonarCloud)

- `.github/workflows/ci.yml` corre en cada push y pull request a `main`: `./mvnw verify` (pruebas unitarias, reglas de arquitectura y pruebas de integración con Testcontainers), cobertura con JaCoCo y análisis en SonarCloud.
- En `main`, además, publica la imagen Docker en GitHub Container Registry (`ghcr.io/<usuario>/<repo>`), como indica el ADR-016.
- Secreto requerido en GitHub (Settings → Secrets and variables → Actions): `SONAR_TOKEN`.
- Variables de SonarCloud en el `pom.xml`: `sonar.organization` y `sonar.projectKey`.
