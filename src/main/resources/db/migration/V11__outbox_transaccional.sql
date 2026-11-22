-- ADR-006: eventos persistidos en la misma transacción del cambio de negocio.
CREATE TABLE outbox_event (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    agregado varchar(80) NOT NULL,
    agregado_id varchar(80) NOT NULL,
    tipo_evento varchar(120) NOT NULL,
    payload jsonb NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos integer NOT NULL DEFAULT 0,
    creado_en timestamptz NOT NULL DEFAULT now(),
    publicado_en timestamptz,
    ultimo_error varchar(1000),
    CONSTRAINT ck_outbox_estado CHECK (estado IN ('PENDIENTE','PUBLICADO','ERROR'))
);
CREATE INDEX ix_outbox_pendientes ON outbox_event(estado,creado_en) WHERE estado='PENDIENTE';
