-- Datos iniciales. Los ids coinciden con las variables del entorno de Postman
-- (actYoga=1, actFuncional=2, actRunning=3, espSarmiento=1, espTejas=2, espGimnasio=3, ej...=1..5).

-- Criterios climáticos: cada actividad tolera condiciones distintas (yoga es la más exigente).
INSERT INTO tipo_actividad (nombre, descripcion, prob_lluvia_max, viento_max_kmh, temp_min_c) VALUES
    ('Yoga',      'Yoga al aire libre',                  30, 25, 10),
    ('Funcional', 'Entrenamiento funcional en grupo',    50, 40,  5),
    ('Running',   'Running grupal',                      70, 45,  0);

INSERT INTO espacio (nombre, direccion, latitud, longitud, al_aire_libre) VALUES
    ('Parque Sarmiento',      'Av. Poeta Lugones y Av. Hipólito Yrigoyen', -31.429000, -64.180000, TRUE),
    ('Parque de las Tejas',   'Av. Concepción Arenal y Chacabuco',          -31.433000, -64.188000, TRUE),
    ('Gimnasio Nueva Córdoba', 'Obispo Trejo 900',                           -31.425000, -64.187000, FALSE);

INSERT INTO ejercicio (nombre, descripcion, nivel) VALUES
    ('Saludo al sol', 'Secuencia de posturas encadenadas con la respiración', 'INICIAL'),
    ('Árbol',         'Postura de equilibrio sobre una pierna',               'INICIAL'),
    ('Burpees',       'Sentadilla, plancha, flexión y salto',                 'INTERMEDIO'),
    ('Sentadillas',   'Sentadilla profunda con peso corporal',                'INICIAL'),
    ('Trote suave',   'Trote continuo a ritmo conversacional',                'INICIAL');

INSERT INTO ejercicio_actividad (ejercicio_id, tipo_actividad_id) VALUES
    (1, 1), (2, 1),          -- Yoga
    (3, 2), (4, 2),          -- Funcional
    (4, 3), (5, 3), (5, 2);  -- Running (y el trote también sirve en funcional)
