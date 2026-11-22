ALTER TABLE reserva ADD COLUMN huella_solicitud varchar(64);
CREATE INDEX ix_reserva_estado_fechas ON reserva(estado, fecha_llegada, fecha_salida);

CREATE TABLE pago (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id uuid NOT NULL REFERENCES reserva(id),
    huesped_id uuid NOT NULL REFERENCES usuario(id),
    clave_idempotencia varchar(80) NOT NULL,
    huella_solicitud varchar(64) NOT NULL,
    estado varchar(20) NOT NULL,
    total numeric(12,2) NOT NULL,
    fecha_procesado timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_pago_idempotencia UNIQUE(huesped_id, clave_idempotencia)
);

CREATE TABLE resena (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id uuid NOT NULL UNIQUE REFERENCES reserva(id),
    publicacion_id uuid NOT NULL REFERENCES publicacion(id),
    huesped_id uuid NOT NULL REFERENCES usuario(id),
    calificacion integer NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario varchar(1000) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_resena_publicacion ON resena(publicacion_id,fecha_creacion DESC);
