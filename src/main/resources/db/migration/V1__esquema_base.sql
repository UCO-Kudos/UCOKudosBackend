-- V1 · Esquema de la línea base (ver docs/11-modelo-de-datos-y-api.md)
-- Incluye contenido con campos de réplica para no migrar después.

CREATE TABLE facultad (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre TEXT NOT NULL UNIQUE
);

CREATE TABLE programa_academico (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      TEXT NOT NULL,
    facultad_id UUID NOT NULL REFERENCES facultad(id),
    UNIQUE (facultad_id, nombre)
);

CREATE TABLE persona (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombres            TEXT NOT NULL,
    apellidos          TEXT NOT NULL,
    correo             TEXT NOT NULL UNIQUE,
    celular            TEXT NOT NULL UNIQUE,      -- formato E.164, p. ej. +573001234567
    hash_clave         TEXT NOT NULL,             -- BCrypt
    estado_cuenta      TEXT NOT NULL DEFAULT 'REGISTRADA'
        CHECK (estado_cuenta IN ('REGISTRADA','APROBADA','ACTIVA','SUSPENDIDA','RECHAZADA')),
    correo_verificado  BOOLEAN NOT NULL DEFAULT FALSE,
    celular_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    creada_en          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE asignacion_rol (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id  UUID NOT NULL REFERENCES persona(id),
    rol         TEXT NOT NULL CHECK (rol IN
        ('ADMIN','RECTOR','COORD_ACADEMICO','COORD_FACULTAD','COORD_PROGRAMA','DOCENTE','ESTUDIANTE')),
    facultad_id UUID REFERENCES facultad(id),
    programa_id UUID REFERENCES programa_academico(id),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    desde       TIMESTAMPTZ NOT NULL DEFAULT now(),
    hasta       TIMESTAMPTZ
);

-- RN-01 .. RN-04 garantizadas en la base de datos (defensa en profundidad)
CREATE UNIQUE INDEX ux_rol_activo_persona ON asignacion_rol (persona_id) WHERE activo;
CREATE UNIQUE INDEX ux_rector_activo      ON asignacion_rol ((rol)) WHERE activo AND rol = 'RECTOR';
CREATE UNIQUE INDEX ux_coord_acad_activo  ON asignacion_rol ((rol)) WHERE activo AND rol = 'COORD_ACADEMICO';
CREATE UNIQUE INDEX ux_coord_fac_activo   ON asignacion_rol (facultad_id) WHERE activo AND rol = 'COORD_FACULTAD';
CREATE UNIQUE INDEX ux_coord_prog_activo  ON asignacion_rol (programa_id) WHERE activo AND rol = 'COORD_PROGRAMA';

CREATE TABLE persona_programa (
    persona_id  UUID NOT NULL REFERENCES persona(id),
    programa_id UUID NOT NULL REFERENCES programa_academico(id),
    vinculo     TEXT NOT NULL CHECK (vinculo IN ('ESTUDIANTE','DOCENTE')),
    PRIMARY KEY (persona_id, programa_id, vinculo)
);

CREATE TABLE categoria (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre TEXT NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_categoria_nombre ON categoria (lower(nombre));

-- Kudos y réplicas en una sola tabla (RN-12, RN-13)
CREATE TABLE contenido (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo            TEXT NOT NULL CHECK (tipo IN ('KUDO','REPLICA')),
    autor_id        UUID NOT NULL REFERENCES persona(id),
    texto           TEXT NOT NULL,                       -- sin límite de longitud de negocio
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    estado          TEXT NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','VETADO')),
    destinatario_id UUID REFERENCES persona(id),
    categoria_id    UUID REFERENCES categoria(id),
    padre_id        UUID REFERENCES contenido(id),
    kudo_raiz_id    UUID REFERENCES contenido(id),
    CHECK (
        (tipo = 'KUDO' AND destinatario_id IS NOT NULL AND categoria_id IS NOT NULL
                       AND padre_id IS NULL AND kudo_raiz_id IS NULL)
        OR
        (tipo = 'REPLICA' AND padre_id IS NOT NULL AND kudo_raiz_id IS NOT NULL)
    ),
    CHECK (tipo <> 'KUDO' OR autor_id <> destinatario_id)  -- S-01
);
CREATE INDEX ix_kudo_destinatario ON contenido (destinatario_id, creado_en DESC, id DESC) WHERE tipo = 'KUDO';
CREATE INDEX ix_kudo_autor        ON contenido (autor_id, creado_en DESC, id DESC) WHERE tipo = 'KUDO';
CREATE INDEX ix_kudo_categoria    ON contenido (categoria_id, creado_en DESC, id DESC) WHERE tipo = 'KUDO';
CREATE INDEX ix_contenido_padre   ON contenido (padre_id, creado_en, id);
CREATE INDEX ix_contenido_raiz    ON contenido (kudo_raiz_id);

CREATE TABLE adjunto (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kudo_id              UUID NOT NULL REFERENCES contenido(id),
    nombre_original      TEXT NOT NULL,
    tipo_mime            TEXT NOT NULL,
    tamano_bytes         BIGINT NOT NULL CHECK (tamano_bytes > 0),
    sha256               TEXT NOT NULL,
    clave_almacenamiento TEXT NOT NULL,
    subido_por           UUID NOT NULL REFERENCES persona(id),
    subido_en            TIMESTAMPTZ NOT NULL DEFAULT now(),
    eliminado            BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_adjunto_kudo ON adjunto (kudo_id);

CREATE TABLE notificacion (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kudo_id         UUID NOT NULL REFERENCES contenido(id),
    destinatario_id UUID NOT NULL REFERENCES persona(id),
    canal           TEXT NOT NULL CHECK (canal IN ('CORREO','WHATSAPP','PUSH')),
    estado          TEXT NOT NULL CHECK (estado IN ('PENDIENTE','ENVIADA','FALLIDA','OMITIDA')),
    intentos        INT NOT NULL DEFAULT 0,
    ultimo_error    TEXT,
    creada_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    enviada_en      TIMESTAMPTZ
);
CREATE INDEX ix_notificacion_kudo ON notificacion (kudo_id);

CREATE TABLE auditoria (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ocurrido_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id      UUID REFERENCES persona(id),
    accion        TEXT NOT NULL,
    recurso_tipo  TEXT NOT NULL,
    recurso_id    UUID,
    resultado     TEXT NOT NULL,
    trace_id      TEXT NOT NULL,
    detalle       JSONB
);
CREATE INDEX ix_auditoria_trace ON auditoria (trace_id, ocurrido_en);

-- Auditoría de solo inserción: ni UPDATE ni DELETE
CREATE FUNCTION auditoria_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La tabla auditoria es de solo inserción';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditoria_inmutable
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_inmutable();
