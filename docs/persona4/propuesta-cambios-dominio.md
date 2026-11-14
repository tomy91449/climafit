# Cambios de dominio (chat del 12/11): propuesta para acordar con el equipo

Estos cambios tocan contratos de la Fase 0. Según la regla 2 del plan, **se acuerdan con los 4 y se
actualizan primero en `docs/contratos.md`**. Hasta entonces, el código de la Persona 4 los implementa de
forma **compatible hacia atrás**: todo campo nuevo es opcional, así que la colección de Postman de la
propuesta sigue funcionando sin cambios.

## 1. Qué decidió la Persona 4 (a confirmar)

| Tema | Decisión |
|---|---|
| mm de lluvia vs. probabilidad | Se **complementan**: son dos criterios independientes y superar **cualquiera** de los dos es NO_APTA. |
| mm de lluvia por hora | Se toma el **máximo por hora** de las horas de la clase (no se suman). |
| Márgenes de EN_RIESGO | Los de la propuesta (10 puntos, 10 km/h, 2 °C) y, para los mm, **20 % del límite** (propuesta mía; se cambia con `climafit.clima.margen-lluvia-mm-proporcion`). Con límite 0 mm no hay zona de riesgo. |
| Límites "más estrictos" | Se bajan los máximos y se sube la temperatura mínima, más en las actividades más sensibles. **Los números de abajo son provisorios.** |
| Nombres de campos | Mnemotécnicos: `lluviaMaxMmHora` (criterio de la actividad) y `precipitacionMm` (valor del pronóstico). |

## 2. Criterios provisorios por actividad

| id | Actividad | Lluvia máx. (%) | Viento máx. (km/h) | Temp. mín. (°C) | Lluvia máx. (mm/h) |
|---|---|---|---|---|---|
| 1 | Yoga | 15 (antes 20) | 22 (antes 25) | 14 (antes 12) | 0,0 |
| 2 | Funcional | 35 (antes 40) | 30 (antes 35) | 6 (antes 5) | 1,0 |
| 3 | Running | 50 (antes 60) | 35 (antes 40) | 4 (antes 2) | 2,5 |
| 4 | Acrobacia aérea (nueva) | 10 | 15 | 15 | 0,0 |
| 5 | Patinaje (nueva) | 20 | 30 | 8 | 0,0 |

Restricción que cuida la colección de Postman: el límite de viento de Yoga tiene que quedar **entre 20 y 30 km/h**
(la comprobación CP4 usa viento de 20 km/h y espera EN_RIESGO). Con 22 se cumple.

## 3. Contratos que cambian (todos con campos opcionales)

| Dónde | Cambio | Dueño |
|---|---|---|
| `POST/PUT /actividades` y respuesta | campo `lluviaMaxMmHora` (decimal, ≥ 0, opcional) | Persona 2 |
| Tabla `tipo_actividad` (V1 de ms-actividades) | columna `lluvia_max_mm_hora DOUBLE PRECISION NULL` | Persona 2 |
| Datos iniciales (V2 de ms-actividades) | valores de la tabla de arriba y las actividades 4 y 5 | Persona 2 |
| `POST /internal/evaluaciones`, `criterios` | `lluviaMaxMmHora` opcional; si falta, no se evalúan los mm | Persona 4 |
| `POST /internal/evaluaciones`, respuesta | `precipitacionMm` en cada evaluación | Persona 4 |
| `GET /clima/pronostico`, cada hora | `precipitacionMm` | Persona 4 |
| `POST /clima/escenarios` | `precipitacionMm` opcional (por defecto 0) | Persona 4 |
| Tabla `evaluacion_climatica` (V2 de ms-clases) | columna `precipitacion_mm DOUBLE PRECISION` | Persona 4 |
| `GET /clases/{id}/evaluaciones` y `POST /clases/{id}/evaluaciones` | `precipitacionMm` | Persona 4 |
| `ActividadesClient.obtenerActividad` (DTO) | campo `lluviaMaxMmHora` | Persona 3 |

**Fase 3 (cierre del plan):** el DER de `evaluacion_climatica` y de `tipo_actividad` cambia respecto de la propuesta:
hay que actualizar el diagrama y avisar a la cátedra.
