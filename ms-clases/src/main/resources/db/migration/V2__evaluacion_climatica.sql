-- Persona 4 (plan de trabajo, tarea 4.4): tabla evaluacion_climatica del DER de la propuesta.
-- Se guardan TODAS las evaluaciones, incluidas las FALLIDA (RN16): por eso los valores del
-- pronóstico pueden ser nulos.
CREATE TABLE evaluacion_climatica (
    id                BIGSERIAL PRIMARY KEY,
    clase_id          BIGINT       NOT NULL REFERENCES clase (id),
    fecha_evaluacion  TIMESTAMP    NOT NULL,
    inicio_evaluado   TIMESTAMP    NOT NULL,
    resultado         VARCHAR(20)  NOT NULL,   -- APTA, EN_RIESGO, NO_APTA, FALLIDA
    motivo            VARCHAR(500),
    prob_lluvia       INT,
    viento_kmh        INT,
    temperatura_c     INT,
    tormenta          BOOLEAN,
    -- CAMBIO DE DOMINIO (chat del 12/11): máximo de mm de lluvia por hora de la ventana.
    -- No está en el DER de la propuesta: hay que avisar a la cátedra (Fase 3, cierre).
    precipitacion_mm  DOUBLE PRECISION
);

CREATE INDEX idx_evaluacion_clase_fecha ON evaluacion_climatica (clase_id, fecha_evaluacion DESC);
