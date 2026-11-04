-- Esquema de actividades_db (ver docs/diagramas/der-actividades.mmd).
-- Flyway ejecuta cada archivo V<n>__*.sql una sola vez y en orden. NUNCA se edita una migración
-- ya aplicada: si hay que cambiar algo, se agrega una nueva (V3__..., V4__...).

CREATE TABLE tipo_actividad (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(80)  NOT NULL UNIQUE,
    descripcion     VARCHAR(255),
    prob_lluvia_max INT          NOT NULL CHECK (prob_lluvia_max BETWEEN 0 AND 100),
    viento_max_kmh  INT          NOT NULL CHECK (viento_max_kmh >= 0),
    temp_min_c      INT          NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE espacio (
    id            BIGSERIAL PRIMARY KEY,
    nombre        VARCHAR(120)  NOT NULL UNIQUE,
    direccion     VARCHAR(255),
    latitud       NUMERIC(9, 6) NOT NULL,
    longitud      NUMERIC(9, 6) NOT NULL,
    al_aire_libre BOOLEAN       NOT NULL,
    activo        BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE ejercicio (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    descripcion    VARCHAR(500),
    nivel          VARCHAR(20)  NOT NULL CHECK (nivel IN ('INICIAL', 'INTERMEDIO', 'AVANZADO')),
    -- "sub" de Keycloak del profesor dueño; NULL = ejercicio base (RN08)
    propietario_id VARCHAR(64),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Relación N a N: qué ejercicios son compatibles con qué actividades (RN07)
CREATE TABLE ejercicio_actividad (
    ejercicio_id      BIGINT NOT NULL REFERENCES ejercicio (id),
    tipo_actividad_id BIGINT NOT NULL REFERENCES tipo_actividad (id),
    PRIMARY KEY (ejercicio_id, tipo_actividad_id)
);
