-- Datos iniciales con ids fijos (plan de trabajo, Fase 0, punto 10). La colección de Postman usa estos ids.

-- prob_lluvia_max: probabilidad (%) a partir de la cual se considera que va a llover (80 % = "muy probable").
-- lluvia_max_mm: milímetros por hora que la actividad tolera. Por debajo de prob_lluvia_max la lluvia no se
-- mira. Por encima: si llueve más de lluvia_max_mm la clase se suspende; si llueve más de la mitad de lo
-- tolerado (sin superarlo) queda en riesgo y decide el profesor (RN10, ajustada después de la propuesta).
INSERT INTO tipo_actividad (id, nombre, descripcion, prob_lluvia_max, lluvia_max_mm, viento_max_kmh, temp_min_c, activo) VALUES
    (1, 'Yoga',            'Yoga al aire libre sobre mat',                             80, 0.5, 35,  5, TRUE),
    (2, 'Funcional',       'Entrenamiento funcional con peso corporal',                80, 2.0, 35,  5, TRUE),
    (3, 'Running',         'Grupo de running con distintos ritmos',                    80, 4.0, 40,  2, TRUE),
    (4, 'Acrobacia aérea', 'Telas y aro colgados de árboles; muy sensible al viento',  80, 0.2, 20, 10, TRUE),
    (5, 'Patinaje',        'Patinaje sobre pista o paseo; no se dicta con lluvia',     80, 0.1, 60,  2, TRUE);

INSERT INTO espacio (id, nombre, direccion, latitud, longitud, al_aire_libre, activo) VALUES
    (1, 'Parque Sarmiento',    'Av. Poeta Lugones y Av. Hipólito Yrigoyen, Córdoba', -31.429000, -64.180000, TRUE,  TRUE),
    (2, 'Parque de las Tejas', 'Av. Hipólito Yrigoyen y Chacabuco, Córdoba',         -31.433000, -64.188000, TRUE,  TRUE),
    (3, 'Gimnasio Centro',     'Centro, Córdoba',                                     -31.416700, -64.183300, FALSE, TRUE);

INSERT INTO ejercicio (id, nombre, descripcion, nivel, propietario_id, activo) VALUES
    (1, 'Saludo al sol',     'Secuencia de posturas encadenadas con la respiración',  'INICIAL',    NULL, TRUE),
    (2, 'Postura del árbol', 'Equilibrio sobre una pierna con el otro pie en el muslo', 'INICIAL',  NULL, TRUE),
    (3, 'Burpees',           'Sentadilla, plancha, flexión y salto',                  'INTERMEDIO', NULL, TRUE),
    (4, 'Sentadillas',       'Flexión de rodillas con la espalda recta',              'INICIAL',    NULL, TRUE),
    (5, 'Trote continuo',    'Trote a ritmo conversacional',                          'INICIAL',    NULL, TRUE),
    (6, 'Subida en tela',    'Trepar la tela con llave de pie y bajar controlado',    'INTERMEDIO', NULL, TRUE),
    (7, 'Deslizamiento en línea recta', 'Impulso alternado y deslizamiento sobre un pie', 'INICIAL', NULL, TRUE);

INSERT INTO ejercicio_actividad (ejercicio_id, tipo_actividad_id) VALUES
    (1, 1),
    (2, 1),
    (3, 2),
    (4, 2),
    (5, 3),
    (6, 4),
    (7, 5);

-- Los ids se cargaron a mano: las secuencias siguen desde el siguiente número libre.
ALTER TABLE tipo_actividad ALTER COLUMN id RESTART WITH 6;
ALTER TABLE espacio ALTER COLUMN id RESTART WITH 4;
ALTER TABLE ejercicio ALTER COLUMN id RESTART WITH 8;
