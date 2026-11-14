-- Esquema de notificaciones_db (ver docs/diagramas/der-notificaciones.mmd).
-- Va en V2 y no en V1 porque V1 ya está subido al repo: si alguien ya levantó la base, editar V1
-- haría fallar la validación de Flyway (cambia el checksum).

-- Preferencias de contacto de cada usuario (F10, RN14). Un registro por usuario de Keycloak.
CREATE TABLE contacto (
    usuario_id      VARCHAR(64) PRIMARY KEY,           -- "sub" de Keycloak
    telefono        VARCHAR(16),                       -- formato E.164 (+5493511234567)
    acepta_whatsapp BOOLEAN     NOT NULL DEFAULT FALSE,
    actualizado     TIMESTAMP   NOT NULL
);

-- Cada cambio de clase que ms-clases pide avisar. El id lo genera ms-clases (RN15).
CREATE TABLE evento_notificacion (
    evento_id       VARCHAR(100) PRIMARY KEY,
    tipo            VARCHAR(20)  NOT NULL
        CHECK (tipo IN ('SUSPENSION', 'REPROGRAMACION', 'SIN_FECHA', 'CANCELACION', 'ALERTA_RIESGO')),
    clase_id        BIGINT       NOT NULL,             -- referencia lógica a ms-clases (no hay FK entre bases)
    profesor_id     VARCHAR(64)  NOT NULL,             -- dueño de la clase: permite el control de RN05 al consultar
    mensaje         VARCHAR(500) NOT NULL,
    fecha_recepcion TIMESTAMP    NOT NULL
);

CREATE INDEX ix_evento_notificacion_clase ON evento_notificacion (clase_id);

-- Un aviso a un destinatario. PENDIENTE -> ENVIADA | OMITIDA | FALLIDA.
CREATE TABLE notificacion (
    id                   BIGSERIAL PRIMARY KEY,
    evento_id            VARCHAR(100) NOT NULL REFERENCES evento_notificacion (evento_id),
    usuario_id           VARCHAR(64)  NOT NULL,
    telefono             VARCHAR(16),                  -- teléfono al que se envió (foto del momento del envío)
    estado               VARCHAR(20)  NOT NULL
        CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'OMITIDA', 'FALLIDA')),
    intentos             INT          NOT NULL DEFAULT 0,
    ultimo_error         VARCHAR(500),
    id_mensaje_proveedor VARCHAR(64),                  -- "sid" que devuelve Twilio
    fecha_envio          TIMESTAMP,
    -- RN15: un mismo evento se notifica UNA sola vez a cada destinatario, aunque el pedido se repita.
    CONSTRAINT uk_notificacion_evento_usuario UNIQUE (evento_id, usuario_id)
);

-- La tarea de recuperación busca las PENDIENTE.
CREATE INDEX ix_notificacion_estado ON notificacion (estado);
