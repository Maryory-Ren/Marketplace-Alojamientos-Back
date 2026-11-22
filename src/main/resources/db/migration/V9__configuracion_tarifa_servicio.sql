-- ADR-012: tarifa de servicio centralizada y persistida (no hardcodeada en la aplicación).
CREATE TABLE configuracion_marketplace (
    clave varchar(80) PRIMARY KEY,
    valor numeric(8,4) NOT NULL CHECK (valor >= 0 AND valor <= 100),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now()
);
INSERT INTO configuracion_marketplace(clave, valor) VALUES ('TARIFA_SERVICIO_PORCENTAJE', 8.5000);
