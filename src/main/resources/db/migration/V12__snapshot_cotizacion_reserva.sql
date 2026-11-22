-- La tarifa y el total quedan congelados al crear la reserva; cambios futuros de configuración
-- no modifican el importe histórico de una operación ya iniciada.
ALTER TABLE reserva
    ADD COLUMN noches integer NOT NULL DEFAULT 0,
    ADD COLUMN precio_noche numeric(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN subtotal numeric(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN tarifa_limpieza numeric(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN tarifa_servicio numeric(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN porcentaje_tarifa_servicio numeric(8,4) NOT NULL DEFAULT 0,
    ADD COLUMN total numeric(12,2) NOT NULL DEFAULT 0;
