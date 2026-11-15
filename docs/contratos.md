# ClimaFit — Contratos del equipo

Este documento es la referencia única de los contratos entre componentes (plan de trabajo, Fase 0 y
regla 2: "un cambio a un contrato se acuerda con los 4 y se actualiza primero acá").

La base es la [propuesta](propuesta.md): endpoints (sección 07), tablas (sección 08), reglas (4.2) y
estados no se repiten acá. Este documento fija lo que la propuesta no fijaba y **lo que cambió después
de la propuesta**.

## 1. Puertos, paquetes y perfiles

| Componente | Desde el IDE | Dentro de Docker | Publicado al host |
|---|---|---|---|
| api-gateway | 8080 | 8080 | 8080 |
| Keycloak | — | 8080 | 8090 |
| ms-actividades | 8081 | 8080 | no |
| ms-clases | 8082 | 8080 | no |
| ms-clima | 8083 | 8080 | no |
| ms-notificaciones | 8084 | 8080 | no |
| PostgreSQL | — | 5432 | 5432 |

- Dentro de Docker todos los servicios escuchan en **8080** (`SERVER_PORT` en `docker-compose.yml`).
  Las URLs entre servicios llegan por variables de entorno (`MS_ACTIVIDADES_URL`, `MS_CLIMA_URL`,
  `MS_NOTIFICACIONES_URL`).
- `groupId` de todos los proyectos: `ar.edu.utn.frc.climafit`. Paquete base: `ar.edu.utn.frc.climafit.<servicio>`.
- Perfiles de Spring: `docker` y `demo` en Docker Compose (`SPRING_PROFILES_ACTIVE=docker,demo`);
  `local` para correr desde el IDE; `stub` solo para pruebas con clientes falsos.

## 2. Keycloak

- Realm `climafit`. Roles de realm: `ADMIN`, `PROFESOR`, `ALUMNO`, `SERVICIO`.
- Cliente público `climafit-api` (grant de contraseña, lo usa Postman).
- Cliente confidencial `climafit-servicios` (client credentials, rol `SERVICIO`): token de servicio para
  las llamadas entre microservicios.
- Usuarios de prueba: `admin1`, `profe1`, `profe2`, `alumno1`, `alumno2`, `alumno3`. Los usuarios se
  guardan en las bases por su `sub`.

## 3. Errores

Formato único (lo arma `climafit-commons`): `status`, `codigo`, `mensaje`, `ruta`, `correlationId`, `fecha`.

| Código | Status | Quién lo emite |
|---|---|---|
| `DATOS_INVALIDOS` | 400 | commons (validación) y servicios |
| `NO_AUTENTICADO` | 401 | gateway y commons, con `WWW-Authenticate: Bearer` |
| `ACCESO_DENEGADO` | 403 | gateway y commons (RN05, RN08) |
| `NO_ENCONTRADO` | 404 | commons (`RecursoNoEncontradoException`) |
| `SERVICIO_NO_DISPONIBLE` | 503 | commons (cliente HTTP) y ms-clima |
| `CUPO_COMPLETO` | 409 | ms-clases, RN01 |
| `CLASE_NO_DISPONIBLE` | 409 | ms-clases, RN02 |
| `YA_INSCRIPTO` | 409 | ms-clases, RN02 |
| `SUPERPOSICION_ALUMNO` | 409 | ms-clases, RN03 |
| `SUPERPOSICION_PROFESOR` | 409 | ms-clases, RN04 |
| `CLASE_NO_MODIFICABLE` | 409 | ms-clases, RN06 |
| `EJERCICIO_INCOMPATIBLE` | 422 | ms-clases, RN07 |
| `CLASE_NO_EVALUABLE` | 409 | ms-clases, RN09 |
| `CLASE_NO_EN_RIESGO` | 409 | ms-clases, RN12 |

## 4. Uso de climafit-commons

Todos los microservicios usan la librería real (no hay copias provisorias):

- `new NegocioException(HttpStatus status, String codigo, String mensaje)` — en ese orden.
- `UsuarioActual`: `id()` (el `sub` del token), `roles()`, `tieneRol(..)`, `esAdmin()`, `esProfesor()`,
  `esAlumno()`, `esServicio()`.
- `CorrelationId.CABECERA` (`X-Correlation-Id`) y `CorrelationId.CLAVE_MDC`.
- Llamadas entre servicios: `ClienteServicioFactory.crear(urlBase)` devuelve un `RestClient` con token
  de servicio, timeouts de 2 s (conexión) y 5 s (lectura), hasta 2 reintentos en lecturas y propagación
  del correlation id. Si el otro servicio no responde: `ServicioNoDisponibleException` (503).
- Para que haya token de servicio, el `application.yml` del servicio registra el cliente OAuth2
  `climafit-servicios` (ver ms-clases).

## 5. Fechas

`LocalDateTime` en hora de Córdoba (`America/Argentina/Cordoba`), formato `2026-11-20T18:00:00`, sin zona.

## 6. Criterios climáticos (cambio posterior a la propuesta)

Acordado después de la entrega de la propuesta. Cambia el DER de `tipo_actividad` y de
`evaluacion_climatica`: **hay que avisar a la cátedra** (plan, cierre de la Fase 3).

### 6.1 Campos

| Dato | JSON | Columna | Dónde |
|---|---|---|---|
| mm de lluvia por hora que tolera la actividad | `lluviaMaxMmHora` | `tipo_actividad.lluvia_max_mm_hora` | ms-actividades (`POST/PUT /actividades` y respuestas), criterios de `POST /internal/evaluaciones` |
| mm de lluvia por hora pronosticados | `precipitacionMm` | `evaluacion_climatica.precipitacion_mm` | ms-clima (`GET /clima/pronostico`, `POST /clima/escenarios`, respuesta de `/internal/evaluaciones`) y evaluaciones de ms-clases |

- `lluviaMaxMmHora` es **obligatorio** en ms-actividades (≥ 0), así que siempre llega con valor.
- Si la clase dura varias horas se toma la hora con más lluvia (no se suman).

### 6.2 RN10 — Evaluación del pronóstico

La probabilidad de lluvia es un **filtro**, no un límite: la lluvia solo cuenta si la probabilidad
alcanza `probLluviaMax` (≥). Por debajo se considera que no va a llover y los mm se ignoran.

| Resultado | Condición |
|---|---|
| **NO_APTA** | la lluvia cuenta y `precipitacionMm > lluviaMaxMmHora`; o el viento supera `vientoMaxKmh`; o la temperatura queda por debajo de `tempMinC`; o hay tormenta eléctrica |
| **EN_RIESGO** | ningún límite se supera, pero: la lluvia cuenta y `precipitacionMm ≥ 80 %` de `lluviaMaxMmHora` (margen del 20 %; con límite 0 no hay zona de riesgo); o el viento queda a 10 km/h o menos del máximo; o la temperatura queda a 2 °C o menos de la mínima |
| **APTA** | cualquier otro caso |

Ejemplos con Yoga (80 %, 0,5 mm/h, 35 km/h, 5 °C):

| Pronóstico | Resultado |
|---|---|
| 85 % y 0 mm/h | APTA |
| 70 % y 3 mm/h | APTA (no llega al 80 %) |
| 90 % y 0,45 mm/h | EN_RIESGO |
| 90 % y 5 mm/h | NO_APTA |
| viento de 30 km/h | EN_RIESGO |

> **Pendiente (Persona 4):** `EvaluadorClimatico` de ms-clima todavía aplica la regla anterior: la
> probabilidad sola da NO_APTA y existe el riesgo "a 10 puntos de la probabilidad máxima". Falta
> ajustarlo a esta tabla. `docs/persona4/propuesta-cambios-dominio.md` queda reemplazado por esta sección.

### 6.3 Datos iniciales (`V2__datos_iniciales.sql` de ms-actividades)

| id | Actividad | Prob. lluvia (%) | Lluvia máx. (mm/h) | Viento máx. (km/h) | Temp. mín. (°C) |
|---|---|---|---|---|---|
| 1 | Yoga | 80 | 0,5 | 35 | 5 |
| 2 | Funcional | 80 | 2,0 | 35 | 5 |
| 3 | Running | 80 | 4,0 | 40 | 2 |
| 4 | Acrobacia aérea | 80 | 0,2 | 20 | 10 |
| 5 | Patinaje | 80 | 0,1 | 60 | 2 |

- Espacios: 1 Parque Sarmiento (-31.4290, -64.1800, al aire libre), 2 Parque de las Tejas (-31.4330,
  -64.1880, al aire libre), 3 Gimnasio Centro (techado).
- Ejercicios base: 1 Saludo al sol y 2 Postura del árbol (Yoga), 3 Burpees y 4 Sentadillas (Funcional),
  5 Trote continuo (Running), 6 Subida en tela (Acrobacia aérea), 7 Deslizamiento en línea recta (Patinaje).
- La colección de Postman usa estos ids. La comprobación CP4 usa un escenario de viento de 30 km/h
  (límite de Yoga: 35 km/h).

## 7. Contratos internos (entre servicios)

Todas estas llamadas llevan token de servicio (rol `SERVICIO`). Las rutas `/internal/**` no se exponen
en el gateway.

**ms-clases → ms-actividades**

- `GET /actividades/{id}`: `id`, `nombre`, `descripcion`, `probLluviaMax`, `lluviaMaxMmHora`,
  `vientoMaxKmh`, `tempMinC`, `activo`.
- `GET /espacios/{id}`: `id`, `nombre`, `direccion`, `latitud`, `longitud`, `alAireLibre`, `activo`.
- `POST /ejercicios/compatibilidad` (RN07): recibe `actividadId` y `ejercicioIds`; responde
  `compatible` e `incompatibles` (lista de ids).

**ms-clases → ms-clima: `POST /internal/evaluaciones`**

```json
{"latitud": -31.4290, "longitud": -64.1800,
 "criterios": {"probLluviaMax": 80, "vientoMaxKmh": 35, "tempMinC": 5, "lluviaMaxMmHora": 0.5},
 "ventanas": [{"inicio": "2026-11-20T18:00:00", "fin": "2026-11-20T19:00:00"}]}
```

Responde `{"evaluaciones": [...]}`, una por ventana y en el mismo orden, cada una con `inicio`,
`resultado`, `motivo`, `probLluvia`, `vientoKmh`, `temperaturaC`, `tormenta` y `precipitacionMm`.
Si el pronóstico no está disponible responde 503 `SERVICIO_NO_DISPONIBLE` (RN16).

**ms-clases → ms-notificaciones: `POST /internal/notificaciones`** (responde 202)

```json
{"eventoId": "CLASE-7-CAMBIO-15", "tipo": "REPROGRAMACION", "claseId": 7, "profesorId": "<sub>",
 "mensaje": "Tu clase de Yoga del 20/11 18:00 se reprogramó al 21/11 18:00: lluvia 5 mm/h.",
 "destinatarios": ["<sub alumno 1>", "<sub alumno 2>"]}
```

- `eventoId` tiene el formato `CLASE-{claseId}-CAMBIO-{cambioId}`. Un `eventoId` repetido no crea
  notificaciones nuevas (RN15).
- `tipo`: `SUSPENSION`, `REPROGRAMACION`, `SIN_FECHA`, `CANCELACION`, `ALERTA_RIESGO`.

## 8. Dueños de paquetes en ms-clases

- Persona 3: `clase`, `inscripcion`, `estado`, `historial`, `aviso`, `client.actividades`.
- Persona 4: `evaluacion`, `reprogramacion`, `tarea`, `client.clima`.
- La Persona 4 usa, sin modificarlos, `CambioEstadoService`, `AvisoService`, `ClaseRepository`,
  `InscripcionRepository` y `ActividadesClient` de la Persona 3.
